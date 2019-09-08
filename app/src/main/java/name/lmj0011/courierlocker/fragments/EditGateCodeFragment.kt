package name.lmj0011.courierlocker.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_create_gate_code.view.*
import name.lmj0011.courierlocker.MainActivity

import name.lmj0011.courierlocker.databinding.FragmentCreateGateCodeBinding
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.databinding.FragmentEditGateCodeBinding
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 *
 */
class EditGateCodeFragment : Fragment() {

    private lateinit var binding: FragmentEditGateCodeBinding
    private lateinit var mainActivity: MainActivity
    private var gateCode: GateCode? = null
    private lateinit var gateCodeViewModel: GateCodeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_gate_code, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).gateCodeDao
        val viewModelFactory = GateCodeViewModelFactory(dataSource, application)
        val args = EditGateCodeFragmentArgs.fromBundle(arguments!!)
        this.gateCodeViewModel = ViewModelProviders.of(this, viewModelFactory).get(GateCodeViewModel::class.java)

        binding.gateCodeViewModel = this.gateCodeViewModel

        mainActivity.hideFab()

        this.gateCode = gateCodeViewModel.getGateCode(args.gateCodeId)
        this.injectGateCodeIntoView(this.gateCode)
        binding.saveButton.setOnClickListener(this::saveButtonOnClickListener)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.supportActionBar?.title = "Edit Gate Code"
        mainActivity.supportActionBar?.subtitle = gateCode?.address
    }

    private fun injectGateCodeIntoView(gc: GateCode?) {
        gc?.let {
            binding.addressTextView.text = it.address

            val codesContainer: LinearLayout = binding.createGateCodeFragmentLinearLayout
            for (idx in 0..codesContainer.childCount) {
                val et = codesContainer.getChildAt(idx)

                if (et is EditText && it.codes.getOrNull(idx).isNullOrEmpty().not()) {
                    et.setText(it.codes[idx])
                }
            }
        }

    }

    @Suppress("UNUSED_PARAMETER")
    private fun saveButtonOnClickListener(v: View) {
        val codesContainer: LinearLayout = binding.createGateCodeFragmentLinearLayout
        val address: String = binding.addressTextView.text.toString()
        val codes: ArrayList<String> = arrayListOf()

        for (idx in 0..codesContainer.childCount) {
            val et = codesContainer.getChildAt(idx)

            if (et is EditText && et.text.toString().isNotBlank()) {
                codes.add(et.text.toString())
            }
        }

        if (address.isBlank() || codes.size < 1) {
            Toast.makeText(context, "Must enter an address and at least 1 code", Toast.LENGTH_LONG).show()
            return
        }

        this.gateCode?.let {
            it.address = address
            it.codes.clear()
            it.codes.addAll(codes)
        }

        this.gateCodeViewModel.updateGateCode(gateCode)
        Toast.makeText(context, "Updated gate code", Toast.LENGTH_SHORT).show()
        this.findNavController().navigate(R.id.gateCodesFragment)
    }


}
