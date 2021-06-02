package name.lmj0011.courierlocker.fragments

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.zhuinden.livedatacombinetuplekt.combineTuple
import name.lmj0011.courierlocker.*
import name.lmj0011.courierlocker.database.*
import name.lmj0011.courierlocker.databinding.FragmentBubbleCurrentStatusBinding
import name.lmj0011.courierlocker.fragments.bottomsheets.BottomSheetCreateTripBubbleFragment
import name.lmj0011.courierlocker.fragments.bottomsheets.BottomSheetNavigableBuildingsFragment
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.helpers.interfaces.Addressable
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import org.kodein.di.instance

class CurrentStatusBubbleFragment : Fragment(R.layout.fragment_bubble_current_status) {
    private lateinit var activity: CurrentStatusBubbleActivity
    private lateinit var binding: FragmentBubbleCurrentStatusBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var bottomSheetCreateTripBubbleFragment: BottomSheetCreateTripBubbleFragment
    private lateinit var bottomSheetNavigableBuildingsFragment: BottomSheetNavigableBuildingsFragment
    lateinit var gateCodeViewModel: GateCodeViewModel
    lateinit var tripViewModel: TripViewModel
    lateinit var apartmentViewModel: ApartmentViewModel

    private lateinit var recentTripsListIterator: MutableListIterator<Trip>
    private lateinit var listOfRecentTrips: MutableList<Trip>
    private val mTrip = MutableLiveData<Trip?>()
    private var resetRecentTripsOrder: Boolean = true

    private lateinit var recentAddressablesListIterator: MutableListIterator<Addressable>
    private var listOfRecentGateCodes = mutableListOf<GateCode>()
    private var listOfAddressables = mutableListOf<Addressable>()
    private var listOfApartments = mutableListOf<Apartment>()
    private val mAddressable = MutableLiveData<Addressable?>()

