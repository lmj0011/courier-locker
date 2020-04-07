package name.lmj0011.courierlocker.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.databinding.ListItemMapBinding
import name.lmj0011.courierlocker.fragments.dialogs.NavigateToAptBuildingDialogFragment
import name.lmj0011.courierlocker.helpers.LocationHelper


class MapListAdapter(private val clickListener: MapListener, private val parentFragment: Fragment): ListAdapter<Apartment, MapListAdapter.ViewHolder>(MapDiffCallback())
{
    override fun getItemId(position: Int): Long {
        // return the Item's database row id
        return super.getItem(position).id
    }

    class ViewHolder private constructor(val binding: ListItemMapBinding) : RecyclerView.ViewHolder(binding.root)
    {

        fun bind(clickListener: MapListener, apt: Apartment) {
            val popup = PopupMenu(binding.root.context, binding.root)

            binding.apartment = apt
            binding.aptNameTextView.text = apt.name
            binding.aptAddressTextView.text = apt.address
            binding.feedSrcTextView.text = "id: ${apt.id} | source: ${apt.sourceUrl}"
            binding.clickListener = clickListener

            binding.aptMapImageBtn.visibility = ImageButton.VISIBLE
            if (apt.mapImageUrl.isNullOrBlank()) {
                binding.aptMapImageBtn.visibility = ImageButton.GONE
            }

            binding.buildingImageBtn.visibility = ImageButton.VISIBLE
            if (apt.buildings.isEmpty()) {
                binding.buildingImageBtn.visibility = ImageButton.GONE
            }

            if(!binding.root.resources.getBoolean(R.bool.DEBUG_MODE)) {
                binding.feedSrcTextView.visibility = TextView.GONE
            }

            apt.buildings.forEach {
                popup.menu.add(it.number)
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
                popup.show()
            }

            binding.aptMapImageBtn.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(apt.mapImageUrl), "image/*")
                }
                startActivity(binding.root.context, intent, null)
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

    class MapListener(val clickListener: (aptId: Long) -> Unit) {
        fun onClick(apt: Apartment) = clickListener(apt.id)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apt = getItem(position)
        holder.bind(clickListener, apt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, this.parentFragment)
    }

    fun filterByClosestGateCodeLocation(list: MutableList<Apartment>): MutableList<Apartment> {
        return list.sortedBy {
            LocationHelper.calculateApproxDistanceBetweenMapPoints(
                LocationHelper.lastLatitude.value!!,
                LocationHelper.lastLongitude.value!!,
                it.latitude,
                it.longitude
            )
        }.toMutableList()
    }
}
