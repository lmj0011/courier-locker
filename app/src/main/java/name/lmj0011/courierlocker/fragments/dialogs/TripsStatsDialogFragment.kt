package name.lmj0011.courierlocker.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.factories.TripViewModelFactory
import name.lmj0011.courierlocker.helpers.launchIO
import name.lmj0011.courierlocker.helpers.withUIContext
import name.lmj0011.courierlocker.viewmodels.TripViewModel

class TripsStatsDialogFragment : DialogFragment() {
    lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let{
            val builder = MaterialAlertDialogBuilder(requireContext())

            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_trips_stats, null)

            dialogView = view
            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onResume() {
        super.onResume()
        val dataSource = CourierLockerDatabase.getInstance(requireActivity().application).tripDao
        val viewModelFactory = TripViewModelFactory(dataSource, requireActivity().application)
        val tripViewModel = ViewModelProviders.of(this, viewModelFactory).get(TripViewModel::class.java)

        launchIO {
            val totalToday = tripViewModel.todayTotalMoney()
            val totalWeek = tripViewModel.weekTotalMoney()
            val totalMonth = tripViewModel.monthTotalMoney()
            val totalYearToDate = tripViewModel.yearToDateTotalMoney()

            withUIContext {
                dialogView.findViewById<TextView>(R.id.todayTotalPayTextView).text = "Today: $totalToday"
                dialogView.findViewById<TextView>(R.id.weekTotalPayTextView).text = "This Week: $totalWeek"
                dialogView.findViewById<TextView>(R.id.monthTotalPayTextView).text = "This Month: $totalMonth"
                dialogView.findViewById<TextView>(R.id.yearToDateTextView).text = "YTD: $totalYearToDate"
            }
        }
    }

}