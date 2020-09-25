package name.lmj0011.courierlocker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.databinding.ListItemGateCodeBinding
import name.lmj0011.courierlocker.helpers.LocationHelper

class GateCodeListAdapter(private val clickListener: GateCodeListener): PagedListAdapter<GateCode, GateCodeListAdapter.ViewHolder>(GateCodeDiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemGateCodeBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(clickListener: GateCodeListener, gc: GateCode?) {
            gc?.let {
                binding.gateCode = gc
                binding.clickListener = clickListener
                binding.addressString.text = gc.address
                binding.gateCode1.text = ""
                binding.otherGateCodes.text = ""

                when{
                    gc.codes.isNotEmpty()-> {
                        binding.gateCode1.text = gc.codes[0]
                    }
                    else -> { /*do nothing*/}
                }
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
        val gc: GateCode? = getItem(position)
        holder.bind(clickListener, gc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }
}