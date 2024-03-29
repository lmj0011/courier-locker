package name.lmj0011.courierlocker.fragments


import android.location.Address
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
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
import name.lmj0011.courierlocker.databinding.FragmentEditCustomerBinding
import name.lmj0011.courierlocker.factories.CustomerViewModelFactory
import name.lmj0011.courierlocker.fragments.dialogs.DeleteCustomerDialogFragment
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.launchUI
import name.lmj0011.courierlocker.viewmodels.CustomerViewModel
import org.kodein.di.instance


/**
 * A simple [Fragment] subclass.
 *
 */
class EditCustomerFragment : Fragment(), DeleteCustomerDialogFragment.NoticeDialogListener {

    private lateinit var binding: FragmentEditCustomerBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var locationHelper: LocationHelper
    private var fragmentJob = Job()
    private var addressAutoCompleteJob: Job? = null
    private var customer: Customer? = null
    private var customerAddressLatitude = 0.0
    private var customerAddressLongitude = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_customer, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).customerDao
        val viewModelFactory = CustomerViewModelFactory(dataSource, application)
        val args = EditCustomerFragmentArgs.fromBundle(requireArguments())
        this.customerViewModel = ViewModelProvider(this, viewModelFactory).get(CustomerViewModel::class.java)
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        binding.customerViewModel = this.customerViewModel

        binding.editCustomerDeleteButton.setOnClickListener {
            val dialog = DeleteCustomerDialogFragment()
            dialog.show(childFragmentManager, "DeleteCustomerDialogFragment")

        }

        binding.editCustomerBadImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorDefaultFace, null))

        binding.editCustomerGoodImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorDefaultFace, null))

        customerViewModel.customers.observe(viewLifecycleOwner, {
            if (!binding.editCustomerSaveButton.isEnabled || !binding.editCustomerDeleteButton.isEnabled) {
                hideProgressBar()
                findNavController().navigateUp()
            }
        })

        customerViewModel.customer.observe(viewLifecycleOwner, {
            this.customer  = it
            mainActivity.supportActionBar?.subtitle = customer?.name

            this.injectCustomerIntoView(it)
        })

        customerViewModel.setCustomer(args.customerId)

        binding.editCustomerGoodImpressionImageView.setOnClickListener {
            colorHappyFace()
            this.customer?.impression = 0
        }

        binding.editCustomerBadImpressionImageView.setOnClickListener {
            colorSadFace()
            this.customer?.impression = 1
        }

        binding.editCustomerSaveButton.setOnClickListener(this::saveButtonOnClickListener)

        /// Auto Complete Text View Adapter setup

        // Initialize a new array adapter object
        val adapter = AddressAutoSuggestAdapter(
            mainActivity, // Context
            android.R.layout.simple_dropdown_item_1line
        )

        // Set the AutoCompleteTextView adapter
        binding.editCustomerAddressAutoCompleteTextView.setAdapter(adapter)

        // Auto complete threshold
        // The minimum number of characters to type to show the drop down
        binding.editCustomerAddressAutoCompleteTextView.threshold = 1

        // Set an item click listener for auto complete text view
        binding.editCustomerAddressAutoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener{
                _, _, position, _ ->
            val address: Address? = adapter.getItem(position)

            address?.let {
                binding.editCustomerAddressAutoCompleteTextView.setText(it.getAddressLine(0))
                this@EditCustomerFragment.customerAddressLatitude = it.latitude
                this@EditCustomerFragment.customerAddressLongitude = it.longitude
            }

        }

        binding.editCustomerAddressAutoCompleteTextView.addTextChangedListener(object:
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
        binding.editCustomerAddressAutoCompleteTextView.setOnDismissListener { }


        // Set a focus change listener for auto complete text view
        binding.editCustomerAddressAutoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { _, b ->
            if(b){
                binding.editCustomerAddressAutoCompleteTextView.showDropDown()
            }
        }

        ///////////////////////////

        /// setting current location's address into the address textview
        binding.editCustomerInsertMyLocationButton.setOnClickListener {
            launchUI {
                val address = locationHelper.getFromLocation(binding.root, locationHelper.lastLatitude.value!!, locationHelper.lastLongitude.value!!, 1)

                when{
                    address.isNotEmpty() -> {
                        binding.editCustomerAddressAutoCompleteTextView.setText(address[0].getAddressLine(0))
                        this@EditCustomerFragment.customerAddressLatitude = address[0].latitude
                        this@EditCustomerFragment.customerAddressLongitude = address[0].longitude
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

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob.cancel()
        addressAutoCompleteJob?.cancel()
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // User touched the dialog's positive button
        showProgressBar()
        binding.editCustomerDeleteButton.isEnabled = false
        customerViewModel.deleteCustomer(this.customer!!.id)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
    }


    private fun colorHappyFace() {
        binding.editCustomerBadImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorDefaultFace, null))

        binding.editCustomerGoodImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorHappyFace, null))
    }

    private fun colorSadFace() {
        binding.editCustomerGoodImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorDefaultFace, null))

        binding.editCustomerBadImpressionImageView.drawable
            .setTint(resources.getColor(R.color.colorSadFace, null))
    }

    private fun injectCustomerIntoView(c: Customer?) {
        c?.let {
            binding.editCustomerNameEditText.setText(it.name)
            binding.editCustomerAddressAutoCompleteTextView.setText(it.address)
            binding.editCustomerNoteEditText.setText(it.note)

            when(c.impression) {
                0 -> {
                    colorHappyFace()
                }
                else -> {
                    colorSadFace()
                }
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
        binding.editCustomerSaveButton.isEnabled = false

        val name = binding.editCustomerNameEditText.text.toString()
        val address = binding.editCustomerAddressAutoCompleteTextView.text.toString()
        val note = binding.editCustomerNoteEditText.text.toString()

        this.customer?.let {
            it.name = name
            it.address = address
            it.note = note
        }

        this.customerViewModel.updateCustomer(customer)
        mainActivity.hideKeyBoard(v.rootView)
        this.findNavController().navigate(R.id.customersFragment)
    }


}
