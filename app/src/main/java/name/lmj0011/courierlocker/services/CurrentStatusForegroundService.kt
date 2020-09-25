package name.lmj0011.courierlocker.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.Stop
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.NotificationHelper
import name.lmj0011.courierlocker.helpers.Util
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import timber.log.Timber
import java.lang.Exception

// extension function for LiveData
// ref: https://stackoverflow.com/a/54969114/2445763
fun <T> LiveData<T>.observeOnce(observer: Observer<T>) {
    observeForever(object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

class CurrentStatusForegroundService : LifecycleService() {
    private var mutableListOfGateCodes = mutableListOf<GateCode>()
    private var mutableListOfTrips = mutableListOf<Trip>()

    companion object {
        private lateinit var gateCodeViewModel: GateCodeViewModel
        private lateinit var tripViewModel: TripViewModel
        private lateinit var latitudeObserver: Observer<Double>
        private lateinit var gateCodesObserver: Observer<MutableList<GateCode>>
        private lateinit var tripsObserver: Observer<MutableList<Trip>>
        private lateinit var recentTripsObserver: Observer<MutableList<Trip>>
        private lateinit var recentTripsListIterator: MutableListIterator<Trip>
        private lateinit var listOfRecentTrips: MutableList<Trip>

        fun startService(context: Context) {
            val startIntent = Intent(context, CurrentStatusForegroundService::class.java)
            val application = requireNotNull(context.applicationContext as Application)
            gateCodeViewModel = GateCodeViewModel(CourierLockerDatabase.getInstance(application).gateCodeDao, application)
            tripViewModel = TripViewModel(CourierLockerDatabase.getInstance(application).tripDao, application)

            recentTripsObserver = Observer {
                it?.let {
                    listOfRecentTrips = it.take(3).toMutableList()
                    resetListOfRecentTripsIterator()
                }
            }

            tripViewModel.trips.observeForever(recentTripsObserver)

            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, CurrentStatusForegroundService::class.java)
            context.stopService(stopIntent)
        }

        private fun resetListOfRecentTripsIterator() {
            if(!::listOfRecentTrips.isInitialized) return

            recentTripsListIterator = listOfRecentTrips.listIterator()
        }


        fun recentTripsNotification(context: Context) {
            var trip: Trip?

            if(!recentTripsListIterator.hasNext()) {
                resetListOfRecentTripsIterator()
            }

            try {
                trip = recentTripsListIterator.next()
            } catch (ex: NoSuchElementException) {
                return
            }


            trip?.let { t ->
                val notificationActionIntent = Intent(context, SetTripDropoffReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_UPDATE_DROP_OFF
                    putExtra("TripId", t.id)
                    addCategory("recent trips")
                }

                val notificationContentIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("editTripId", t.id.toInt())
                    addCategory("recent trips")
                }


                val notificationNextRecentTripIntent = Intent(context, NextRecentTripNotificationReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_NEXT_RECENT_TRIP
                    addCategory("next trip")
                }

                val actionPendingIntent = PendingIntent.getBroadcast(context, 0, notificationActionIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                val contentPendingIntent = PendingIntent.getActivity(context, 0, notificationContentIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                val actionNextRecentTripIntent = PendingIntent.getBroadcast(context, 0, notificationNextRecentTripIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                val notification = NotificationHelper.getRecentTripNotificationBuilder(context)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(Util.formatRecentTripMessage(t)))
                    .setContentIntent(contentPendingIntent)
                    .addAction(R.drawable.ic_action_name, "Add Stop", actionPendingIntent)

                if(listOfRecentTrips.size > 1) {
                    notification.addAction(R.drawable.ic_action_name, "Next Trip", actionNextRecentTripIntent)
                }

                NotificationManagerCompat.from(context).apply {
                    notify(NotificationHelper.RECENT_TRIP_NOTIFICATION_ID, notification.build())
                }
            }
        }

        fun clearNotifications(context: Context) {
            NotificationManagerCompat.from(context).apply {
                cancel(NotificationHelper.RECENT_TRIP_NOTIFICATION_ID)
                cancel(NotificationHelper.TRIPS_TODAY_NOTIFICATION_ID)
                cancel(NotificationHelper.NEARBY_GATECODES_NOTIFICATION_ID)
            }
        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // add category to Intent to make it unique
        val notificationIntent = Intent(this, MainActivity::class.java).addCategory("Running Foreground service.")
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val closeServiceNotificationIntent = Intent(this,  CloseServicePendingIntentNotificationReceiver::class.java).addCategory("Running Foreground service.")
        val closeServicePendingIntent = PendingIntent.getBroadcast(this, 0, closeServiceNotificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val notification = NotificationCompat.Builder(this, NotificationHelper.CURRENT_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_name)
            .setShowWhen(false)
            .setContentTitle("Current Status")
            .setContentText("Running Foreground service.")
            .setContentIntent(pendingIntent)
            .setGroup(NotificationHelper.NOTIFICATION_GROUP_KEY_FOREGROUND)
            .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
            .setGroupSummary(true)
            .setSortKey("a")
            .setColor(ContextCompat.getColor(this,R.color.colorPrimary))
            .addAction(R.drawable.ic_action_name, "Close", closeServicePendingIntent)
            .build()

        startForeground(NotificationHelper.RUNNING_FOREGROUND_SERVICES_NOTIFICATION_ID, notification)

        tripViewModel.trips.observeOnce(Observer { recentTripsNotification(this) })
        this.startTripsTodayNotification()
        this.startNearbyGatecodesNotification()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        try {
            tripViewModel.trips.removeObserver(recentTripsObserver)
            GlobalScope.launch {
                // dismiss any lingering notifications
                delay(3000)
                clearNotifications(this@CurrentStatusForegroundService)
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }

        super.onDestroy()
    }


    private fun startTripsTodayNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java).addCategory("trips today")
        notificationIntent.putExtra("menuItemId", R.id.nav_trips)

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        var notification: Notification

        tripsObserver = Observer {
            mutableListOfTrips = it

            if (it.isNullOrEmpty().not()) {
                notification = NotificationCompat.Builder(this, NotificationHelper.CURRENT_STATUS_CHANNEL_ID)
                    .setShowWhen(false)
                    .setContentTitle("today's stats")
                    .setContentText("earnings: ${tripViewModel.todayTotalMoney} | trips: ${tripViewModel.todayCompletedTrips}")
                    .setSmallIcon(R.drawable.ic_action_name)
                    .setContentIntent(pendingIntent)
                    .setGroup(NotificationHelper.NOTIFICATION_GROUP_KEY_FOREGROUND)
                    .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                    .setOnlyAlertOnce(true)
                    .setSortKey("c")
                    .setColor(ContextCompat.getColor(this,R.color.colorPrimary))
                    .build()

                NotificationManagerCompat.from(this).apply {
                    notify(NotificationHelper.TRIPS_TODAY_NOTIFICATION_ID, notification)
                }
            }
        }

        tripViewModel.tripPayAmountsForToday.observe(this, Observer {})
        tripViewModel.trips.observe(this, tripsObserver)
    }

    private fun startNearbyGatecodesNotification () {
        val notificationIntent = Intent(this, MainActivity::class.java).addCategory("nearest gate code")
        notificationIntent.putExtra("menuItemId", R.id.nav_gate_codes)

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        var notification: Notification

        gateCodesObserver = Observer {
            mutableListOfGateCodes = it
        }

        gateCodeViewModel.gateCodes.observe(this, gateCodesObserver)


        latitudeObserver = Observer {
            if (mutableListOfGateCodes.isNullOrEmpty().not()) {

                val list = mutableListOfGateCodes.let { list ->
                    list.sortedBy {
                        LocationHelper.calculateApproxDistanceBetweenMapPoints(
                            LocationHelper.lastLatitude.value!!,
                            LocationHelper.lastLongitude.value!!,
                            it.latitude,
                            it.longitude
                        )
                    }
                }.take(1)

                notification = NotificationCompat.Builder(this, NotificationHelper.CURRENT_STATUS_CHANNEL_ID)
                    .setShowWhen(false)
                    .setContentTitle("nearest gate code")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(Util.formatGateCodes(list)))
                    .setSmallIcon(R.drawable.ic_action_name)
                    .setContentIntent(pendingIntent)
                    .setGroup(NotificationHelper.NOTIFICATION_GROUP_KEY_FOREGROUND)
                    .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                    .setOnlyAlertOnce(true)
                    .setSortKey("d")
                    .setColor(ContextCompat.getColor(this,R.color.colorPrimary))
                    .build()

                NotificationManagerCompat.from(this).apply {
                    notify(NotificationHelper.NEARBY_GATECODES_NOTIFICATION_ID, notification)
                }
            }

        }

        LocationHelper.lastLatitude.observe(this, latitudeObserver)
    }

    class SetTripDropoffReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val tripId  = intent.extras?.getLong("TripId")
            val address = LocationHelper.getFromLocation(null, LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!, 1)

            val trip = tripViewModel.trips.value?.find { t ->
                t.id == tripId
            }

            trip?.let{ t ->
                if(address.isNotEmpty()) {
                    val stop = Stop(address[0].getAddressLine(0), address[0].latitude, address[0].longitude)
                    t.stops.add(stop)
                }

                tripViewModel.updateTrip(t)

                val notificationActionIntent = Intent(context, SetTripDropoffReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_UPDATE_DROP_OFF
                    putExtra("TripId", t.id)
                    addCategory("recent trips")
                }

                val notificationContentIntent = Intent(context, MainActivity::class.java).addCategory("trips today").apply {
                    putExtra("menuItemId", R.id.nav_trips)
                }

                val notificationDeleteIntent = Intent(context, RecentTripNotificationDeleteReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_DELETE_DROP_OFF
                    putExtra("TripId", t.id)
                    addCategory("recent trip")
                }

                val notificationNextRecentTripIntent = Intent(context, NextRecentTripNotificationReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_NEXT_RECENT_TRIP
                    addCategory("next trip")
                }

                val actionPendingIntent = PendingIntent.getBroadcast(context, 0, notificationActionIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                val contentPendingIntent = PendingIntent.getActivity(context, 0, notificationContentIntent, 0)
                val deleteIntent = PendingIntent.getBroadcast(context, 0, notificationDeleteIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                val actionNextRecentTripIntent = PendingIntent.getBroadcast(context, 0, notificationNextRecentTripIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                val notification = NotificationHelper.getRecentTripNotificationBuilder(context)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(Util.formatRecentTripMessage(t)))
                    .setContentIntent(contentPendingIntent)
                    .addAction(R.drawable.ic_action_name, "Add Stop", actionPendingIntent)
                    .setDeleteIntent(deleteIntent)

                if(listOfRecentTrips.size > 1) {
                    notification.addAction(R.drawable.ic_action_name, "Next Trip", actionNextRecentTripIntent)
                }

                NotificationManagerCompat.from(context).apply {
                    notify(NotificationHelper.RECENT_TRIP_NOTIFICATION_ID, notification.build())
                }
                //////
            }

        }
    }

    class RecentTripNotificationDeleteReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            recentTripsNotification(context)
        }
    }

    class NextRecentTripNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            recentTripsNotification(context)
        }
    }

    class CloseServicePendingIntentNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stopService(context)
        }
    }
}