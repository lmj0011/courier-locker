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
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.Building
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentAptBuildingDetailsBinding
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentAptUnitDetailsBinding
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentAptUnitFloorOptionsBinding
import name.lmj0011.courierlocker.helpers.SwipeHelper
import name.lmj0011.courierlocker.helpers.Util

class BottomSheetAptUnitFloorOptionsFragment(private val apartment: Apartment, private val applyOptionsCallback: (apartment: Apartment) -> Unit): BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentAptUnitFloorOptionsBinding
    private lateinit var bottomSheetDialog: BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_fragment_apt_unit_floor_options, container, false)
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
        binding = BottomsheetFragmentAptUnitFloorOptionsBinding.bind(view)

        binding.floorsAboveGroundInput.setText(apartment.aboveGroundFloorCount.toString())

        binding.floorsBelowGroundInput.setText(apartment.belowGroundFloorCount.toString())

        binding.floorAsBlueprintSwitch.isChecked = apartment.floorOneAsBlueprint


        binding.floorAsBlueprintSwitch.setOnCheckedChangeListener { _, isChecked ->
            apartment.floorOneAsBlueprint = isChecked
        }

        binding.applyButton.setOnClickListener {
             binding.floorsAboveGroundInput.text.toString().toIntOrNull()?.let { num ->
                 apartment.aboveGroundFloorCount = num
            }

            binding.floorsBelowGroundInput.text.toString().toIntOrNull()?.let { num ->
                apartment.belowGroundFloorCount = num
            }

            applyOptionsCallback(apartment)
            dismiss()
        }
    }

    private fun setupRecyclerView() {}

    private fun setupObservers() {}
}