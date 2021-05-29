package name.lmj0011.courierlocker.fragments

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.DeepLinkActivity
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.TripListAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentTripsBinding
import name.lmj0011.courierlocker.factories.TripViewModelFactory
import name.lmj0011.courierlocker.fragments.dialogs.ClearAllTripsDialogFragment
import name.lmj0011.courierlocker.fragments.dialogs.TripsStatsDialogFragment
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.helpers.interfaces.SearchableRecyclerView
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import timber.log.Timber
import java.io.FileOutputStream

/**
 * A simple [Fragment] subclass.
 *
 */
class TripsFragment : Fragment(R.layout.fragment_trips),
    ClearAllTripsDialogFragment.NoticeDialogListener,
    SearchableRecyclerView
{
    private lateinit var binding: FragmentTripsBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: TripViewModelFactory
    private lateinit var tripViewModel: TripViewModel
    private lateinit var listAdapter: TripListAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dateRangePair: androidx.core.util.Pair<Long, Long>
    private var fragmentJob: Job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = requireActivity() as MainActivity
        setHasOptionsMenu(true)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).tripDao
        viewModelFactory = TripViewModelFactory(dataSource, application)
        tripViewModel = ViewModelProviders.of(this, viewModelFactory).get(TripViewModel::class.java)

        mainActivity.showFabAndSetListener({
            findNavController()
                .navigate(TripsFragmentDirections.actionTripsFragmentToCreateTripFragment())
        }, R.drawable.ic_fab_add)
        mainActivity.supportActionBar?.subtitle = null

        setupBinding(view)
        setupObservers()
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
            R.id.action_maps_search -> {
                this@TripsFragment.toggleSearch(mainActivity, binding.tripsSearchView, true)
                true
            }
            R.id.action_trips_export -> {
                val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select date range")
                        .setSelection(
                            androidx.core.util.Pair(
                                MaterialDatePicker.thisMonthInUtcMilliseconds(),
                                MaterialDatePicker.todayInUtcMilliseconds()
                            )
                        )
                        .build()

                dateRangePicker.addOnPositiveButtonClickListener {
                    dateRangePair = it
                    val fileName = "${Util.getDateRangeFileNamePrefix(dateRangePair)}_trips.csv"
                    this.createTripsCsvFile(fileName)
                }

                dateRangePicker.show(childFragmentManager, this::class.simpleName)
                true
            }
            R.id.action_edit_gig_labels -> {
                this.findNavController().navigate(R.id.action_tripsFragment_to_gigLabelsFragment)
                true
            }
            R.id.action_trips_clear_all -> {
                this.showClearAllTripsDialog()
                true
            }
            R.id.action_trips_add_to_home -> {
                if (ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
                    ShortcutInfoCompat.Builder(requireContext(), resources.getString(R.string.shortcut_trips))
                        .setIcon(IconCompat.createWithResource(requireContext(), R.mipmap.ic_trips_shortcut))
                        .setShortLabel("Trips")
                        .setIntent(
                            Intent(requireContext(), DeepLinkActivity::class.java).apply {
                                action = MainActivity.INTENT_SHOW_TRIPS
                            }
                        )
                        .build().also { shortCutInfo ->
                            ShortcutManagerCompat.requestPinShortcut(requireContext(), shortCutInfo, null)
                        }

                } else {
                    mainActivity.showToastMessage(getString(R.string.cant_pinned_shortcuts))
                }
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
                data?.let { intent ->
                    uiScope.launch(Dispatchers.IO) {
                        val trips = tripViewModel.getTripsInDateRange(dateRangePair.first!!, dateRangePair.second!!)
                        val str = Util.getCsvFromTripList(trips)

                        withContext(Dispatchers.Main) {
                            intent.data?.let {
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

    private fun setupBinding(view: View) {
        binding = FragmentTripsBinding.bind(view)

        binding.swipeRefresh.setOnRefreshListener { observeTrips() }

        listAdapter = TripListAdapter(TripListAdapter.TripListener { tripId ->
            this.findNavController().navigate(TripsFragmentDirections.actionTripsFragmentToEditTripFragment(tripId.toInt()))
        })

        binding.tripList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.tripList.adapter = listAdapter

        binding.tripViewModel = tripViewModel

        binding.lifecycleOwner = this


        binding.totalPayTextView.setOnClickListener {
            val dialog = TripsStatsDialogFragment()
            dialog.show(childFragmentManager, "TripsStatsDialogFragment")
        }


        if(!sharedPreferences.getBoolean("enableDebugMode", false)) {
            binding.generateCustomerBtn.visibility = View.GONE
        }

        binding.tripsSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tripViewModel.filterText.postValue(newText)
                return false
            }
        })

        binding.tripsSearchView.setOnCloseListener {
            this@TripsFragment.toggleSearch(mainActivity, binding.tripsSearchView, false)
            false
        }

        binding.tripsSearchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) { } else{
                binding.tripsSearchView.setQuery("", true)
                this@TripsFragment.toggleSearch(mainActivity, binding.tripsSearchView, false)
            }
        }

        updateTodaysTotalMoneyUI()
    }

    private fun setupObservers() {
        observeTrips()
    }

    private fun updateTodaysTotalMoneyUI() {
        launchIO {
            val total = tripViewModel.todayTotalMoney()
            withUIContext {
                binding.totalPayTextView.text = total
            }
        }
    }

    /**
     * starts a new Trips observer, ie. Refreshes the recyclerview
     */
    private fun observeTrips () {
        tripViewModel.tripsPaged.removeObservers(viewLifecycleOwner)

        tripViewModel.tripsPaged.observe(viewLifecycleOwner, {
            listAdapter.submitList(it)
            listAdapter.notifyItemRangeChanged(0, Const.DEFAULT_PAGE_COUNT)
            binding.tripList.scrollToPosition(0)
        })

        binding.swipeRefresh.isRefreshing = false
    }

    private fun showClearAllTripsDialog() {
        // Create an instance of the dialog fragment and show it
        val dialog = ClearAllTripsDialogFragment()
        dialog.show(childFragmentManager, "ClearAllTripsDialogFragment")
    }


    // ref: https://developer.android.com/guide/topics/providers/document-provider#create
    private fun createTripsCsvFile(fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            // Create a file with the requested MIME type.
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        startActivityForResult(intent, MainActivity.TRIPS_WRITE_REQUEST_CODE)
    }
}