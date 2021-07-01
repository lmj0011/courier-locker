package name.lmj0011.courierlocker.fragments


import android.location.Address
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentCreateOrEditApartmentMapBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.launchUI
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import org.kodein.di.instance
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 *
 */
class CreateOrEditApartmentMapFragment : Fragment() {

    private lateinit var binding: FragmentCreateOrEditApartmentMapBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: ApartmentViewModelFactory
    private lateinit var apartmentViewModel: ApartmentViewModel
    private lateinit var locationHelper: LocationHelper
    private var selectedApt = MutableLiveData<Apartment>()
    private var fragmentJob = Job()
    private var addressAutoCompleteJob: Job? = null
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    private val defaultZero = 0.0
    private var aptAddressLatitude: Double = defaultZero
    private var aptAddressLongitude: Double = defaultZero

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_or_edit_apartment_map, container, false
        )

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).apartmentDao
        viewModelFactory = ApartmentViewModelFactory(dataSource, application)
        val args = CreateOrEditApartmentMapFragmentArgs.fromBundle(requireArguments())
        apartmentViewModel = ViewModelProvider(this, viewModelFactory).get(ApartmentViewModel::class.java)
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        selectedApt.observe(viewLifecycleOwner, Observer {
            mainActivity.supportActionBar?.title = "Edit Place"
            mainActivity.supportActionBar?.subtitle = it.name

            binding.createApartmentMapNameEditText.setText(it.name)
            binding.createApartmentMapAddressAutoCompleteTextView.isEnabled = false
            binding.createApartmentMapAddressAutoCompleteTextView.setText(it.address)
            binding.createApartmentMapInsertMyLocationButton.visibility = View.GONE
        })

        uiScope.launch {
            withContext(Dispatchers.IO) {
                val apt = apartmentViewModel.database.get(args.aptId)
                apt?.let{ selectedApt.postValue(it) }
            }
        }

        binding.apartmentViewModel = this.apartmentViewModel

        /// Auto Complete Text View Adapter setup

        // Initialize a new array adapter object
        val adapter = AddressAutoSuggestAdapter(
            mainActivity, // Context
            android.R.layout.simple_dropdown_item_1line
        )

        // Set the AutoCompleteTextView adapter
        binding.createApartmentMapAddressAutoCompleteTextView.setAdapter(adapter)

        // Auto complete threshold
        // The minimum number of characters to type to show the drop down
        binding.createApartmentMapAddressAutoCompleteTextView.threshold = 1

        // Set an item click listener for auto complete text view
        binding.createApartmentMapAddressAutoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val address: Address? = adapter.getItem(position)

                address?.let {
                    binding.createApartmentMapAddressAutoCompleteTextView.setText(
                        it.getAddressLine(
                            0
                        )
                    )
                    this@CreateOrEditApartmentMapFragment.aptAddressLatitude = it.latitude
                    this@CreateOrEditApartmentMapFragment.aptAddressLongitude = it.longitude
                }

            }

        binding.createApartmentMapAddressAutoCompleteTextView.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                locationHelper.performAddressAutoComplete(
                    s.toString(),
                    adapter
                )
            }
        })

        // Set a dismiss listener for auto complete text view
        binding.createApartmentMapAddressAutoCompleteTextView.setOnDismissListener { }


        // Set a focus change listener for auto complete text view
        binding.createApartmentMapAddressAutoCompleteTextView.onFocusChangeListener =
            View.OnFocusChangeListener { _, b ->
                try {
                    if (b) {
                        binding.createApartmentMapAddressAutoCompleteTextView.showDropDown()
                    }
                } catch (ex: WindowManager.BadTokenException) {
                    Timber.e(ex)
                }
            }

        ///////////////////////////

        /// setting current location's address into the address textview
        binding.createApartmentMapInsertMyLocationButton.setOnClickListener {
            launchUI {
                val address = locationHelper.getFromLocation(
                    binding.root,
                    locationHelper.lastLatitude.value!!,
                    locationHelper.lastLongitude.value!!,
                    1
                )

                when {
                    address.isNotEmpty() -> {
                        binding.createApartmentMapAddressAutoCompleteTextView.setText(
                            address[0].getAddressLine(
                                0
                            )
                        )
                        this@CreateOrEditApartmentMapFragment.aptAddressLatitude = address[0].latitude
                        this@CreateOrEditApartmentMapFragment.aptAddressLongitude = address[0].longitude
                    }
                    else -> {
                        mainActivity.showToastMessage("Unable to resolve an Address from current location")
                    }
                }
            }
        }
        //////////////////

        apartmentViewModel.apartments.observe(viewLifecycleOwner, Observer {
            if(!binding.createApartmentMapSaveButton.isEnabled) {
                hideProgressBar()
                findNavController().navigate(R.id.mapsFragment)
            }
        })

        binding.createApartmentMapSaveButton.setOnClickListener(this::saveButtonOnClickListener)

        binding.createApartmentMapAddressAutoCompleteTextView.requestFocus()

        binding.lifecycleOwner = this

        return binding.root
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
        addressAutoCompleteJob?.cancel()
    }


    private fun applyPreferences() {}

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
        binding.createApartmentMapSaveButton.isEnabled = false

        val apt = selectedApt.value
        val name = binding.createApartmentMapNameEditText.text.toString()
        val address: String = binding.createApartmentMapAddressAutoCompleteTextView.text.toString()
        val lat = this@CreateOrEditApartmentMapFragment.aptAddressLatitude
        val lng = this@CreateOrEditApartmentMapFragment.aptAddressLongitude


        if (apt is Apartment) {
            apt.name = name
            this.apartmentViewModel.updateApartment(apt)
        } else {
            if (address.isBlank() || lat.equals(defaultZero) || lng.equals(defaultZero)) {
                mainActivity.showToastMessage("Must enter an address")
                return
            }

            val apt = Apartment(name = name, address = address, latitude = lat, longitude = lng)

            this.apartmentViewModel.insertApartments(mutableListOf(apt))
        }
        mainActivity.hideKeyBoard(v.rootView)
    }
}
