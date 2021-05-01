package name.lmj0011.courierlocker

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commitNow
import name.lmj0011.courierlocker.fragments.CurrentStatusBubbleFragment
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.PermissionHelper
import org.kodein.di.instance

class CurrentStatusBubbleActivity: AppCompatActivity(R.layout.activity_current_status_bubble) {
    val currentStatusFragment = CurrentStatusBubbleFragment()
    private lateinit var locationHelper: LocationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationHelper = (applicationContext as CourierLockerApplication).kodein.instance()

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                // The home fragment for this Bubble
                replace(R.id.container, currentStatusFragment, CURRENT_STATUS_BUBBLE_FRAGMENT_TAG)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as CourierLockerApplication).showCurrentStatusServiceNotification(false)

        if(!PermissionHelper.permissionAccessFineLocationApproved) {
            PermissionHelper.requestFineLocationAccess(this)
            this.showToastMessage("Location permission is required for some features to work.", Toast.LENGTH_LONG)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        supportFragmentManager.commitNow {
            detach(currentStatusFragment)
            attach(currentStatusFragment)
        }
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments

        /**
         * fragments for this activity are expected NOT to go past 1-level deep
         */
        if (fragments.size > 1) {
            supportFragmentManager.commitNow {
                fragments.filter {
                    it.tag != CURRENT_STATUS_BUBBLE_FRAGMENT_TAG
                }.forEach { frag ->
                    setCustomAnimations(0, R.anim.slide_out_to_right)
                    remove(frag)
                }
                setCustomAnimations(R.anim.slide_in_from_left, 0)
                detach(currentStatusFragment)
                attach(currentStatusFragment)
                show(currentStatusFragment)
            }
        } else super.onBackPressed()
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

    fun showToastMessage(message: String, duration: Int = Toast.LENGTH_SHORT, position: Int = Gravity.TOP) {
        val toast = Toast.makeText(this, message, duration)
        toast.setGravity(position, 0, 150)
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