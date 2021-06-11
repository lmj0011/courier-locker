package name.lmj0011.courierlocker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.databinding.ListItemMapBinding
import name.lmj0011.courierlocker.fragments.MapsFragmentDirections
import name.lmj0011.courierlocker.fragments.bottomsheets.BottomSheetNavigableBuildingsFragment
import name.lmj0011.courierlocker.fragments.dialogs.DeleteApartmentDialogFragment
import name.lmj0011.courierlocker.helpers.ListLock
import name.lmj0011.courierlocker.helpers.Util


class MapListAdapter(private val clickListener: MapListener, private val parentFragment: Fragment, private val viewMode: Int = VIEW_MODE_NORMAL): PagingDataAdapter<Apartment, MapListAdapter.ViewHolder>(MapDiffCallback())
{
    companion object {
        const val VIEW_MODE_NORMAL = 0

        // the ImageButtons are hidden
        const val VIEW_MODE_COMPACT = 1
    }

    class ViewHolder private constructor(val binding: ListItemMapBinding) : RecyclerView.ViewHolder(binding.root)
    {

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

        fun bind(clickListener: MapListener, apt: Apartment?, viewMode: Int) {
            when(viewMode) {
                VIEW_MODE_NORMAL -> getNormalViewBindings(clickListener, apt)
                VIEW_MODE_COMPACT -> getCompactViewBindings(clickListener, apt)
            }

            binding.executePendingBindings()
        }

        private fun getNormalViewBindings(clickListener: MapListener, apt: Apartment?) {
            apt?.let {
                binding.apartment = apt
                binding.aptNameTextView.text = apt.name
                binding.aptAddressTextView.text = Util.addressShortener(address = apt.address, offset = 3)
                binding.clickListener = clickListener

                if(apt.gateCodeId > 0L) {
                    binding.gateCodeImageBtn.visibility = ImageButton.VISIBLE
                    binding.gateCodeImageBtn.setOnClickListener {
                        clickListener.gateCodeBtnListener(apt)
                    }
                } else binding.gateCodeImageBtn.visibility = ImageButton.GONE

                binding.buildingImageBtn.visibility = ImageButton.VISIBLE
                if (apt.buildings.isEmpty()) {
                    binding.buildingImageBtn.visibility = ImageButton.GONE
                }

                binding.buildingImageBtn.setOnClickListener {
                    ListLock.lock()
                    BottomSheetNavigableBuildingsFragment(apt)
                        .show(parentFragment.childFragmentManager, "BottomSheetNavigableBuildingsFragment")
                }

                binding.aptMapImageBtn.setOnClickListener {
                    parentFragment.findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToEditAptBuildingsMapsFragment(apt.id))
                }

                binding.deleteImageBtn.setOnClickListener {
                    val dialog = DeleteApartmentDialogFragment(apt, clickListener.deleteBtnListener)
                    dialog.show(parentFragment.childFragmentManager, "DeleteApartmentDialogFragment")
                }
            }
        }

        private fun getCompactViewBindings(clickListener: MapListener, apt: Apartment?) {
            apt?.let {
                binding.apartment = apt
                binding.aptNameTextView.text = apt.name
                binding.aptAddressTextView.text = apt.address
                binding.clickListener = clickListener
            }

            binding.buildingImageBtn.visibility = ImageButton.GONE
            binding.aptMapImageBtn.visibility = ImageButton.GONE
            binding.deleteImageBtn.visibility = ImageButton.GONE
            binding.gateCodeImageBtn.visibility = ImageButton.GONE
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

    class MapListener(val clickListener: (apt: Apartment) -> Unit,
                      val gateCodeBtnListener: (apt: Apartment) -> Unit,
                      val deleteBtnListener: (apt: Apartment) -> Unit) {
        fun onClick(apt: Apartment) = clickListener(apt)

        fun onGateCodeBtnClick(apt: Apartment) = gateCodeBtnListener(apt)

        fun onDeleteBtnListener(apt: Apartment) = deleteBtnListener(apt)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apt: Apartment? = getItem(position)

        holder.bind(clickListener, apt, viewMode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, this.parentFragment)
    }

}

