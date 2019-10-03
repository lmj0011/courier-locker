package name.lmj0011.courierlocker.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import name.lmj0011.courierlocker.MainActivity

import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.databinding.FragmentEditGateCodeBinding
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory
import name.lmj0011.courierlocker.fragments.dialogs.DeleteGateCodeDialogFragment
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel


/**
 * A simple [Fragment] subclass.
 *
 */
class EditGateCodeFragment : Fragment(), DeleteGateCodeDialogFragment.NoticeDialogListener {

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

        gateCodeViewModel.gateCode.observe(viewLifecycleOwner, Observer {
            this.gateCode  = it
            mainActivity.supportActionBar?.subtitle = gateCode?.address

            this.injectGateCodeIntoView(it)
        })

        gateCodeViewModel.setGateCode(args.gateCodeId)

        binding.editGateCodeSaveButton.setOnClickListener(this::saveButtonOnClickListener)

        binding.editGateCodeDeleteButton.setOnClickListener {
            val dialog = DeleteGateCodeDialogFragment()
            dialog.show(childFragmentManager, "DeleteGateCodeDialogFragment")

        }

        return binding.root
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        gateCodeViewModel.deleteGateCode(this.gateCode!!.id)
        Toast.makeText(context, "Deleted a gate code entry", Toast.LENGTH_SHORT).show()
        this.findNavController().navigate(R.id.gateCodesFragment)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {

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
