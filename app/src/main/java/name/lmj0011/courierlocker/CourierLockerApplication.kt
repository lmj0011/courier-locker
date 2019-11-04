package name.lmj0011.courierlocker

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import com.mooveit.library.Fakeit
import name.lmj0011.courierlocker.helpers.NotificationHelper
import shortbread.Shortbread
import timber.log.Timber

class CourierLockerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Fakeit.init()
        AndroidThreeTen.init(this)
        Shortbread.create(this)
        NotificationHelper.init(this)
    }
}
