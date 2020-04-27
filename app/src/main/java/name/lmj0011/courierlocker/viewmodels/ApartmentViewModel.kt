package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.database.*

class ApartmentViewModel(
    val database: ApartmentDao,
    application: Application
) : AndroidViewModel(application) {

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    var apartments = database.getAllApartments()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


    fun insertApartments(apts: MutableList<Apartment>) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                this@ApartmentViewModel.database.insertAll(apts)
            }
        }
    }

    fun updateApartment(apt: Apartment?) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                apt?.let {
                    this@ApartmentViewModel.database.update(apt)
                }
            }
        }

    }

    fun deleteAll(apts: MutableList<Apartment>) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                this@ApartmentViewModel.database.deleteAll(apts)
            }
        }
    }
}