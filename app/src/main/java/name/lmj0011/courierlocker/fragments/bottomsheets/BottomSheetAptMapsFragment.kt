package name.lmj0011.courierlocker.fragments.bottomsheets

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
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
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.helpers.launchIO
import name.lmj0011.courierlocker.services.observeOnce
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel

class BottomSheetAptMapsFragment(private val selectedAptMap: (apt: Apartment) -> Unit): BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentAptMapsBinding
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private val apartmentViewModel by viewModels<ApartmentViewModel> {
        ApartmentViewModelFactory(
            CourierLockerDatabase.getInstance(requireActivity().application).apartmentDao,
            requireActivity().application
        )
    }
    private lateinit var listAdapter: MapListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_fragment_apt_maps, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        return bottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupRecyclerView()
        setupObservers()
    }

    private fun setupBinding(view: View) {
        binding = BottomsheetFragmentAptMapsBinding.bind(view)
    }

    private fun setupRecyclerView() {
        listAdapter = MapListAdapter(MapListAdapter.MapListener(
            { apt ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Associate Gate Codes with")
                    .setMessage(apt.name)
                    .setPositiveButton("Yes") { _, _ ->
                        selectedAptMap(apt)
                        dismiss()
                    }
                    .setNeutralButton("Cancel") {_, _ -> }
                    .show()

                bottomSheetDialog.dismiss()

            },
            {},
            {}),
            requireParentFragment(),
            MapListAdapter.VIEW_MODE_COMPACT
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.mapList.addItemDecoration(decor)
        binding.mapList.adapter = listAdapter
    }

    private fun setupObservers() {
        apartmentViewModel.apartmentsWithoutGateCodePaged.observeOnce { apts ->
            listAdapter.submitList(apts)
            listAdapter.notifyDataSetChanged()
        }
    }
}