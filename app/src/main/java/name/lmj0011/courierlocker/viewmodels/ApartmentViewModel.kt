package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.paging.*
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.database.*
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.Util
import org.kodein.di.instance

class ApartmentViewModel(
    val database: ApartmentDao,
    application: Application
) : AndroidViewModel(application) {

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    private val locationHelper: LocationHelper = (application as CourierLockerApplication).kodein.instance()

    var isOrderedByNearest = MutableLiveData<Boolean>().apply { postValue(false) }

    var filterText = MutableLiveData<String>().apply { postValue(null) }

    var apartments = database.getAllApartments()

    private val doubleTrigger = MediatorLiveData<Pair<Boolean?, String?>>().apply {
        addSource(isOrderedByNearest) {
            value = Pair(it, filterText.value)
        }

        addSource(filterText) {
            value = Pair(isOrderedByNearest.value, it)
        }
    }


    var apartmentsWithoutGateCodePaged = Pager(
        config = Util.getDefaultPagingConfig(),
        initialKey = null,
        database.getAllApartmentsWithoutGateCodeByThePage().asPagingSourceFactory()
    ).flow.cachedIn(viewModelScope).asLiveData()

    var apartmentsPaged: LiveData<PagingData<Apartment>> = Transformations.switchMap(doubleTrigger) { pair ->
        val filterByLocation = pair.first
        val query = pair.second

        return@switchMap if (query.isNullOrEmpty()) {
            Pager(
                config = Util.getDefaultPagingConfig(),
                initialKey = null,
                database.getAllApartmentsByThePage().mapByPage { list ->
                    when(filterByLocation) {
                        true -> {
                            list.sortedBy {
                                locationHelper.calculateApproxDistanceBetweenMapPoints(
                                    locationHelper.lastLatitude.value!!,
                                    locationHelper.lastLongitude.value!!,
                                    it.latitude,
                                    it.longitude
                                )
                            }
                        }
                        else -> { list }
                    }
                }.asPagingSourceFactory()
            ).flow.cachedIn(viewModelScope).asLiveData()
        } else {
            Pager(
                config = Util.getDefaultPagingConfig(),
                initialKey = null,
                database.getAllApartmentsByThePageFiltered("%$query%").mapByPage { list ->
                    when(filterByLocation) {
                        true -> {
                            list.sortedBy {
                                locationHelper.calculateApproxDistanceBetweenMapPoints(
                                    locationHelper.lastLatitude.value!!,
                                    locationHelper.lastLongitude.value!!,
                                    it.latitude,
                                    it.longitude
                                )
                            }
                        }
                        else -> { list }
                    }
                }.asPagingSourceFactory()
            ).flow.cachedIn(viewModelScope).asLiveData()
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getRelatedGateCode(gateCodeId: Long) = database.getRelatedGateCode(gateCodeId)


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