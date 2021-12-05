package name.lmj0011.courierlocker.fragments


import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.BuildingUnit
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentCreateOrEditApartmentMapBinding
import name.lmj0011.courierlocker.databinding.FragmentEditAptUnitsMapBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.fragments.bottomsheets.BottomSheetAptUnitDetailsFragment
import name.lmj0011.courierlocker.fragments.bottomsheets.BottomSheetAptUnitFloorOptionsFragment
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import org.kodein.di.instance
import timber.log.Timber
import java.lang.Exception


/**
 * A simple [Fragment] subclass.
 *
 */
class EditAptUnitsMapFragment : Fragment(){

    private lateinit var binding: FragmentEditAptUnitsMapBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModelFactory: ApartmentViewModelFactory
    private lateinit var apartmentViewModel: ApartmentViewModel
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var gMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<AptBldgUnitClusterItem>
    private lateinit var pinDropMarker: Marker
    private lateinit var preferences: PreferenceHelper
    private lateinit var args: EditAptUnitsMapFragmentArgs
    private var selectedApt = MutableLiveData<Apartment>()
    private var selectedBldgUnit: BuildingUnit? = null
    private var activePolylinePairList: MutableList<Pair<Polyline, Circle>?> = mutableListOf()
    private var fragmentJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)
    private var selectionMode = Const.BUILDING_UNIT_SELECTION_MODE

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainActivity = activity as MainActivity
        preferences = (mainActivity.application as CourierLockerApplication).kodein.instance()
        setHasOptionsMenu(true)

        try {
            binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_edit_apt_units_map, container, false)
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
        args = EditAptUnitsMapFragmentArgs.fromBundle(requireArguments())
        apartmentViewModel = ViewModelProvider(this, viewModelFactory).get(ApartmentViewModel::class.java)
        mapFragment = childFragmentManager.findFragmentById(R.id.editAptUnitsMapFragment) as SupportMapFragment

        selectedApt.observe(viewLifecycleOwner, Observer {
            if(it.buildings.isNotEmpty()) {
                mainActivity.supportActionBar?.subtitle = it.name
            }

            val item = binding.floorsSpinner.selectedItem

            if(item != null) {
                refreshMapAtSpecificFloor(item.toString())
            } else {
                refreshMap()
            }
        })

        uiScope.launch {
            withContext(Dispatchers.IO) {
                val apt = apartmentViewModel.database.get(args.aptId)!!
                selectedApt.postValue(apt)
            }
        }

        mapFragment.getMapAsync { map ->
            gMap = map
            applyPreferences()
            gMap.moveCamera(CameraUpdateFactory.zoomTo(17f))
            clusterManager = ClusterManager(mainActivity, gMap)

            val renderer = object: DefaultClusterRenderer<AptBldgUnitClusterItem>(mainActivity, gMap, clusterManager) {
                override fun onBeforeClusterItemRendered(item: AptBldgUnitClusterItem, markerOptions: MarkerOptions) {
                    val ig = IconGenerator(requireContext()).apply {
                        setStyle(IconGenerator.STYLE_BLUE)
                    }
                    val iconBitmap = ig.makeIcon(item.bldgUnit.number)

                    markerOptions.position(item.position)
                    markerOptions.title(item.title)
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconBitmap))
                }

            }

            clusterManager.renderer = renderer

            gMap.isMyLocationEnabled = true

            gMap.setOnCameraIdleListener(clusterManager)
            gMap.setOnMarkerClickListener(clusterManager)

            gMap.setOnMapClickListener {
                this.hideAddUnitUI()
                this.hideAddWaypointUI()
            }

            gMap.setOnMapLongClickListener {
                pinDropMarker.position = it
                pinDropMarker.isVisible = true
                clearPolylines()

                when(selectionMode) {
                    Const.BUILDING_UNIT_SELECTION_MODE -> {
                        selectedBldgUnit = BuildingUnit("", "", it.latitude, it.longitude)
                        this.showAddUnitUI()
                    }
                    Const.WAYPOINT_SELECTION_MODE -> {
                        this.showAddWaypointUI()
                        binding.saveWaypointButton.isEnabled = true

                        selectedBldgUnit?.let { unit ->
                            unit.waypointLatitude = it.latitude
                            unit.waypointLongitude = it.longitude
                            drawBldgUnitPolyline(unit)
                        }
                    }
                }
            }

            clusterManager.setOnClusterItemClickListener {
                selectedBldgUnit = it.bldgUnit
                pinDropMarker.isVisible = false

                val bottomSheet = BottomSheetAptUnitDetailsFragment(
                    buildingUnit = it.bldgUnit,
                    editBuildingUnitCallback = {
                        val inputEditTextField = EditText(requireContext())
                        inputEditTextField.also { et ->
                            et.hint = "unit #"
                            et.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        }

                        selectedBldgUnit?.number?.let{ num ->
                            if(num.isNotBlank()) inputEditTextField.setText(num)
                        }

                        val builder = MaterialAlertDialogBuilder(requireContext())

                        builder
                            .setView(inputEditTextField)
                            .setMessage("")
                            .setPositiveButton("Save") { _, _ ->
                                selectedBldgUnit?.let{ unit ->
                                    selectedApt.value!!.buildingUnits.remove(unit)
                                    unit.number = inputEditTextField.text.toString()
                                    selectedApt.value!!.buildingUnits.add(unit)

                                }
                                launchUI {
                                    withContext(Dispatchers.IO) {
                                        apartmentViewModel.updateApartment(selectedApt.value)
                                        selectedApt.postValue(selectedApt.value)
                                    }
                                }
                            }
                            .setNeutralButton("Cancel") { _,_ ->

                            }

                        builder.show()

                        inputEditTextField.postDelayed({
                            inputEditTextField.requestFocus()
                            inputEditTextField.setSelection(inputEditTextField.length())
                            (requireActivity() as MainActivity).showKeyBoard(inputEditTextField)
                        }, 100)
                    },
                    addWaypointCallback = {
                        selectionMode = Const.WAYPOINT_SELECTION_MODE
                        this.showAddWaypointUI()
                        binding.saveWaypointButton.isEnabled = false
                    },
                    removeWaypointCallback = {
                        val oldUnit = selectedBldgUnit!!

                        val newUnit = BuildingUnit(
                            number = oldUnit.number,
                            floorNumber = oldUnit.floorNumber,
                            latitude = oldUnit.latitude,
                            longitude = oldUnit.longitude,
                            hasWaypoint = false,
                        )

                        selectedApt.value?.let { apt ->
                            apt.buildingUnits.remove(oldUnit)
                            apt.buildingUnits.add(newUnit)

                            launchIO {
                                apartmentViewModel.updateApartment(apt)
                                selectedApt.postValue(apt)
                            }
                        }
                    },
                    removeBuildingUnitCallback = {
                        val builder = MaterialAlertDialogBuilder(requireContext())

                        builder
                            .setTitle("Unit ${it.bldgUnit.number}")
                            .setMessage("Remove this unit?")
                            .setPositiveButton("Yes") { _, _ ->
                                selectedBldgUnit?.let{ unit -> selectedApt.value!!.buildingUnits.remove(unit) }
                                launchUI {
                                    withContext(Dispatchers.IO) {
                                        apartmentViewModel.updateApartment(selectedApt.value)
                                        selectedApt.postValue(selectedApt.value)
                                    }
                                }
                            }
                            .setNeutralButton("Cancel") { _,_ ->

                            }

                        builder.show()
                    },
                    dismissCallback = {
                        clearPolylines()
                    }
                )

                if (it.bldgUnit.hasWaypoint) drawBldgUnitPolyline(it.bldgUnit)

                bottomSheet
                    .show(childFragmentManager, "BottomSheetAptUnitDetailsFragment")

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

        }

        binding.addButton.setOnClickListener {
            uiScope.launch {
                val unit = BuildingUnit(
                    binding.unitEditText.text.toString(),
                    binding.floorsSpinner.selectedItem.toString(),
                    pinDropMarker.position.latitude,
                    pinDropMarker.position.longitude
                )

                selectedBldgUnit = unit

                selectedApt.value?.let { apt ->
                    apt.buildingUnits.add(unit)

                    if(apt.floorOneAsBlueprint && unit.floorNumber == "1") {
                        cascadeUnit(apt, unit)
                    }

                    withContext(Dispatchers.IO) {
                        apartmentViewModel.updateApartment(apt)
                        selectedApt.postValue(apt)
                    }
                }

                mainActivity.hideKeyBoard(binding.unitEditText)
            }
        }

        binding.floorsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Timber.d("refreshMapAtSpecificFloor(${binding.floorsSpinner.selectedItem})")
                refreshMapAtSpecificFloor(binding.floorsSpinner.selectedItem.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }

        binding.saveWaypointButton.setOnClickListener {
            val oldUnit = selectedBldgUnit!!

            val newUnit = BuildingUnit(
                number = oldUnit.number,
                floorNumber = oldUnit.floorNumber,
                latitude = oldUnit.latitude,
                longitude = oldUnit.longitude,
                hasWaypoint = true,
                waypointLatitude = pinDropMarker.position.latitude,
                waypointLongitude = pinDropMarker.position.longitude

            )

            selectedApt.value?.let { apt ->
                apt.buildingUnits.remove(oldUnit)
                apt.buildingUnits.add(newUnit)

                launchIO {
                    apartmentViewModel.updateApartment(apt)
                    selectedApt.postValue(apt)
                }
            }
        }

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity.supportActionBar?.subtitle = "Long press to start marking units"
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_apt_units_maps, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_change_map_type -> {
                // https://stackoverflow.com/a/27178343/2445763
                val builder = MaterialAlertDialogBuilder(requireContext())
                val checkedItem = gMap.mapType - 1

                builder.setTitle("Change Map Type")
                builder.setSingleChoiceItems(
                    arrayOf("Default", "Satellite", "Terrain", "Hybrid"),
                    checkedItem
                ) { dialog, position ->
                    val mapType = when(position) {
                        0 -> GoogleMap.MAP_TYPE_NORMAL
                        1 -> GoogleMap.MAP_TYPE_SATELLITE
                        2 -> GoogleMap.MAP_TYPE_TERRAIN
                        3 -> GoogleMap.MAP_TYPE_HYBRID
                        else -> GoogleMap.MAP_TYPE_NORMAL
                    }

                    gMap.mapType = mapType
                    preferences.googleMapType = mapType

                    dialog.dismiss()
                }

                builder.create().show()
                true
            }
            R.id.action_nav_to_buildings -> {
                findNavController()
                    .navigate(
                        EditAptUnitsMapFragmentDirections
                            .actionEditAptUnitsMapFragmentToEditAptBuildingsMapFragment(args.aptId)
                    )
                true
            }
            R.id.action_show_floor_options -> {
                val bottomSheet = BottomSheetAptUnitFloorOptionsFragment(selectedApt.value!!) { apt ->
                    launchUI {
                        if(apt.floorOneAsBlueprint) {
                            apt.buildingUnits
                                .filter { unt -> unt.floorNumber == "1" }
                                .forEach { unt -> cascadeUnit(apt, unt) }
                        }

                        withContext(Dispatchers.IO) {
                            apartmentViewModel.updateApartment(apt)
                            selectedApt.postValue(apt)
                        }
                        refreshMap()
                    }
                }

                bottomSheet
                    .show(childFragmentManager, "BottomSheetAptUnitFloorOptionsFragment")

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun applyPreferences() {
        val mapType = preferences.googleMapType
        gMap.mapType = mapType
    }

    /**
     *  The given [unit] is used to create "blank" units for each floor of the [apt],
     *  all of which have the same lat/lng coordinates. All the user has to do at this point, is
     *  enter the desired unit number.
     */
    private fun cascadeUnit(apt: Apartment, unit: BuildingUnit) {
        val floors = apartmentViewModel.getFloorArray(apt)
        val unit1Lat = unit.latitude
        val unit1Lng = unit.longitude

        val floorsToNotCopyTo = apt.buildingUnits.filter { unt ->
            unt.latitude == unit1Lat && unt.longitude == unit1Lng
        }.map { unt ->
            unt.floorNumber
        }.distinct()

        Timber.d("floorsToNotCopyTo: $floorsToNotCopyTo")

        floors.forEach { floorNumber ->
            if (floorsToNotCopyTo.contains(floorNumber).not()) {
                apt.buildingUnits.add(
                    BuildingUnit(
                        "",
                        floorNumber,
                        unit1Lat,
                        unit1Lng
                    )
                )
                Timber.d("will create unit on floor $floorNumber, using floor 1 as blueprint")
            }
        }
    }


    private fun refreshMap() {
        gMap.clear()
        clusterManager.clearItems()

        pinDropMarker = gMap.addMarker(
            MarkerOptions()
                .position(LatLng(0.0,0.0))
                .visible(false)
        )

        selectedApt.value?.let { apt ->
            val floorArray = apartmentViewModel.getFloorArray(apt)

            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                floorArray
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.floorsSpinner.adapter = adapter

                // positions the spinner to the 1st floor
                binding.floorsSpinner.setSelection((apt.aboveGroundFloorCount - 1), true)
            }

            apt.buildingUnits
                .filter {  unit ->
                    unit.floorNumber == binding.floorsSpinner.selectedItem.toString()
                }
                .forEach { unit ->
                    val item = AptBldgUnitClusterItem(unit)
                    clusterManager.addItem(item)
                }
        }

        clusterManager.cluster()

        when(selectedBldgUnit) {
            is BuildingUnit -> {
                gMap.moveCamera(CameraUpdateFactory.newLatLng(
                    LatLng(selectedBldgUnit!!.latitude, selectedBldgUnit!!.longitude)
                ))

            }
            else -> {
                gMap.moveCamera(CameraUpdateFactory.zoomTo(17f))

                gMap.moveCamera(CameraUpdateFactory.newLatLng(
                    LatLng(selectedApt.value!!.latitude, selectedApt.value!!.longitude)
                ))
            }
        }
        this.hideAddUnitUI()
        this.hideAddWaypointUI()
    }

    private fun refreshMapAtSpecificFloor(floor: String) {
        gMap.clear()
        clusterManager.clearItems()

        pinDropMarker = gMap.addMarker(
            MarkerOptions()
                .position(LatLng(0.0,0.0))
                .visible(false)
        )

        selectedApt.value?.let { apt ->
            apt.buildingUnits
                .filter {  unit ->
                    unit.floorNumber == floor
                }
                .forEach { unit ->
                    val item = AptBldgUnitClusterItem(unit)
                    clusterManager.addItem(item)
                }
        }

        clusterManager.cluster()

        when(selectedBldgUnit) {
            is BuildingUnit -> {
                gMap.moveCamera(CameraUpdateFactory.newLatLng(
                    LatLng(selectedBldgUnit!!.latitude, selectedBldgUnit!!.longitude)
                ))
            }
            else -> {
                gMap.moveCamera(CameraUpdateFactory.zoomTo(17f))

                gMap.moveCamera(CameraUpdateFactory.newLatLng(
                    LatLng(selectedApt.value!!.latitude, selectedApt.value!!.longitude)
                ))
            }
        }

        this.hideAddUnitUI()
        this.hideAddWaypointUI()
    }

    private fun hideAddUnitUI() {
        val transition = Slide(Gravity.TOP)
        transition.duration = 50
        transition.addTarget(binding.inputView)
        transition.addTarget(binding.addButton)
        transition.addTarget(binding.unitEditText)

        TransitionManager.beginDelayedTransition(binding.editAptUnitsMapContainer, transition)
        binding.inputView.visibility = View.GONE
        binding.addButton.visibility = View.GONE
        binding.unitEditText.visibility = View.GONE

        pinDropMarker.isVisible = false
        mainActivity.hideKeyBoard(binding.unitEditText)
        binding.floorsSpinner.isEnabled = true
    }

    private fun showAddUnitUI() {
        val transition = Slide(Gravity.TOP)
        transition.duration = 50
        transition.addTarget(binding.inputView)
        transition.addTarget(binding.addButton)
        transition.addTarget(binding.unitEditText)

        TransitionManager.beginDelayedTransition(binding.editAptUnitsMapContainer, transition)

        binding.inputView.visibility = View.VISIBLE
        binding.addButton.visibility = View.VISIBLE
        binding.unitEditText.visibility = View.VISIBLE
        binding.unitEditText.setText("")
        binding.unitEditText.requestFocus()
        mainActivity.showKeyBoard(binding.unitEditText)
        binding.unitEditText.isEnabled = true

        hideAddWaypointUI()
        binding.floorsSpinner.isEnabled = false
    }

    private fun hideAddWaypointUI() {
        val transition = Slide(Gravity.TOP)
        transition.duration = 50
        transition.addTarget(binding.addWaypointInputView)
        transition.addTarget(binding.saveWaypointButton)
        transition.addTarget(binding.addWaypointTextView)

        TransitionManager.beginDelayedTransition(binding.editAptUnitsMapContainer, transition)
        binding.addWaypointInputView.visibility = View.GONE
        binding.saveWaypointButton.visibility = View.GONE
        binding.addWaypointTextView.visibility = View.GONE

        pinDropMarker.isVisible = false
        binding.floorsSpinner.isEnabled = true
        clearPolylines()

    }

    private fun showAddWaypointUI() {
        val transition = Slide(Gravity.TOP)
        transition.duration = 50
        transition.addTarget(binding.addWaypointInputView)
        transition.addTarget(binding.saveWaypointButton)
        transition.addTarget(binding.addWaypointTextView)

        TransitionManager.beginDelayedTransition(binding.editAptUnitsMapContainer, transition)
        binding.addWaypointInputView.visibility = View.VISIBLE
        binding.saveWaypointButton.visibility = View.VISIBLE
        binding.addWaypointTextView.visibility = View.VISIBLE

        hideAddUnitUI()
        binding.floorsSpinner.isEnabled = false
    }

    private fun drawBldgUnitPolyline(unit: BuildingUnit) {
        val polyline = gMap.addPolyline(
            PolylineOptions()
                .clickable(false)
                .add(
                    LatLng(unit.waypointLatitude, unit.waypointLongitude),
                    LatLng(unit.latitude, unit.longitude)
                )
        )

        val pair = Util.stylePolyline(gMap, polyline)
        activePolylinePairList.add(pair)
    }

    private fun clearPolylines() {
        activePolylinePairList.forEach { pair ->
            pair?.first?.remove()
            pair?.second?.remove()
        }

        activePolylinePairList.clear()
    }

}
