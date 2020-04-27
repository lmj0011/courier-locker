package name.lmj0011.courierlocker.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.SettingsActivity
import name.lmj0011.courierlocker.database.BaseDao
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.fragments.dialogs.AboutDialogFragment
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream


class SettingsFragment : PreferenceFragmentCompat() {
    lateinit var settingsActivity: SettingsActivity
    lateinit var baseDao: BaseDao
    private var fragmentJob: Job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        settingsActivity = activity as SettingsActivity
        val application = requireNotNull(settingsActivity).application
        baseDao = CourierLockerDatabase.getInstance(application).baseDao
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            val dialog = AboutDialogFragment()
            dialog.show(childFragmentManager, "AboutDialogFragment")
            true
        }

        findPreference<Preference>("exportAppData")?.setOnPreferenceClickListener {
            this.backupDB ()
            true
        }

        findPreference<Preference>("importAppData")?.setOnPreferenceClickListener {
            this.importDB()
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            Timber.i("Intent failed! [requestCode: $resultCode, resultCode: $resultCode]")
            return
        }

        when(requestCode) {
            SettingsActivity.DB_EXPORT_REQUEST_CODE -> {
                data?.data?.let {
                    // ref: https://developer.android.com/guide/topics/providers/document-provider#edit
                    settingsActivity.contentResolver.openFileDescriptor(it, "w")?.use { p ->
                        val bytes = CourierLockerDatabase.getDbData(settingsActivity)
                        bytes?.let {
                            FileOutputStream(p.fileDescriptor).use { outputStream ->
                                outputStream.write(it)
                            }
                        }

                    }
                }
            }
            SettingsActivity.DB_IMPORT_REQUEST_CODE -> {
                data?.data?.let {
                    settingsActivity.contentResolver.openFileDescriptor(it, "r")?.use { p ->
                        FileInputStream(p.fileDescriptor).use { inputStream ->
                            CourierLockerDatabase.setDbData(settingsActivity, inputStream.readBytes())
                            inputStream.close()

                            val intent = Intent(settingsActivity, MainActivity::class.java)
                            intent.putExtra("importedAppData", true)
                            startActivity(intent)
                            settingsActivity.onBackPressed()
                        }
                    }
                }
            }
        }
    }

    private fun backupDB () {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                baseDao.checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
            }

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                addCategory(Intent.CATEGORY_OPENABLE)

                // Create a file with the requested MIME type.
                type = "application/vnd.sqlite3"
                putExtra(Intent.EXTRA_TITLE, "courier_locker_database.bak.db")
            }

            startActivityForResult(intent, SettingsActivity.DB_EXPORT_REQUEST_CODE)
        }

    }

    private fun importDB () {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            type = "application/vnd.sqlite3"
        }

        startActivityForResult(intent, SettingsActivity.DB_IMPORT_REQUEST_CODE)
    }
}