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
import androidx.lifecycle.Observer
import name.lmj0011.courierlocker.MainActivity
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.adapters.GateCodeListAdapter
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.viewmodels.TripViewModel

class CurrentStatusForegroundService : Service() {
    private var mutableListOfGateCodes = mutableListOf<GateCode>()
    private var mutableListOfTrips = mutableListOf<Trip>()

    companion object {
        private lateinit var gateCodeViewModel: GateCodeViewModel
        private lateinit var tripViewModel: TripViewModel
        private lateinit var latitudeObserver: Observer<Double>
        private lateinit var gateCodesObserver: Observer<MutableList<GateCode>>
        private lateinit var tripsObserver: Observer<MutableList<Trip>>
        private lateinit var pendingTripsObserver: Observer<MutableList<Trip>>
        private lateinit var pendingTripsListIterator: MutableListIterator<Trip>
        private lateinit var listOfPendingTrips: MutableList<Trip>

        fun startService(context: Context) {
            val startIntent = Intent(context, CurrentStatusForegroundService::class.java)
            val application = requireNotNull(context.applicationContext as Application)
            gateCodeViewModel = GateCodeViewModel(CourierLockerDatabase.getInstance(application).gateCodeDao, application)
            tripViewModel = TripViewModel(CourierLockerDatabase.getInstance(application).tripDao, application)

            pendingTripsObserver = Observer {
                it?.let {
                    listOfPendingTrips = it.filter { t ->
                        t.dropOffAddress.isNullOrBlank()
                    }.toMutableList()

                    resetListOfPendingTripsIterator()
                }
            }

            tripViewModel.trips.observeForever(pendingTripsObserver)

            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, CurrentStatusForegroundService::class.java)
            removeAllObservers()
            context.stopService(stopIntent)
        }

        private fun resetListOfPendingTripsIterator() {
            if(!::listOfPendingTrips.isInitialized) return

            pendingTripsListIterator = listOfPendingTrips.listIterator()
        }


