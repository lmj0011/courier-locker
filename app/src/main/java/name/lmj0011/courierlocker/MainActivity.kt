package name.lmj0011.courierlocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.core.view.GravityCompat
import android.view.MenuItem
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import androidx.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.app_bar_main.view.*
import kotlinx.coroutines.*
import timber.log.Timber
import name.lmj0011.courierlocker.databinding.ActivityMainBinding
import name.lmj0011.courierlocker.fragments.TripsFragmentDirections
import name.lmj0011.courierlocker.fragments.dialogs.ImportedAppDataDialogFragment
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.PermissionHelper
import name.lmj0011.courierlocker.helpers.PreferenceHelper
import name.lmj0011.courierlocker.services.CurrentStatusForegroundService
import org.kodein.di.instance
import shortbread.Shortcut



class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration : AppBarConfiguration
    private lateinit var topLevelDestinations: Set<Int>
    private lateinit var preferences: PreferenceHelper


    companion object {
        const val TRIPS_WRITE_REQUEST_CODE = 104
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        Timber.i("onCreate Called")

        preferences = (applicationContext as CourierLockerApplication).kodein.instance()
        val sendCrashReports = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sendCrashReports", true)!!

        if(sendCrashReports) {
            Fabric.with(this, Crashlytics())
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.navHostFragment)
        setContentView(binding.root)

        // AppBar Navigation configuration
        topLevelDestinations = setOf(R.id.tripsFragment, R.id.gateCodesFragment, R.id.customersFragment, R.id.mapsFragment)
        appBarConfiguration = AppBarConfiguration.Builder(topLevelDestinations)
            .setDrawerLayout(binding.drawerLayout)
            .build()

        setSupportActionBar(binding.drawerLayout.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setNavigationItemSelectedListener(this::onNavigationItemSelected)

        // hide the fab initially
        binding.drawerLayout.fab.hide()
    }

    override fun onRestart() {
        super.onRestart()
        Timber.i("onRestart Called")
        CurrentStatusForegroundService.stopService(this)
    }

    override fun onStart() {
        super.onStart()
        Timber.i("onStart Called")

        if(!PermissionHelper.permissionAccessFineLocationApproved) {
            PermissionHelper.requestFineLocationAccess(this)
        } else {
            LocationHelper.startLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume Called")

        // any message that the previous Activity wants to send
        intent.extras?.getString("messageFromCaller")?.let {
            this.showToastMessage(it, Toast.LENGTH_LONG)
        }

        intent.extras?.getInt("menuItemId")?.let {
            this.navigateTo(it)
        }

        intent.extras?.getInt("editTripId")?.let {
            when {
                it > 0 -> navController.navigate(TripsFragmentDirections.actionTripsFragmentToEditTripFragment(it))
            }
        }

        intent.extras?.getBoolean("importedAppData")?.let {
            if(!it) return

            val dialog = ImportedAppDataDialogFragment()
            dialog.show(supportFragmentManager, "ImportedAppDataDialogFragment")
        }

        if(!PermissionHelper.permissionAccessFineLocationApproved) {
            this.showToastMessage("Location permission is required for some features to work.", Toast.LENGTH_LONG)
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause Called")

        if(preferences.enableCurrentStatusService() && PermissionHelper.permissionAccessFineLocationApproved) {
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

    override fun onSupportNavigateUp(): Boolean {
        when(navController.currentDestination?.id) {
            R.id.editTripFragment -> {
                // resets the recyclerview to original state
                navController.navigate(R.id.tripsFragment)
                return true
            }
            R.id.editAptBuildingsMapsFragment -> {
                // resets the recyclerview to original state
                navController.navigate(R.id.mapsFragment)
                return true
            }
            R.id.editGateCodeFragment -> {
                // resets the recyclerview to original state
                navController.navigate(R.id.gateCodesFragment)
                return true
            }
            R.id.editCustomerFragment -> {
                // resets the recyclerview to original state
                navController.navigate(R.id.customersFragment)
                return true
            }

            else -> { }
        }

        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }else {
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

    fun showToastMessage(message: String, duration: Int = Toast.LENGTH_SHORT, position: Int = Gravity.TOP) {
        val toast = Toast.makeText(this, message, duration)
        toast.setGravity(position, 0, 150)
        toast.show()
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

    fun showKeyBoard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyBoard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
    }

    @Shortcut(id = "shortcut_trips", rank = 4, icon = R.mipmap.ic_trips_shortcut, shortLabel = "Trips")
    fun shortCutToTrips() {
        navController.navigate(R.id.tripsFragment)
    }

    @Shortcut(id = "shortcut_maps", rank = 3, icon = R.mipmap.ic_maps_shortcut, shortLabel = "Maps")
    fun shortCutToMaps() {
        navController.navigate(R.id.mapsFragment)
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
            R.id.nav_maps -> {
                navController.navigate(R.id.mapsFragment)
            }
            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
