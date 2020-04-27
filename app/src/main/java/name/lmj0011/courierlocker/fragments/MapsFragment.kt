package name.lmj0011.courierlocker.fragments


import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.MapListAdapter
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentMapsBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.helpers.ListLock
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.PermissionHelper
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel


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
    private var fragmentJob: Job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    private val scrollListener = object: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            // unlock list when scrolled all the way to the top
            if (!recyclerView.canScrollVertically(-1)) {
                ListLock.unlock()
            } else {
                ListLock.lock()
            }
        }
    }

    /**
     * This Observer will cause the recyclerView to refresh itself periodically
     */
    private val lastLocationListener = Observer<Double> {
        if(!ListLock.isListLocked) {
            this.refreshList()
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

        listAdapter = MapListAdapter( MapListAdapter.MapListener(
            {aptId ->
                this.findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToCreateOrEditApartmentMapFragment(aptId))
            },
            {aptId ->
                uiScope.launch{
                    withContext(Dispatchers.IO) {
                        apartmentViewModel.database.deleteByApartmentId(aptId)
                    }
                }
            }
        ), this)

        binding.mapList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.mapList.adapter = listAdapter

        binding.lifecycleOwner = this

        apartmentViewModel.apartments.observe(viewLifecycleOwner, Observer {
            if (!ListLock.isListLocked) {
                it?.let {
                    this.submitListToAdapter(it)
                }
            }
        })

        LocationHelper.lastLatitude.observe(viewLifecycleOwner, lastLocationListener)

        binding.liveLocationUpdatingSwitch.setOnCheckedChangeListener { _, isChecked ->
            ListLock.unlock()
            sharedPreferences.edit().apply {
                putBoolean("mapsLocationUpdating", isChecked)
                commit()
            }
        }

        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.generateMapBtn.visibility = View.GONE
        }

        binding.swipeRefresh.setOnRefreshListener {
            ListLock.unlock()
            this.findNavController().navigate(R.id.mapsFragment)

//            TODO implement some sort of apt merge strategy
//            apartmentViewModel.apartments.value?.let {
//                val apts = it.filter {apt -> // gather apts that are from external sources
//                    !apt.sourceUrl.isNullOrBlank()
//                }.toMutableList()
//
//                apartmentViewModel.deleteAll(apts)
//            }
//
//            this.refreshList()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        if(!PermissionHelper.permissionAccessFineLocationApproved) {
            PermissionHelper.requestFineLocationAccess(mainActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        mainActivity.showFabAndSetListener(this::fabOnClickListenerCallback, R.drawable.ic_fab_add)
        mainActivity.supportActionBar?.subtitle = null
        this.applyPreferences()
        this.refreshList()
        binding.mapList.addOnScrollListener(scrollListener)
    }

    override fun onPause() {
        super.onPause()
        binding.mapList.removeOnScrollListener(scrollListener)
        ListLock.unlock()
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob?.cancel()
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

    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToCreateOrEditApartmentMapFragment(0L))
    }

    private fun applyPreferences() {
        binding.liveLocationUpdatingSwitch.isChecked = sharedPreferences.getBoolean("mapsLocationUpdating", false)
    }

    private fun refreshList() {
        val apts =  apartmentViewModel.apartments.value
        apts?.let{ this.submitListToAdapter(it) }
        binding.swipeRefresh.isRefreshing = false

//            TODO implement some sort of apt merge strategy
//        binding.swipeRefresh.isRefreshing = true
//
//        val str = sharedPreferences.getString(resources.getString(R.string.sp_key_map_feed_list), "")!!
//        val feedList = str.lines().filter {
//            !it.isNullOrBlank()
//        }
//
//        uiScope.launch {
//            try {
//                withContext(Dispatchers.IO) {
//                    val psr = PlexmapsXmlParser()
//                    val apts = psr.parseFeeds(arrayOf("https://courierlocker.org/plexmaps/feed")).toMutableList()
//
//                    withContext(Dispatchers.Main) {
//                        apartmentViewModel.insertApartments(apts)
//                        binding.swipeRefresh.isRefreshing = false
//                    }
//                }
//            } catch (ex: Exception) {
//                mainActivity.showToastMessage("Map Feed Error\n\n${ex.message.toString()}")
//            } finally {
//                binding.swipeRefresh.isRefreshing = false
//            }
//
//        }

    }

    private fun submitListToAdapter (list: MutableList<Apartment>) {
        if (binding.liveLocationUpdatingSwitch.isChecked) {
            listAdapter.submitList(listAdapter.filterByClosestGateCodeLocation(list))
            binding.mapList.smoothScrollToPosition(0)
        } else {
            listAdapter.submitList(list)
        }
        listAdapter.notifyDataSetChanged()
    }

}