        fun pendingTripsNotification(context: Context) {
            var trip: Trip?

            try {
                trip = pendingTripsListIterator.next()
            } catch (ex: NoSuchElementException) {
                return
            }

            if(!pendingTripsListIterator.hasNext()) {
                resetListOfPendingTripsIterator()
            }

            trip?.let { t ->
                val notificationActionIntent = Intent(context, SetTripDropoffReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_UPDATE_DROP_OFF
                    putExtra("TripId", t.id)
                    addCategory("pending trip")
                }

                val notificationContentIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("menuItemId", R.id.nav_trips)
                    addCategory("pending trip")
                }

                val notificationDeleteIntent = Intent(context, PendingTripNotificationDeleteReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_DELETE_DROP_OFF
                    putExtra("TripId", t.id)
                    addCategory("pending trip")
                }

                val notificationNextPendingTripIntent = Intent(context, NextPendingTripNotificationReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_NEXT_PENDING_TRIP
                    addCategory("next pending")
                }

                val actionPendingIntent = PendingIntent.getBroadcast(context, 0, notificationActionIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                val contentPendingIntent = PendingIntent.getActivity(context, 0, notificationContentIntent, 0)
                val deleteIntent = PendingIntent.getBroadcast(context, 0, notificationDeleteIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                val actionNextPendingTripIntent = PendingIntent.getBroadcast(context, 0, notificationNextPendingTripIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                val notification = NotificationHelper.getPendingTripNotificationBuilder(context)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(formatPendingTripMessage(t)))
                    .setContentIntent(contentPendingIntent)
                    .addAction(R.drawable.ic_action_name, "Update Drop-off", actionPendingIntent)
                    .setDeleteIntent(deleteIntent)

                if(listOfPendingTrips.size > 1) {
                    notification.addAction(R.drawable.ic_action_name, "Next Pending", actionNextPendingTripIntent)
                }

                NotificationManagerCompat.from(context).apply {
                    notify(NotificationHelper.PENDING_TRIP_NOTIFICATION_ID, notification.build())
                }
            }
        }

        private fun removeAllObservers() {
            if(::gateCodeViewModel.isInitialized && ::gateCodesObserver.isInitialized) {
                gateCodeViewModel.gateCodes.removeObserver(gateCodesObserver)
            }

            if (::latitudeObserver.isInitialized){
                LocationHelper.lastLatitude.removeObserver(latitudeObserver)
            }

            if (::tripViewModel.isInitialized && ::tripsObserver.isInitialized){
                tripViewModel.trips.removeObserver(tripsObserver)
            }

            if (::tripViewModel.isInitialized && ::pendingTripsObserver.isInitialized){
                tripViewModel.trips.removeObserver(pendingTripsObserver)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LocationHelper.setFusedLocationClient(this)
        LocationHelper.startLocationUpdates()

        //do heavy work on a background thread

        // add category to Intent to make it unique
        val notificationIntent = Intent(this, MainActivity::class.java).addCategory("Running Foreground service.")
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(this, NotificationHelper.CURRENT_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_name)
            .setContentText("Running Foreground service.")
            .setContentIntent(pendingIntent)
            .setGroup(NotificationHelper.NOTIFICATION_GROUP_KEY_FOREGROUND)
            .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
            .setGroupSummary(true)
            .setSortKey("a")
            .setColor(ContextCompat.getColor(this,R.color.colorPrimary))
            .build()

        startForeground(NotificationHelper.RUNNING_FOREGROUND_SERVICES_NOTIFICATION_ID, notification)

        tripViewModel.trips.observeOnce(Observer { pendingTripsNotification(this) })
        this.startTripsTodayNotification()
        this.startNearbyGatecodesNotification()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
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
                    .setContentTitle("trips today")
                    .setContentText("earnings: ${tripViewModel.todayTotalMoney} | pending: ${tripViewModel.totalPendingTrips} | completed: ${tripViewModel.todayCompletedTrips}")
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

        tripViewModel.trips.observeForever(tripsObserver)
    }

    private fun startNearbyGatecodesNotification () {
        val notificationIntent = Intent(this, MainActivity::class.java).addCategory("nearest gate code")
        notificationIntent.putExtra("menuItemId", R.id.nav_gate_codes)

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val listAdapter = GateCodeListAdapter(GateCodeListAdapter.GateCodeListener { })
        var mutableList: MutableList<GateCode>
        var list: List<GateCode>
        var notification: Notification

        gateCodesObserver = Observer {
            mutableListOfGateCodes = it
        }

        gateCodeViewModel.gateCodes.observeForever(gateCodesObserver)


        latitudeObserver = Observer {
            if (mutableListOfGateCodes.isNullOrEmpty().not()) {

                mutableList = listAdapter.filterByClosestGateCodeLocation(mutableListOfGateCodes)
                list = mutableList.take(1).toList()

                notification = NotificationCompat.Builder(this, NotificationHelper.CURRENT_STATUS_CHANNEL_ID)
                    .setContentTitle("nearest gate code")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(formatGateCodes(list)))
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

        LocationHelper.lastLatitude.observeForever(latitudeObserver)
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
                    t.dropOffAddress = address[0].getAddressLine(0)
                    t.dropOffAddressLatitude = address[0].latitude
                    t.dropOffAddressLongitude = address[0].longitude
                }

                tripViewModel.updateTrip(t)

                val notificationActionIntent = Intent(context, SetTripDropoffReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_UPDATE_DROP_OFF
                    putExtra("TripId", t.id)
                    addCategory("pending trip")
                }

                val notificationContentIntent = Intent(context, MainActivity::class.java).addCategory("trips today").apply {
                    putExtra("menuItemId", R.id.nav_trips)
                }

                val notificationDeleteIntent = Intent(context, PendingTripNotificationDeleteReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_DELETE_DROP_OFF
                    putExtra("TripId", t.id)
                    addCategory("pending trip")
                }

                val notificationNextPendingTripIntent = Intent(context, NextPendingTripNotificationReceiver::class.java).apply {
                    action = NotificationHelper.ACTION_NEXT_PENDING_TRIP
                    addCategory("next pending")
                }

                val actionPendingIntent = PendingIntent.getBroadcast(context, 0, notificationActionIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                val contentPendingIntent = PendingIntent.getActivity(context, 0, notificationContentIntent, 0)
                val deleteIntent = PendingIntent.getBroadcast(context, 0, notificationDeleteIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                val actionNextPendingTripIntent = PendingIntent.getBroadcast(context, 0, notificationNextPendingTripIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                val notification = NotificationHelper.getPendingTripNotificationBuilder(context)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(formatPendingTripMessage(t)))
                    .setContentIntent(contentPendingIntent)
                    .addAction(R.drawable.ic_action_name, "Update Drop-off", actionPendingIntent)
                    .setDeleteIntent(deleteIntent)

                if(listOfPendingTrips.size > 1) {
                    notification.addAction(R.drawable.ic_action_name, "Next Pending", actionNextPendingTripIntent)
                }

                NotificationManagerCompat.from(context).apply {
                    notify(NotificationHelper.PENDING_TRIP_NOTIFICATION_ID, notification.build())
                }
                //////
            }

        }
    }

    class PendingTripNotificationDeleteReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            pendingTripsNotification(context)
        }
    }

    class NextPendingTripNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            pendingTripsNotification(context)
        }
    }
}