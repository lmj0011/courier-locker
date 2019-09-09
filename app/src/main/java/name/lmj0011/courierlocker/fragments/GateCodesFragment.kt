package name.lmj0011.courierlocker.fragments


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.GateCodeAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentGateCodesBinding
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory
import name.lmj0011.courierlocker.helpers.ItemTouchHelperClass
import name.lmj0011.courierlocker.helpers.LocationHelper
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 *
 */
class GateCodesFragment : Fragment() {

    private lateinit var binding: FragmentGateCodesBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: GateCodeViewModelFactory
    private lateinit var adapter: GateCodeAdapter
    private lateinit var gateCodeViewModel: GateCodeViewModel

    private val onSwipedCallback: (RecyclerView.ViewHolder, Int) -> Unit = { viewHolder, _ ->
        val gateCodeId = adapter.getItemId(viewHolder.adapterPosition)

        gateCodeViewModel.deleteGateCode(gateCodeId)
        Toast.makeText(context, "Deleted a gate code entry", Toast.LENGTH_SHORT).show()
        adapter.notifyDataSetChanged()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_gate_codes, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).gateCodeDao
        viewModelFactory = GateCodeViewModelFactory(dataSource, application)
        gateCodeViewModel = ViewModelProviders.of(this, viewModelFactory).get(GateCodeViewModel::class.java)

        adapter = GateCodeAdapter( GateCodeAdapter.GateCodeListener { gateCodeId ->
            this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToEditGateCodeFragment(gateCodeId.toInt()))
        })

        // Create an observer on gateCodeViewModel.gateCodes that tells
        // the Adapter when there is new data.
        gateCodeViewModel.gateCodes.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(adapter.filterByClosestGateCodeLocation(it))
            }
        })

        val itemTouchHelperCallback = ItemTouchHelperClass(mainActivity, this.onSwipedCallback).swipeLeftToDeleteCallback

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.gateCodesList)

        binding.gateCodesList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        binding.gateCodesList.adapter = adapter

        binding.gateCodeViewModel = gateCodeViewModel

        binding.lifecycleOwner = this

        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.generateGateCodesBtn.visibility = View.GONE
        }

        val permissionVal = ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        when{
            permissionVal != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(mainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MainActivity.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
            else -> {/* nothing */}
        }


        return binding.root
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            MainActivity.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == 0) {
                    LocationHelper.startLocationUpdates()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainActivity.showFabAndSetListener(this::fabOnClickListenerCallback, R.drawable.ic_fab_add)
        mainActivity.supportActionBar?.title = "Gate Codes"
        mainActivity.supportActionBar?.subtitle = null

        when(ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION)){
            PackageManager.PERMISSION_GRANTED -> LocationHelper.startLocationUpdates()
            else -> {/* nothing */}
        }


    }


    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToCreateGateCodeFragment())
    }


}
