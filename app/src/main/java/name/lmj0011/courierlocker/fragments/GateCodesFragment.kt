package name.lmj0011.courierlocker.fragments


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentGateCodesBinding
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory


/**
 * A simple [Fragment] subclass.
 *
 */
class GateCodesFragment : Fragment() {

    private lateinit var binding: FragmentGateCodesBinding
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_gate_codes, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).gateCodeDao
        val viewModelFactory = GateCodeViewModelFactory(dataSource, application)
        val gateCodeViewModel = ViewModelProviders.of(this, viewModelFactory).get(GateCodeViewModel::class.java)

        binding.gateCodeViewModel = gateCodeViewModel

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.showFabAndSetListener(this::fabOnClickListenerCallback, R.drawable.ic_fab_add)
        mainActivity.supportActionBar?.title = "Gate Codes"

    }


    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToCreateGateCodeFragment())
// TODO hook this back up after adding recycler view
//        this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToEditGateCodeFragment())
    }

}
