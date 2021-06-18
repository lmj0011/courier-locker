package name.lmj0011.courierlocker.fragments


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.DeepLinkActivity
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.MapListAdapter
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentMapsBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.helpers.*
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
    private lateinit var preferences: PreferenceHelper
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
            this.refreshList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_maps, container, false)

        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        val application = requireActivity().application as CourierLockerApplication
        preferences = application.kodein.instance()
        val dataSource = CourierLockerDatabase.getInstance(application).apartmentDao
        viewModelFactory = ApartmentViewModelFactory(dataSource, application)
        apartmentViewModel = ViewModelProvider(this, viewModelFactory).get(ApartmentViewModel::class.java)
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        listAdapter = MapListAdapter( MapListAdapter.MapListener(
            {apt ->
                this.findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToCreateOrEditApartmentMapFragment(apt.id))
            },
            { apt ->
                launchIO {
                    val gateCode = apartmentViewModel.getRelatedGateCode(apt.gateCodeId)

                    gateCode?.also {
                        withUIContext {
                            MaterialAlertDialogBuilder(binding.root.context)
                                .setTitle("Gate Codes")
                                .setMessage("${apt.name}\n\n${gateCode.codes.joinToString(", ")}")
                                .setPositiveButton("Ok") { _, _ ->}
                                .setNeutralButton("Disassociate") { dialog0, _ ->
                                    dialog0.dismiss()
                                    MaterialAlertDialogBuilder(binding.root.context)
                                        .setTitle("Disassociate Gate Codes?")
                                        .setMessage("${apt.name}\n\n${gateCode.codes.joinToString(", ")}")
                                        .setPositiveButton("Yes") { dialog1, _ ->
                                            dialog1.dismiss()
                                            apt.gateCodeId = 0
                                            apartmentViewModel.updateApartment(apt)
                                            findNavController().navigate(R.id.mapsFragment)
                                        }
                                        .setNeutralButton("Cancel") {dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .show()
                                }
                                .show()
                        }
                    }
                }

            },
            {apt ->
                uiScope.launch{
                    withContext(Dispatchers.IO) {
                        apartmentViewModel.database.deleteByApartmentId(apt.id)
                    }
                    listAdapter.notifyDataSetChanged()
                }
            }
        ), this,
            MapListAdapter.VIEW_MODE_NORMAL
        )

        binding.mapList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.mapList.adapter = listAdapter

        binding.lifecycleOwner = this

        apartmentViewModel.apartmentsPaged.observe(viewLifecycleOwner, {
            listAdapter.submitData(viewLifecycleOwner.lifecycle, it)
            listAdapter.notifyItemRangeChanged(0, Const.DEFAULT_PAGE_COUNT)
            binding.mapList.scrollToPosition(0)
        })

        locationHelper.lastLatitude.observe(viewLifecycleOwner, lastLocationListener)

        binding.liveLocationUpdatingSwitch.setOnCheckedChangeListener { _, isChecked ->
            ListLock.unlock()
            apartmentViewModel.isOrderedByNearest.postValue(isChecked)
            preferences.mapsIsOrderedByNearest = isChecked
        }

        if(!preferences.devControlsEnabled) {
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
            R.id.action_maps_add_to_home -> {
                if (ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
                    ShortcutInfoCompat.Builder(requireContext(), resources.getString(R.string.shortcut_maps))
                        .setIcon(IconCompat.createWithResource(requireContext(), R.mipmap.ic_maps_shortcut))
                        .setShortLabel("Maps")
                        .setIntent(
                            Intent(requireContext(), DeepLinkActivity::class.java).apply {
                                action = MainActivity.INTENT_SHOW_MAPS
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

    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToCreateOrEditApartmentMapFragment(0L))
    }

    private fun applyPreferences() {
        val isChecked = preferences.mapsIsOrderedByNearest
        binding.liveLocationUpdatingSwitch.isChecked = isChecked
        apartmentViewModel.isOrderedByNearest.postValue(isChecked)
    }

    private fun refreshList() {
        binding.swipeRefresh.isRefreshing = false
    }


}
