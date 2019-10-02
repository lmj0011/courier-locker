package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import name.lmj0011.courierlocker.R

class TripsStatsDialogFragment : DialogFragment() {
    // Use this instance of the interface to deliver action events
    private lateinit var listener: TripsStatsDialogListener

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface TripsStatsDialogListener {
        fun getTripTotals(dialog: DialogFragment): Map<String, String>
    }

    // Override the Fragment.onAttach() method to instantiate the TripsStatsDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the TripsStatsDialogListener so we can send events to the host
            listener = parentFragment as TripsStatsDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(("$parentFragment must implement TripsStatsDialogListener"))
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let{
            val builder = AlertDialog.Builder(it)

            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_trips_stats, null)

            view.findViewById<TextView>(R.id.todayTotalPayTextView).text = "Today: ${listener.getTripTotals(this)["today"]}"
            view.findViewById<TextView>(R.id.monthTotalPayTextView).text = "This Month: ${listener.getTripTotals(this)["month"]}"
            view.findViewById<TextView>(R.id.toDateTotalPayTextView).text = "To Date: ${listener.getTripTotals(this)["toDate"]}"

            builder.setView(view)

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}