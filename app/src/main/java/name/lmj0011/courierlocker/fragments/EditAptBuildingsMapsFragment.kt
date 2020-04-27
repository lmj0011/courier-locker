package name.lmj0011.courierlocker.fragments


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.*
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.google.maps.android.ui.IconGenerator.STYLE_PURPLE
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.Building
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentCreateOrEditApartmentMapBinding
import name.lmj0011.courierlocker.databinding.FragmentEditAptBuildingsMapBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.helpers.AptBldgClusterItem
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import java.lang.Exception


/**
 * A simple [Fragment] subclass.
 *
 */
class EditAptBuildingsMapsFragment : Fragment(){

    private lateinit var binding: FragmentEditAptBuildingsMapBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: ApartmentViewModelFactory
    private lateinit var apartmentViewModel: ApartmentViewModel
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var gMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<AptBldgClusterItem>
    private lateinit var pinDropMarker: Marker
    private var selectedApt = MutableLiveData<Apartment>()
    private var selectedBldg: Building? = null
    private var fragmentJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity = activity as MainActivity

        try {
            binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_edit_apt_buildings_map, container, false)
        } catch (ex: Exception) {
            // hacky work-around to Google Maps SDK crashing
            // ref: https://issuetracker.google.com/issues/154855417
            mainActivity.findNavController(R.id.navHostFragment).navigate(R.id.mapsFragment)
            mainActivity.showToastMessage("Problem loading the Google Map, please try again later.")

            val binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_create_or_edit_apartment_map, container, false
            ) as FragmentCreateOrEditApartmentMapBinding

            return binding.root
        }

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).apartmentDao
        viewModelFactory = ApartmentViewModelFactory(dataSource, application)
        val args = EditAptBuildingsMapsFragmentArgs.fromBundle(arguments!!)
        apartmentViewModel = ViewModelProviders.of(this, viewModelFactory).get(ApartmentViewModel::class.java)
        mapFragment = childFragmentManager.findFragmentById(R.id.editAptBuildingsMapFragment) as SupportMapFragment

        selectedApt.observe(viewLifecycleOwner, Observer {
            if(it.buildings.isNotEmpty()) {
                mainActivity.supportActionBar?.subtitle = it.name
            }
            this.refreshMap()
        })

        uiScope.launch {
            withContext(Dispatchers.IO) {
                arguments?.let {
                    val apt = apartmentViewModel.database.get(args.aptId)!!
                    selectedApt.postValue(apt)
                }
            }
        }

        mapFragment.getMapAsync { map ->
            gMap = map
            clusterManager = ClusterManager(mainActivity, gMap)

            val renderer = object: DefaultClusterRenderer<AptBldgClusterItem>(mainActivity, gMap, clusterManager) {
                private val ig = IconGenerator(mainActivity)

                init {
                    ig.setStyle(STYLE_PURPLE)
                }

                override fun onBeforeClusterItemRendered(item: AptBldgClusterItem, markerOptions: MarkerOptions) {
                    val iconBitmap = ig.makeIcon(item.bldg.number)

                    markerOptions.position(item.position)
                    markerOptions.title(item.title)
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconBitmap))
                }

            }

            clusterManager.renderer = renderer

            gMap.isMyLocationEnabled = true
            gMap.uiSettings.isMapToolbarEnabled = true /* doesn't seem to work with map clustering */

            gMap.setOnCameraIdleListener(clusterManager)
            gMap.setOnMarkerClickListener(clusterManager)

            gMap.setOnMapClickListener {
                this.hideEditUI()
            }

            gMap.setOnMapLongClickListener {
                pinDropMarker.position = it
                pinDropMarker.isVisible = true
                selectedBldg = Building("", it.latitude, it.longitude)
                this.showEditUI(null)
            }

            clusterManager.setOnClusterItemClickListener {
                val marker = renderer.getMarker(it)
                marker.showInfoWindow()
                selectedBldg = it.bldg
                pinDropMarker.isVisible = false
                this.showEditUI(it.bldg)

                true
            }

            clusterManager.setOnClusterClickListener {
                // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
                // inside of bounds, then animate to center of the bounds.

                // Create the builder to collect all essential cluster items for the bounds.
                // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
                // inside of bounds, then animate to center of the bounds.

                // Create the builder to collect all essential cluster items for the bounds.
                val builder = LatLngBounds.builder()
                for (item in it.items) {
                    builder.include(item.position)
                }

                // Get the LatLngBounds
                val bounds = builder.build()

                // Animate camera to zoom to center to the bound of clusters
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, gMap.cameraPosition.zoom + 1.5f))

                true
            }

            gMap.moveCamera(CameraUpdateFactory.zoomTo(16f))
        }

        binding.addButton.setOnClickListener {
            uiScope.launch {
                val b = Building(
                    binding.buildingEditText.text.toString(),
                    pinDropMarker.position.latitude,
                    pinDropMarker.position.longitude
                )

                selectedBldg = b

                selectedApt.value?.let { apt ->
                    apt.buildings.add(b)

                    withContext(Dispatchers.IO) {
                        apartmentViewModel.updateApartment(apt)
                        selectedApt.postValue(apt)
                    }
                }

                mainActivity.hideKeyBoard(binding.buildingEditText)
            }
        }

        binding.navToBldgImageButton.setOnClickListener {
            val gmmIntentUri = Uri.parse("google.navigation:q=${selectedBldg?.latitude},${selectedBldg?.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        binding.removeButton.setOnClickListener {
            uiScope.launch {
                selectedBldg?.let{ selectedApt.value!!.buildings.remove(it) }

                withContext(Dispatchers.IO) {
                    apartmentViewModel.updateApartment(selectedApt.value)
                    selectedApt.postValue(selectedApt.value)
                }

                mainActivity.showToastMessage("removed building: ${selectedBldg?.number}")
            }
        }

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.hideFab()
        mainActivity.supportActionBar?.subtitle = "Long press to start marking building(s)"
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

    private fun applyPreferences() { }


    private fun refreshMap() {
        gMap.clear()
        clusterManager.clearItems()

        pinDropMarker = gMap.addMarker(
            MarkerOptions()
                .position(LatLng(0.0,0.0))
                .visible(false)
        )

        selectedApt.value!!.buildings.forEach { bldg ->
            bldg?.let{
                val item = AptBldgClusterItem(it)
                clusterManager.addItem(item)
            }
        }

        clusterManager.cluster()

        when(selectedBldg) {
            is Building -> {
                gMap.moveCamera(CameraUpdateFactory.newLatLng(
                    LatLng(selectedBldg!!.latitude, selectedBldg!!.longitude)
                ))

            }
            else -> {
                gMap.moveCamera(CameraUpdateFactory.newLatLng(
                    LatLng(selectedApt.value!!.latitude, selectedApt.value!!.longitude)
                ))
            }
        }

        this.hideEditUI()
    }

    private fun hideEditUI() {
        val transition = Slide(Gravity.TOP)
        transition.duration = 50
        transition.addTarget(binding.inputView)
        transition.addTarget(binding.addButton)
        transition.addTarget(binding.navToBldgImageButton)
        transition.addTarget(binding.removeButton)
        transition.addTarget(binding.buildingEditText)

        TransitionManager.beginDelayedTransition(binding.editAptBuildingsMapContainer, transition)
        binding.inputView.visibility = View.GONE
        binding.addButton.visibility = View.GONE
        binding.navToBldgImageButton.visibility = View.GONE
        binding.removeButton.visibility = View.GONE
        binding.buildingEditText.visibility = View.GONE

        pinDropMarker.isVisible = false
        mainActivity.hideKeyBoard(binding.buildingEditText)
    }

    private fun showEditUI(bldg: Building?) {
        val transition = Slide(Gravity.TOP)
        transition.duration = 50
        transition.addTarget(binding.inputView)
        transition.addTarget(binding.addButton)
        transition.addTarget(binding.navToBldgImageButton)
        transition.addTarget(binding.removeButton)
        transition.addTarget(binding.buildingEditText)

        TransitionManager.beginDelayedTransition(binding.editAptBuildingsMapContainer, transition)
        this.addOrRemoveBldgUI(bldg)
    }

    private fun addOrRemoveBldgUI(bldg: Building?) {
        when (bldg) {
            is Building -> { // set edit ui to Remove a Building
                binding.inputView.visibility = View.VISIBLE
                binding.addButton.visibility = View.GONE
                binding.navToBldgImageButton.visibility = View.VISIBLE
                binding.removeButton.visibility = View.VISIBLE
                binding.buildingEditText.visibility = View.GONE
            }
            else -> { // set edit ui to Add a Building to this Apartment
                binding.inputView.visibility = View.VISIBLE
                binding.addButton.visibility = View.VISIBLE
                binding.navToBldgImageButton.visibility = View.GONE
                binding.removeButton.visibility = View.GONE
                binding.buildingEditText.visibility = View.VISIBLE
                binding.buildingEditText.setText("")
                binding.buildingEditText.requestFocus()
                mainActivity.showKeyBoard(binding.buildingEditText)
                binding.buildingEditText.isEnabled = true
            }
        }
    }

}
