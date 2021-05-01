package name.lmj0011.courierlocker.fragments

import android.app.Application
import android.location.Address
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProviders
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.CurrentStatusBubbleActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.*
import name.lmj0011.courierlocker.databinding.FragmentBubbleCreateTripBinding
import name.lmj0011.courierlocker.factories.TripViewModelFactory
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.launchIO
import name.lmj0011.courierlocker.helpers.withUIContext
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import org.kodein.di.instance

class CreateTripBubbleFragment : Fragment(R.layout.fragment_bubble_create_trip) {
    private lateinit var activity: CurrentStatusBubbleActivity
    private lateinit var binding: FragmentBubbleCreateTripBinding
    private lateinit var locationHelper: LocationHelper
    private var mTrip: Trip? = null
    lateinit var dataSource: TripDao
    lateinit var tripViewModel: TripViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataSource = CourierLockerDatabase.getInstance(requireActivity().application).tripDao
        val viewModelFactory = TripViewModelFactory(dataSource, requireActivity().application)
        this.tripViewModel = ViewModelProviders.of(this, viewModelFactory).get(TripViewModel::class.java)
        activity = requireActivity() as CurrentStatusBubbleActivity
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        refreshDataSources()
        setupBinding(view)
        setupObservers()
    }

    private fun setupBinding(view: View) {
        binding = FragmentBubbleCreateTripBinding.bind(view)
        binding.lifecycleOwner = this

        launchIO {
            val spinnerValues  = ArrayList<String>()
            val gigs = dataSource.getAllGigsThatAreVisible()

            gigs.forEach {
                spinnerValues.add(it.name)
            }

            withUIContext {
                binding.gigSpinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, spinnerValues).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }
        }

        /**
         * Setting up the AutoCompleteTextView
         */
        binding.autoCompleteTextView.setAdapter(
            AddressAutoSuggestAdapter(
                activity,
                android.R.layout.simple_dropdown_item_1line
            )
        )
        binding.autoCompleteTextView.threshold = 1

        binding.autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener {
            _,_,position,_ ->

            val address: Address? =
                (binding.autoCompleteTextView.adapter as AddressAutoSuggestAdapter).getItem(position)

            address?.let {
                binding.autoCompleteTextView.setText(it.getAddressLine(0))
                binding.autoCompleteTextView.tag =
                    Stop(it.getAddressLine(0), it.latitude, it.longitude)
            }
        }

        binding.autoCompleteTextView.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                (binding.autoCompleteTextView.adapter as AddressAutoSuggestAdapter)
                    .notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                locationHelper.performAddressAutoComplete(
                    s.toString(),
                    (binding.autoCompleteTextView.adapter as AddressAutoSuggestAdapter)
                )
            }
        })

        binding.autoCompleteTextView.post {
            launchIO {
                try {
                    // inserting the current location address into this AutoCompleteTextView
                    val address = locationHelper.getFromLocation(binding.root, locationHelper.lastLatitude.value!!, locationHelper.lastLongitude.value!!, 1)

                    withUIContext {
                        if (address.isNotEmpty()) {
                            binding.autoCompleteTextView.setText(address[0].getAddressLine(0))
                            val stop = Stop(address[0].getAddressLine(0), address[0].latitude, address[0].longitude)
                            binding.autoCompleteTextView.tag = stop
                        } else {
                            activity.showToastMessage("Unable to resolve an Address from current location")
                        }
                    }
                } catch (ex: Exception) {
                    ex.message?.let { msg -> activity.showToastMessage(msg) }
                }
            }

        }
        /**
         *
         */

        binding.payAmountEditText.post {
            binding.payAmountEditText.requestFocus()
            activity.showKeyBoard(binding.payAmountEditText)
        }

        binding.createTripButton.setOnClickListener {
            showProgressBar()
            activity.hideKeyBoard(binding.container)

            mTrip = Trip().apply {
                val stop = binding.autoCompleteTextView.tag as Stop

                pickupAddress = stop.address
                pickupAddressLatitude = stop.latitude
                pickupAddressLongitude = stop.longitude
                dropOffAddress = stop.address
                dropOffAddressLatitude = stop.latitude
                dropOffAddressLongitude = stop.longitude

                val pay = binding.payAmountEditText.text.toString()
               payAmount = if(!tripViewModel.validatePayAmount(pay)) {
                   "0"
               } else pay

                gigName = binding.gigSpinner.selectedItem.toString()
                stops = mutableListOf(stop)
            }

            tripViewModel.insertTrip(mTrip!!)
        }


        binding.navBackImageButton.setOnClickListener {
            navBack()
        }

    }

    private fun setupObservers() {

        tripViewModel.payAmountValidated.observe(viewLifecycleOwner,  {
            it?.let {
                if(!it){
                    activity.showToastMessage("No amount was entered.")
                }
            }
        })

        tripViewModel.errorMsg.observe(viewLifecycleOwner, {
            if (it.isNotBlank()) activity.showToastMessage(it)
        })

        tripViewModel.tripsPaged.observe(viewLifecycleOwner, { newPagedList ->
            hideProgressBar()
            if(mTrip != null) {
                navBack()
            }

        })

    }

    private fun navBack() {
        activity.hideKeyBoard(binding.container)

        val target = activity.supportFragmentManager
            .findFragmentByTag(CurrentStatusBubbleActivity.CREATE_TRIP_BUBBLE_FRAGMENT_TAG)
        activity.supportFragmentManager.commitNow {
            target?.let { frag ->
                setCustomAnimations(0, R.anim.slide_out_to_right)
                remove(frag)
                setCustomAnimations(R.anim.slide_in_from_left, 0)
                detach(activity.currentStatusFragment)
                attach(activity.currentStatusFragment)
                show(activity.currentStatusFragment)
            }
        }
    }

    private fun refreshDataSources() {
        val application = requireNotNull(requireContext().applicationContext as Application)
        tripViewModel = TripViewModel(CourierLockerDatabase.getInstance(application).tripDao, application)
    }

    private fun showProgressBar() {
        binding.progressBar.isIndeterminate = true
        binding.progressBar.isVisible = true
    }

    private fun hideProgressBar() {
        binding.progressBar.isVisible = false
    }
}