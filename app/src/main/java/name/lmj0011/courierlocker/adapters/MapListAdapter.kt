package name.lmj0011.courierlocker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedListAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.databinding.ListItemMapBinding
import name.lmj0011.courierlocker.fragments.MapsFragmentDirections
import name.lmj0011.courierlocker.fragments.dialogs.DeleteApartmentDialogFragment
import name.lmj0011.courierlocker.fragments.dialogs.NavigateToAptBuildingDialogFragment
import name.lmj0011.courierlocker.helpers.ListLock


class MapListAdapter(private val clickListener: MapListener, private val parentFragment: Fragment): PagedListAdapter<Apartment, MapListAdapter.ViewHolder>(MapDiffCallback())
{
    class ViewHolder private constructor(val binding: ListItemMapBinding) : RecyclerView.ViewHolder(binding.root)
    {

        fun bind(clickListener: MapListener, apt: Apartment?) {
            val popup = PopupMenu(binding.root.context, binding.root)

            apt?.let {
                binding.apartment = apt
                binding.aptNameTextView.text = apt.name
                binding.aptAddressTextView.text = apt.address
                binding.feedSrcTextView.text = "id: ${apt.id} | source: ${apt.sourceUrl}"
                binding.clickListener = clickListener

                binding.buildingImageBtn.visibility = ImageButton.VISIBLE
                if (apt.buildings.isEmpty()) {
                    binding.buildingImageBtn.visibility = ImageButton.GONE
                }

            if(!PreferenceManager.getDefaultSharedPreferences(binding.root.context).getBoolean("enableDebugMode", false)) {
                binding.feedSrcTextView.visibility = TextView.GONE
            }

                // ref: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.comparisons/natural-order.html
                val lengthThenNatural = compareBy<String> { it.length }
                    .then(naturalOrder())

                apt.buildings.filter { it.number != null }.map { it.number }.sortedWith(lengthThenNatural).forEach {
                    popup.menu.add(it)
                }

                popup.setOnMenuItemClickListener {
                    val buildNumber = it.title.toString()

                    val bld = apt.buildings.filter {
                        it.number == buildNumber
                    }.firstOrNull()

                    bld?.let { building ->
                        // Create an instance of the dialog fragment and show it
                        val dialog = NavigateToAptBuildingDialogFragment(building, apt.name)
                        dialog.show(parentFragment.childFragmentManager, "NavigateToAptBuildingDialogFragment")
                    }

                    true
                }

                binding.buildingImageBtn.setOnClickListener {
                    ListLock.lock()
                    popup.show()
                }

                binding.aptMapImageBtn.setOnClickListener {
                    parentFragment.findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToEditAptBuildingsMapsFragment(apt.id))
                }

                binding.deleteImageBtn.setOnClickListener {
                    val dialog = DeleteApartmentDialogFragment(apt, clickListener.deleteBtnListener)
                    dialog.show(parentFragment.childFragmentManager, "DeleteApartmentDialogFragment")
                }
            }

            binding.executePendingBindings()
        }

        companion object {
            lateinit var parentFragment: Fragment
                private set

            fun from(parent: ViewGroup, pf: Fragment): ViewHolder {
                parentFragment = pf
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemMapBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }

    }

    class MapDiffCallback : DiffUtil.ItemCallback<Apartment>() {
        override fun areItemsTheSame(oldItem: Apartment, newItem: Apartment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Apartment, newItem: Apartment): Boolean {
            return oldItem == newItem
        }
    }

    class MapListener(val clickListener: (aptId: Long) -> Unit, val deleteBtnListener: (aptId: Long) -> Unit) {
        fun onClick(apt: Apartment) = clickListener(apt.id)

        fun onDeleteBtnClick(apt: Apartment) = deleteBtnListener(apt.id)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apt: Apartment? = getItem(position)

        holder.bind(clickListener, apt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, this.parentFragment)
    }

}

