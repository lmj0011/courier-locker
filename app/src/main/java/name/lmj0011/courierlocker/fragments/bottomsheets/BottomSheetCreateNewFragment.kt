package name.lmj0011.courierlocker.fragments.bottomsheets

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.MapListAdapter
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentAptMapsBinding
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentCreateNewBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.helpers.Const
import name.lmj0011.courierlocker.services.observeOnce
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel

class BottomSheetCreateNewFragment(private val navController: NavController): BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentCreateNewBinding
    private lateinit var bottomSheetDialog: BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_fragment_create_new, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        return bottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
    }

    private fun setupBinding(view: View) {
        binding = BottomsheetFragmentCreateNewBinding.bind(view)

        binding.dismissImageView.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        binding.newTripTab.setOnClickListener {
            navController.navigate(R.id.createTripFragment)
            bottomSheetDialog.dismiss()
        }

        binding.newMapTab.setOnClickListener {
            navController.navigate(R.id.createOrEditApartmentMapFragment)
            bottomSheetDialog.dismiss()
        }

        binding.newGateCodeTab.setOnClickListener {
            navController.navigate(R.id.createGateCodeFragment)
            bottomSheetDialog.dismiss()
        }

        binding.newCustomerTab.setOnClickListener {
            navController.navigate(R.id.createCustomerFragment)
            bottomSheetDialog.dismiss()
        }
    }


}