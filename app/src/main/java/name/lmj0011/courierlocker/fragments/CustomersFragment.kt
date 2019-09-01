package name.lmj0011.courierlocker.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import name.lmj0011.courierlocker.MainActivity

import name.lmj0011.courierlocker.databinding.FragmentCustomersBinding
import name.lmj0011.courierlocker.R

/**
 * A simple [Fragment] subclass.
 *
 */
class CustomersFragment : Fragment() {

    private lateinit var binding: FragmentCustomersBinding
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_customers, container, false)

        mainActivity = activity as MainActivity

        mainActivity.hideFab()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.supportActionBar?.title = "Customers"
        mainActivity.supportActionBar?.subtitle = null
    }


}
