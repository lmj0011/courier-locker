package name.lmj0011.courierlocker.fragments


import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.transition.Slide
import androidx.transition.TransitionManager
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.maps.android.ui.IconGenerator
import com.google.maps.android.ui.IconGenerator.STYLE_BLUE
import kotlinx.android.synthetic.main.fragment_edit_apt_buildings_map.view.*
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.MapListAdapter
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.Building
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.databinding.FragmentEditAptBuildingsMapBinding
import name.lmj0011.courierlocker.databinding.FragmentMapsBinding
import name.lmj0011.courierlocker.factories.ApartmentViewModelFactory
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import timber.log.Timber


/**
 * A simple [Fragment] subclass.
 *
 */
class EditAptBuildingsMapsFragment : Fragment(){

    private lateinit var binding: FragmentEditAptBuildingsMapBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var listAdapter: MapListAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModelFactory: ApartmentViewModelFactory
    private lateinit var apartmentViewModel: ApartmentViewModel
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var gMap: GoogleMap
    private lateinit var pinDropMarker: Marker
    private var selectedApt = MutableLiveData<Apartment>()
    private var selectedBldg: Building? = null
    private var fragmentJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_apt_buildings_map, container, false)

        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = CourierLockerDatabase.getInstance(application).apartmentDao
        viewModelFactory = ApartmentViewModelFactory(dataSource, application)
        apartmentViewModel = ViewModelProviders.of(this, viewModelFactory).get(ApartmentViewModel::class.java)
        mapFragment = childFragmentManager.findFragmentById(R.id.editAptBuildingsMapFragment) as SupportMapFragment

        uiScope.launch {
            withContext(Dispatchers.IO) {
                arguments?.let {
                    val apt = apartmentViewModel.database.get(it.getLong("aptId"))!!
                    selectedApt.postValue(apt)
                }
            }
        }

        mapFragment.getMapAsync { map ->
            gMap = map

            gMap.isMyLocationEnabled = true

            gMap.setOnMapClickListener {
                this.hideEditUI()
            }

            gMap.setOnMapLongClickListener {
                pinDropMarker.position = it
                pinDropMarker.isVisible = true
                selectedBldg = Building("", it.latitude, it.longitude)
                this.showEditUI(null)
            }

            gMap.setOnMarkerClickListener(this::onMarkerClick)



            selectedApt.observe(viewLifecycleOwner, Observer {
                if(it.buildings.isNotEmpty()) {
                    mainActivity.supportActionBar?.subtitle = it.name
                }
                this.refreshMap()
            })
        }

        binding.addButton.setOnClickListener {
            uiScope.launch {
                selectedBldg = Building(
                    binding.buildingEditText.text.toString(),
                    pinDropMarker.position.latitude,
                    pinDropMarker.position.longitude
                )

                val list = selectedApt.value!!.buildings.toMutableList()
                list.add(selectedBldg!!)
                selectedApt.value!!.buildings = list.toList()

                withContext(Dispatchers.IO) {
                    apartmentViewModel.updateApartment(selectedApt.value)
                    selectedApt.postValue(selectedApt.value)
                }

                mainActivity.hideKeyBoard(binding.buildingEditText)
            }
        }

        binding.removeButton.setOnClickListener {
            uiScope.launch {
                val list = selectedApt.value!!.buildings.toMutableList()
                list.remove(selectedBldg)
                selectedApt.value!!.buildings = list.toList()
                selectedBldg = null

                withContext(Dispatchers.IO) {
                    apartmentViewModel.updateApartment(selectedApt.value)
                    selectedApt.postValue(selectedApt.value)
                }
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

        pinDropMarker = gMap.addMarker(
            MarkerOptions()
                .position(LatLng(0.0,0.0))
                .visible(false)
        )

        selectedApt.value!!.buildings.forEach { bldg ->
            val position = LatLng(bldg.latitude, bldg.longitude)
            val ig = IconGenerator(mainActivity)
            ig.setStyle(STYLE_BLUE)
            val iconBitmap = ig.makeIcon(bldg.number)
            gMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("building: ${bldg.number}")
                    .icon(BitmapDescriptorFactory.fromBitmap(iconBitmap))
            ).tag = bldg
        }

        //
        when(selectedBldg) {
            is Building -> {
                gMap.moveCamera(CameraUpdateFactory.newLatLng(
                    LatLng(selectedBldg!!.latitude, selectedBldg!!.longitude)
                ))

                gMap.moveCamera(CameraUpdateFactory.zoomTo(gMap.cameraPosition.zoom))
            }
            else -> {
                gMap.moveCamera(CameraUpdateFactory.newLatLng(
                    LatLng(selectedApt.value!!.latitude, selectedApt.value!!.longitude)
                ))
                gMap.moveCamera(CameraUpdateFactory.zoomTo(16f))
            }
        }

        this.hideEditUI()
    }

    private fun onMarkerClick(mk: Marker): Boolean {
        val bldg = mk.tag as? Building
        bldg?.let{ selectedBldg = it }
        mk.showInfoWindow()
        pinDropMarker.isVisible = false
        this.showEditUI(bldg)
        gMap.uiSettings.isMapToolbarEnabled = true

        return false
    }

    private fun hideEditUI() {
        val transition = Slide(Gravity.TOP)
        transition.duration = 50
        transition.addTarget(binding.inputView)
        transition.addTarget(binding.addButton)
        transition.addTarget(binding.removeButton)
        transition.addTarget(binding.buildingEditText)

        TransitionManager.beginDelayedTransition(binding.editAptBuildingsMapContainer, transition)
        binding.inputView.visibility = View.GONE
        binding.addButton.visibility = View.GONE
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
        transition.addTarget(binding.removeButton)
        transition.addTarget(binding.buildingEditText)

        TransitionManager.beginDelayedTransition(binding.editAptBuildingsMapContainer, transition)
        this.addOrRemoveBldg(bldg)
    }

    private fun addOrRemoveBldg(bldg: Building?) {
        when (bldg) {
            is Building -> { // set edit ui to Remove a Building
                binding.inputView.visibility = View.VISIBLE
                binding.addButton.visibility = View.GONE
                binding.removeButton.visibility = View.VISIBLE
                binding.buildingEditText.visibility = View.GONE
            }
            else -> { // set edit ui to Add a Building to this Apartment
                binding.inputView.visibility = View.VISIBLE
                binding.addButton.visibility = View.VISIBLE
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
