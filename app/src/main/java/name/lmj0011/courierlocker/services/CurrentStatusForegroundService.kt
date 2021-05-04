package name.lmj0011.courierlocker.services

import android.app.*
import android.content.*
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.*
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.Stop
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel
import name.lmj0011.courierlocker.viewmodels.TripViewModel
import org.kodein.di.instance

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
    private lateinit var gateCodeViewModel: GateCodeViewModel
    lateinit var tripViewModel: TripViewModel
        private set
    private lateinit var latitudeObserver: Observer<Double>
    private lateinit var gateCodesObserver: Observer<MutableList<GateCode>>
    private lateinit var recentTripsObserver: Observer<PagedList<Trip>>
    private lateinit var recentTripsListIterator: MutableListIterator<Trip>
    lateinit var listOfRecentTrips: MutableList<Trip>
        private set
    private lateinit var locationHelper: LocationHelper

    inner class CurrentStatusServiceBinder : Binder() {
        fun getService(): CurrentStatusForegroundService {
            return this@CurrentStatusForegroundService
        }
    }


    /**
     * starts this foreground service
     */
    fun start(context: Context) {
        val application = requireNotNull(context.applicationContext as CourierLockerApplication)
        gateCodeViewModel = GateCodeViewModel(CourierLockerDatabase.getInstance(application).gateCodeDao, application)
        tripViewModel = TripViewModel(CourierLockerDatabase.getInstance(application).tripDao, application)

        val preferences: PreferenceHelper = application.kodein.instance()
        locationHelper = application.kodein.instance()

        recentTripsObserver = Observer { pagedList ->
            listOfRecentTrips = pagedList.take(3).toMutableList()
            resetListOfRecentTripsIterator()
        }

        tripViewModel.tripsPaged.observe(this, recentTripsObserver)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notificationBuilder = Notification.Builder(this, NotificationHelper.CURRENT_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_name)
            .setShowWhen(false)
            .setContentTitle("Current Status")
            .setContentText("Running Foreground service.")
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(this,R.color.colorPrimary))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && preferences.showCurrentStatusAsBubble()) {
            val bubbleData = getBubbleMetadata()

            notificationBuilder
                .setBubbleMetadata(bubbleData.first.build())
                .setShortcutId(bubbleData.third.id)
                .setLocusId(LocusId(bubbleData.third.id))
                .style = Notification.MessagingStyle(bubbleData.second).setGroupConversation(false)

            val notification = notificationBuilder.build()

            NotificationManagerCompat.from(context).apply {
                notify(NotificationHelper.CURRENT_STATUS_NOTIFICATION_ID, notification)
            }

        } else {
            notificationBuilder
                .setGroup(NotificationHelper.NOTIFICATION_CURRENT_STATUS_GROUP_KEY)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroupSummary(true)

            val notification = notificationBuilder.build()
            startForeground(NotificationHelper.CURRENT_STATUS_NOTIFICATION_ID, notification)

            this.startTripsTodayNotification()
            tripViewModel.tripsPaged.observeOnce {
                recentTripsNotification(this)
            }
            this.startNearbyGatecodesNotification()
        }
    }

    /**
     * stops this foreground service
     */
    fun stop() {
        if(::tripViewModel.isInitialized) tripViewModel.tripsPaged.removeObserver(recentTripsObserver)
        if(::gateCodeViewModel.isInitialized) gateCodeViewModel.gateCodes.removeObservers(this)
        if(::locationHelper.isInitialized) locationHelper.lastLatitude.removeObservers(this)


        stopForeground(true)

        NotificationManagerCompat.from(this@CurrentStatusForegroundService).apply {
            cancel(NotificationHelper.CURRENT_STATUS_NOTIFICATION_ID)

            // in case this notification is still around because of late observer cancellation
            cancel(NotificationHelper.NEARBY_GATECODES_NOTIFICATION_ID)
        }
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


        trip.let { t ->
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

        launchIO {
            val total = tripViewModel.todayTotalMoney()
            val size = tripViewModel.todayCompletedTrips()
            val mThis = this@CurrentStatusForegroundService

            withUIContext {
                val notificationBuilder = Notification.Builder(mThis, NotificationHelper.CURRENT_STATUS_CHANNEL_ID)
                    .setShowWhen(false)
                    .setContentTitle("today's stats")
                    .setContentText("earnings: $total | trips: $size")
                    .setSmallIcon(R.drawable.ic_action_name)
                    .setContentIntent(pendingIntent)
                    .setGroup(NotificationHelper.NOTIFICATION_CURRENT_STATUS_GROUP_KEY)
                    .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                    .setOnlyAlertOnce(true)
                    .setSortKey("c")
                    .setColor(ContextCompat.getColor(mThis, R.color.colorPrimary))


                notification = notificationBuilder.build()

                NotificationManagerCompat.from(mThis).apply {
                    notify(NotificationHelper.TRIPS_TODAY_NOTIFICATION_ID, notification)
                }
            }
        }


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
                        locationHelper.calculateApproxDistanceBetweenMapPoints(
                            locationHelper.lastLatitude.value!!,
                            locationHelper.lastLongitude.value!!,
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
                    .setGroup(NotificationHelper.NOTIFICATION_CURRENT_STATUS_GROUP_KEY)
                    .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                    .setOnlyAlertOnce(true)
                    .setSortKey("b")
                    .setColor(ContextCompat.getColor(this,R.color.colorPrimary))
                    .build()

                NotificationManagerCompat.from(this).apply {
                    notify(NotificationHelper.NEARBY_GATECODES_NOTIFICATION_ID, notification)
                }
            }

        }

        locationHelper.lastLatitude.observe(this, latitudeObserver)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getBubbleMetadata(): Triple<Notification.BubbleMetadata.Builder, Person, ShortcutInfo> {
        // Create bubble intent
        val target = Intent(this, CurrentStatusBubbleActivity::class.java).apply {
            action = CurrentStatusBubbleActivity.INTENT_VIEW
        }
        val bubbleIntent = PendingIntent.getActivity(this, 0, target, 0 )


        // now create dynamic shortcut
       val shortcutManager: ShortcutManager = getSystemService() ?: throw IllegalStateException()

        val shortIcon = IconCompat.createWithResource(this, R.mipmap.ic_launcher_round)
            .toIcon(this)

        val person = Person.Builder()
            .setName("Courier Locker")
            .setKey(CurrentStatusBubbleActivity.PERSON_ID)
            .setIcon(shortIcon)
            .setBot(true)
            .setImportant(true)
            .build()

       val shortcutInfo =  ShortcutInfo.Builder(this, CurrentStatusBubbleActivity.CONTACT_ID)
                .setLocusId(LocusId(CurrentStatusBubbleActivity.CONTACT_ID))
                .setActivity(ComponentName(this, CurrentStatusBubbleActivity::class.java))
                .setShortLabel("Current Status")
                .setIcon(shortIcon)
                .setLongLived(true)
                .setIntent(
                    Intent(this, CurrentStatusBubbleActivity::class.java).apply {
                        action = CurrentStatusBubbleActivity.INTENT_VIEW
                    }
                )
                .setPerson(person)
                .build()

        val bubbleBuilder = Notification.BubbleMetadata
            .Builder(bubbleIntent, shortIcon).apply {
            setDesiredHeight(resources.getDimensionPixelSize(R.dimen.bubble_height))
            setAutoExpandBubble(false)
            setSuppressNotification(false)
        }

        shortcutManager.pushDynamicShortcut(shortcutInfo)

        return Triple(bubbleBuilder, person, shortcutInfo)
    }


     class SetTripDropoffReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isBounded = (context.applicationContext as CourierLockerApplication).isCurrentStatusServiceBounded
            val locationHelper: LocationHelper = (context.applicationContext as CourierLockerApplication).kodein.instance()

            if (isBounded) {
                val service = (context.applicationContext as CourierLockerApplication).currentStatusService

                service.tripViewModel.tripsPaged.observeOnce { pagedList ->
                    val tripId = intent.extras?.getLong("TripId")
                    val address = locationHelper.getFromLocation(
                        null,
                        locationHelper.lastLatitude.value!!,
                        locationHelper.lastLongitude.value!!,
                        1
                    )

                    val trip = pagedList.find { t ->
                        t.id == tripId
                    }

                    trip?.let { t ->
                        if (address.isNotEmpty()) {
                            val stop = Stop(
                                address[0].getAddressLine(0),
                                address[0].latitude,
                                address[0].longitude
                            )
                            t.stops.add(stop)
                        }

                        service.tripViewModel.updateTrip(t)

                        val notificationActionIntent =
                            Intent(context, SetTripDropoffReceiver::class.java).apply {
                                action = NotificationHelper.ACTION_UPDATE_DROP_OFF
                                putExtra("TripId", t.id)
                                addCategory("recent trips")
                            }

                        val notificationContentIntent =
                            Intent(context, DeepLinkActivity::class.java).apply {
                                action = MainActivity.INTENT_SHOW_TRIPS
                                putExtra("menuItemId", R.id.nav_trips)
                            }

                        val notificationNextRecentTripIntent =
                            Intent(context, NextRecentTripNotificationReceiver::class.java).apply {
                                action = NotificationHelper.ACTION_NEXT_RECENT_TRIP
                            }

                        val actionPendingIntent = PendingIntent.getBroadcast(
                            context,
                            0,
                            notificationActionIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT
                        )
                        val contentPendingIntent =
                            PendingIntent.getActivity(context, 0, notificationContentIntent, 0)
                        val actionNextRecentTripIntent = PendingIntent.getBroadcast(
                            context,
                            0,
                            notificationNextRecentTripIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT
                        )

                        val notification =
                            NotificationHelper.getRecentTripNotificationBuilder(context)
                                .setStyle(
                                    NotificationCompat.BigTextStyle()
                                        .bigText(Util.formatRecentTripMessage(t))
                                )
                                .setContentIntent(contentPendingIntent)
                                .addAction(
                                    R.drawable.ic_action_name,
                                    "Add Stop",
                                    actionPendingIntent
                                )

                        if (service.listOfRecentTrips.size > 1) {
                            notification.addAction(
                                R.drawable.ic_action_name,
                                "Next Trip",
                                actionNextRecentTripIntent
                            )
                        }

                        NotificationManagerCompat.from(context).apply {
                            notify(
                                NotificationHelper.RECENT_TRIP_NOTIFICATION_ID,
                                notification.build()
                            )
                        }
                        //////
                    }
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