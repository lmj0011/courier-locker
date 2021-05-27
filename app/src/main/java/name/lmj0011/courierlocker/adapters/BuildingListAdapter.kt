package name.lmj0011.courierlocker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.database.Building
import name.lmj0011.courierlocker.databinding.ListItemBuildingBinding

class BuildingListAdapter(private val clickListener: BuildingListener, private val parentFragment: Fragment): ListAdapter<Building, BuildingListAdapter.ViewHolder>(MapDiffCallback())
{
    class ViewHolder private constructor(val binding: ListItemBuildingBinding) : RecyclerView.ViewHolder(binding.root)
    {

        fun bind(clickListeners: BuildingListener, building: Building?) {
            building?.let {
                binding.buildingTextView.text = building.number
            }

            binding.executePendingBindings()
        }

        companion object {
            lateinit var parentFragment: Fragment
                private set

            fun from(parent: ViewGroup, pf: Fragment): ViewHolder {
                parentFragment = pf
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemBuildingBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }

    }

    class MapDiffCallback : DiffUtil.ItemCallback<Building>() {
        override fun areItemsTheSame(oldItem: Building, newItem: Building): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Building, newItem: Building): Boolean {
            return oldItem == newItem
        }
    }

    class BuildingListener(private val clickListener: (bldg: Building) -> Unit) {
        fun onClick(bldg: Building) = clickListener(bldg)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bldg: Building? = getItem(position)

        holder.bind(clickListener, bldg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, this.parentFragment)
    }

    public override fun getItem(position: Int): Building {
        return super.getItem(position)
    }

}

