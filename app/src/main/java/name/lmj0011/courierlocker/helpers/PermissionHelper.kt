package name.lmj0011.courierlocker.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

object PermissionHelper {

    const val FINE_LOCATION_ACCESS_RQUEST_CODE = 100

    var permissionAccessFineLocationApproved = false
        private set

    fun checkPermissionApprovals(context: Context){

        permissionAccessFineLocationApproved = ActivityCompat
            .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun requestFineLocationAccess (activity: Activity) {
        ActivityCompat.requestPermissions(activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            FINE_LOCATION_ACCESS_RQUEST_CODE
        )
    }
}