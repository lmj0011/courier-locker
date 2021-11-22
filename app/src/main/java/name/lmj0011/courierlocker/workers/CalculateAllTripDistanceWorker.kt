package name.lmj0011.courierlocker.workers

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.helpers.NotificationHelper
import name.lmj0011.courierlocker.receivers.CancelWorkerByTagReceiver
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import timber.log.Timber
import java.lang.Exception
import kotlin.math.ceil

class CalculateAllTripDistanceWorker (private val appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    companion object {
        const val Progress = "Progress"
    }

    private var notificationCancelWorkerPendingIntent: PendingIntent

    init {
        val notificationCancelWorkerIntent = Intent(appContext, CancelWorkerByTagReceiver::class.java).apply {
            val tagArray = this@CalculateAllTripDistanceWorker.tags.toTypedArray()
            putExtra(appContext.getString(R.string.intent_extra_key_worker_tags), tagArray)
        }

        notificationCancelWorkerPendingIntent = PendingIntent.getBroadcast(appContext, 0, notificationCancelWorkerIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)
    }

    override suspend fun doWork(): Result {
        val application = appContext.applicationContext as Application
        val dataSource = CourierLockerDatabase.getInstance(appContext).tripDao
        val tripViewModel = TripViewModel(dataSource, application)
        val noDistanceTrips = dataSource.getAllNoDistanceTrips().filter { trip ->
            /**
             * We have all the Trips with distances = 0.0
             * now we have to determine if there were any stops
             * other than the pick up
             */
            when {
                (trip.pickupAddress != trip.dropOffAddress) -> true

                // if there any Stops and the address on any one is different from the trip's drop-off address
                (trip.stops.any() && trip.stops.any { stop -> stop.address !=  trip.dropOffAddress}) -> true

                // driver didn't go anywhere (canceled order, maybe) or forgot to add a Stop
                else -> false
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CALCULATE_ALL_TRIP_DISTANCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle(appContext.getString(R.string.notification_calculate_all_trip_distance))
            .setOnlyAlertOnce(true)
            .setColor((applicationContext as CourierLockerApplication).colorPrimaryResId)
            .addAction(0, appContext.getString(R.string.notification_action_button_cancel), notificationCancelWorkerPendingIntent)
            .build()

        val foregroundInfo = ForegroundInfo(NotificationHelper.CALCULATE_ALL_TRIP_DISTANCE_NOTIFICATION_ID, notification)
        setForeground(foregroundInfo)

        Timber.d("noDistanceTrips.size: ${noDistanceTrips.size}, noDistanceTrips: $noDistanceTrips")

        if (noDistanceTrips.isNotEmpty()) {
            var progress = 0f
            // (progressIncrement * <collection>.size) should always equal a minimum of 100
            var progressIncrement =  (100f / noDistanceTrips.size)


            var errorCnt = 0
            noDistanceTrips.forEachIndexed { index, trip ->
                try {
                    trip.distance = tripViewModel.calculateTripDistance(trip)
                    dataSource.update(trip)
                } catch (ex: Exception) {
                    errorCnt += 1
                    Timber.e(ex, "problem calculating trip distance for Trip: $trip")
                }

                progress = progress.plus(progressIncrement)
                val roundedUpProgress = ceil(progress).toInt()

                var message = when {
                    errorCnt > 0 -> "errors: $errorCnt (${index.plus(1)}/${noDistanceTrips.size})"
                    else -> "(${index.plus(1)}/${noDistanceTrips.size})"
                }


                showProgress(roundedUpProgress, message)
                setProgress(workDataOf(Progress to roundedUpProgress))
            }
        }

        // gives progress enough time to get passed to Observer(s)
        delay(1000L)
        return Result.success()
    }

    private fun showProgress(progress: Int, message: String = "") {
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CALCULATE_ALL_TRIP_DISTANCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle(appContext.getString(R.string.notification_calculate_all_trip_distance))
            .setContentText(message)
            .setProgress(100, progress, false)
            .setColor((applicationContext as CourierLockerApplication).colorPrimaryResId)
            .addAction(0, appContext.getString(R.string.notification_action_button_cancel), notificationCancelWorkerPendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).apply {
            notify(NotificationHelper.CALCULATE_ALL_TRIP_DISTANCE_NOTIFICATION_ID, notification)
        }
    }
}