package name.lmj0011.courierlocker.fragments.bottomsheets

import android.app.Application
import android.app.Dialog
import android.location.Address
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.bottomsheet_fragment_bubble_create_trip.view.*
import kotlinx.coroutines.delay
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.CurrentStatusBubbleActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.*
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentBubbleCreateTripBinding
import name.lmj0011.courierlocker.factories.TripViewModelFactory
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import org.kodein.di.instance

class BottomSheetCreateTripBubbleFragment(private val dismissCallback: () -> Unit): BottomSheetDialogFragment() {
    private lateinit var activity: CurrentStatusBubbleActivity
    private lateinit var binding: BottomsheetFragmentBubbleCreateTripBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var mTrip: Trip? = null
    lateinit var dataSource: TripDao
    lateinit var tripViewModel: TripViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_fragment_bubble_create_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataSource = CourierLockerDatabase.getInstance(requireActivity().application).tripDao
        val viewModelFactory = TripViewModelFactory(dataSource, requireActivity().application)
        this.tripViewModel = ViewModelProvider(this, viewModelFactory).get(TripViewModel::class.java)
        activity = requireActivity() as CurrentStatusBubbleActivity
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        refreshDataSources()
        setupBinding(view)
        setupObservers()
    }

    override fun onResume() {
        fetchCurrentAddressForAutoCompleteTextView()
        super.onResume()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.peekHeight = 0

        return bottomSheetDialog
    }

    private fun setupBinding(view: View) {
        binding = BottomsheetFragmentBubbleCreateTripBinding.bind(view)
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

        binding.autoCompleteTextView.post { fetchCurrentAddressForAutoCompleteTextView() }

        binding.payAmountEditText.post {
            launchUI {
                binding.payAmountEditText.requestFocus()
                withIOContext{ delay(200) }
                activity.showKeyBoard(binding.payAmountEditText)
            }

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
                dismissDialog()
            }

        })

        bottomSheetDialog.behavior.addBottomSheetCallback(object :BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // only need this BottomSheet in 2 states
                if (newState != BottomSheetBehavior.STATE_EXPANDED || newState != BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

    }

    private fun fetchCurrentAddressForAutoCompleteTextView() {
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

    private fun dismissDialog() {
        activity.hideKeyBoard(binding.container)
        dismissCallback()
        bottomSheetDialog.dismiss()
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