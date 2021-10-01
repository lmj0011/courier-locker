package name.lmj0011.courierlocker.fragments


import android.location.Address
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.databinding.FragmentCreateGateCodeBinding
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.launchUI
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import org.kodein.di.instance
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass.
 *
 */
class CreateGateCodeFragment : Fragment() {

    private lateinit var binding: FragmentCreateGateCodeBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var gateCodeViewModel: GateCodeViewModel
    private lateinit var locationHelper: LocationHelper
    private var fragmentJob = Job()
    private var addressAutoCompleteJob: Job? = null
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    private var gateCodeAddressLatitude: Double = 0.toDouble()
    private var gateCodeAddressLongitude: Double = 0.toDouble()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_gate_code, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).gateCodeDao
        val viewModelFactory = GateCodeViewModelFactory(dataSource, application)
        this.gateCodeViewModel = ViewModelProvider(this, viewModelFactory).get(GateCodeViewModel::class.java)
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        binding.gateCodeViewModel = this.gateCodeViewModel


        binding.createGateCodeSaveButton.setOnClickListener(this::saveButtonOnClickListener)


        gateCodeViewModel.gateCodes.observe(viewLifecycleOwner, {
            if (!binding.createGateCodeSaveButton.isEnabled) {
                hideProgressBar()
                findNavController().navigateUp()
            }
        })

        /// Auto Complete Text View Adapter setup

        // Initialize a new array adapter object
        val adapter = AddressAutoSuggestAdapter(
            mainActivity, // Context
            android.R.layout.simple_dropdown_item_1line
        )

        // Set the AutoCompleteTextView adapter
        binding.createGateCodeAddressAutoCompleteTextView.setAdapter(adapter)

        // Auto complete threshold
        // The minimum number of characters to type to show the drop down
        binding.createGateCodeAddressAutoCompleteTextView.threshold = 1

        // Set an item click listener for auto complete text view
        binding.createGateCodeAddressAutoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener{
                _, _, position, _ ->
            val address: Address? = adapter.getItem(position)

            address?.let {
                binding.createGateCodeAddressAutoCompleteTextView.setText(it.getAddressLine(0))
                this@CreateGateCodeFragment.gateCodeAddressLatitude = it.latitude
                this@CreateGateCodeFragment.gateCodeAddressLongitude = it.longitude
            }

        }

        binding.createGateCodeAddressAutoCompleteTextView.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                adapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                locationHelper.performAddressAutoComplete(s.toString(), adapter)
            }
        })

        // Set a dismiss listener for auto complete text view
        binding.createGateCodeAddressAutoCompleteTextView.setOnDismissListener { }


        // Set a focus change listener for auto complete text view
        binding.createGateCodeAddressAutoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if(b){
                binding.createGateCodeAddressAutoCompleteTextView.showDropDown()
            }
        }

        ///////////////////////////

        /// setting current location's address into the address textview
        binding.createGateCodeInsertMyLocationButton.setOnClickListener {
            launchUI {
                val address = locationHelper.getFromLocation(binding.root, locationHelper.lastLatitude.value!!, locationHelper.lastLongitude.value!!, 1)

                when{
                    address.isNotEmpty() -> {
                        binding.createGateCodeAddressAutoCompleteTextView.setText(address[0].getAddressLine(0))
                        this@CreateGateCodeFragment.gateCodeAddressLatitude = address[0].latitude
                        this@CreateGateCodeFragment.gateCodeAddressLongitude = address[0].longitude
                    }
                    else -> {
                        mainActivity.showToastMessage("Unable to resolve an Address from current location")
                    }
                }
            }
        }
        //////////////////

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
        binding.createGateCodeSaveButton.isEnabled = false

        val codesContainer: LinearLayout = binding.createGateCodeFragmentLinearLayout
        val address: String = binding.createGateCodeAddressAutoCompleteTextView.text.toString()
        val codes: ArrayList<String> = arrayListOf()
        val lat = this@CreateGateCodeFragment.gateCodeAddressLatitude
        val lng = this@CreateGateCodeFragment.gateCodeAddressLongitude

        for (idx in 0..codesContainer.childCount) {
            val et = codesContainer.getChildAt(idx)

            if (et is EditText && et.text.toString().isNotBlank()) {
                codes.add(et.text.toString())
            }
        }

        if (address.isBlank() || codes.size < 1) {
            mainActivity.showToastMessage("Must enter an address and at least 1 code")
            return
        }

        this.gateCodeViewModel.insertGateCode(address, codes.toTypedArray(), lat, lng)
        mainActivity.hideKeyBoard(v.rootView)
    }


}
