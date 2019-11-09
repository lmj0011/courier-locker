package name.lmj0011.courierlocker.helpers

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
    const val ACTION_UPDATE_DROP_OFF = "name.lmj0011.courierlocker.services.ACTION_UPDATE_DROP_OFF"
    const val ACTION_DELETE_DROP_OFF = "name.lmj0011.courierlocker.services.ACTION_DELETE_DROP_OFF"
    const val ACTION_NEXT_PENDING_TRIP = "name.lmj0011.courierlocker.services.ACTION_NEXT_PENDING_TRIP"
    const val NOTIFICATION_GROUP_KEY_FOREGROUND = "name.lmj0011.courierlocker.NOTIFICATION_GROUP_KEY_FOREGROUND"
    const val RUNNING_FOREGROUND_SERVICES_NOTIFICATION_ID = 1000
    const val NEARBY_GATECODES_NOTIFICATION_ID = 1002
    const val TRIPS_TODAY_NOTIFICATION_ID = 1003
    const val PENDING_TRIP_NOTIFICATION_ID = 1004


    /**
     * create all necessary Notification channels here
     */
    fun init(application: Application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CURRENT_STATUS_CHANNEL_ID, "Current Status", NotificationManager.IMPORTANCE_DEFAULT)
            serviceChannel.setSound(null, null)

            val manager = application.getSystemService(NotificationManager::class.java)

            manager!!.createNotificationChannel(serviceChannel)
        }
    }

    fun getPendingTripNotificationBuilder(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CURRENT_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_name)
            .setContentTitle("pending trip")
            .setGroup(NOTIFICATION_GROUP_KEY_FOREGROUND)
            .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
            .setOnlyAlertOnce(true)
            .setSortKey("b")
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
    }
}