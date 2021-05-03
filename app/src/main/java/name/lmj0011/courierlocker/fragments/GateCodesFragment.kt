package name.lmj0011.courierlocker.fragments


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.DeepLinkActivity
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.GateCodeListAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.databinding.FragmentGateCodesBinding
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory
import name.lmj0011.courierlocker.helpers.ListLock
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.interfaces.SearchableRecyclerView
import org.kodein.di.instance

/**
 * A simple [Fragment] subclass.
 *
 */
class GateCodesFragment : Fragment(), SearchableRecyclerView {

    private lateinit var binding: FragmentGateCodesBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: GateCodeViewModelFactory
    private lateinit var listAdapter: GateCodeListAdapter
    private lateinit var gateCodeViewModel: GateCodeViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var locationHelper: LocationHelper
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
            inflater, R.layout.fragment_gate_codes, container, false)

        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).gateCodeDao
        viewModelFactory = GateCodeViewModelFactory(dataSource, application)
        gateCodeViewModel = ViewModelProviders.of(this, viewModelFactory).get(GateCodeViewModel::class.java)
        locationHelper = (requireContext().applicationContext as CourierLockerApplication).kodein.instance()

        listAdapter = GateCodeListAdapter( GateCodeListAdapter.GateCodeListener { gateCodeId ->
            this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToEditGateCodeFragment(gateCodeId.toInt()))
        })

        gateCodeViewModel.gatecodesPaged.observe(viewLifecycleOwner, Observer {
            this.submitListToAdapter(it)
        })

        locationHelper.lastLatitude.observe(viewLifecycleOwner, lastLocationListener)

        binding.gateCodesList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.gateCodesList.adapter = listAdapter

        binding.gateCodeViewModel = gateCodeViewModel

        binding.lifecycleOwner = this

        binding.liveLocationUpdatingSwitch.setOnCheckedChangeListener { _, isChecked ->
            ListLock.unlock()
            gateCodeViewModel.isOrderedByNearest.postValue(isChecked)
            sharedPreferences.edit().apply {
                putBoolean("gateCodesLocationUpdating", isChecked)
                commit()
            }
        }

        if(!sharedPreferences.getBoolean("enableDebugMode", false)) {
            binding.generateGateCodesBtn.visibility = View.GONE
        }

        binding.gateCodesSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                gateCodeViewModel.filterText.postValue(newText)
                return false
            }
        })

        binding.gateCodesSearchView.setOnCloseListener {
            this@GateCodesFragment.toggleSearch(mainActivity, binding.gateCodesSearchView, false)
            false
        }

        binding.gateCodesSearchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                ListLock.lock()
                binding.gateCodesList.removeOnScrollListener(scrollListener)
            } else{
                binding.gateCodesSearchView.setQuery("", true)
                this@GateCodesFragment.toggleSearch(mainActivity, binding.gateCodesSearchView, false)
                ListLock.unlock()
                binding.gateCodesList.addOnScrollListener(scrollListener)
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.showFabAndSetListener(this::fabOnClickListenerCallback, R.drawable.ic_fab_add)
        mainActivity.supportActionBar?.subtitle = null
        this.applyPreferences()
        this.refreshList()
        binding.gateCodesList.addOnScrollListener(scrollListener)
    }

    override fun onPause() {
        super.onPause()
        binding.gateCodesList.removeOnScrollListener(scrollListener)
        ListLock.unlock()
    }
    override fun onDestroy() {
        super.onDestroy()
        fragmentJob?.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.gatecodes, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_gate_codes_search -> {
                this@GateCodesFragment.toggleSearch(mainActivity, binding.gateCodesSearchView, true)
                true
            }
            R.id.action_gate_codes_add_to_home -> {
                if (ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
                    ShortcutInfoCompat.Builder(requireContext(), resources.getString(R.string.shortcut_gatecodes))
                        .setIcon(IconCompat.createWithResource(requireContext(), R.mipmap.ic_gatecodes_shortcut))
                        .setShortLabel("Gate Codes")
                        .setIntent(
                            Intent(requireContext(), DeepLinkActivity::class.java).apply {
                                action = MainActivity.INTENT_SHOW_GATE_CODES
                                putExtra("menuItemId", R.id.nav_gate_codes)
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

    private fun refreshList() {
        val gcs = gateCodeViewModel.gatecodesPaged.value
        gcs?.let{ this.submitListToAdapter(gcs) }
    }

    private fun submitListToAdapter (list: PagedList<GateCode>) {
        listAdapter.submitList(list)
        listAdapter.notifyDataSetChanged()
    }

    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToCreateGateCodeFragment())
    }

    private fun applyPreferences() {
        val isChecked = sharedPreferences.getBoolean("gateCodesLocationUpdating", false)
        binding.liveLocationUpdatingSwitch.isChecked = isChecked
        gateCodeViewModel.isOrderedByNearest.postValue(isChecked)
    }

}
