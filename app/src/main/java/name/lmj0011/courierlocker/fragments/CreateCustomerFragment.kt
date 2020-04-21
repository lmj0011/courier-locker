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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import name.lmj0011.courierlocker.MainActivity

import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.AddressAutoSuggestAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.Customer
import name.lmj0011.courierlocker.databinding.FragmentCreateCustomerBinding
import name.lmj0011.courierlocker.factories.CustomerViewModelFactory
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.viewmodels.CustomerViewModel


/**
 * A simple [Fragment] subclass.
 *
 */
class CreateCustomerFragment : Fragment() {

    private lateinit var binding: FragmentCreateCustomerBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var customerViewModel: CustomerViewModel
    private var customer = Customer()
    private var fragmentJob = Job()
    private var addressAutoCompleteJob: Job? = null
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_customer, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).customerDao
        val viewModelFactory = CustomerViewModelFactory(dataSource, application)
        this.customerViewModel = ViewModelProviders.of(this, viewModelFactory).get(CustomerViewModel::class.java)

        binding.customerViewModel = this.customerViewModel

        mainActivity.hideFab()

        blankfaces()
        forceRenderImageViews()

        binding.createCustomerGoodImpressionImageView.setOnClickListener {
            colorHappyFace()
            this.customer?.impression = 0
        }

        binding.createCustomerBadImpressionImageView.setOnClickListener {
            colorSadFace()
            this.customer?.impression = 1
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
                parent,view,position,id->
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
                LocationHelper.performAddressAutoComplete(s.toString(), adapter, addressAutoCompleteJob, uiScope)
            }
        })

        // Set a dismiss listener for auto complete text view
        binding.createCustomerAddressAutoCompleteTextView.setOnDismissListener { }


        // Set a focus change listener for auto complete text view
        binding.createCustomerAddressAutoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if(b){
                binding.createCustomerAddressAutoCompleteTextView.showDropDown()
            }
        }

        ///////////////////////////

        /// setting current location's address into the address textview
        binding.createCustomerInsertMyLocationButton.setOnClickListener {
            val address = LocationHelper.getFromLocation(binding.root, LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!, 1)

            when{
                address.isNotEmpty() -> {
                    binding.createCustomerAddressAutoCompleteTextView.setText(address[0].getAddressLine(0))
                    this.customer.addressLatitude = address[0].latitude
                    this.customer.addressLongitude = address[0].longitude
                }
                else -> {
                    mainActivity.showToastMessage("Unable to resolve an Address from current location")
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


    /**
     * set faces to default color
     */
    private fun blankfaces() {
        DrawableCompat.setTint(
            ContextCompat.getDrawable(mainActivity, R.drawable.ic_happy_face)!!,
            ContextCompat.getColor(mainActivity, R.color.colorDefaultFace)
        )

        DrawableCompat.setTint(
            ContextCompat.getDrawable(mainActivity, R.drawable.ic_sad_face)!!,
            ContextCompat.getColor(mainActivity, R.color.colorDefaultFace)
        )
    }

    private fun colorHappyFace() {
        blankfaces()
        DrawableCompat.setTint(
            ContextCompat.getDrawable(mainActivity, R.drawable.ic_happy_face)!!,
            ContextCompat.getColor(mainActivity, R.color.colorHappyFace)
        )
        forceRenderImageViews()
    }

    private fun colorSadFace() {
        blankfaces()
        DrawableCompat.setTint(
            ContextCompat.getDrawable(mainActivity, R.drawable.ic_sad_face)!!,
            ContextCompat.getColor(mainActivity, R.color.colorSadFace)
        )
        forceRenderImageViews()
    }

    private fun forceRenderImageViews() {
        binding.createCustomerGoodImpressionImageView.setImageDrawable(null)
        binding.createCustomerGoodImpressionImageView.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_happy_face)!!)

        binding.createCustomerBadImpressionImageView.setImageDrawable(null)
        binding.createCustomerBadImpressionImageView.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_sad_face)!!)
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
        mainActivity.showToastMessage("Saved customer")
        this.findNavController().navigate(R.id.customersFragment)
    }


}
