package name.lmj0011.courierlocker

import android.app.Application
import android.os.Build
import androidx.lifecycle.ViewModelProviders
import androidx.work.*
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.soloader.SoLoader
import com.jakewharton.threetenabp.AndroidThreeTen
import com.mooveit.library.Fakeit
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.factories.GigLabelViewModelFactory
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.NotificationHelper
import name.lmj0011.courierlocker.helpers.PermissionHelper
import name.lmj0011.courierlocker.helpers.PreferenceHelper
import name.lmj0011.courierlocker.viewmodels.GigLabelViewModel
import name.lmj0011.courierlocker.workers.CreateBackupWorker
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import shortbread.Shortbread
import timber.log.Timber
import java.util.concurrent.TimeUnit


class CourierLockerApplication : Application() {

    val kodein = DI.direct {
        bind<PreferenceHelper>() with singleton { PreferenceHelper(this@CourierLockerApplication) }
    }

    override fun onCreate() {
        super.onCreate()
        LocationHelper.setFusedLocationClient(this)
        Timber.plant(Timber.DebugTree())
        Fakeit.init()
        AndroidThreeTen.init(this)
        Shortbread.create(this)
        NotificationHelper.init(this)
        PermissionHelper.checkPermissionApprovals(this)

        SoLoader.init(this, false)
        if (BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(this)) {
            val client = AndroidFlipperClient.getInstance(this)
            client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
            client.addPlugin(DatabasesFlipperPlugin(this))
            client.start()
        }


        // initializing this view model here in order to set up some default values in a fresh database
        val gigLabelDataSource = CourierLockerDatabase.getInstance(this).gigLabelDao
        GigLabelViewModel(gigLabelDataSource, this)
    }
}
