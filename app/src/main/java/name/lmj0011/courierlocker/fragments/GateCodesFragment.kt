package name.lmj0011.courierlocker.fragments


import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
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
import com.google.android.gms.location.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.GateCodeAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentGateCodesBinding
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.factories.GateCodeViewModelFactory
import name.lmj0011.courierlocker.helpers.GeoLocation
import name.lmj0011.courierlocker.helpers.ItemTouchHelperClass
import name.lmj0011.courierlocker.helpers.LocationHelper
import timber.log.Timber
import java.io.IOException
import java.util.*


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
    private lateinit var locationCallback: LocationCallback

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
                adapter.submitList(it)
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

        locationCallback = this.getLocationCallback()

        val permissionVal = ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        when{
            permissionVal != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(mainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MainActivity.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
            else -> {/* should not make it to here */}
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
                    LocationHelper.startLocationUpdates(locationCallback)
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        LocationHelper.stopLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        mainActivity.showFabAndSetListener(this::fabOnClickListenerCallback, R.drawable.ic_fab_add)
        mainActivity.supportActionBar?.title = "Gate Codes"
        mainActivity.supportActionBar?.subtitle = null

        when(ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION)){
            PackageManager.PERMISSION_GRANTED -> LocationHelper.startLocationUpdates(locationCallback)
            else -> LocationHelper.stopLocationUpdates(locationCallback)
        }


    }


    private fun fabOnClickListenerCallback() {
        this.findNavController().navigate(GateCodesFragmentDirections.actionGateCodesFragmentToCreateGateCodeFragment())
    }


    private fun getLocationCallback(): LocationCallback
    {
        return object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                var addresses: List<Address> = emptyList()

                if (locationResult.locations.isNotEmpty()) {
                    // get latest location
                    val location = locationResult.lastLocation

                    // TODO profile and determine whether to move this logic into a Worker or Coroutine
                    val geocoder = Geocoder(mainActivity, Locale.getDefault())

//                    val geolocation = GeoLocation.fromDegrees(location.latitude, location.longitude)
//                    val boundingBox = geolocation.boundingCoordinates(10.toDouble(), 3958.8) // numbers are in miles

                    try {
                        addresses = geocoder.getFromLocation(location.latitude,location.longitude,1)
//                        addresses = geocoder.getFromLocationName(
//                            "204 Royal Pines",
//                            1,
//                            boundingBox[0].latitudeInDegrees,
//                            boundingBox[0].longitudeInDegrees,
//                            boundingBox[1].latitudeInDegrees,
//                            boundingBox[1].longitudeInDegrees
//                        )
                    } catch (ioException: IOException) {
                        // Catch network or other I/O problems.
                        Timber.e(ioException.message)
                    } catch (illegalArgumentException: IllegalArgumentException) {
                        // Catch invalid latitude or longitude values.
                        Timber.e(illegalArgumentException.message)
                    }

                    // Handle case where no address was found.
                    if (addresses.isEmpty()) {
                        Timber.i("lat:${location.latitude}, long: ${location.longitude}, address: n/a")
                    } else {
                        val address = addresses[0]
                        Timber.i("lat:${location.latitude}, long: ${location.longitude}, address: ${address.getAddressLine(0)}")
                    }
                }


            }
        }

    }

}
