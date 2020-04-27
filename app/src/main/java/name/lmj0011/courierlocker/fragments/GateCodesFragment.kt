package name.lmj0011.courierlocker.fragments


import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
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


/**
 * A simple [Fragment] subclass.
 *
 */
class GateCodesFragment : Fragment() {

    private lateinit var binding: FragmentGateCodesBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: GateCodeViewModelFactory
    private lateinit var listAdapter: GateCodeListAdapter
    private lateinit var gateCodeViewModel: GateCodeViewModel
    private lateinit var sharedPreferences: SharedPreferences
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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).gateCodeDao
        viewModelFactory = GateCodeViewModelFactory(dataSource, application)
        gateCodeViewModel = ViewModelProviders.of(this, viewModelFactory).get(GateCodeViewModel::class.java)

        listAdapter = GateCodeListAdapter( GateCodeListAdapter.GateCodeListener { gateCodeId ->
            this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToEditGateCodeFragment(gateCodeId.toInt()))
        })


        gateCodeViewModel.gateCodes.observe(viewLifecycleOwner, Observer {
            if (!ListLock.isListLocked) {
                it?.let { this.submitListToAdapter(it) }
            }
        })

        LocationHelper.lastLatitude.observe(viewLifecycleOwner, lastLocationListener)

        binding.gateCodesList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))

        binding.gateCodesList.adapter = listAdapter

        binding.gateCodeViewModel = gateCodeViewModel

        binding.lifecycleOwner = this

        binding.liveLocationUpdatingSwitch.setOnCheckedChangeListener { _, isChecked ->
            ListLock.unlock()
            sharedPreferences.edit().apply {
                putBoolean("gateCodesLocationUpdating", isChecked)
                commit()
            }
        }

        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.generateGateCodesBtn.visibility = View.GONE
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

    private fun refreshList() {
        val gcs = gateCodeViewModel.gateCodes.value
        gcs?.let{ this.submitListToAdapter(gcs) }
    }

    private fun submitListToAdapter (list: MutableList<GateCode>) {
        if (binding.liveLocationUpdatingSwitch.isChecked) {
            listAdapter.submitList(listAdapter.filterByClosestGateCodeLocation(list))
            binding.gateCodesList.smoothScrollToPosition(0)
        } else {
            listAdapter.submitList(list)
        }
        listAdapter.notifyDataSetChanged()
    }

    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToCreateGateCodeFragment())
    }

    private fun applyPreferences() {
        binding.liveLocationUpdatingSwitch.isChecked = sharedPreferences.getBoolean("gateCodesLocationUpdating", false)
    }

}
