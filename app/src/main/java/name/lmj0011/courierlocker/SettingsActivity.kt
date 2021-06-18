package name.lmj0011.courierlocker

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * I guess this is the preferred way of implementing a Settings page
 * ref: https://developer.android.com/guide/topics/ui/settings
 * ref: https://stackoverflow.com/a/44949401/2445763
 */
class SettingsActivity : AppCompatActivity() {
    companion object {
        const val DB_EXPORT_REQUEST_CODE = 105
        const val DB_IMPORT_REQUEST_CODE = 106
        const val BACKUP_DIR_REQUEST_CODE = 107
    }

    // indeterminate progress indicator
    private lateinit var progressIndicator: LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        progressIndicator = findViewById(R.id.progress_indicator)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onSupportNavigateUp(): Boolean {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        return true
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

    fun toggleProgressIndicator(show: Boolean = true) {
        if(show) progressIndicator.visibility = View.VISIBLE
        else progressIndicator.visibility = View.GONE
    }
}