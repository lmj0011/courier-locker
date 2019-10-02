package name.lmj0011.courierlocker.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.TripListAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentTripsBinding
import name.lmj0011.courierlocker.factories.TripViewModelFactory
import name.lmj0011.courierlocker.fragments.dialogs.ClearAllTripsDialogFragment
import name.lmj0011.courierlocker.fragments.dialogs.TripsStatsDialogFragment
import name.lmj0011.courierlocker.helpers.getCsvFromTripList
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import timber.log.Timber
import java.io.FileOutputStream

/**
 * A simple [Fragment] subclass.
 *
 */
class TripsFragment : Fragment(),
    ClearAllTripsDialogFragment.NoticeDialogListener,
    TripsStatsDialogFragment.TripsStatsDialogListener
{
    private lateinit var binding: FragmentTripsBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: TripViewModelFactory
    private lateinit var tripViewModel: TripViewModel
    private lateinit var listAdapter: TripListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_trips, container, false)

        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).tripDao
        viewModelFactory = TripViewModelFactory(dataSource, application)
        tripViewModel = ViewModelProviders.of(this, viewModelFactory).get(TripViewModel::class.java)

        listAdapter = TripListAdapter(TripListAdapter.TripListener { tripId ->
            this.findNavController().navigate(TripsFragmentDirections.actionTripsFragmentToEditTripFragment(tripId.toInt()))
        })

        binding.tripList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.tripList.adapter = listAdapter

        binding.tripViewModel = tripViewModel

        binding.lifecycleOwner = this

        tripViewModel.trips.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
            refreshTotals()
        })

        binding.swipeRefresh.setOnRefreshListener {
            this.findNavController().navigate(R.id.tripsFragment)
        }

        binding.totalPayTextView.setOnClickListener {
            val dialog = TripsStatsDialogFragment()
            dialog.show(childFragmentManager, "TripsStatsDialogFragment")
        }


        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.generateCustomerBtn.visibility = View.GONE
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.showFabAndSetListener(this::fabOnClickListenerCallback, R.drawable.ic_fab_add)
        mainActivity.supportActionBar?.title = "Trips"
        mainActivity.supportActionBar?.subtitle = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.trips, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_trips_export -> {
                this.createFile("text/csv", "trips.csv")
                true
            }
            R.id.action_trips_clear_all -> {
                this.showClearAllTripsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            Timber.i("Intent failed! [requestCode: $resultCode, resultCode: $resultCode]")
            return
        }

        when(requestCode) {
            MainActivity.TRIPS_WRITE_REQUEST_CODE -> {
                val trips = this.tripViewModel.trips.value?.toMutableList()
                val str = getCsvFromTripList(trips?.reversed())

                data?.data?.let {
                    // ref: https://developer.android.com/guide/topics/providers/document-provider#edit
                    mainActivity.contentResolver.openFileDescriptor(it, "w")?.use { p ->
                        FileOutputStream(p.fileDescriptor).use { outputStream ->
                            outputStream.write(str.toByteArray())
                        }
                    }
                }
            }
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // User touched the dialog's positive button
        tripViewModel.clearAllTrips()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
    }

    override fun getTripTotals(dialog: DialogFragment): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["today"] = tripViewModel.todayTotalMoney
        map["month"] = tripViewModel.monthTotalMoney
        map["toDate"] = tripViewModel.totalMoney

        return map
    }

    private fun showClearAllTripsDialog() {
        // Create an instance of the dialog fragment and show it
        val dialog = ClearAllTripsDialogFragment()
        dialog.show(childFragmentManager, "ClearAllTripsDialogFragment")
    }

    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(TripsFragmentDirections.actionTripsFragmentToCreateTripFragment())
    }

    // ref: https://developer.android.com/guide/topics/providers/document-provider#create
    private fun createFile(mimeType: String, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            // Create a file with the requested MIME type.
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        startActivityForResult(intent, MainActivity.TRIPS_WRITE_REQUEST_CODE)
    }

    private fun refreshTotals() {
        binding.totalPayTextView.text = tripViewModel.todayTotalMoney
    }
}