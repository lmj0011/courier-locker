package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mooveit.library.Fakeit
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.GateCodeDao
import kotlin.random.Random
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.ApartmentDao


class ApartmentViewModel(
    val database: ApartmentDao,
    application: Application
) : AndroidViewModel(application) {

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    val apartments = database.getAllApartments()

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

    fun deleteAll() {
        uiScope.launch {
            withContext(Dispatchers.IO){
                this@ApartmentViewModel.database.clear()
            }
        }
    }
}