    private val latitudeObserver: Observer<Double> = Observer { populateRecentAddressables() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val application = requireNotNull(requireContext().applicationContext as Application)
        gateCodeViewModel = GateCodeViewModel(CourierLockerDatabase.getInstance(application).gateCodeDao, application)
        tripViewModel = TripViewModel(CourierLockerDatabase.getInstance(application).tripDao, application)
        apartmentViewModel = ApartmentViewModel(CourierLockerDatabase.getInstance(application).apartmentDao, application)
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()
        bottomSheetCreateTripBubbleFragment = BottomSheetCreateTripBubbleFragment { refreshUI() }

        activity = requireActivity() as CurrentStatusBubbleActivity
        mTrip.postValue(null)
        mAddressable.postValue(null)

        setupBinding(view)
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun refreshUI() {
        launchIO {
            val total = tripViewModel.todayTotalMoney()
            val size = tripViewModel.todayCompletedTrips()
            withUIContext {
                binding.quickStatusTextView.text = "$total | $size"
                tripViewModel.tripsPaged.refresh()
            }
        }
    }

    private fun setupBinding(view: View) {
        binding = FragmentBubbleCurrentStatusBinding.bind(view)
        binding.lifecycleOwner = this

        binding.recentTripsContainer.setOnClickListener {
            val intent = Intent(context, DeepLinkActivity::class.java).apply {
                action = MainActivity.INTENT_EDIT_TRIP
                mTrip.value?.let { trip ->
                    putExtra("editTripId", trip.id.toInt())
                }
            }
            startActivity(intent)
        }

        binding.newTripButton.setOnClickListener { btnView ->
            synchronized(btnView) {
                btnView.isEnabled = false

                bottomSheetCreateTripBubbleFragment
                    .show(childFragmentManager, "BottomSheetCreateTripBubbleFragment")

                btnView.postDelayed({ btnView.isEnabled = true },
                    resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                )
            }
        }

        binding.addStopButton.setOnClickListener { btnView ->
            synchronized(btnView) {
                btnView.isEnabled = false
                showProgressBar()

                launchIO {
                    try {
                        val address = locationHelper.getFromLocation(null, locationHelper.lastLatitude.value!!, locationHelper.lastLongitude.value!!, 1)
                        if(address.isNotEmpty()) {
                            val stop = Stop(address[0].getAddressLine(0), address[0].latitude, address[0].longitude)

                            resetRecentTripsOrder = false

                            mTrip.value?.let { trip ->
                                trip.stops.add(stop)
                                tripViewModel.updateTrip(trip)
                            }
                        }
                    } catch (ex: Exception) {
                        ex.message?.let { msg -> activity.showToastMessage(msg) }
                    } finally {
                        withUIContext {
                            btnView.postDelayed({
                                btnView.isEnabled = true
                                hideProgressBar()
                            }, 500)
                        }
                    }
                }

            }
        }

        binding.nextTripImageButton.setOnClickListener {
            val trip = recentTripsListIterator.withIndex().next()
            mTrip.postValue(trip.value)

            // reset the iterator
            if (!recentTripsListIterator.hasNext()) {
                recentTripsListIterator = listOfRecentTrips.listIterator()
            }
        }

        binding.nearestAddressableContainer.setOnClickListener {
            locationHelper.lastLatitude.observe(viewLifecycleOwner, latitudeObserver)
        }

        binding.openMapButton.setOnClickListener {
            listOfApartments.find { apt ->
                when (val addressable = mAddressable.value) {
                    is GateCode -> apt.gateCodeId == addressable.id
                    is Apartment -> apt.id == addressable.id
                    else -> false
                }
            }?.let { apt ->
                val intent = Intent(context, DeepLinkActivity::class.java).apply {
                    action = MainActivity.INTENT_EDIT_APARTMENT_MAP
                    putExtra("aptId", apt.id)
                }
                startActivity(intent)
            }
        }

        binding.buildingsButton.setOnClickListener {
            bottomSheetNavigableBuildingsFragment
                .show(childFragmentManager, "BottomSheetNavigableBuildingsFragment")
        }

        binding.nextAddressableImageButton.setOnClickListener {
            /**
             * disable this observer for undisturbed navigating the recentGateCodesList,
             * the user can tap the current gatecode to start observing nearby gatecodes again
             */
            locationHelper.lastLatitude.removeObserver(latitudeObserver)

            if(::recentAddressablesListIterator.isInitialized
                && recentAddressablesListIterator.hasNext()) {
                val addr = recentAddressablesListIterator.withIndex().next()
                mAddressable.postValue(addr.value)
            } else {
                recentAddressablesListIterator = listOfAddressables.listIterator()
                recentAddressablesListIterator.next().let { addressable ->
                    mAddressable.postValue(addressable)
                }
            }
        }
    }

    private fun setupObservers() {
        tripViewModel.tripsPaged.observe(viewLifecycleOwner, { newPagedList ->
            listOfRecentTrips = newPagedList.take(3).toMutableList()
            recentTripsListIterator = listOfRecentTrips.listIterator()

            /**
             * Here we're determining whether to update the Trip currently in the view
             * or show the first Trip in [recentTripsListIterator], which would normally
             * mean we created a new Trip
             *
             * for example, we'll want to update the Trip currently in View if we performed an
             * "Add Stop" action from the Current Status Bubble
             */
            val trip = listOfRecentTrips.find { ele ->
                val targetTrip: Trip? = mTrip.value
                (targetTrip != null && ele.id == targetTrip.id)
            }

            if (trip is Trip && !resetRecentTripsOrder) {
                mTrip.postValue(trip)
                resetRecentTripsOrder = true
            } else {
                recentTripsListIterator.next().let { t ->
                    mTrip.postValue(t)
                }
            }
        })

        locationHelper.lastLatitude.observe(viewLifecycleOwner, latitudeObserver)

        combineTuple(gateCodeViewModel.gateCodes, apartmentViewModel.apartments)
            .observe(viewLifecycleOwner, { (gateCodes, apartments) ->
                if (gateCodes != null && apartments != null) {
                    listOfRecentGateCodes = gateCodes
                    listOfApartments = apartments
                    populateRecentAddressables()
                }
            })

        mTrip.observe(viewLifecycleOwner, { trip ->
            trip?.run { injectTripIntoView(this) }
        })

        mAddressable.observe(viewLifecycleOwner, { addressable ->
            addressable?.run { injectAddressableIntoView(this) }
        })
    }

    private fun showProgressBar() {
        binding.progressBar.isIndeterminate = true
        binding.progressBar.isVisible = true
    }

    private fun hideProgressBar() {
        binding.progressBar.isVisible = false
    }

    private fun populateRecentAddressables() {
        launchDefault {
            listOfAddressables = mutableListOf()
            listOfAddressables.addAll(listOfApartments.filter { apt -> apt.gateCodeId < 1 }) // apts w/o gatecode relation
            listOfAddressables.addAll(listOfRecentGateCodes)

            listOfAddressables = listOfAddressables.sortedBy { addressable ->
                locationHelper.calculateApproxDistanceBetweenMapPoints(
                    locationHelper.lastLatitude.value!!,
                    locationHelper.lastLongitude.value!!,
                    addressable.latitude,
                    addressable.longitude
                )
            }.take(5).toMutableList()

            recentAddressablesListIterator = listOfAddressables.listIterator()

            if(recentAddressablesListIterator.hasNext()) {
                recentAddressablesListIterator.next().let { addressable ->
                    mAddressable.postValue(addressable)
                }
            }
        }
    }

    private fun injectTripIntoView(trip: Trip) {
        binding.tripPickupAddressTextView.text = HtmlCompat
            .fromHtml(
                "<b>start:</b> ${Util.addressShortener(trip.pickupAddress)}",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

        binding.tripDropoffAddressTextView.text = HtmlCompat
            .fromHtml(
                "<b>end:</b> ${Util.addressShortener(trip.dropOffAddress)}",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

        if (trip.payAmount.isBlank()) {
            binding.tripPayTextView.visibility = TextView.GONE
        } else {
            binding.tripPayTextView.text = Util.numberFormatInstance.format(trip.payAmount.toDouble())
        }

        if (trip.gigName.isBlank()) {
            binding.tripGigTextView.visibility = TextView.GONE
        } else {
            binding.tripGigTextView.text = trip.gigName
        }
    }

    private fun injectAddressableIntoView(addressable: Addressable) {
        binding.addressableAddressTextView.text = Util.addressShortener(
            address = addressable.address,
            offset = 3
        )

        when(addressable) {
            is GateCode -> {
                if(addressable.codes.isNotEmpty()) {
                    binding.gateCodesTextView.visibility = View.VISIBLE
                    "gate codes: ${addressable.codes.take(4).joinToString(", ")}".also { binding.gateCodesTextView.text = it }
                } else {
                    "gate codes: n/a".also { binding.gateCodesTextView.text = it }
                }

                val apt = listOfApartments.find { apt ->
                    apt.gateCodeId == addressable.id
                }

                if (apt != null) {
                    binding.addressableNameTextView.text = apt.name
                    binding.openMapButton.visibility = View.VISIBLE

                    if (apt.buildings.size > 0) {
                        binding.buildingsButton.visibility = View.VISIBLE
                        bottomSheetNavigableBuildingsFragment = BottomSheetNavigableBuildingsFragment(apt)
                    } else binding.buildingsButton.visibility = View.GONE

                } else {
                    binding.addressableNameTextView.text = ""
                    binding.openMapButton.visibility = View.GONE
                    binding.buildingsButton.visibility = View.GONE
                }
            }
            is Apartment -> {
                binding.addressableNameTextView.text = addressable.name
                binding.gateCodesTextView.text = ""
                binding.openMapButton.visibility = View.VISIBLE
                "gate codes: n/a".also { binding.gateCodesTextView.text = it }

                if (addressable.buildings.size > 0) {
                    binding.buildingsButton.visibility = View.VISIBLE
                    bottomSheetNavigableBuildingsFragment = BottomSheetNavigableBuildingsFragment(addressable)
                } else binding.buildingsButton.visibility = View.GONE
            }
        }
    }
}