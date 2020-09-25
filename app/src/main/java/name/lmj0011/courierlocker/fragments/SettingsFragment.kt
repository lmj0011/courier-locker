package name.lmj0011.courierlocker.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.preference.*
import androidx.work.*
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.list_item_gig_label.view.*
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.SettingsActivity
import name.lmj0011.courierlocker.database.*
import name.lmj0011.courierlocker.fragments.dialogs.AboutDialogFragment
import name.lmj0011.courierlocker.helpers.AppDataImportExportHelper
import name.lmj0011.courierlocker.helpers.PreferenceHelper
import name.lmj0011.courierlocker.services.CurrentStatusForegroundService
import name.lmj0011.courierlocker.workers.CalculateAllTripDistanceWorker
import name.lmj0011.courierlocker.workers.CreateBackupWorker
import org.kodein.di.instance
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit


class SettingsFragment : PreferenceFragmentCompat() {
    lateinit var settingsActivity: SettingsActivity
    lateinit var settingsDao: SettingsDao
    private var fragmentJob: Job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)
    private lateinit var preferences: PreferenceHelper

    private lateinit var createBackupPref: Preference
    private lateinit var restoreBackupPref: Preference
    private lateinit var enableAutomaticBackupsPref: SwitchPreferenceCompat
    private lateinit var googleDirectionsKeyPref: Preference
    private lateinit var aboutPref: Preference
    private lateinit var automaticBackupLocationPref: Preference
    private lateinit var boundingCoordinatesDistance: EditTextPreference
    private lateinit var enableCurrentStatusService: SwitchPreferenceCompat

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        settingsActivity = activity as SettingsActivity
        val application = settingsActivity.application as CourierLockerApplication
        preferences = application.kodein.instance()
        settingsDao = CourierLockerDatabase.getInstance(application).settingsDao

        setPreferencesFromResource(R.xml.preferences, rootKey)

        // assign preferences to variables. No data-binding as of now.
        createBackupPref = findPreference("createBackup")!!
        restoreBackupPref = findPreference("restoreBackup")!!
        enableAutomaticBackupsPref = findPreference("enableAutomaticBackups")!!
        googleDirectionsKeyPref = findPreference("googleDirectionsKey")!!
        aboutPref = findPreference("about")!!
        automaticBackupLocationPref = findPreference("automaticBackupLocation")!!
        boundingCoordinatesDistance = findPreference(application.getString(R.string.pref_key_bounding_coordinates_distance))!!
        boundingCoordinatesDistance = findPreference(application.getString(R.string.pref_key_bounding_coordinates_distance))!!
        enableCurrentStatusService = findPreference(application.getString(R.string.pref_enable_current_status_service))!!

        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            val prfScreen = findPreference<PreferenceScreen>("preferenceScreen")
            val debugCategory = findPreference<PreferenceCategory>("debugCategory")
            prfScreen?.removePreference(debugCategory)
        } else {
            boundingCoordinatesDistance.apply {
                val v = preferences.boundingCoordinatesDistance
                title = context.getString(R.string.pref_bounding_coordinates_distance_title, v.toString())

                setOnBindEditTextListener {
                    it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    it.setSelection(it.text.toString().length)
                }

                setOnPreferenceChangeListener { pref, newValue ->
                    val v = newValue as String
                    pref.title = pref.context.getString(R.string.pref_bounding_coordinates_distance_title, newValue.toDouble().toString())
                    preferences.boundingCoordinatesDistance = v.toDouble()
                    true
                }
            }
        }

        googleDirectionsKeyPref.setOnPreferenceChangeListener { _, newValue ->
            when(newValue) {
                true -> {
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                    val workerRequest = OneTimeWorkRequestBuilder<CalculateAllTripDistanceWorker>()
                        .setConstraints(constraints)
                        .addTag(settingsActivity.getString(R.string.calculate_all_trip_distance_one_time_worker_tag))
                        .build()

                    WorkManager.getInstance(application).enqueue(workerRequest)
                }
                else -> {}
            }
            true
        }

        aboutPref.setOnPreferenceClickListener {
            val dialog = AboutDialogFragment()
            dialog.show(childFragmentManager, "AboutDialogFragment")
            true
        }

        createBackupPref.setOnPreferenceClickListener {
            this.createBackup()
            true
        }

        restoreBackupPref.setOnPreferenceClickListener {
            this.importDB()
            true
        }

        automaticBackupLocationPref.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, SettingsActivity.CODE_BACKUP_DIR_REQUEST_CODE)
            true
        }

        if(enableAutomaticBackupsPref.isChecked) {
            var summary: String? = preferences.defaultBackupDir.toString()

            try {
                summary = DocumentFile.fromTreeUri(preferenceManager.context,
                    Uri.parse(preferences.automaticBackupLocation)
                )!!.uri.lastPathSegment
            } catch (ex: IllegalArgumentException) {
                // this error should only be thrown, if the User hasn't selected a directory yet
            }

            automaticBackupLocationPref.summary = summary
            automaticBackupLocationPref.isVisible = true
        }

        enableAutomaticBackupsPref.setOnPreferenceChangeListener { _, newValue ->
            var summary: String? = preferences.defaultBackupDir.toString()

            try {
                summary = DocumentFile.fromTreeUri(preferenceManager.context,
                    Uri.parse(preferences.automaticBackupLocation)
                )!!.uri.lastPathSegment
            } catch (ex: IllegalArgumentException) {
                // this error should only be thrown, if the User hasn't selected a directory yet
            }


            automaticBackupLocationPref.summary = summary ?: "null"
            automaticBackupLocationPref.isVisible = when(newValue) {
                true -> {
                    enqueueBackupWorker()
                    true
                }
                else -> {
                    cancelBackupWorker()
                    false
                }
            }
            true
        }

        enableCurrentStatusService.setOnPreferenceChangeListener { _, newValue ->
            when (newValue) {
                true -> {
                    CurrentStatusForegroundService.startService(preferences.context)
                }
                else -> {
                    CurrentStatusForegroundService.stopService(preferences.context)
                }
            }
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
                    this.exportAppDataToFile(it)
                }
            }
            SettingsActivity.DB_IMPORT_REQUEST_CODE -> {
                data?.data?.let {
                    this.restoreAppDataFromFile(it)
                }
            }
            SettingsActivity.CODE_BACKUP_DIR_REQUEST_CODE -> {
                data?.data?.let {
                    val activity = activity ?: return

                    // Get UriPermission so it's possible to write files
                    val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION

                    activity.contentResolver.takePersistableUriPermission(it, flags)

                    val parentFolder = DocumentFile.fromTreeUri(activity, it)!!

                    // Set backup Uri
                    preferences.automaticBackupLocation = parentFolder.uri.toString()

                    val summary = DocumentFile.fromTreeUri(preferenceManager.context, parentFolder.uri)!!.uri.lastPathSegment
                    automaticBackupLocationPref.summary = summary

                    cancelBackupWorker()
                    enqueueBackupWorker()
                }
            }
        }
    }

    private fun createBackup () {
        uiScope.launch {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                addCategory(Intent.CATEGORY_OPENABLE)

                // Create a file with the requested MIME type.
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "courierlocker_backup.json")
            }

            startActivityForResult(intent, SettingsActivity.DB_EXPORT_REQUEST_CODE)
        }

    }

    private fun importDB () {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            type = "application/json"
        }

        startActivityForResult(intent, SettingsActivity.DB_IMPORT_REQUEST_CODE)
    }

    private fun exportAppDataToFile(uri: Uri) {
        settingsActivity.showToastMessage(getString(R.string.exporting_app_data_starting))
        settingsActivity.toggleProgressIndicator(true)
        uiScope.launch(Dispatchers.IO) {
            try {
                val trips = settingsDao.getAllTrips()
                val apartments = settingsDao.getAllApartments()
                val gatecodes = settingsDao.getAllGateCodes()
                val customers = settingsDao.getAllCustomers()
                val gigLabels = settingsDao.getAllGigs()

                val rootObj = AppDataImportExportHelper.appModelsToJson(trips, apartments, gatecodes, customers, gigLabels)

                settingsActivity.contentResolver.openFileDescriptor(uri, "w")?.use { p ->
                    rootObj.toString().toByteArray()?.let {
                        FileOutputStream(p.fileDescriptor).use { outputStream ->
                            outputStream.write(it)
                            outputStream.close()
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    settingsActivity.showToastMessage(getString(R.string.exported_app_data_successfully))
                    settingsActivity.toggleProgressIndicator(false)
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    settingsActivity.showToastMessage("${ex.message}")
                    settingsActivity.toggleProgressIndicator(false)
                }
            }


        }
    }

    private fun restoreAppDataFromFile(uri: Uri) {
        settingsActivity.showToastMessage(getString(R.string.importing_app_data_starting))
        settingsActivity.toggleProgressIndicator(true)

        uiScope.launch(Dispatchers.IO) {
            try {
                settingsActivity.contentResolver.openFileDescriptor(uri, "r")?.use { p ->
                    FileInputStream(p.fileDescriptor).use { inputStream ->
                        val gson = Gson()
                        val rootObj = JsonParser().parse(String(inputStream.readBytes())).asJsonObject
                        inputStream.close()

                        val trips = gson.fromJson<List<Trip>>(rootObj.get("trips"))
                        val apartments = gson.fromJson<List<Apartment>>(rootObj.get("apartments"))
                        val customers = gson.fromJson<List<Customer>>(rootObj.get("customers"))
                        val gatecodes = gson.fromJson<List<GateCode>>(rootObj.get("gatecodes"))
                        val gigLabels = gson.fromJson<List<GigLabel>>(rootObj.get("gigLabels"))

                        settingsDao.clearTrips()
                        settingsDao.clearApartments()
                        settingsDao.clearCustomers()
                        settingsDao.clearGateCodes()

                        settingsDao.insertTrips(trips.toMutableList())
                        settingsDao.insertApartments(apartments.toMutableList())
                        settingsDao.insertCustomers(customers.toMutableList())
                        settingsDao.insertGateCodes(gatecodes.toMutableList())
                        settingsDao.insertGigLabels(gigLabels.toMutableList())

                        withContext(Dispatchers.Main) {
                            settingsActivity.showToastMessage(getString(R.string.imported_app_data_successfully))
                            settingsActivity.toggleProgressIndicator(false)

                            delay(2000)

                            val intent = Intent(settingsActivity, MainActivity::class.java)
                            startActivity(intent)
                            settingsActivity.onBackPressed()
                        }
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    settingsActivity.showToastMessage("${ex.message}")
                    settingsActivity.toggleProgressIndicator(false)
                }
            }

        }
    }

    /**
     *  starts worker for automatic backups
     */
    private fun enqueueBackupWorker() {
        val context = preferenceManager.context
        val workManager = WorkManager.getInstance(context)

        val createBackupWorkerRequest = PeriodicWorkRequestBuilder<CreateBackupWorker>(
            24, TimeUnit.HOURS, // runs once a day
            24, TimeUnit.HOURS
        )
        .addTag(context.getString(R.string.create_app_backup_one_time_worker_tag))
        .build()

        workManager.enqueueUniquePeriodicWork(context.getString(R.string.create_app_backup_one_time_worker), ExistingPeriodicWorkPolicy.REPLACE, createBackupWorkerRequest)
    }

    /**
     *  cancels worker for automatic backups
     */
    private fun cancelBackupWorker() {
        WorkManager
            .getInstance(preferenceManager.context)
            .cancelAllWorkByTag(preferenceManager.context.getString(R.string.create_app_backup_one_time_worker_tag))
    }
}