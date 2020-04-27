package name.lmj0011.courierlocker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

/**
 * I guess this is the preferred way of implementing a Settings page
 * ref: https://developer.android.com/guide/topics/ui/settings
 * ref: https://stackoverflow.com/a/44949401/2445763
 */
class SettingsActivity : AppCompatActivity() {
    companion object {
        const val DB_EXPORT_REQUEST_CODE = 105
        const val DB_IMPORT_REQUEST_CODE = 106
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("onCreate Called")

        setContentView(R.layout.activity_settings)

        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}