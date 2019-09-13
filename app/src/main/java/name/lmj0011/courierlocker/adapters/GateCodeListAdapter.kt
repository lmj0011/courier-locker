package name.lmj0011.courierlocker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.databinding.ListItemGateCodeBinding
import name.lmj0011.courierlocker.helpers.LocationHelper

class GateCodeListAdapter(private val clickListener: GateCodeListener): ListAdapter<GateCode, GateCodeListAdapter.ViewHolder>(GateCodeDiffCallback()) {
    override fun getItemId(position: Int): Long {
        // return the Item's database row id
        return super.getItem(position).id
    }

    class ViewHolder private constructor(val binding: ListItemGateCodeBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(clickListener: GateCodeListener, gc: GateCode) {
            binding.gateCode = gc
            binding.clickListener = clickListener
            binding.addressString.text = gc.address
            binding.gateCode1.text = ""
            binding.otherGateCodes.text = ""

            when{
                gc.codes.size > 1 && gc.codes.drop(1).isNotEmpty() -> {
                    binding.gateCode1.text = gc.codes[0]
                    binding.otherGateCodes.text = gc.codes.drop(1).reduce { acc, it ->
                        when{
                            it.isNullOrBlank() -> "$acc"
                            else -> "$acc, $it"
                        }
                    }
                }
                gc.codes.size > 0 -> {
                    binding.gateCode1.text = gc.codes[0]
                }
                else -> { /*do nothing*/}
            }

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemGateCodeBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }

    class GateCodeDiffCallback : DiffUtil.ItemCallback<GateCode>() {
        override fun areItemsTheSame(oldItem: GateCode, newItem: GateCode): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GateCode, newItem: GateCode): Boolean {
            return oldItem == newItem
        }
    }

    class GateCodeListener(val clickListener: (gateCodeId: Long) -> Unit) {
        fun onClick(gc: GateCode) = clickListener(gc.id)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gc = getItem(position)

        holder.bind(clickListener, gc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    fun filterByClosestGateCodeLocation(list: MutableList<GateCode>): MutableList<GateCode> {
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