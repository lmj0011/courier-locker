package name.lmj0011.courierlocker.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.databinding.ListItemTripBinding
import name.lmj0011.courierlocker.helpers.Util
import name.lmj0011.courierlocker.helpers.getTripDate
import name.lmj0011.courierlocker.helpers.metersToMiles

class TripListAdapter(private val clickListener: TripListener): ListAdapter<Trip, TripListAdapter.ViewHolder>(TripDiffCallback()) {
    override fun getItemId(position: Int): Long {
        // return the Item's database row id
        return super.getItem(position).id
    }

    class ViewHolder private constructor(val binding: ListItemTripBinding, val context: Context) : RecyclerView.ViewHolder(binding.root){

        private val googleApiKey = PreferenceManager.getDefaultSharedPreferences(context).getString("advancedDirectionsApiKey", "")!!

        fun bind(clickListener: TripListener, trip: Trip) {
            binding.trip = trip
            binding.clickListener = clickListener
            binding.tripDateTextView.text = HtmlCompat.fromHtml("<b>${getTripDate(trip)}</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.tripPickupAddressTextView.text = HtmlCompat.fromHtml("<b>start:</b> ${trip.pickupAddress}", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.tripDropoffAddressTextView.text = HtmlCompat.fromHtml("<b>end:</b> ${trip.dropOffAddress}", HtmlCompat.FROM_HTML_MODE_LEGACY)

            if(googleApiKey.isNullOrBlank() || trip.distance == 0.0) {
                binding.tripDistanceTextView.visibility = TextView.GONE
            } else {
                binding.tripDistanceTextView.text = HtmlCompat.fromHtml("<b>distance:</b> ${metersToMiles(trip.distance)} mi", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }

            if (trip.payAmount.isNullOrBlank()) {
                binding.tripPayTextView.visibility = TextView.GONE
            } else {
                binding.tripPayTextView.text = HtmlCompat.fromHtml("<b>pay:</b> ${Util.numberFormatInstance.format(trip.payAmount.toDouble())} ", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }

            if (trip.gigName.isNullOrBlank()) {
                binding.tripGigTextView.visibility = TextView.GONE
            } else {
                binding.tripGigTextView.text = HtmlCompat.fromHtml("<b>gig:</b> ${trip.gigName}", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemTripBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding, parent.context)
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