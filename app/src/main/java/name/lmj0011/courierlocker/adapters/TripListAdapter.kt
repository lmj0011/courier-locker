package name.lmj0011.courierlocker.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.paging.PagedListAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.databinding.ListItemTripBinding
import name.lmj0011.courierlocker.helpers.Util

class TripListAdapter(private val clickListener: TripListener): PagedListAdapter<Trip, TripListAdapter.ViewHolder>(TripDiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemTripBinding, val context: Context) : RecyclerView.ViewHolder(binding.root){

        private val googleApiKey = when(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("googleDirectionsKey", false)) {
            true -> context.resources.getString(R.string.google_directions_key)
            else -> ""
        }

        fun bind(clickListener: TripListener, trip: Trip?) {
            trip?.let {
                binding.trip = trip
                binding.clickListener = clickListener
                binding.tripDateTextView.text = HtmlCompat.fromHtml("<b>${Util.getTripDate(trip)}</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                binding.tripPickupAddressTextView.text = HtmlCompat.fromHtml("<b>start:</b> ${trip.pickupAddress}", HtmlCompat.FROM_HTML_MODE_LEGACY)
                binding.tripDropoffAddressTextView.text = HtmlCompat.fromHtml("<b>end:</b> ${trip.dropOffAddress}", HtmlCompat.FROM_HTML_MODE_LEGACY)

                if(googleApiKey.isNullOrBlank()) {
                    binding.tripDistanceTextView.visibility = TextView.GONE
                } else {
                    binding.tripDistanceTextView.text = HtmlCompat.fromHtml("<b>distance:</b> ${Util.metersToMiles(trip.distance)} mi", HtmlCompat.FROM_HTML_MODE_LEGACY)
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
        val trip: Trip? = getItem(position)

        holder.bind(clickListener, trip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    fun filterBySearchQuery(query: String?, list: MutableList<Trip>): MutableList<Trip> {
        if (query.isNullOrBlank()) return list

        return list.filter {
            val inDate = Util.getTripDate(it).contains(query, true)
            val inPickupAddress = it.pickupAddress.contains(query, true)
            val inDropOffAddress = it.dropOffAddress.contains(query, true)
            val inPayAmount = it.payAmount.contains(query, true)
            val inGigName = it.gigName.contains(query, true)

            return@filter inGigName || inPickupAddress || inDropOffAddress|| inPayAmount || inDate
        }.toMutableList()
    }
}