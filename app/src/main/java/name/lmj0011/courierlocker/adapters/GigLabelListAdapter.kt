package name.lmj0011.courierlocker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.GigLabel
import name.lmj0011.courierlocker.databinding.ListItemGigLabelBinding
import name.lmj0011.courierlocker.fragments.dialogs.EditGigLabelNameDialogFragment
import name.lmj0011.courierlocker.fragments.dialogs.SimpleConfirmationDialogFragment
import timber.log.Timber

class GigLabelListAdapter(private val clickListener: GigLabelListener, private val parentFragment: Fragment): ListAdapter<GigLabel, GigLabelListAdapter.ViewHolder>(MapDiffCallback())
{
    class ViewHolder private constructor(val binding: ListItemGigLabelBinding) : RecyclerView.ViewHolder(binding.root)
    {

        fun bind(clickListeners: GigLabelListener, gigLabel: GigLabel?) {
            gigLabel?.let {
                binding.title.text = it.name

                if (gigLabel.visible) {
                    binding.visibilityToggleGigLabelButton.setImageResource(R.drawable.ic_baseline_visibility_24)
                } else {
                    binding.visibilityToggleGigLabelButton.setImageResource(R.drawable.ic_baseline_visibility_off_24)
                }


                binding.visibilityToggleGigLabelButton.setOnClickListener {
                    if (gigLabel.visible) {
                        gigLabel.visible = false
                        binding.visibilityToggleGigLabelButton.setImageResource(R.drawable.ic_baseline_visibility_off_24)
                    } else {
                        gigLabel.visible = true
                        binding.visibilityToggleGigLabelButton.setImageResource(R.drawable.ic_baseline_visibility_24)
                    }

                    clickListeners.onVisibilityChanged(gigLabel)
                }

                binding.deleteGigLabelButton.setOnClickListener {
                    val dialog = SimpleConfirmationDialogFragment("Delete this label?", "${gigLabel.name}"){
                        clickListeners.onDeleteGigLabel(gigLabel)
                    }

                    dialog.show(parentFragment.childFragmentManager, parentFragment.tag)
                }

                binding.editGigLabelButton.setOnClickListener {
                    val dialog = EditGigLabelNameDialogFragment(gigLabel){ newName ->
                        newName?.let { name -> gigLabel.name = name }
                        clickListeners.onEditGigLabel(gigLabel)
                    }

                    dialog.show(parentFragment.childFragmentManager, parentFragment.tag)
                }

                binding.gigLabel = gigLabel
            }

            binding.executePendingBindings()
        }

        companion object {
            lateinit var parentFragment: Fragment
                private set

            fun from(parent: ViewGroup, pf: Fragment): ViewHolder {
                parentFragment = pf
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemGigLabelBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }

    }

    class MapDiffCallback : DiffUtil.ItemCallback<GigLabel>() {
        override fun areItemsTheSame(oldItem: GigLabel, newItem: GigLabel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GigLabel, newItem: GigLabel): Boolean {
            return oldItem == newItem
        }
    }

    class GigLabelListener(private val mapOfListeners: Map<String, (g: GigLabel) -> Unit>) {
        fun onClick(gigLabel: GigLabel) = mapOfListeners["viewClickListener"]?.invoke(gigLabel)

        fun onVisibilityChanged(gigLabel: GigLabel) = mapOfListeners["visibilityListener"]?.invoke(gigLabel)

        fun onDeleteGigLabel(gigLabel: GigLabel) = mapOfListeners["deleteListener"]?.invoke(gigLabel)

        fun onEditGigLabel(gigLabel: GigLabel) = mapOfListeners["editListener"]?.invoke(gigLabel)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gigLabel: GigLabel? = getItem(position)

        holder.bind(clickListener, gigLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, this.parentFragment)
    }

    public override fun getItem(position: Int): GigLabel? {
        return super.getItem(position)
    }

}

