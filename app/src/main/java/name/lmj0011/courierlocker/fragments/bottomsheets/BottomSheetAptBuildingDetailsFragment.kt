package name.lmj0011.courierlocker.fragments.bottomsheets

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.Building
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentAptBuildingDetailsBinding

class BottomSheetAptBuildingDetailsFragment(
    private val building: Building,
    private val addWaypointCallback: () -> Unit,
    private val removeWaypointCallback: () -> Unit,
    private val removeBuildingCallback: () -> Unit,
    private val dismissCallback: () -> Unit
): BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentAptBuildingDetailsBinding
    private lateinit var bottomSheetDialog: BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_fragment_apt_building_details, container, false)
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissCallback()
    }

    private fun setupBinding(view: View) {
        binding = BottomsheetFragmentAptBuildingDetailsBinding.bind(view)
        binding.buildingName.text = "Building ${building.number}"

        binding.navigateButton.setOnClickListener {
            var gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${building.latitude},${building.longitude}&travelmode=driving")

            if(building.hasWaypoint) {
                gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${building.latitude},${building.longitude}&waypoints=${building.waypointLatitude},${building.waypointLongitude}&travelmode=driving")
            }

            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        binding.addWaypointButton.setOnClickListener {
            addWaypointCallback()
            dismiss()
        }

        binding.removeWaypointButton.setOnClickListener {
            removeWaypointCallback()
            dismiss()
        }

        binding.deleteButton.setOnClickListener {
            removeBuildingCallback()
            dismiss()
        }

        if(building.hasWaypoint) {
            binding.addWaypointButton.visibility = View.GONE
            binding.removeWaypointButton.visibility = View.VISIBLE
        } else {
            binding.addWaypointButton.visibility = View.VISIBLE
            binding.removeWaypointButton.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {}

    private fun setupObservers() {}
}