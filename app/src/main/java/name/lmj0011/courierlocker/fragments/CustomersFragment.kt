package name.lmj0011.courierlocker.fragments


import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import name.lmj0011.courierlocker.MainActivity

import name.lmj0011.courierlocker.databinding.FragmentCustomersBinding
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.CustomerListAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.factories.CustomerViewModelFactory
import name.lmj0011.courierlocker.fragments.dialogs.ClearAllCustomersDialogFragment
import name.lmj0011.courierlocker.viewmodels.CustomerViewModel

/**
 * A simple [Fragment] subclass.
 *
 */
class CustomersFragment : Fragment(), ClearAllCustomersDialogFragment.NoticeDialogListener {

    private lateinit var binding: FragmentCustomersBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: CustomerViewModelFactory
    private lateinit var listAdapter: CustomerListAdapter
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_customers, container, false)

        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).customerDao
        viewModelFactory = CustomerViewModelFactory(dataSource, application)
        customerViewModel = ViewModelProviders.of(this, viewModelFactory).get(CustomerViewModel::class.java)

        listAdapter = CustomerListAdapter( CustomerListAdapter.CustomerListener { customerId ->
            this.findNavController().navigate(CustomersFragmentDirections.actionCustomersFragmentToEditCustomerFragment(customerId.toInt()))
        })

        customerViewModel.customers.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
        })

        binding.customerList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.customerList.adapter = listAdapter

        binding.customerViewModel = customerViewModel

        binding.lifecycleOwner = this

        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.generateCustomerBtn.visibility = View.GONE
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.showFabAndSetListener(this::fabOnClickListenerCallback, R.drawable.ic_fab_add)
        mainActivity.supportActionBar?.title = "Customers"
        mainActivity.supportActionBar?.subtitle = null

        initFaceColors()
        this.applyPreferences()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.customers, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_customers_clear_all -> {
                this.showClearAllCustomersDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClearAllCustomersDialogPositiveClick(dialog: DialogFragment) {
        customerViewModel.clearAllCustomers()
    }
    override fun onClearAllCustomersDialogNegativeClick(dialog: DialogFragment) {

    }

    private fun showClearAllCustomersDialog() {
        // Create an instance of the dialog fragment and show it
        val dialog = ClearAllCustomersDialogFragment()
        dialog.show(childFragmentManager, "ClearAllCustomersDialogFragment")
    }

    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(CustomersFragmentDirections.actionCustomersFragmentToCreateCustomerFragment())
    }

    private fun applyPreferences() {
        //
    }

    private fun initFaceColors() {
        DrawableCompat.setTint(
            ContextCompat.getDrawable(mainActivity, R.drawable.ic_happy_face)!!,
            ContextCompat.getColor(mainActivity, R.color.colorHappyFace)
        )

        DrawableCompat.setTint(
            ContextCompat.getDrawable(mainActivity, R.drawable.ic_sad_face)!!,
            ContextCompat.getColor(mainActivity, R.color.colorSadFace)
        )
    }

}
