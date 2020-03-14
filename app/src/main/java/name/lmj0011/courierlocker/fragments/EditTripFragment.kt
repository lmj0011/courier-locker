package name.lmj0011.courierlocker.fragments

import android.location.Address
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import br.com.simplepass.loadingbutton.presentation.State
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
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import name.lmj0011.courierlocker.helpers.getTripDate

/**
 * A simple [Fragment] subclass.
 *
 */
class EditTripFragment : Fragment(), DeleteTripDialogFragment.NoticeDialogListener {
    private lateinit var binding: FragmentEditTripBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var tripViewModel: TripViewModel
    private var trip: Trip? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_trip, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).tripDao
        val viewModelFactory = TripViewModelFactory(dataSource, application)
        val args = EditTripFragmentArgs.fromBundle(arguments!!)
        this.tripViewModel = ViewModelProviders.of(this, viewModelFactory).get(TripViewModel::class.java)

        binding.tripViewModel = this.tripViewModel

        this.tripViewModel.trip.observe(viewLifecycleOwner, Observer {
            this.trip  = it

            this.injectTripIntoView(it)
        })

        this.tripViewModel.setTrip(args.tripId)

        ArrayAdapter.createFromResource(
            mainActivity,
            R.array.gigs_array,
            android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.gigSpinner.adapter = it
        }

        binding.editTripSaveCircularProgressButton.setOnClickListener(this::saveButtonOnClickListener)
        binding.editTripAddStopButton.setOnClickListener(this::addStop)
        binding.editTripRemoveLastStopButton.setOnClickListener(this::removeLastStop)

        binding.editTripDeleteCircularProgressButton.setOnClickListener {
            val dialog = DeleteTripDialogFragment()
            dialog.show(childFragmentManager, "DeleteTripDialogFragment")

        }


        tripViewModel.payAmountValidated.observe(viewLifecycleOwner, Observer {
            it?.let {
                if(!it){
                    mainActivity.showToastMessage("invalid or no amount was entered")
                }
            }
        })

        tripViewModel.trips.observe(viewLifecycleOwner, Observer {
            val btnState = binding.editTripSaveCircularProgressButton.getState()

            // revert button animation and navigate back to Trips
            if (btnState == State.MORPHING || btnState == State.PROGRESS) {
                binding.editTripSaveCircularProgressButton.revertAnimation()
                this.findNavController().navigate(R.id.tripsFragment)
            }
        })

        mainActivity.hideFab()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.supportActionBar?.subtitle = null
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // User touched the dialog's positive button
        tripViewModel.deleteTrip(this.trip!!.id)
        mainActivity.showToastMessage("deleted Trip")
        this.findNavController().navigate(R.id.tripsFragment)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
    }

    private fun injectTripIntoView(trip: Trip?) {
        trip?.let {

            binding.tripDateTextView.text = getTripDate(it)

            val layout: LinearLayout = binding.editTripFragmentLinearLayout
            trip.stops.forEach { stop ->
                val view = AutoCompleteTextView(context)
                val adapter = AddressAutoSuggestAdapter(
                    mainActivity, // Context
                    android.R.layout.simple_dropdown_item_1line
                )
                val handler = LocationHelper.getNewAddressAutoCompleteHandler(adapter)

                view.onItemClickListener = AdapterView.OnItemClickListener{
                        parent,_view,position,id ->
                    val address: Address? = adapter.getItem(position)

                    address?.let { address ->
                        view.setText(address.getAddressLine(0))
                        val stop = Stop(address.getAddressLine(0), address.latitude, address.longitude)
                        view.tag = stop
                    }

                }

                view.addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        adapter.notifyDataSetChanged()
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        handler.removeMessages(MainActivity.TRIP_AUTO_COMPLETE + layout.childCount)

                        val bundle = Bundle()
                        bundle.putString("address", view.text.toString())
                        val msg = handler.obtainMessage(MainActivity.TRIP_AUTO_COMPLETE)
                        msg.data = bundle
                        handler.sendMessageDelayed(msg, MainActivity.AUTO_COMPLETE_DELAY)
                    }
                })

                view.setText(stop.address)
                view.tag = stop

                layout.addView(view)
            }

            binding.payAmountEditText.setText(it.payAmount)

            binding.gigSpinner.setSelection(
                resources.getStringArray(R.array.gigs_array).indexOf(it.gigName)
            )

        }

    }

    @Suppress("UNUSED_PARAMETER")
    private fun removeLastStop(v: View) {
        val layout: LinearLayout = binding.editTripFragmentLinearLayout

        val lastChild = layout.children.last()
        layout.removeView(lastChild)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun addStop(v: View) {
        val layout: LinearLayout = binding.editTripFragmentLinearLayout
        val view = AutoCompleteTextView(context)
        val adapter = AddressAutoSuggestAdapter(
            mainActivity, // Context
            android.R.layout.simple_dropdown_item_1line
        )
        val handler = LocationHelper.getNewAddressAutoCompleteHandler(adapter)

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
                handler.removeMessages(MainActivity.TRIP_AUTO_COMPLETE + layout.childCount)

                val bundle = Bundle()
                bundle.putString("address", view.text.toString())
                val msg = handler.obtainMessage(MainActivity.TRIP_AUTO_COMPLETE)
                msg.data = bundle
                handler.sendMessageDelayed(msg, MainActivity.AUTO_COMPLETE_DELAY)
            }
        })

        layout.addView(view)

        binding.editTripFragmentScrollView.post {
            // inserting the current location address into this AutoCompleteTextView
            val address = LocationHelper.getFromLocation(binding.root, LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!, 1)

            when{
                address.isNotEmpty() -> {
                    view.setText(address[0].getAddressLine(0))
                    val stop = Stop(address[0].getAddressLine(0), address[0].latitude, address[0].longitude)
                    view.tag = stop

                    val scroll = binding.editTripFragmentScrollView
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
        val layout: LinearLayout = binding.editTripFragmentLinearLayout

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

                binding.editTripSaveCircularProgressButton.isEnabled = false
                binding.editTripSaveCircularProgressButton.startAnimation()

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
                }

                this.tripViewModel.updateTrip(trip)
            }
        }



    }
}