package name.lmj0011.courierlocker.fragments

import android.location.Address
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import br.com.simplepass.loadingbutton.presentation.State
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.Stop
import name.lmj0011.courierlocker.databinding.FragmentCreateTripBinding
import name.lmj0011.courierlocker.factories.TripViewModelFactory
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 *
 */
class CreateTripFragment : Fragment() {
    private lateinit var binding: FragmentCreateTripBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var tripViewModel: TripViewModel
    private var fragmentJob = Job()
    private var addressAutoCompleteJob: Job? = null
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_trip, container, false)

        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).tripDao
        val viewModelFactory = TripViewModelFactory(dataSource, application)
        this.tripViewModel = ViewModelProviders.of(this, viewModelFactory).get(TripViewModel::class.java)

        binding.tripViewModel = this.tripViewModel


        uiScope.launch {
            val spinnerValues  = ArrayList<String>()

            withContext(Dispatchers.IO) {
                val gigs = dataSource.getAllGigsThatAreVisible()

                gigs.forEach {
                    spinnerValues.add(it.name)
                }
            }

            binding.gigSpinner.adapter = ArrayAdapter<String>(mainActivity, android.R.layout.simple_spinner_item, spinnerValues).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        binding.createTripSaveCircularProgressButton.setOnClickListener(this::saveButtonOnClickListener)
        binding.createTripAddStopButton.setOnClickListener(this::addStop)
        binding.createTripRemoveLastStopButton.setOnClickListener(this::removeLastStop)

        tripViewModel.payAmountValidated.observe(viewLifecycleOwner, Observer {
            it?.let {
                if(!it){
                    mainActivity.showToastMessage("invalid or no amount was entered")
                }
            }
        })

        tripViewModel.trips.observe(viewLifecycleOwner, Observer {
            val btnState = binding.createTripSaveCircularProgressButton.getState()

            // revert button animation and navigate back to Trips
            if (btnState == State.MORPHING || btnState == State.PROGRESS) {
                binding.createTripSaveCircularProgressButton.revertAnimation()
                this.findNavController().navigate(R.id.tripsFragment)
            }
        })

        tripViewModel.errorMsg.observe(viewLifecycleOwner, Observer {
            if (it.isNotBlank()) mainActivity.showToastMessage(it)
        })

        mainActivity.hideFab()

        this.addStop(binding.root)

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
        inflater.inflate(R.menu.create_trips, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_edit_gig_labels -> {
                this.findNavController().navigate(R.id.action_createTripFragment_to_gigLabelsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun removeLastStop(v: View) {
        val layout: LinearLayout = binding.createTripFragmentLinearLayout

        val lastChild = layout.children.lastOrNull()

        lastChild?.let {
            layout.removeView(it)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun addStop(v: View) {
        val layout: LinearLayout = binding.createTripFragmentLinearLayout
        val view = AutoCompleteTextView(context)
        val adapter = AddressAutoSuggestAdapter(
            mainActivity, // Context
            android.R.layout.simple_dropdown_item_1line
        )

        view.id = View.generateViewId()
        view.hint = "enter address"
        view.setAdapter(adapter)
        view.threshold = 1

        view.onItemClickListener = AdapterView.OnItemClickListener{
                parent,_view,position,id ->
            val address: Address? = adapter.getItem(position)

            address?.let {
                view.setText(it.getAddressLine(0))
                val stop = Stop(it.getAddressLine(0), it.latitude, it.longitude)
                view.tag = stop
            }

        }

        view.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                LocationHelper.performAddressAutoComplete(s.toString(), adapter, addressAutoCompleteJob, uiScope)
            }
        })

        layout.addView(view)

        binding.createTripFragmentScrollView.post {
            // inserting the current location address into this AutoCompleteTextView
            val address = LocationHelper.getFromLocation(binding.root, LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!, 1)

            when{
                address.isNotEmpty() -> {
                    view.setText(address[0].getAddressLine(0))
                    val stop = Stop(address[0].getAddressLine(0), address[0].latitude, address[0].longitude)
                    view.tag = stop

                    // scroll to down to this Stop
                    val scroll = binding.createTripFragmentScrollView
                    scroll.scrollTo(0, scroll.height)
                }
                else -> {
                    layout.removeView(view)
                    mainActivity.showToastMessage("Unable to resolve an Address from current location")
                }
            }
        }


    }

    @Suppress("UNUSED_PARAMETER")
    private fun saveButtonOnClickListener(v: View) {
        var payAmount = binding.payAmountEditText.text.toString()
        val gig = binding.gigSpinner.selectedItem.toString()
        val layout: LinearLayout = binding.createTripFragmentLinearLayout

        val arrayOfStops = layout.children.map {
            it.tag as Stop
        }.toList().toTypedArray()


        when{
            arrayOfStops.isEmpty() -> {
                mainActivity.showToastMessage("This trip has no stops, cannot save.")
            }
            else -> {
                var pickupAddress = arrayOfStops.first().address
                var pickupLat = arrayOfStops.first().latitude
                var pickupLong = arrayOfStops.first().longitude

                var dropOffAddress = arrayOfStops.last().address
                var dropOffLat = arrayOfStops.last().latitude
                var dropOffLong = arrayOfStops.last().longitude


                if(!this.tripViewModel.validatePayAmount(payAmount)) {
                    payAmount = "0"
                }

                binding.createTripSaveCircularProgressButton.isEnabled = false
                binding.createTripSaveCircularProgressButton.startAnimation()

                this.tripViewModel.insertTrip(
                    pickupAddress,
                    pickupLat,
                    pickupLong,
                    dropOffAddress,
                    dropOffLat,
                    dropOffLong,
                    payAmount,
                    gig,
                    arrayOfStops
                )
            }
        }


    }
}