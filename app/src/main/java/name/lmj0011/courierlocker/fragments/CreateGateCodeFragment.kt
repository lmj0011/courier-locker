package name.lmj0011.courierlocker.fragments


import android.location.Address
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.databinding.FragmentCreateGateCodeBinding
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory
import name.lmj0011.courierlocker.helpers.GeoLocation
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import java.io.IOException
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass.
 *
 */
class CreateGateCodeFragment : Fragment() {

    private lateinit var binding: FragmentCreateGateCodeBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var gateCodeViewModel: GateCodeViewModel
    private lateinit var handler: Handler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_gate_code, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).gateCodeDao
        val viewModelFactory = GateCodeViewModelFactory(dataSource, application)
        this.gateCodeViewModel = ViewModelProviders.of(this, viewModelFactory).get(GateCodeViewModel::class.java)

        binding.gateCodeViewModel = this.gateCodeViewModel

        mainActivity.hideFab()

        binding.saveButton.setOnClickListener(this::saveButtonOnClickListener)


        /// Auto Complete Text View Adapter setup

        // Initialize a new array adapter object
        val adapter = AddressAutoSuggestAdapter(
            mainActivity, // Context
            android.R.layout.simple_dropdown_item_1line
        )

        handler = Handler(Handler.Callback {
            if (it.what == MainActivity.TRIGGER_AUTO_COMPLETE) {
                val addressStr = binding.addressAutoCompleteTextView.text.toString()

                if (addressStr.isNullOrEmpty().not()){
                    val geolocation = GeoLocation.fromDegrees(LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!)
                    val boundingBox = geolocation.boundingCoordinates(10.toDouble(), 3958.8) // numbers are in miles

                    try {
                        val addresses = LocationHelper.getGeocoder().getFromLocationName(
                            addressStr,
                            3,
                            boundingBox[0].latitudeInDegrees,
                            boundingBox[0].longitudeInDegrees,
                            boundingBox[1].latitudeInDegrees,
                            boundingBox[1].longitudeInDegrees
                        )
                        adapter.setData(addresses)
                    } catch (e: IOException) {
                        when{
                            e.message == "grpc failed" -> { }
                            else -> throw e
                        }
                    }
                }
            }

            return@Callback false
        })

        // Set the AutoCompleteTextView adapter
        binding.addressAutoCompleteTextView.setAdapter(adapter)

        // Auto complete threshold
        // The minimum number of characters to type to show the drop down
        binding.addressAutoCompleteTextView.threshold = 1

        // Set an item click listener for auto complete text view
        binding.addressAutoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener{
                parent,view,position,id->
            val address: Address? = adapter.getItem(position)

            address?.let {
                binding.addressAutoCompleteTextView.setText(it.getAddressLine(0))
                binding.latitudeHiddenTextView.text = it.latitude.toString()
                binding.longitudeHiddenTextView.text = it.longitude.toString()
            }

        }

        binding.addressAutoCompleteTextView.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                adapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handler.removeMessages(MainActivity.TRIGGER_AUTO_COMPLETE)
                handler.sendEmptyMessageDelayed(MainActivity.TRIGGER_AUTO_COMPLETE, MainActivity.AUTO_COMPLETE_DELAY)
            }
        })

        // Set a dismiss listener for auto complete text view
        binding.addressAutoCompleteTextView.setOnDismissListener { }


        // Set a focus change listener for auto complete text view
        binding.addressAutoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if(b){
                binding.addressAutoCompleteTextView.showDropDown()
            }
        }

        ///////////////////////////

        /// setting current location's address into the address textview
        binding.insertMyLocationButton.setOnClickListener {
            val address = LocationHelper.getGeocoder().getFromLocation(LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!, 1)

            when{
                address.size > 0 -> {
                    binding.addressAutoCompleteTextView.setText(address[0].getAddressLine(0))
                    binding.latitudeHiddenTextView.text = address[0].latitude.toString()
                    binding.longitudeHiddenTextView.text = address[0].longitude.toString()
                }
                else -> {
                    Toast.makeText(mainActivity, "Unable to resolve an Address from current location", Toast.LENGTH_LONG)
                }
            }
        }
        //////////////////

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        handler.removeMessages(MainActivity.TRIGGER_AUTO_COMPLETE)
    }

    override fun onResume() {
        super.onResume()
        mainActivity.supportActionBar?.title = "Add new Gate Code"
        mainActivity.supportActionBar?.subtitle = null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun saveButtonOnClickListener(v: View) {
        val codesContainer: LinearLayout = binding.createGateCodeFragmentLinearLayout
        val address: String = binding.addressAutoCompleteTextView.text.toString()
        val codes: ArrayList<String> = arrayListOf()
        val lat = binding.latitudeHiddenTextView.text.toString().toDouble()
        val lng = binding.longitudeHiddenTextView.text.toString().toDouble()

        for (idx in 0..codesContainer.childCount) {
            val et = codesContainer.getChildAt(idx)

            if (et is EditText && et.text.toString().isNotBlank()) {
                codes.add(et.text.toString())
            }
        }

        if (address.isBlank() || codes.size < 1) {
            Toast.makeText(context, "Must enter an address and at least 1 code", Toast.LENGTH_LONG).show()
            return
        }

        this.gateCodeViewModel.insertGateCode(address, codes.toTypedArray(), lat, lng)
        Toast.makeText(context, "New gate code added", Toast.LENGTH_SHORT).show()
        this.findNavController().navigate(R.id.gateCodesFragment)
    }


}
