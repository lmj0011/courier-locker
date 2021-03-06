package name.lmj0011.courierlocker

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import name.lmj0011.courierlocker.fragments.CurrentStatusBubbleFragment
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.PermissionHelper
import org.kodein.di.instance
import timber.log.Timber

class CurrentStatusBubbleActivity: AppCompatActivity(R.layout.activity_current_status_bubble) {
    private val currentStatusFragment = CurrentStatusBubbleFragment()
    private lateinit var locationHelper: LocationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            locationHelper = (applicationContext as CourierLockerApplication).kodein.instance()
            if (savedInstanceState == null) {
                supportFragmentManager.commitNow {
                    // The home fragment for this Bubble
                    replace(R.id.container, currentStatusFragment, CURRENT_STATUS_BUBBLE_FRAGMENT_TAG)
                }
            }
        } catch (ex: Fragment.InstantiationException) {
            val error = ex.message
            if (error is String && error.contains("name.lmj0011.courierlocker.fragments.bottomsheets")) {
                recreate()
            } else finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if(!PermissionHelper.permissionAccessFineLocationApproved) {
            PermissionHelper.requestFineLocationAccess(this)
            this.showToastMessage("Location permission is required for some features to work.", Toast.LENGTH_LONG)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        PermissionHelper.checkPermissionApprovals(this)

        if(PermissionHelper.permissionAccessFineLocationApproved) {
            locationHelper.startLocationUpdates()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (application as CourierLockerApplication).applyTheme()

        recreate()
    }

    fun showToastMessage(message: String, duration: Int = Toast.LENGTH_SHORT) {
        val toast = Toast.makeText(applicationContext, message, duration)
        toast.show()
    }

    fun showKeyBoard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyBoard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
    }

    companion object {
        /**
         * Intent actions
         */
        // View
        const val INTENT_VIEW = "name.lmj0011.courierlocker.CurrentStatusBubbleActivity.VIEW"

        const val PERSON_ID = "name.lmj0011.courierlocker.CurrentStatusBubbleActivity.PERSON_1"
        // contact id
        const val CONTACT_ID = "name.lmj0011.courierlocker.CurrentStatusBubbleActivity.CONTACT_1"


        /**
         * TAGS
         */
        const val CURRENT_STATUS_BUBBLE_FRAGMENT_TAG =
            "name.lmj0011.courierlocker.CurrentStatusBubbleActivity.CURRENT_STATUS_BUBBLE_FRAGMENT_TAG"
        const val CREATE_TRIP_BUBBLE_FRAGMENT_TAG =
            "name.lmj0011.courierlocker.CurrentStatusBubbleActivity.CREATE_TRIP_BUBBLE_FRAGMENT_TAG"

    }
}