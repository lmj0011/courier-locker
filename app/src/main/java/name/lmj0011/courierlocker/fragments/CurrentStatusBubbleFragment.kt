package name.lmj0011.courierlocker.fragments

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import name.lmj0011.courierlocker.CurrentStatusBubbleActivity
import name.lmj0011.courierlocker.DeepLinkActivity
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.*
import name.lmj0011.courierlocker.databinding.FragmentBubbleCurrentStatusBinding
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.Util
import name.lmj0011.courierlocker.helpers.launchIO
import name.lmj0011.courierlocker.helpers.withUIContext
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.viewmodels.TripViewModel

class CurrentStatusBubbleFragment : Fragment(R.layout.fragment_bubble_current_status) {
    private lateinit var activity: CurrentStatusBubbleActivity
    private lateinit var binding: FragmentBubbleCurrentStatusBinding
    lateinit var gateCodeViewModel: GateCodeViewModel
    lateinit var tripViewModel: TripViewModel
    lateinit var apartmentViewModel: ApartmentViewModel

    private lateinit var recentTripsListIterator: MutableListIterator<Trip>
    private lateinit var listOfRecentTrips: MutableList<Trip>
    private val mTrip = MutableLiveData<Trip?>()

    private lateinit var recentGateCodesListIterator: MutableListIterator<GateCode>
    private lateinit var listOfRecentGateCodes: MutableList<GateCode>
    private val mGateCode = MutableLiveData<GateCode?>()
    private val latitudeObserver: Observer<Double> = Observer {
        if (::listOfRecentGateCodes.isInitialized.not() || listOfRecentGateCodes.isNullOrEmpty()) {
            return@Observer
        }

        listOfRecentGateCodes = listOfRecentGateCodes.sortedBy { gc ->
            LocationHelper.calculateApproxDistanceBetweenMapPoints(
                LocationHelper.lastLatitude.value!!,
                LocationHelper.lastLongitude.value!!,
                gc.latitude,
                gc.longitude
            )
        }.take(3).toMutableList()

        recentGateCodesListIterator = listOfRecentGateCodes.listIterator()

        recentGateCodesListIterator.next().let { gc ->
            mGateCode.postValue(gc)
        }
    }

    private var listOfApartments = mutableListOf<Apartment>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val application = requireNotNull(requireContext().applicationContext as Application)
        gateCodeViewModel = GateCodeViewModel(CourierLockerDatabase.getInstance(application).gateCodeDao, application)
        tripViewModel = TripViewModel(CourierLockerDatabase.getInstance(application).tripDao, application)
        apartmentViewModel = ApartmentViewModel(CourierLockerDatabase.getInstance(application).apartmentDao, application)

        activity = requireActivity() as CurrentStatusBubbleActivity
        mTrip.postValue(null)
        mGateCode.postValue(null)

        setupBinding(view)
        setupObservers()

        launchIO {
            val total = tripViewModel.todayTotalMoney()
            val size = tripViewModel.todayCompletedTrips()
            withUIContext {
                binding.quickStatusTextView.text = "$total | $size"
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

                activity.supportFragmentManager.commitNow {
                    setCustomAnimations(0, R.anim.slide_out_to_left)
                    hide(activity.currentStatusFragment)
                    setCustomAnimations(R.anim.slide_in_from_right, 0)
                    add(R.id.container, CreateTripBubbleFragment(), CurrentStatusBubbleActivity.CREATE_TRIP_BUBBLE_FRAGMENT_TAG)
                }

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
                        val address = LocationHelper.getFromLocation(null, LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!, 1)
                        if(address.isNotEmpty()) {
                            val stop = Stop(address[0].getAddressLine(0), address[0].latitude, address[0].longitude)

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

        binding.nearestGateCodesContainer.setOnClickListener {
            LocationHelper.lastLatitude.observe(viewLifecycleOwner, latitudeObserver)
        }

        binding.openMapButton.setOnClickListener {
            listOfApartments.find { apt ->
                (apt.latitude == mGateCode.value?.latitude)
                        && (apt.longitude == mGateCode.value?.longitude)
            }?.let { apt ->
                val intent = Intent(context, DeepLinkActivity::class.java).apply {
                    action = MainActivity.INTENT_EDIT_APARTMENT_MAP
                    putExtra("aptId", apt.id)
                }
                startActivity(intent)
            }
        }

        binding.nextGateCodeImageButton.setOnClickListener {
            /**
             * disable this observer for undisturbed navigating, user can tap the current gatecode
             * to start observing nearby gatecodes again
             */
            LocationHelper.lastLatitude.removeObserver(latitudeObserver)

            if(::recentGateCodesListIterator.isInitialized
                && recentGateCodesListIterator.hasNext()) {
                val gc = recentGateCodesListIterator.withIndex().next()
                mGateCode.postValue(gc.value)
            } else {
                recentGateCodesListIterator = listOfRecentGateCodes.listIterator()
                recentGateCodesListIterator.next().let { gc ->
                    mGateCode.postValue(gc)
                }
            }
        }
    }

    private fun setupObservers() {
        tripViewModel.tripsPaged.observe(viewLifecycleOwner, { newPagedList ->
            listOfRecentTrips = newPagedList.take(3).toMutableList()

            recentTripsListIterator = listOfRecentTrips.listIterator()

            val trip = mTrip.value
            if (trip != null) {
                newPagedList.find { ele ->
                    if(ele == null) return@find false
                    ele.id == trip.id
                }?.let { t ->
                    mTrip.postValue(t)
                }
            } else {
                recentTripsListIterator.next().let { t ->
                    mTrip.postValue(t)
                }
            }
        })

        gateCodeViewModel.gateCodes.observe(viewLifecycleOwner, { list ->
            listOfRecentGateCodes = list
        })

        LocationHelper.lastLatitude.observe(viewLifecycleOwner, latitudeObserver)

        apartmentViewModel.apartments.observe(viewLifecycleOwner, { list ->
            listOfApartments = list
        })

        mTrip.observe(viewLifecycleOwner, { trip ->
            trip?.run { injectTripIntoView(this) }
        })

        mGateCode.observe(viewLifecycleOwner, { gc ->
            gc?.run { injectGateCodeIntoView(this) }
        })
    }

    private fun showProgressBar() {
        binding.progressBar.isIndeterminate = true
        binding.progressBar.isVisible = true
    }

    private fun hideProgressBar() {
        binding.progressBar.isVisible = false
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

    private fun injectGateCodeIntoView(gateCode: GateCode) {
        if(gateCode.codes.isNotEmpty()) {
            binding.gateCodeTextView.text = gateCode.codes[0]
        }

        binding.gateCodesAddressTextView.text = gateCode.address

        if(gateCode.codes.size > 1) {
            binding.otherGateCodesTextView.text = gateCode.codes.joinToString(", ")
        } else {
            binding.otherGateCodesTextView.text = ""
        }

        binding.nearestGateCodesContainer.visibility = View.VISIBLE
        binding.nearestGateCodesControlsContainer.visibility = View.VISIBLE
    }
}