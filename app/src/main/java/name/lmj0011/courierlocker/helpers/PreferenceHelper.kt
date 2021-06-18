package name.lmj0011.courierlocker.helpers

import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.libraries.maps.GoogleMap
import name.lmj0011.courierlocker.R

// TODO make all preferences accessible from here
class PreferenceHelper(val context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val defaultBackupDir = context.getExternalFilesDir("backups/automatic")!!.toUri()

    fun automaticBackupLocation(): String = prefs.getString("automaticBackupLocation", defaultBackupDir.toString())!!
    fun automaticBackupLocation(dir: String) = prefs.edit { putString("automaticBackupLocation", dir) }

    fun enableCurrentStatusService() = prefs.getBoolean(context.getString(R.string.pref_enable_current_status_service), true)

    fun showCurrentStatusAsBubble() = prefs.getBoolean("showCurrentStatusAsBubble", false)

    fun resetDevOptionsPrefs() {
        devOptionsEnabled = false
        devControlsEnabled = false
        googleDirectionsApiKey = ""
        boundingCoordinatesDistance = 25.0
    }

    // TODO - do rest of prefs this way
    var gateCodesIsOrderedByNearest: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_gate_codes_is_ordered_by_nearest), false)
        set(value) {
            prefs.edit { putBoolean(context.getString(R.string.pref_gate_codes_is_ordered_by_nearest), value) }
        }

    var mapsIsOrderedByNearest: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_maps_is_ordered_by_nearest), false)
        set(value) {
            prefs.edit { putBoolean(context.getString(R.string.pref_maps_is_ordered_by_nearest), value) }
        }

    var devOptionsEnabled: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_dev_options_enabled), false)
        set(value) {
            prefs.edit { putBoolean(context.getString(R.string.pref_dev_options_enabled), value) }
        }

    var devControlsEnabled: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_dev_controls_enabled), false)
        set(value) {
            prefs.edit { putBoolean(context.getString(R.string.pref_dev_controls_enabled), value) }
        }

    var boundingCoordinatesDistance: Double
        get() = prefs.getString(context.getString(R.string.pref_key_bounding_coordinates_distance), "25")!!.toDouble()
        set(value) {
            prefs.edit { putString(context.getString(R.string.pref_key_bounding_coordinates_distance), value.toString()) }
        }

    var googleMapType: Int
            get() = prefs.getInt(context.getString(R.string.pref_google_map_type), GoogleMap.MAP_TYPE_NORMAL)
            set(value) {
                prefs.edit {
                    putInt(context.getString(R.string.pref_google_map_type), value)
                }
            }

    var googleDirectionsApiKey: String
        get() = prefs.getString(context.getString(R.string.pref_google_directions_api_key), "").toString()
        set(value) {
            prefs.edit {
                putString(context.getString(R.string.pref_google_directions_api_key), value)
            }
        }
}
