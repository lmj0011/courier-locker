package name.lmj0011.courierlocker.fragments


import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_maps.view.*
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.GateCodeListAdapter
import name.lmj0011.courierlocker.adapters.MapListAdapter
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentMapsBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.PlexmapsXmlParser
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import timber.log.Timber
import java.lang.Exception


/**
 * A simple [Fragment] subclass.
 *
 */
class MapsFragment : Fragment() {

    private lateinit var binding: FragmentMapsBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var listAdapter: MapListAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModelFactory: ApartmentViewModelFactory
    private lateinit var apartmentViewModel: ApartmentViewModel
    private var fragmentJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    /**
     * This Observer will cause the recyclerView to refresh itself periodically
     */
    private val latitudeObserver = Observer<Double> {
        apartmentViewModel.apartments.value?.lastOrNull()?.let {
            apartmentViewModel.updateApartment(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_maps, container, false)

        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).apartmentDao
        viewModelFactory = ApartmentViewModelFactory(dataSource, application)
        apartmentViewModel = ViewModelProviders.of(this, viewModelFactory).get(ApartmentViewModel::class.java)

        listAdapter = MapListAdapter( MapListAdapter.MapListener {  }, this)

        apartmentViewModel.apartments.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (binding.liveLocationUpdatingSwitch.isChecked) {
                    listAdapter.submitList(listAdapter.filterByClosestGateCodeLocation(it))
                    binding.mapList.smoothScrollToPosition(0)
                } else {
                    listAdapter.submitList(it)
                }
            }
        })

        LocationHelper.lastLatitude.observe(viewLifecycleOwner, latitudeObserver)

        binding.mapList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.mapList.adapter = listAdapter

        binding.lifecycleOwner = this

        binding.liveLocationUpdatingSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().apply {
                putBoolean("mapsLocationUpdating", isChecked)
                commit()
            }
        }

        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.generateMapBtn.visibility = View.GONE
        }

        binding.swipeRefresh.setOnRefreshListener {
            apartmentViewModel.deleteAll()
            this.refreshMapList()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.hideFab()
        mainActivity.supportActionBar?.subtitle = null
        this.applyPreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_map_feeds, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_edit_map_feeds -> {
                this.findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToEditMapFeedsFragment())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun applyPreferences() {
        binding.liveLocationUpdatingSwitch.isChecked = sharedPreferences.getBoolean("mapsLocationUpdating", false)
    }

    private fun refreshMapList() {
        binding.swipeRefresh.isRefreshing = true

        val str = sharedPreferences.getString(resources.getString(R.string.sp_key_map_feed_list), "")!!
        val feedList = str.lines().filter {
            !it.isNullOrBlank()
        }

        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val psr = PlexmapsXmlParser()
                    val apts = psr.parseFeeds(feedList.toTypedArray()).toMutableList()

                    withContext(Dispatchers.Main) {
                        apartmentViewModel.insertApartments(apts)
                        binding.swipeRefresh.isRefreshing = false
                    }
                }
            } catch (ex: Exception) {
                mainActivity.showToastMessage("Map Feed Error\n\n${ex.message.toString()}")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }

        }

    }

}
