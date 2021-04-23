package name.lmj0011.courierlocker.helpers

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import name.lmj0011.courierlocker.R

// TODO make all preferences accessible from here
class PreferenceHelper(val context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val defaultBackupDir = context.getExternalFilesDir("backups/automatic")!!.toUri()

    init {
        // delete this unsafe preference, if it exists
        prefs.edit{ remove("advancedDirectionsApiKey") }
    }

    fun automaticBackupLocation(): String = prefs.getString("automaticBackupLocation", defaultBackupDir.toString())!!
    fun automaticBackupLocation(dir: String) = prefs.edit { putString("automaticBackupLocation", dir) }

    var boundingCoordinatesDistance: Double = prefs.getString(context.getString(R.string.pref_key_bounding_coordinates_distance), "25")!!.toDouble()
        set(value) {
            prefs.edit { putString(context.getString(R.string.pref_key_bounding_coordinates_distance), value.toString()) }
            field = value
        }

    fun enableCurrentStatusService() = prefs.getBoolean(context.getString(R.string.pref_enable_current_status_service), true)

    fun showCurrentStatusAsBubble() = prefs.getBoolean("showCurrentStatusAsBubble", false)
}
