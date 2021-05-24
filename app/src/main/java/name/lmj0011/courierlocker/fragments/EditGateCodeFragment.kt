package name.lmj0011.courierlocker.fragments


import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.databinding.FragmentEditGateCodeBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory
import name.lmj0011.courierlocker.fragments.bottomsheets.BottomSheetAptMapsFragment
import name.lmj0011.courierlocker.fragments.dialogs.DeleteGateCodeDialogFragment
import name.lmj0011.courierlocker.helpers.launchIO
import name.lmj0011.courierlocker.helpers.withUIContext
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel

/**
 * A simple [Fragment] subclass.
 *
 */
class EditGateCodeFragment : Fragment(), DeleteGateCodeDialogFragment.NoticeDialogListener {

    private lateinit var binding: FragmentEditGateCodeBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var gateCode: GateCode
    private lateinit var gateCodeViewModel: GateCodeViewModel
    private lateinit var apartmentViewModel: ApartmentViewModel
    private lateinit var bottomSheetAptMapsFragment: BottomSheetAptMapsFragment
    private lateinit var args: EditGateCodeFragmentArgs
    private lateinit var mapAssociationMenuItem: MenuItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_gate_code, container, false)

        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).gateCodeDao
        val viewModelFactory = GateCodeViewModelFactory(dataSource, application)
        args = EditGateCodeFragmentArgs.fromBundle(requireArguments())
        gateCodeViewModel = ViewModelProvider(this, viewModelFactory).get(GateCodeViewModel::class.java)

        apartmentViewModel= ViewModelProvider(this,
            ApartmentViewModelFactory(CourierLockerDatabase
                .getInstance(application).apartmentDao, application))
            .get(ApartmentViewModel::class.java)

        binding.gateCodeViewModel = gateCodeViewModel

        mainActivity.hideFab()

        gateCodeViewModel.gateCode.observe(viewLifecycleOwner, {
            it?.let { gc ->
                gateCode  = gc
                mainActivity.supportActionBar?.subtitle = gateCode.address

                this.injectGateCodeIntoView(gc)

                launchIO {
                    val apt = gateCodeViewModel.getRelatedApartment(gc.id)

                    apt?.also {
                        withUIContext { mapAssociationMenuItem.isEnabled = false }
                    }
                }
            }
        })

        gateCodeViewModel.setGateCode(args.gateCodeId)

        binding.editGateCodeSaveButton.setOnClickListener(this::saveButtonOnClickListener)

        binding.editGateCodeDeleteButton.setOnClickListener {
            val dialog = DeleteGateCodeDialogFragment()
            dialog.show(childFragmentManager, "DeleteGateCodeDialogFragment")

        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_gatecodes, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        mapAssociationMenuItem = menu.getItem(0)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_gate_codes_link_map -> {
                bottomSheetAptMapsFragment = BottomSheetAptMapsFragment { apt ->
                    apt.gateCodeId = args.gateCodeId.toLong()
                    apartmentViewModel.updateApartment(apt)
                    findNavController().navigate(R.id.gateCodesFragment)
                }

                bottomSheetAptMapsFragment
                    .show(childFragmentManager, "BottomSheetAptMapsFragment")

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        gateCodeViewModel.deleteGateCode(gateCode.id)
        mainActivity.showToastMessage("Deleted a gate code entry")
        findNavController().navigate(R.id.gateCodesFragment)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {

    }

    private fun injectGateCodeIntoView(gc: GateCode?) {
        gc?.let {
            binding.addressTextView.text = it.address
            addGateCode()
            it.codes.forEach { code ->
                addGateCode(code)
            }
        }

    }

    private fun addGateCode(code: String? = null) {
        val containerLayout: LinearLayout = binding.editGateCodeFragmentLinearLayout
        val horizontalLinearLayout = LinearLayout(context)
        val editText = EditText(context)
        val cancelImageButton = ImageButton(context)

        horizontalLinearLayout.id = View.generateViewId()
        horizontalLinearLayout.orientation = LinearLayout.HORIZONTAL
        horizontalLinearLayout.weightSum = 4f

        cancelImageButton.id = View.generateViewId()
        cancelImageButton.setImageResource(R.drawable.ic_baseline_cancel_24)
        cancelImageButton.setBackgroundColor(requireContext().getColor(android.R.color.transparent))
        cancelImageButton.setOnClickListener {
            containerLayout.removeView(horizontalLinearLayout)
        }
        cancelImageButton.layoutParams =
            LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)

        editText.id = View.generateViewId()
        editText.layoutParams =
            LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT, 3.5f)
        editText.hint = "0000"
        editText.setEms(10)
        editText.inputType = InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE

        if(code != null) {
            editText.setText(code)

            horizontalLinearLayout.addView(editText)
            horizontalLinearLayout.addView(cancelImageButton)
            containerLayout.addView(horizontalLinearLayout)
        } else {
            horizontalLinearLayout.addView(editText)
            containerLayout.addView(horizontalLinearLayout)
        }

        val scroll = binding.editGateCodeFragmentScrollView
        scroll.scrollTo(0, scroll.height)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun saveButtonOnClickListener(v: View) {
        val codesContainer: LinearLayout = binding.editGateCodeFragmentLinearLayout
        val address: String = binding.addressTextView.text.toString()
        val codes: ArrayList<String> = arrayListOf()

        for (idx in 0..codesContainer.childCount) {
            val horizontalLayout = codesContainer.getChildAt(idx)

            if (horizontalLayout is LinearLayout) {
                val editText = horizontalLayout.getChildAt(0)
                if (editText is EditText && editText.text.toString().isNotBlank()) {
                    codes.add(editText.text.toString())
                }
            }


        }

        if (address.isBlank() || codes.size < 1) {
            mainActivity.showToastMessage("Must enter an address and at least 1 code")
            return
        }

        gateCode.address = address
        gateCode.codes.clear()
        gateCode.codes.addAll(codes)

        this.gateCodeViewModel.updateGateCode(gateCode)
        mainActivity.showToastMessage("Updated gate code")
        mainActivity.hideKeyBoard(v.rootView)
        findNavController().navigate(R.id.gateCodesFragment)
    }


}
