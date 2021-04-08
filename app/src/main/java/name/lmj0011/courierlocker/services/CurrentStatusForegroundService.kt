package name.lmj0011.courierlocker.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.DeepLinkActivity
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
    private val binder = CurrentStatusServiceBinder()
    private var mutableListOfGateCodes = mutableListOf<GateCode>()
    private var mutableListOfTrips = mutableListOf<Trip>()
    private lateinit var gateCodeViewModel: GateCodeViewModel
    lateinit var tripViewModel: TripViewModel
        private set
    private lateinit var latitudeObserver: Observer<Double>
    private lateinit var gateCodesObserver: Observer<MutableList<GateCode>>
    private lateinit var tripsObserver: Observer<MutableList<Trip>>
    private lateinit var recentTripsObserver: Observer<MutableList<Trip>>
    private lateinit var recentTripsListIterator: MutableListIterator<Trip>
    lateinit var listOfRecentTrips: MutableList<Trip>
        private set

    inner class CurrentStatusServiceBinder : Binder() {
        fun getService(): CurrentStatusForegroundService {
            return this@CurrentStatusForegroundService
        }
    }


    /**
     * starts this foreground service
     */
    fun start(context: Context) {
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

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

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
            .build()

        startForeground(NotificationHelper.CURRENT_STATUS_NOTIFICATION_ID, notification)

        tripViewModel.trips.observeOnce {
            recentTripsNotification(this)
        }
        this.startTripsTodayNotification()
        this.startNearbyGatecodesNotification()
    }

    /**
     * stops this foreground service
     */
    fun stop() {
        tripViewModel.trips.removeObserver(recentTripsObserver)
        LocationHelper.lastLatitude.removeObservers(this)
        gateCodeViewModel.gateCodes.removeObservers(this)
        stopForeground(true)
    }

    private fun resetListOfRecentTripsIterator() {
        if(!::listOfRecentTrips.isInitialized) return

        recentTripsListIterator = listOfRecentTrips.listIterator()
    }


    fun recentTripsNotification(context: Context) {
        var trip: Trip?

        if(!::recentTripsListIterator.isInitialized) return

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

            val notificationContentIntent = Intent(context, DeepLinkActivity::class.java).apply {
                action = MainActivity.INTENT_EDIT_TRIP
                putExtra("editTripId", t.id.toInt())
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


    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private fun startTripsTodayNotification() {
        val notificationIntent = Intent(this, DeepLinkActivity::class.java).apply {
            action = MainActivity.INTENT_SHOW_TRIPS
            putExtra("menuItemId", R.id.nav_trips)
        }

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
        val notificationIntent = Intent(this, DeepLinkActivity::class.java).apply {
            action = MainActivity.INTENT_SHOW_GATE_CODES
            putExtra("menuItemId", R.id.nav_gate_codes)
        }

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
            val isBounded = (context.applicationContext as CourierLockerApplication).isCurrentStatusServiceBounded

            if (isBounded) {
                val service = (context.applicationContext as CourierLockerApplication).currentStatusService

                val tripId  = intent.extras?.getLong("TripId")
                val address = LocationHelper.getFromLocation(null, LocationHelper.lastLatitude.value!!, LocationHelper.lastLongitude.value!!, 1)

                val trip = service.tripViewModel.trips.value?.find { t ->
                    t.id == tripId
                }

                trip?.let{ t ->
                    if(address.isNotEmpty()) {
                        val stop = Stop(address[0].getAddressLine(0), address[0].latitude, address[0].longitude)
                        t.stops.add(stop)
                    }

                    service.tripViewModel.updateTrip(t)

                    val notificationActionIntent = Intent(context, SetTripDropoffReceiver::class.java).apply {
                        action = NotificationHelper.ACTION_UPDATE_DROP_OFF
                        putExtra("TripId", t.id)
                        addCategory("recent trips")
                    }

                    val notificationContentIntent = Intent(context, DeepLinkActivity::class.java).apply {
                        action = MainActivity.INTENT_SHOW_TRIPS
                        putExtra("menuItemId", R.id.nav_trips)
                    }

                    val notificationNextRecentTripIntent = Intent(context, NextRecentTripNotificationReceiver::class.java).apply {
                        action = NotificationHelper.ACTION_NEXT_RECENT_TRIP
                    }

                    val actionPendingIntent = PendingIntent.getBroadcast(context, 0, notificationActionIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                    val contentPendingIntent = PendingIntent.getActivity(context, 0, notificationContentIntent, 0)
                    val actionNextRecentTripIntent = PendingIntent.getBroadcast(context, 0, notificationNextRecentTripIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                    val notification = NotificationHelper.getRecentTripNotificationBuilder(context)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(Util.formatRecentTripMessage(t)))
                        .setContentIntent(contentPendingIntent)
                        .addAction(R.drawable.ic_action_name, "Add Stop", actionPendingIntent)

                    if(service.listOfRecentTrips.size > 1) {
                        notification.addAction(R.drawable.ic_action_name, "Next Trip", actionNextRecentTripIntent)
                    }

                    NotificationManagerCompat.from(context).apply {
                        notify(NotificationHelper.RECENT_TRIP_NOTIFICATION_ID, notification.build())
                    }
                    //////
                }
            }
        }
    }


     class NextRecentTripNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isBounded = (context.applicationContext as CourierLockerApplication).isCurrentStatusServiceBounded

            if (isBounded) {
                val service = (context.applicationContext as CourierLockerApplication).currentStatusService
                service.recentTripsNotification(context)
            }
        }
    }
}