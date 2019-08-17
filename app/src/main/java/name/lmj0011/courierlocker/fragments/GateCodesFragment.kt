package name.lmj0011.courierlocker.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.databinding.FragmentGateCodesBinding

private lateinit var binding: FragmentGateCodesBinding

/**
 * A simple [Fragment] subclass.
 *
 */
class GateCodesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_gate_codes, container, false)

        return binding.root
    }


}
