package name.lmj0011.courierlocker.adapters

import android.content.Context
import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView

/**
 * ref: https://www.truiton.com/2018/06/android-autocompletetextview-suggestions-from-webservice-call/
 * ref: https://github.com/Truiton/AutoSuggestTextViewAPICall/blob/master/app/src/main/java/com/truiton/autosuggesttextviewapicall/AutoSuggestAdapter.java
 */

class AddressAutoSuggestAdapter(context: Context, res: Int) : ArrayAdapter<Address>(context, res), Filterable {

    private val listData: MutableList<Address> = mutableListOf()

    companion object {
        class ViewHolder(val text1: TextView)
    }


    fun setData(list: MutableList<Address>){
        listData.clear()
        listData.addAll(list)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val address = listData[position]
        var cv = convertView

        when{
            cv == null -> {
                // Check if an existing view is being reused, otherwise inflate the view
                cv = LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
                val itemView = cv.findViewById<TextView>(android.R.id.text1)
                val viewHolder = ViewHolder(itemView)

                viewHolder.text1.text = address.getAddressLine(0)
                cv.tag = viewHolder // view lookup cache stored in tag
            }
            cv.tag is ViewHolder -> {
                val viewHolder = cv.tag as ViewHolder
                viewHolder.text1.text = address.getAddressLine(0)
            }
            else -> {
                val itemView = cv.findViewById<TextView>(android.R.id.text1)
                itemView.text = address.getAddressLine(0)
            }
        }

        return cv!!
    }

    override fun getCount(): Int {
        return listData.size
    }

    override fun getItem(position: Int): Address? {
        return listData[position]
    }

    override fun getFilter(): Filter {
        return object: Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (constraint != null) {
                    filterResults.values = listData
                    filterResults.count = listData.size
                }

                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && (results.count > 0)) {
                    this@AddressAutoSuggestAdapter.notifyDataSetChanged()
                } else {
                    this@AddressAutoSuggestAdapter.notifyDataSetInvalidated()
                }
            }
        }
    }
}