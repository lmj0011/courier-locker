package name.lmj0011.courierlocker

import android.content.Intent
import android.os.Bundle
import androidx.core.view.GravityCompat
import android.view.MenuItem
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.*
import androidx.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.app_bar_main.view.*
import timber.log.Timber
import name.lmj0011.courierlocker.databinding.ActivityMainBinding
import name.lmj0011.courierlocker.fragments.TripsFragmentDirections
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.PermissionHelper
import name.lmj0011.courierlocker.services.CurrentStatusForegroundService
import shortbread.Shortcut



class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration : AppBarConfiguration

    companion object {
        // for AutoCompleteTextView handler
        const val TRIGGER_AUTO_COMPLETE = 101
        const val TRIP_PICKUP_AUTO_COMPLETE = 102
        const val TRIP_DROP_OFF_AUTO_COMPLETE = 103
        const val TRIPS_WRITE_REQUEST_CODE = 104
        const val AUTO_COMPLETE_DELAY = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("onCreate Called")

        val sendCrashReports = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sendCrashReports", true)!!

        PermissionHelper.checkPermissionApprovals(this)

        if(sendCrashReports) {
            Fabric.with(this, Crashlytics())
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.navHostFragment)
        setContentView(binding.root)

        // AppBar Navigation configuration
        val topLevelDestinations = setOf(R.id.tripsFragment, R.id.gateCodesFragment, R.id.customersFragment)
        appBarConfiguration = AppBarConfiguration.Builder(topLevelDestinations)
            .setDrawerLayout(binding.drawerLayout)
            .build()

        setSupportActionBar(binding.drawerLayout.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setNavigationItemSelectedListener(this::onNavigationItemSelected)
        /////

        LocationHelper.setFusedLocationClient(this)

        // hide the fab initially
        binding.drawerLayout.fab.hide()
    }

    override fun onStart() {
        super.onStart()
        Timber.i("onStart Called")

        if(!PermissionHelper.permissionAccessFineLocationApproved &&
            !PermissionHelper.backgroundLocationPermissionApproved) {

            // this app currently only needs the ACCESS_FINE_LOCATION permission
            PermissionHelper.requestBackgroundLocationAccess(this)
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume Called")

        val menuItemId  = intent.extras?.getInt("menuItemId")
        menuItemId?.let { this.navigateTo(it) }

        val editTripId  = intent.extras?.getInt("editTripId")
        editTripId?.let {
            when {
                it > 0 -> navController.navigate(TripsFragmentDirections.actionTripsFragmentToEditTripFragment(editTripId))
            }
        }

        CurrentStatusForegroundService.stopService(this)

        if(PermissionHelper.permissionAccessFineLocationApproved) {
            LocationHelper.startLocationUpdates()
        } else {
            Toast.makeText(this,"App has no Location permissions.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause Called")

        if(PermissionHelper.permissionAccessFineLocationApproved) {
            LocationHelper.stopLocationUpdates()
            CurrentStatusForegroundService.startService(this)
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.i("onStop Called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy Called")
        CurrentStatusForegroundService.stopService(this)
    }

    override fun onRestart() {
        super.onRestart()
        Timber.i("onRestart Called")
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.navHostFragment).navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            Timber.i("label: ${navController.currentDestination?.label}")

            when(navController.currentDestination?.label) {
                "Trips" -> {
                    navController.popBackStack(R.id.gateCodesFragment, false)
                }
                "Gate Codes" -> {
                    navController.popBackStack(R.id.gateCodesFragment, false)
                    finish()
                }
                "Customers" -> {
                    navController.popBackStack(R.id.gateCodesFragment, false)
                    finish()
                }

            }

            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        this.navigateTo(item.itemId)

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionHelper.checkPermissionApprovals(this)

        if(PermissionHelper.permissionAccessFineLocationApproved) {
            LocationHelper.startLocationUpdates()
        }
    }


    fun showFabAndSetListener(cb: () -> Unit, imgSrcId: Int) {
        binding.drawerLayout.fab.let {
            it.setOnClickListener(null) // should remove all attached listeners
            it.setOnClickListener { cb() }

            // hide and show to repaint the img src
            it.hide()

            it.setImageResource(imgSrcId)

            it.show()
        }
    }

    fun hideFab() {
        binding.drawerLayout.fab.hide()
    }

    @Shortcut(id = "shortcut_trips", rank = 3, icon = R.mipmap.ic_trips_shortcut, shortLabel = "Trips")
    fun shortCutToTrips() {
        navController.navigate(R.id.tripsFragment)
    }

    @Shortcut(id = "shortcut_gatecodes", rank = 2, icon = R.mipmap.ic_gatecodes_shortcut, shortLabel = "Gate Codes")
    fun shortCutToGatecodes() {
        navController.navigate(R.id.gateCodesFragment)
    }

    @Shortcut(id = "shortcut_customers", rank = 1, icon = R.mipmap.ic_customers_shortcut, shortLabel = "Customers")
    fun shortCutToCustomers() {
        navController.navigate(R.id.customersFragment)
    }

    private fun navigateTo(id: Int) {
        when (id) {
            R.id.nav_gate_codes -> {
                navController.navigate(R.id.gateCodesFragment)
            }
            R.id.nav_customers -> {
                navController.navigate(R.id.customersFragment)
            }
            R.id.nav_trips -> {
                navController.navigate(R.id.tripsFragment)
            }
            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
