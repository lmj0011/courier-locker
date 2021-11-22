package name.lmj0011.courierlocker.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.documentfile.provider.DocumentFile
import androidx.preference.*
import androidx.work.*
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.*
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.*
import name.lmj0011.courierlocker.helpers.*
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
    private var devOptionsCountDown = 0

    private lateinit var createBackupPref: Preference
    private lateinit var restoreBackupPref: Preference
    private lateinit var enableAutomaticBackupsPref: SwitchPreferenceCompat
    private lateinit var automaticBackupLocationPref: Preference
    private lateinit var boundingCoordinatesDistance: EditTextPreference
    private lateinit var enableCurrentStatusService: SwitchPreferenceCompat
    private lateinit var showCurrentStatusAsBubble: SwitchPreferenceCompat
    private lateinit var appThemeListPreference: ListPreference
    private lateinit var appVersionPref: Preference
    private lateinit var appBuildPref: Preference
    private lateinit var appChangelogPref: Preference
    private lateinit var enableDevOptionsPref: SwitchPreferenceCompat
    private lateinit var googleDirectionsApiKeyPref: EditTextPreference

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
        automaticBackupLocationPref = findPreference("automaticBackupLocation")!!
        boundingCoordinatesDistance = findPreference(application.getString(R.string.pref_key_bounding_coordinates_distance))!!
        enableCurrentStatusService = findPreference(application.getString(R.string.pref_enable_current_status_service))!!
        showCurrentStatusAsBubble = findPreference("showCurrentStatusAsBubble")!!
        appThemeListPreference = findPreference(application.getString(R.string.pref_key_mode_night))!!
        appVersionPref = findPreference("appVersion")!!
        appBuildPref = findPreference("appBuild")!!
        appChangelogPref = findPreference("appChangelog")!!
        enableDevOptionsPref = findPreference(application.getString(R.string.pref_dev_options_enabled))!!
        googleDirectionsApiKeyPref = findPreference(application.getString(R.string.pref_google_directions_api_key))!!


        when {
            BuildConfig.DEBUG -> {
                enableDevOptionsPref.isChecked = true
                enableDevOptionsPref.isEnabled = false
                setupDevOptions()
            }
            enableDevOptionsPref.isChecked -> {
                setupDevOptions()
            }
            else -> removeDevOptions()
        }

        /**
         * Show some Preferences only on devices running Android 11 or higher
         */
        showCurrentStatusAsBubble.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        showCurrentStatusAsBubble.isEnabled = enableCurrentStatusService.isChecked


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
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            startActivityForResult(intent, SettingsActivity.BACKUP_DIR_REQUEST_CODE)
            true
        }

        if(enableAutomaticBackupsPref.isChecked) {
            var summary: String? = preferences.defaultBackupDir.toString()

            try {
                summary = DocumentFile.fromTreeUri(preferenceManager.context,
                    Uri.parse(preferences.automaticBackupLocation())
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
                    Uri.parse(preferences.automaticBackupLocation())
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
                    application.startCurrentStatusService()
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        showCurrentStatusAsBubble.isEnabled = true
                    }
                }
                else -> {
                    application.stopCurrentStatusService()
                    showCurrentStatusAsBubble.isEnabled = false
                }
            }
            true
        }

        showCurrentStatusAsBubble.setOnPreferenceChangeListener { _, newValue ->
            settingsActivity.toggleProgressIndicator(true)

            /**
             * We need to restart the Service so the updated preferences take effect
             */
            application.stopCurrentStatusService()

            launchIO {
                delay(1000L)
                withUIContext {
                    application.startCurrentStatusService()
                    settingsActivity.toggleProgressIndicator(false)
                }
            }

            true
        }

        appThemeListPreference.setOnPreferenceChangeListener { _, newValue ->
            when(newValue) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }

        appVersionPref.summary = BuildConfig.VERSION_NAME

        appBuildPref.summary = "${resources.getString(R.string.app_build)}, ${if(BuildConfig.DEBUG) "debug" else "production"}"

        /**
         * developer options unlocking sequence, only if we're not using a DEBUG build nor
         * have dev options enabled in preferences
         */
        if(!BuildConfig.DEBUG && !enableDevOptionsPref.isChecked) {
            appBuildPref.setOnPreferenceClickListener {
                val maxCount = 8
                devOptionsCountDown ++

                if(maxCount == devOptionsCountDown) {
                    enableDevOptionsPref.isChecked = true
                    (requireActivity() as SettingsActivity)
                        .showToastMessage("Developer Options unlocked!", Toast.LENGTH_LONG)
                    requireActivity().recreate()
                } else {
                    (requireActivity() as SettingsActivity)
                        .showToastMessage("${maxCount - devOptionsCountDown} taps away from unlocking Developer Options")
                }

                true
            }
        }

        enableDevOptionsPref.setOnPreferenceClickListener {
            if(!enableDevOptionsPref.isChecked) {
                preferences.resetDevOptionsPrefs()
                removeDevOptions()
            }
            true
        }


        val changeLogUrl = "https://github.com/lmj0011/courier-locker/commits/v${BuildConfig.VERSION_NAME}"
        appChangelogPref.summary = changeLogUrl

        appChangelogPref.setOnPreferenceClickListener { _ ->
            Util.openUrlInWebBrowser(requireContext(), changeLogUrl)
            true
        }

        googleDirectionsApiKeyPref.apply {
            text?.map { "•" }?.joinToString("").let { str ->
                summary = str
            }
        }

        googleDirectionsApiKeyPref.setOnPreferenceChangeListener { _, newValue ->
            // update the summary
            (newValue as String)

            newValue.map { "•" }.joinToString("").let { str ->
                googleDirectionsApiKeyPref.summary = str
            }

            // dispatch Worker to update distance values for all Trips
            if(newValue.isNotBlank())  {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workerRequest = OneTimeWorkRequestBuilder<CalculateAllTripDistanceWorker>()
                    .setConstraints(constraints)
                    .addTag(settingsActivity.getString(R.string.calculate_all_trip_distance_one_time_worker_tag))
                    .build()

                WorkManager.getInstance(application).enqueue(workerRequest)
            }
            true
        }

    }

    @SuppressLint("WrongConstant")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (resultCode != Activity.RESULT_OK) {
            Timber.i("Intent failed! [requestCode: $resultCode, resultCode: $resultCode]")
            return
        }

        when(requestCode) {
            SettingsActivity.DB_EXPORT_REQUEST_CODE -> {
                intent?.data?.let {
                    // ref: https://developer.android.com/guide/topics/providers/document-provider#edit
                    this.exportAppDataToFile(it)
                }
            }
            SettingsActivity.DB_IMPORT_REQUEST_CODE -> {
                intent?.data?.let {
                    this.restoreAppDataFromFile(it)
                }
            }
            SettingsActivity.BACKUP_DIR_REQUEST_CODE -> {
                intent?.data?.let {

                    // Get UriPermission so it's possible to write files
                    val flags = intent.flags and (Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    requireActivity().contentResolver.takePersistableUriPermission(it, flags)

                    val parentFolder = DocumentFile.fromTreeUri(requireActivity(), it)!!

                    // Set backup Uri
                    preferences.automaticBackupLocation(parentFolder.uri.toString())

                    val summary = DocumentFile.fromTreeUri(preferenceManager.context, parentFolder.uri)!!.uri.lastPathSegment
                    automaticBackupLocationPref.summary = summary

                    cancelBackupWorker()
                    enqueueBackupWorker()
                }
            }
        }
    }

    private fun setupDevOptions() {
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

    private fun removeDevOptions() {
        val prfScreen = findPreference<PreferenceScreen>("preferenceScreen")
        val debugCategory = findPreference<PreferenceCategory>("debugCategory")
        prfScreen?.removePreference(debugCategory)
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
                        val rootObj = JsonParser.parseString(String(inputStream.readBytes())).asJsonObject
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