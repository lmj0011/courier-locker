package name.lmj0011.courierlocker.helpers

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY
import androidx.core.content.ContextCompat
import name.lmj0011.courierlocker.R


/**
 *  This is a static class mainly for calling functions that return a NotificationCompat.Builder. If you're
 *  calling the same notification in more than one place, then save some code by calling its Builder from here
 */
object NotificationHelper {
    const val CURRENT_STATUS_CHANNEL_ID = "name.lmj0011.courierlocker.helpers.NotificationHelper#currentStatus"
    const val CALCULATE_ALL_TRIP_DISTANCE_CHANNEL_ID = "name.lmj0011.courierlocker.helpers.NotificationHelper#calculateAllTripDistance"
    const val APP_BACKUP_CHANNEL_ID = "name.lmj0011.courierlocker.helpers.NotificationHelper#appBackup"
    const val ACTION_UPDATE_DROP_OFF = "name.lmj0011.courierlocker.services.ACTION_UPDATE_DROP_OFF"
    const val ACTION_NEXT_RECENT_TRIP = "name.lmj0011.courierlocker.services.ACTION_NEXT_RECENT_TRIP"
    const val NOTIFICATION_CURRENT_STATUS_GROUP_KEY = "name.lmj0011.courierlocker.NOTIFICATION_CURRENT_STATUS_GROUP_KEY"
    const val CURRENT_STATUS_NOTIFICATION_ID = 1000
    const val NEARBY_GATECODES_NOTIFICATION_ID = 1002
    const val TRIPS_TODAY_NOTIFICATION_ID = 1003
    const val RECENT_TRIP_NOTIFICATION_ID = 1004
    const val CALCULATE_ALL_TRIP_DISTANCE_NOTIFICATION_ID = 1005
    const val CREATE_APP_BACKUP_NOTIFICATION_ID = 1006


    /**
     * create all necessary Notification channels here
     */
    fun init(application: Application) {
        val serviceChannel1 = NotificationChannel(CURRENT_STATUS_CHANNEL_ID, "Current Status", NotificationManager.IMPORTANCE_DEFAULT)
        serviceChannel1.setSound(null, null)

        val serviceChannel2 = NotificationChannel(CALCULATE_ALL_TRIP_DISTANCE_CHANNEL_ID, "Calculate all Trip Distances", NotificationManager.IMPORTANCE_DEFAULT)
        serviceChannel2.setSound(null, null)

        val serviceChannel3 = NotificationChannel(APP_BACKUP_CHANNEL_ID, "Backups", NotificationManager.IMPORTANCE_DEFAULT)
        serviceChannel3.setSound(null, null)

        val manager = application.getSystemService(NotificationManager::class.java)

        manager!!.createNotificationChannels(mutableListOf(serviceChannel1, serviceChannel2, serviceChannel3))
    }

    fun getRecentTripNotificationBuilder(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CURRENT_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_name)
            .setShowWhen(false)
            .setContentTitle("recent trips")
            .setGroup(NOTIFICATION_CURRENT_STATUS_GROUP_KEY)
            .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
            .setOnlyAlertOnce(true)
            .setSortKey("a")
            .setColor(ContextCompat.getColor(context, R.color.colorDefaultIcon))
    }
}