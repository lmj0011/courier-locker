package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.paging.*
import com.mooveit.library.Fakeit
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.GateCodeDao
import kotlin.random.Random
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.CourierLockerApplication
import name.lmj0011.courierlocker.helpers.LocationHelper
import name.lmj0011.courierlocker.helpers.Util
import org.kodein.di.instance


class GateCodeViewModel(
    val database: GateCodeDao,
    application: Application
) : AndroidViewModel(application) {

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    private val locationHelper: LocationHelper = (application as CourierLockerApplication).kodein.instance()

    var isOrderedByNearest = MutableLiveData<Boolean>().apply { postValue(false) }

    var filterText = MutableLiveData<String>().apply { postValue(null) }

    var gateCodes = database.getAllGateCodes()

    val gateCode = MutableLiveData<GateCode?>()

    private val doubleTrigger = MediatorLiveData<Pair<Boolean?, String?>>().apply {
        addSource(isOrderedByNearest) {
            value = Pair(it, filterText.value)
        }

        addSource(filterText) {
            value = Pair(isOrderedByNearest.value, it)
        }
    }



    var gatecodesPaged: LiveData<PagingData<GateCode>> = Transformations.switchMap(doubleTrigger) { pair ->
        val filterByLocation = pair.first
        val query = pair.second

        return@switchMap if (query.isNullOrEmpty()) {
            Pager(
                config = Util.getDefaultPagingConfig(),
                initialKey = null,
                database.getAllGateCodesByThePage()
                    .mapByPage { list ->
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
                database.getAllGateCodesByThePageFiltered("%$query%")
                    .mapByPage { list ->
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

    fun getRelatedApartment(gateCodeId: Long) = database.getRelatedApartment(gateCodeId)


    fun setGateCode(idx: Int) {
        uiScope.launch {

            val gc = withContext(Dispatchers.IO){
                this@GateCodeViewModel.database.get(idx.toLong())
            }

            this@GateCodeViewModel.gateCode.postValue(gc)
        }
    }


    fun insertGateCode(address: String, codes: Array<String>, lat: Double, lng: Double) {
        uiScope.launch {
            val gateCode = GateCode().apply {
                this.address = address
                this.codes.addAll(codes)
                this.latitude = lat
                this.longitude = lng
            }

            withContext(Dispatchers.IO){
                this@GateCodeViewModel.database.insert(gateCode)
            }
        }

    }

    fun updateGateCode(gateCode: GateCode?) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                gateCode?.let {
                    this@GateCodeViewModel.database.update(gateCode)
                }
            }
        }

    }

    fun deleteGateCode(idx: Long) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                val apt = this@GateCodeViewModel.database.getRelatedApartment(idx)
                apt?.run {
                    gateCodeId = 0
                    this@GateCodeViewModel.database.update(this)
                }
                this@GateCodeViewModel.database.deleteByGateCodeId(idx)
            }
        }
    }


    fun insertNewRandomGateCodeRow() {
        uiScope.launch {
            val gateCode = GateCode(address= "${Fakeit.address().streetAddress()}")
            val randomValuesList = List(6) { Random.nextInt(1000, 9999) }

            gateCode.codes.addAll(arrayOf(
                "#${randomValuesList[0]}",
                "#${randomValuesList[1]}",
                "#${randomValuesList[2]}",
                "#${randomValuesList[3]}",
                "#${randomValuesList[4]}",
                "#${randomValuesList[5]}"
            ))

            withContext(Dispatchers.IO){
                this@GateCodeViewModel.database.insert(gateCode)
            }
        }



    }
}