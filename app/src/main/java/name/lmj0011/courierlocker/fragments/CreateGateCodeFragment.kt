package name.lmj0011.courierlocker.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.app_bar_main.*
import name.lmj0011.courierlocker.MainActivity

import name.lmj0011.courierlocker.databinding.FragmentCreateGateCodeBinding
import name.lmj0011.courierlocker.R
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 *
 */
class CreateGateCodeFragment : Fragment() {

    private lateinit var binding: FragmentCreateGateCodeBinding
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_gate_code, container, false)

        mainActivity = activity as MainActivity

        mainActivity.hideFab()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.supportActionBar?.title = "New Gate Code"
    }


}
