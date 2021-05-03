package name.lmj0011.courierlocker

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.jakewharton.threetenabp.AndroidThreeTen
import com.mooveit.library.Fakeit
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.helpers.*
import name.lmj0011.courierlocker.services.CurrentStatusForegroundService
import name.lmj0011.courierlocker.viewmodels.GigLabelViewModel
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import timber.log.Timber


class CourierLockerApplication : Application() {
    lateinit var currentStatusService: CurrentStatusForegroundService
        private set
    var isCurrentStatusServiceBounded: Boolean = false
        private set

    val kodein = DI.direct {
        bind<PreferenceHelper>() with singleton { PreferenceHelper(this@CourierLockerApplication) }
        bind<LocationHelper>() with singleton { LocationHelper(this@CourierLockerApplication) }
    }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val boundServiceConn = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as CurrentStatusForegroundService.CurrentStatusServiceBinder
            currentStatusService = binder.getService()
            isCurrentStatusServiceBounded = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isCurrentStatusServiceBounded = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Fakeit.init()
        AndroidThreeTen.init(this)
        NotificationHelper.init(this)
        PermissionHelper.checkPermissionApprovals(this)

        /**
         * Bind to CurrentStatusForegroundService, we'll leave it to the OS to kill this Service
         * when it's no longer being used.
         */
        Intent(this, CurrentStatusForegroundService::class.java).also { intent ->
            bindService(intent, boundServiceConn, Context.BIND_AUTO_CREATE)
        }

        // TODO - this probably should be moved into a DB migration
        // initializing this view model here in order to set up some default values in a fresh database
        val gigLabelDataSource = CourierLockerDatabase.getInstance(this).gigLabelDao
        GigLabelViewModel(gigLabelDataSource, this)
    }

    fun startCurrentStatusService() {
        if (isCurrentStatusServiceBounded) {
            Timber.d("CurrentStatusForegroundService.start")
            currentStatusService.start(this)
        }
    }

    fun stopCurrentStatusService() {
        if (isCurrentStatusServiceBounded) {
            Timber.d("CurrentStatusForegroundService.stop")
            currentStatusService.stop()
        }
    }
}
