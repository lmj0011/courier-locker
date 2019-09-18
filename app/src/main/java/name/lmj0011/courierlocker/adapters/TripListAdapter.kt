package name.lmj0011.courierlocker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.databinding.ListItemTripBinding

class TripListAdapter(private val clickListener: TripListener): ListAdapter<Trip, TripListAdapter.ViewHolder>(TripDiffCallback()) {
    override fun getItemId(position: Int): Long {
        // return the Item's database row id
        return super.getItem(position).id
    }

    class ViewHolder private constructor(val binding: ListItemTripBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(clickListener: TripListener, trip: Trip) {
            binding.trip = trip
            binding.clickListener = clickListener
            binding.tripDateTextView.text = HtmlCompat.fromHtml("<b>${trip.date}</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.tripPickupAddressTextView.text = HtmlCompat.fromHtml("<b>pickup:</b> ${trip.pickupAddress}", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.tripDropoffAddressTextView.text = HtmlCompat.fromHtml("<b>drop-off:</b> ${trip.dropOffAddress}", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.tripMilesTextView.text = HtmlCompat.fromHtml("<b>distance:</b> $${trip.distance}", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.tripPayTextView.text = HtmlCompat.fromHtml("<b>pay:</b> $${trip.payAmount}", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.tripGigTextView.text = HtmlCompat.fromHtml("<b>gig:</b> ${trip.gigName}", HtmlCompat.FROM_HTML_MODE_LEGACY)

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemTripBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }

    class TripDiffCallback : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem == newItem
        }
    }

    class TripListener(val clickListener: (tripId: Long) -> Unit) {
        fun onClick(trip: Trip) = clickListener(trip.id)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = getItem(position)

        holder.bind(clickListener, trip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }
}