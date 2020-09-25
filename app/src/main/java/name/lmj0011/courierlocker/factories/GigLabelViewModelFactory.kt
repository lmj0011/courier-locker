package name.lmj0011.courierlocker.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import name.lmj0011.courierlocker.database.ApartmentDao
import name.lmj0011.courierlocker.database.GigLabelDao
import name.lmj0011.courierlocker.viewmodels.ApartmentViewModel
import name.lmj0011.courierlocker.viewmodels.GigLabelViewModel

/**
 * This is pretty much boiler plate code for a ViewModel Factory.
 *
 * Provides the Dao and context to the ViewModel.
 */
class GigLabelViewModelFactory(
    private val dataSource: GigLabelDao,
    private val application: Application) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GigLabelViewModel::class.java)) {
            return GigLabelViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}