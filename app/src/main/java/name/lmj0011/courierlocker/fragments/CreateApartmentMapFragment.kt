package name.lmj0011.courierlocker.fragments


import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import androidx.transition.Slide
import androidx.transition.TransitionManager
import android.view.*
import android.widget.AdapterView
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import br.com.simplepass.loadingbutton.presentation.State
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.maps.android.ui.IconGenerator
import com.google.maps.android.ui.IconGenerator.STYLE_BLUE
import kotlinx.android.synthetic.main.fragment_edit_apt_buildings_map.view.*
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.adapters.MapListAdapter
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.Building
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentCreateApartmentMapBinding
import name.lmj0011.courierlocker.databinding.FragmentEditAptBuildingsMapBinding
import name.lmj0011.courierlocker.databinding.FragmentMapsBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 *
 */
class CreateApartmentMapFragment : Fragment(){

    private lateinit var binding: FragmentCreateApartmentMapBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: ApartmentViewModelFactory
    private lateinit var apartmentViewModel: ApartmentViewModel
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var handler: Handler
    private var fragmentJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    private val defaultZero = 0.0
    private var aptAddressLatitude: Double = defaultZero
    private var aptAddressLongitude: Double = defaultZero

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_apartment_map, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).apartmentDao
        viewModelFactory = ApartmentViewModelFactory(dataSource, application)
        apartmentViewModel = ViewModelProviders.of(this, viewModelFactory).get(ApartmentViewModel::class.java)

        binding.apartmentViewModel = this.apartmentViewModel

        /// Auto Complete Text View Adapter setup

        // Initialize a new array adapter object
        val adapter = AddressAutoSuggestAdapter(
            mainActivity, // Context
            android.R.layout.simple_dropdown_item_1line
        )

        handler = LocationHelper.getNewAddressAutoCompleteHandler(adapter)

        // Set the AutoCompleteTextView adapter
        binding.createApartmentMapAddressAutoCompleteTextView.setAdapter(adapter)

        // Auto complete threshold
        // The minimum number of characters to type to show the drop down
        binding.createApartmentMapAddressAutoCompleteTextView.threshold = 1

        // Set an item click listener for auto complete text view
        binding.createApartmentMapAddressAutoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener{
                parent,view,position,id->
            val address: Address? = adapter.getItem(position)

            address?.let {
                binding.createApartmentMapAddressAutoCompleteTextView.setText(it.getAddressLine(0))
                this@CreateApartmentMapFragment.aptAddressLatitude = it.latitude
                this@CreateApartmentMapFragment.aptAddressLongitude = it.longitude
            }

        }

        binding.createApartmentMapAddressAutoCompleteTextView.addTextChangedListener(object:
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handler.removeMessages(MainActivity.TRIGGER_AUTO_COMPLETE)

                val bundle = Bundle()
                bundle.putString("address", binding.createApartmentMapAddressAutoCompleteTextView.text.toString())
                val msg = handler.obtainMessage(MainActivity.TRIGGER_AUTO_COMPLETE)
                msg.data = bundle
                handler.sendMessageDelayed(msg, MainActivity.AUTO_COMPLETE_DELAY)
            }
        })

        // Set a dismiss listener for auto complete text view
        binding.createApartmentMapAddressAutoCompleteTextView.setOnDismissListener { }


        // Set a focus change listener for auto complete text view
        binding.createApartmentMapAddressAutoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if(b){
                binding.createApartmentMapAddressAutoCompleteTextView.showDropDown()
            }
        }

        ///////////////////////////

        /// setting current location's address into the address textview
        binding.createApartmentMapInsertMyLocationButton.setOnClickListener {
            val address = LocationHelper.getFromLocation(binding.root, LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!, 1)

            when{
                address.isNotEmpty() -> {
                    binding.createApartmentMapAddressAutoCompleteTextView.setText(address[0].getAddressLine(0))
                    this@CreateApartmentMapFragment.aptAddressLatitude = address[0].latitude
                    this@CreateApartmentMapFragment.aptAddressLongitude = address[0].longitude
                }
                else -> {
                    mainActivity.showToastMessage("Unable to resolve an Address from current location")
                }
            }
        }
        //////////////////

        apartmentViewModel.apartments.observe(viewLifecycleOwner, Observer {
            val btnState = binding.createApartmentMapCircularProgressButton.getState()

            // revert button animation and navigate back to Trips
            if (btnState == State.MORPHING || btnState == State.PROGRESS) {
                binding.createApartmentMapCircularProgressButton.revertAnimation()
                this.findNavController().navigate(R.id.mapsFragment)
            }
        })

        binding.createApartmentMapCircularProgressButton.setOnClickListener(this::saveButtonOnClickListener)


        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        handler.removeMessages(MainActivity.TRIGGER_AUTO_COMPLETE)
    }

    override fun onResume() {
        super.onResume()
        mainActivity.hideFab()
        mainActivity.supportActionBar?.subtitle = null
        this.applyPreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob.cancel()
    }


    private fun applyPreferences() { }

    @Suppress("UNUSED_PARAMETER")
    private fun saveButtonOnClickListener(v: View) {
        val name = binding.createApartmentMapName.text.toString()
        val address: String = binding.createApartmentMapAddressAutoCompleteTextView.text.toString()
        val lat = this@CreateApartmentMapFragment.aptAddressLatitude
        val lng = this@CreateApartmentMapFragment.aptAddressLongitude

        if (address.isBlank() || lat.equals(defaultZero)|| lng.equals(defaultZero)) {
            mainActivity.showToastMessage("Must enter an address")
            return
        }

        val apt = Apartment(name=name, address = address, latitude = lat, longitude = lng)

        this.apartmentViewModel.insertApartments(mutableListOf(apt))
        binding.createApartmentMapCircularProgressButton.isEnabled = false
        binding.createApartmentMapCircularProgressButton.startAnimation()
    }

}
