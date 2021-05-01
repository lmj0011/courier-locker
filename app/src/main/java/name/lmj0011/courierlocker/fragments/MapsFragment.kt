package name.lmj0011.courierlocker.fragments


import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
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
import name.lmj0011.courierlocker.helpers.interfaces.SearchableRecyclerView
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import org.kodein.di.instance
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 *
 */
class MapsFragment : Fragment(), SearchableRecyclerView {

    private lateinit var binding: FragmentMapsBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var listAdapter: MapListAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModelFactory: ApartmentViewModelFactory
    private lateinit var apartmentViewModel: ApartmentViewModel
    private lateinit var locationHelper: LocationHelper
    private var fragmentJob: Job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    private val scrollListener = object: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            Timber.i("scrollListener called!")
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
            Timber.i("lastLocationListener called!")
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
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        listAdapter = MapListAdapter( MapListAdapter.MapListener(
            {aptId ->
                this.findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToCreateOrEditApartmentMapFragment(aptId))
            },
            {aptId ->
                uiScope.launch{
                    withContext(Dispatchers.IO) {
                        apartmentViewModel.database.deleteByApartmentId(aptId)
                    }
                    listAdapter.notifyDataSetChanged()
                }
            }
        ), this)

        binding.mapList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.mapList.adapter = listAdapter

        binding.lifecycleOwner = this

        apartmentViewModel.apartmentsPaged.observe(viewLifecycleOwner, Observer {
            this.submitListToAdapter(it)
        })

        locationHelper.lastLatitude.observe(viewLifecycleOwner, lastLocationListener)

        binding.liveLocationUpdatingSwitch.setOnCheckedChangeListener { _, isChecked ->
            ListLock.unlock()
            apartmentViewModel.isOrderedByNearest.postValue(isChecked)
            sharedPreferences.edit().apply {
                putBoolean("mapsLocationUpdating", isChecked)
                commit()
            }
        }

        if(!sharedPreferences.getBoolean("enableDebugMode", false)!!) {
            binding.generateMapBtn.visibility = View.GONE
        }

        binding.mapsSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                apartmentViewModel.filterText.postValue(newText)
                return false
            }
        })

        binding.mapsSearchView.setOnCloseListener {
            this@MapsFragment.toggleSearch(mainActivity, binding.mapsSearchView, false)
            false
        }

        binding.mapsSearchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                ListLock.lock()
                binding.mapList.removeOnScrollListener(scrollListener)
            } else{
                binding.mapsSearchView.setQuery("", true)
                this@MapsFragment.toggleSearch(mainActivity, binding.mapsSearchView, false)
                ListLock.unlock()
                binding.mapList.addOnScrollListener(scrollListener)
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            ListLock.unlock()
            this.findNavController().navigate(R.id.mapsFragment)
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
        inflater.inflate(R.menu.maps, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_maps_search -> {
                this@MapsFragment.toggleSearch(mainActivity, binding.mapsSearchView, true)
                true
            }
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
        val isChecked = sharedPreferences.getBoolean("mapsLocationUpdating", false)
        binding.liveLocationUpdatingSwitch.isChecked = isChecked
        apartmentViewModel.isOrderedByNearest.postValue(isChecked)
    }

    private fun refreshList() {
        val apts =  apartmentViewModel.apartmentsPaged.value
        apts?.let{ this.submitListToAdapter(it) }
        binding.swipeRefresh.isRefreshing = false
    }

    private fun submitListToAdapter (list: PagedList<Apartment>) {
        listAdapter.submitList(list)
        listAdapter.notifyDataSetChanged()
    }

}
