package name.lmj0011.courierlocker

import android.app.Application
import timber.log.Timber

class CourierLockerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
