package name.lmj0011.courierlocker.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.helpers.AppDataImportExportHelper
import name.lmj0011.courierlocker.helpers.NotificationHelper
import name.lmj0011.courierlocker.helpers.PreferenceHelper
import name.lmj0011.courierlocker.helpers.Util
import name.lmj0011.courierlocker.receivers.CancelWorkerByTagReceiver
import org.kodein.di.instance
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class CreateBackupWorker (private val appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    companion object {
        const val Progress = "Progress"
    }

    private lateinit var preferences: PreferenceHelper
    private var notificationCancelWorkerPendingIntent: PendingIntent

    init {
        val notificationCancelWorkerIntent = Intent(appContext, CancelWorkerByTagReceiver::class.java).apply {
            val tagArray = this@CreateBackupWorker.tags.toTypedArray()
            putExtra(appContext.getString(R.string.intent_extra_key_worker_tags), tagArray)
        }

        notificationCancelWorkerPendingIntent = PendingIntent.getBroadcast(appContext, 0, notificationCancelWorkerIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)
    }

    override suspend fun doWork(): Result {
        val application = appContext.applicationContext as CourierLockerApplication
        preferences = application.kodein.instance()
        val settingsDao = CourierLockerDatabase.getInstance(application).settingsDao

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.APP_BACKUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle(appContext.getString(R.string.creating_app_backup))
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(appContext, R.color.colorDefaultIcon))
            .addAction(0, appContext.getString(R.string.notification_action_button_cancel), notificationCancelWorkerPendingIntent)
            .build()

        val foregroundInfo = ForegroundInfo(NotificationHelper.CREATE_APP_BACKUP_NOTIFICATION_ID, notification)
        setForeground(foregroundInfo)

        try {
            val trips = settingsDao.getAllTrips()
            val apartments = settingsDao.getAllApartments()
            val gatecodes = settingsDao.getAllGateCodes()
            val customers = settingsDao.getAllCustomers()
            val gigLabels = settingsDao.getAllGigs()

            showProgress(25, "25%")
            setProgress(workDataOf(Progress to 25))

            val rootObj = AppDataImportExportHelper.appModelsToJson(trips, apartments, gatecodes, customers, gigLabels)

            showProgress(50, "50%")
            setProgress(workDataOf(Progress to 50))


            preferences.defaultBackupDir.toFile().mkdirs()

            var targetDocFile: DocumentFile? = null
            var fd: ParcelFileDescriptor?
            var targetFile = File(preferences.defaultBackupDir.toFile().absolutePath + "/${Util.getUniqueFileNamePrefix()}-courierlocker-backup.json")
            targetFile.createNewFile()

            try {
                val tree = DocumentFile.fromTreeUri(appContext, Uri.parse(preferences.automaticBackupLocation()))!!
                targetDocFile = tree.createFile("application/json", "${Util.getUniqueFileNamePrefix()}-courierlocker-backup.json")
            } catch (ex: IllegalArgumentException) {
                // this error should only be thrown, if the User hasn't selected a directory yet
            }

            if (targetDocFile != null) { // we will write a backup to the user selected directory
                fd = appContext.contentResolver.openFileDescriptor(targetDocFile.uri, "w")
                targetFile.delete()
            } else { // we will write a backup to the default directory
                fd = appContext.contentResolver.openFileDescriptor(targetFile.toUri(), "w")
            }

            fd?.use { p ->
                rootObj.toString().toByteArray()?.let {
                    FileOutputStream(p.fileDescriptor).use { outputStream ->
                        outputStream.write(it)
                        outputStream.close()
                    }
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }

        showProgress(100, "100%")
        setProgress(workDataOf(Progress to 100))

        delay(1000)
        return Result.success()
    }

    private fun showProgress(progress: Int, message: String = "") {
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.APP_BACKUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle(appContext.getString(R.string.creating_app_backup))
            .setContentText(message)
            .setProgress(100, progress, false)
            .setColor(ContextCompat.getColor(appContext, R.color.colorDefaultIcon))
            .addAction(0, appContext.getString(R.string.notification_action_button_cancel), notificationCancelWorkerPendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).apply {
            notify(NotificationHelper.CREATE_APP_BACKUP_NOTIFICATION_ID, notification)
        }
    }
}