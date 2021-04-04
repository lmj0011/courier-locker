package name.lmj0011.courierlocker

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
        finish()
        return true
    }

    fun showToastMessage(message: String, duration: Int = Toast.LENGTH_SHORT) {
        val toast = Toast.makeText(this, message, duration)
        toast.setGravity(Gravity.TOP, 0, 150)
        toast.show()
    }

    fun toggleProgressIndicator(show: Boolean = true) {
        if(show) progressIndicator.visibility = View.VISIBLE
        else progressIndicator.visibility = View.GONE
    }
}