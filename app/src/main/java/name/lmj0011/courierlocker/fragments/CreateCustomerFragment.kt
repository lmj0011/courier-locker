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
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.MainActivity

import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.Customer
import name.lmj0011.courierlocker.databinding.FragmentCreateCustomerBinding
import name.lmj0011.courierlocker.factories.CustomerViewModelFactory
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.viewmodels.CustomerViewModel
import org.kodein.di.instance


/**
 * A simple [Fragment] subclass.
 *
 */
class CreateCustomerFragment : Fragment() {

    private lateinit var binding: FragmentCreateCustomerBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var locationHelper: LocationHelper
    private var customer = Customer()
    private var fragmentJob = Job()
    private var addressAutoCompleteJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_customer, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).customerDao
        val viewModelFactory = CustomerViewModelFactory(dataSource, application)
        this.customerViewModel = ViewModelProvider(this, viewModelFactory).get(CustomerViewModel::class.java)
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        binding.customerViewModel = this.customerViewModel

        binding.createCustomerBadImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorDefaultFace, null))

        binding.createCustomerGoodImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorDefaultFace, null))

        binding.createCustomerGoodImpressionImageView.setOnClickListener {
            colorHappyFace()
            this.customer.impression = 0
        }

        binding.createCustomerBadImpressionImageView.setOnClickListener {
            colorSadFace()
            this.customer.impression = 1
        }

        binding.createCustomerSaveButton.setOnClickListener(this::saveButtonOnClickListener)

        /// Auto Complete Text View Adapter setup

        // Initialize a new array adapter object
        val adapter = AddressAutoSuggestAdapter(
            mainActivity, // Context
            android.R.layout.simple_dropdown_item_1line
        )

        // Set the AutoCompleteTextView adapter
        binding.createCustomerAddressAutoCompleteTextView.setAdapter(adapter)

        // Auto complete threshold
        // The minimum number of characters to type to show the drop down
        binding.createCustomerAddressAutoCompleteTextView.threshold = 1

        // Set an item click listener for auto complete text view
        binding.createCustomerAddressAutoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener{
                _,_,position,_->
            val address: Address? = adapter.getItem(position)

            address?.let {
                binding.createCustomerAddressAutoCompleteTextView.setText(it.getAddressLine(0))
                this.customer.addressLatitude = it.latitude
                this.customer.addressLongitude = it.longitude
            }

        }

        binding.createCustomerAddressAutoCompleteTextView.addTextChangedListener(object:
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                locationHelper.performAddressAutoComplete(s.toString(), adapter)
            }
        })

        // Set a dismiss listener for auto complete text view
        binding.createCustomerAddressAutoCompleteTextView.setOnDismissListener { }


        // Set a focus change listener for auto complete text view
        binding.createCustomerAddressAutoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, b ->
            if(b){
                binding.createCustomerAddressAutoCompleteTextView.showDropDown()
            }
        }

        ///////////////////////////

        /// setting current location's address into the address textview
        binding.createCustomerInsertMyLocationButton.setOnClickListener {
            launchUI {
                val address = locationHelper.getFromLocation(binding.root, locationHelper.lastLatitude.value!!, locationHelper.lastLongitude.value!!, 1)

                when{
                    address.isNotEmpty() -> {
                        binding.createCustomerAddressAutoCompleteTextView.setText(address[0].getAddressLine(0))
                        this@CreateCustomerFragment.customer.addressLatitude = address[0].latitude
                        this@CreateCustomerFragment.customer.addressLongitude = address[0].longitude
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


    private fun colorHappyFace() {
        binding.createCustomerBadImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorDefaultFace, null))

        binding.createCustomerGoodImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorHappyFace, null))
    }

    private fun colorSadFace() {
        binding.createCustomerGoodImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorDefaultFace, null))

        binding.createCustomerBadImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorSadFace, null))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun saveButtonOnClickListener(v: View) {
        this.customer.name = binding.createCustomerNameEditText.text.toString()
        this.customer.address = binding.createCustomerAddressAutoCompleteTextView.text.toString()
        this.customer.note = binding.createCustomerNoteEditText.text.toString()

        this.customerViewModel.insertCustomer(
            this.customer.name,
            this.customer.address,
            this.customer.addressLatitude,
            this.customer.addressLongitude,
            this.customer.impression,
            this.customer.note
        )

        mainActivity.hideKeyBoard(v.rootView)
        this.findNavController().navigate(R.id.customersFragment)
    }


}
