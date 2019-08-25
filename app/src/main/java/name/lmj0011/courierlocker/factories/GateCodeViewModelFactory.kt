package name.lmj0011.courierlocker.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import name.lmj0011.courierlocker.database.GateCodeDao
import name.lmj0011.courierlocker.viewmodels.GateCodeViewModel

/**
 * This is pretty much boiler plate code for a ViewModel Factory.
 *
 * Provides the GateCodeDao and context to the ViewModel.
 */
class GateCodeViewModelFactory(
    private val dataSource: GateCodeDao,
    private val application: Application) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GateCodeViewModel::class.java)) {
            return GateCodeViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}