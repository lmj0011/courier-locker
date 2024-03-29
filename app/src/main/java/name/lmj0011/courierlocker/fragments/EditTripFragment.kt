package name.lmj0011.courierlocker.fragments

import android.location.Address
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.Stop
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.databinding.FragmentEditTripBinding
import name.lmj0011.courierlocker.factories.TripViewModelFactory
import name.lmj0011.courierlocker.fragments.dialogs.DeleteTripDialogFragment
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.Util
import name.lmj0011.courierlocker.helpers.launchIO
import name.lmj0011.courierlocker.helpers.launchUI
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import org.kodein.di.instance

/**
 * A simple [Fragment] subclass.
 *
 */
class EditTripFragment : Fragment(), DeleteTripDialogFragment.NoticeDialogListener {
    private lateinit var binding: FragmentEditTripBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var tripViewModel: TripViewModel
    private lateinit var locationHelper: LocationHelper
    private var fragmentJob = Job()
    private var addressAutoCompleteJob: Job? = null
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)
    private var trip: Trip? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_trip, container, false)

        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).tripDao
        val viewModelFactory = TripViewModelFactory(dataSource, application)
        val args = EditTripFragmentArgs.fromBundle(requireArguments())
        this.tripViewModel = ViewModelProvider(this, viewModelFactory).get(TripViewModel::class.java)
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        binding.tripViewModel = this.tripViewModel

        this.tripViewModel.trip.observe(viewLifecycleOwner, Observer {
            this.trip  = it

            this.injectTripIntoView(it)
        })

        this.tripViewModel.setTrip(args.tripId)

        binding.editTripSaveButton.setOnClickListener(this::saveButtonOnClickListener)
        binding.editTripAddStopButton.setOnClickListener{
            addStop()
        }

        binding.editTripDeleteButton.setOnClickListener {
            val dialog = DeleteTripDialogFragment()
            dialog.show(childFragmentManager, "DeleteTripDialogFragment")

        }


        tripViewModel.payAmountValidated.observe(viewLifecycleOwner, {
            it?.let {
                if(!it){
                    mainActivity.showToastMessage("No amount was entered.")
                }
            }
        })

        tripViewModel.trips.observe(viewLifecycleOwner, {
            if (!binding.editTripSaveButton.isEnabled || !binding.editTripDeleteButton.isEnabled) {
                hideProgressBar()
                findNavController().navigateUp()
            }
        })

        tripViewModel.errorMsg.observe(viewLifecycleOwner, {
            if (it.isNotBlank()) mainActivity.showToastMessage(it, Toast.LENGTH_LONG)
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.supportActionBar?.subtitle = null
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob.cancel()
        addressAutoCompleteJob?.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_trips, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_edit_gig_labels -> {
                this.findNavController().navigate(R.id.action_editTripFragment_to_gigLabelsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // User touched the dialog's positive button
        showProgressBar()
        binding.editTripDeleteButton.isEnabled = false
        tripViewModel.deleteTrip(this.trip!!.id)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
    }

    private fun injectTripIntoView(trip: Trip?) {
        trip?.let {

            binding.tripDateTextView.text = Util.getTripDate(it)

            val layout: LinearLayout = binding.editTripFragmentLinearLayout
            layout.removeAllViewsInLayout()
            trip.stops.forEach { stop ->
                addStop(stop)
            }

            binding.payAmountEditText.setText(it.payAmount)

            uiScope.launch {
                val spinnerValues  = ArrayList<String>()

                withContext(Dispatchers.IO) {
                    val gigNames = CourierLockerDatabase.getInstance(mainActivity.application).tripDao.getAllGigsThatAreVisible()
                        .map { g -> g.name }
                        .toMutableList()

                    /**
                     * gig label may be named something that is now hidden or deleted
                     * in that case, we'll add it dynamically to the spinner
                    **/
                    if(!gigNames.any { name -> name == it.gigName }) spinnerValues.add(it.gigName)

                    gigNames.forEach { name ->
                        spinnerValues.add(name)
                    }
                }

                binding.gigSpinner.adapter = ArrayAdapter<String>(mainActivity, android.R.layout.simple_spinner_item, spinnerValues).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                binding.gigSpinner.setSelection(spinnerValues.indexOf(it.gigName))
            }
        }

    }

    private fun addStop(stop: Stop? = null) {
        val containerLayout: LinearLayout = binding.editTripFragmentLinearLayout
        val horizontalLinearLayout = LinearLayout(context)
        val addressTextView = AutoCompleteTextView(context)
        val cancelImageButton = ImageButton(context)

        horizontalLinearLayout.id = View.generateViewId()
        horizontalLinearLayout.orientation = LinearLayout.HORIZONTAL
        horizontalLinearLayout.weightSum = 4f

        cancelImageButton.id = View.generateViewId()
        cancelImageButton.setImageResource(R.drawable.ic_baseline_cancel_24)
        cancelImageButton.setBackgroundColor(requireContext().getColor(android.R.color.transparent))
        cancelImageButton.setOnClickListener {
            containerLayout.removeView(horizontalLinearLayout)
        }
        cancelImageButton.layoutParams =
            LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)

        val adapter = AddressAutoSuggestAdapter(
            mainActivity, // Context
            android.R.layout.simple_dropdown_item_1line
        )

        addressTextView.id = View.generateViewId()
        addressTextView.layoutParams =
            LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT, 3.5f)
        addressTextView.hint = "enter address"
        addressTextView.setAdapter(adapter)
        addressTextView.threshold = 1

        addressTextView.onItemClickListener = AdapterView.OnItemClickListener{
                parent,_view,position,id ->
            val address: Address? = adapter.getItem(position)

            address?.let {
                addressTextView.setText(it.getAddressLine(0))
                val stop = Stop(it.getAddressLine(0), it.latitude, it.longitude)
                horizontalLinearLayout.tag = stop
            }

        }

        addressTextView.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                locationHelper.performAddressAutoComplete(s.toString(), adapter)
            }
        })

        horizontalLinearLayout.addView(addressTextView)
        horizontalLinearLayout.addView(cancelImageButton)
        containerLayout.addView(horizontalLinearLayout)

        binding.editTripFragmentScrollView.post {
            launchUI {
                // inserting the current location address into this AutoCompleteTextView
                val address = locationHelper.getFromLocation(binding.root, locationHelper.lastLatitude.value!!, locationHelper.lastLongitude.value!!, 1)

                when{
                    stop != null -> {
                        addressTextView.setText(stop.address)
                        horizontalLinearLayout.tag = stop
                    }
                    address.isNotEmpty() -> {
                        val newStop = Stop(address[0].getAddressLine(0), address[0].latitude, address[0].longitude)
                        addressTextView.setText(newStop.address)
                        horizontalLinearLayout.tag = newStop
                    }
                    else -> {
                        containerLayout.removeView(view)
                        mainActivity.showToastMessage("Unable to resolve an Address from current location")
                    }
                }

                val scroll = binding.editTripFragmentScrollView
                scroll.scrollTo(0, scroll.height)
            }
        }
    }

    private fun showProgressBar() {
        binding.progressBar.isIndeterminate = true
        binding.progressBar.isVisible = true
    }

    private fun hideProgressBar() {
        binding.progressBar.isVisible = false
    }

    @Suppress("UNUSED_PARAMETER")
    private fun saveButtonOnClickListener(v: View) {
        showProgressBar()
        binding.editTripSaveButton.isEnabled = false

        var payAmount = binding.payAmountEditText.text.toString()
        val gig = binding.gigSpinner.selectedItem.toString()
        val layout: LinearLayout = binding.editTripFragmentLinearLayout

        val arrayOfStops = layout.children.map {
            it.tag as Stop
        }.toList().toTypedArray()

        when{
            arrayOfStops.isEmpty() -> {
                mainActivity.showToastMessage("This trip has no stops, cannot save.")
            }
            else -> {
                val pickupAddress = arrayOfStops.first().address
                val pickupLat = arrayOfStops.first().latitude
                val pickupLong = arrayOfStops.first().longitude

                val dropOffAddress = arrayOfStops.last().address
                val dropOffLat = arrayOfStops.last().latitude
                val dropOffLong = arrayOfStops.last().longitude


                if(!this.tripViewModel.validatePayAmount(payAmount)) {
                    payAmount = "0"
                }

                this.trip?.let {
                    it.pickupAddress = pickupAddress
                    it.pickupAddressLatitude = pickupLat
                    it.pickupAddressLongitude = pickupLong
                    it.dropOffAddress = dropOffAddress
                    it.dropOffAddressLatitude = dropOffLat
                    it.dropOffAddressLongitude = dropOffLong
                    it.payAmount = payAmount
                    it.gigName = gig
                    it.stops = arrayOfStops.toMutableList()

                    launchIO { tripViewModel.updateTrip(it) }
                    mainActivity.hideKeyBoard(v.rootView)
                }
            }
        }



    }
}