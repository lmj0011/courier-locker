package name.lmj0011.courierlocker.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import name.lmj0011.courierlocker.database.ApartmentDao
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel

/**
 * This is pretty much boiler plate code for a ViewModel Factory.
 *
 * Provides the Dao and context to the ViewModel.
 */
class ApartmentViewModelFactory(
    private val dataSource: ApartmentDao,
    private val application: Application) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ApartmentViewModel::class.java)) {
            return ApartmentViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}