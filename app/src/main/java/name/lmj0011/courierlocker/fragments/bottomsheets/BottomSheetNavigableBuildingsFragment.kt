package name.lmj0011.courierlocker.fragments.bottomsheets

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.BuildingListAdapter
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.Building
import name.lmj0011.courierlocker.databinding.BottomsheetFragmentBuildingsBinding
import name.lmj0011.courierlocker.helpers.SwipeHelper
import name.lmj0011.courierlocker.helpers.Util


@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

class BottomSheetNavigableBuildingsFragment(private val apartment: Apartment): BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentBuildingsBinding
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var listAdapter: BuildingListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_fragment_buildings, container, false)
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
        binding = BottomsheetFragmentBuildingsBinding.bind(view)
        binding.buildingListName.text = apartment.name
        binding.buildingListAddress.text = Util.addressShortener(
            address = apartment.address,
            offset = 3
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerView() {
        listAdapter = BuildingListAdapter(BuildingListAdapter.BuildingListener { bldg ->

        }, requireParentFragment())

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.buildingList.addItemDecoration(decor)
        binding.buildingList.adapter = listAdapter

        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(binding.buildingList) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                val bldg = listAdapter.getItem(position)

                return listOf(getBuildingNavigateSwipeButton(bldg))
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.buildingList)

        val list = apartment.buildings.sortedWith(compareBy({it.number.length}, {it.number}))

        listAdapter.submitList(list)
        listAdapter.notifyDataSetChanged()
    }

    private fun setupObservers() {}

    private fun getBuildingNavigateSwipeButton(bldg: Building) : SwipeHelper.UnderlayButton {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)

        return SwipeHelper.UnderlayButton(
            requireContext(),
            "Directions",
            14.0f,
            typedValue.resourceId,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    val gmmIntentUri: Uri = Uri.parse("geo:0,0?z=18&q=${bldg.latitude},${bldg.longitude}(bldg: ${bldg.number})")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)

                    val address = Util.addressShortener(
                        address = apartment.address,
                        offset = 3
                    )

                    Toast.makeText(
                        requireContext().applicationContext,
                        "${apartment.name}, $address | Bldg ${bldg.number}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}