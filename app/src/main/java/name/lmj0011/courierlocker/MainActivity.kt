package name.lmj0011.courierlocker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.view.GravityCompat
import android.view.MenuItem
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.app_bar_main.view.*
import timber.log.Timber
import name.lmj0011.courierlocker.databinding.ActivityMainBinding
import name.lmj0011.courierlocker.helpers.LocationHelper

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration : AppBarConfiguration

    companion object {
        const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100

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
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume Called")

        when(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            PackageManager.PERMISSION_GRANTED -> LocationHelper.startLocationUpdates()
            else -> {
                Toast.makeText(this,"Location permissions are not enabled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause Called")
        LocationHelper.stopLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        Timber.i("onStop Called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy Called")
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
                "TripsFragment" -> {
                    navController.popBackStack(R.id.gateCodesFragment, false)
                }
                "GateCodesFragment" -> {
                    navController.popBackStack(R.id.gateCodesFragment, false)
                    finish()
                }
                "fragment_customers" -> {
                    navController.popBackStack(R.id.gateCodesFragment, false)
                    finish()
                }

            }

            super.onBackPressed()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if(!resources.getBoolean(R.bool.DEBUG_MODE)) { // if not in DEBUG mode then:

            // hide the "Force Crash" option
            menu?.findItem(R.id.action_force_crash)?.let {
                it.isVisible = false
                it.isEnabled = false
            }
        }


        return super.onPrepareOptionsMenu(menu)
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
            R.id.action_force_crash -> {
                Crashlytics.getInstance().crash()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
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

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
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
}
