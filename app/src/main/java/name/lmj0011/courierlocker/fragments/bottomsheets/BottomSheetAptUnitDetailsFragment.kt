package name.lmj0011.courierlocker.fragments.bottomsheets

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.BuildingListAdapter
import name.lmj0011.courierlocker.database.Building
import name.lmj0011.courierlocker.database.BuildingUnit
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentAptBuildingDetailsBinding
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentAptUnitDetailsBinding
import name.lmj0011.courierlocker.helpers.SwipeHelper
import name.lmj0011.courierlocker.helpers.Util

class BottomSheetAptUnitDetailsFragment(
    private val buildingUnit: BuildingUnit,
    private val editBuildingUnitCallback: () -> Unit,
    private val removeBuildingUnitCallback: () -> Unit): BottomSheetDialogFragment()
{
    private lateinit var binding: BottomsheetFragmentAptUnitDetailsBinding
    private lateinit var bottomSheetDialog: BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_fragment_apt_unit_details, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        return bottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupRecyclerView()
        setupObservers()
    }

    private fun setupBinding(view: View) {
        binding = BottomsheetFragmentAptUnitDetailsBinding.bind(view)
        binding.unitName.text = "Unit ${buildingUnit.number}"

        binding.navigateButton.setOnClickListener {
            val gmmIntentUri = Uri.parse("google.navigation:q=${buildingUnit.latitude},${buildingUnit.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        binding.editButton.setOnClickListener {
            editBuildingUnitCallback()
            dismiss()
        }

        binding.deleteButton.setOnClickListener {
            removeBuildingUnitCallback()
            dismiss()
        }
    }

    private fun setupRecyclerView() {}

    private fun setupObservers() {}
}