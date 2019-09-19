package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mooveit.library.Fakeit
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.database.Trip
import name.lmj0011.courierlocker.database.TripDao
import name.lmj0011.courierlocker.helpers.setTripTimestamp


class TripViewModel(
    val database: TripDao,
    application: Application
) : AndroidViewModel(application) {

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    val trips = database.getAllTrips()

    val trip = MutableLiveData<Trip?>()


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun setTrip(idx: Int) {
        uiScope.launch {

            val trip = withContext(Dispatchers.IO){
                this@TripViewModel.database.get(idx.toLong())
            }

            this@TripViewModel.trip.postValue(trip)
        }
    }

    fun insertTrip(
        pickupAddress: String,
        pickupAddressLatitude: Double,
        pickupAddressLongitude: Double,
        dropOffAddress: String?,
        dropOffAddressLatitude: Double?,
        dropOffAddressLongitude: Double?,
        distance: Double?, // TODO - use Google Directions API to obtain this
        payAmount: String?,
        gigName: String

    ) {
        uiScope.launch {
            val trip = Trip().apply {
                setTripTimestamp(this)
                this.pickupAddress = pickupAddress
                this.pickupAddressLatitude = pickupAddressLatitude
                this.pickupAddressLongitude = pickupAddressLongitude
                dropOffAddress?.let { this.dropOffAddress = it }
                dropOffAddressLatitude?.let { this.dropOffAddressLatitude = it }
                dropOffAddressLongitude?.let { this.dropOffAddressLongitude = it }
                distance?.let { this.distance = it  }
                payAmount?.let { this.payAmount = it }
                this.gigName = gigName
            }

            withContext(Dispatchers.IO){
                this@TripViewModel.database.insert(trip)
            }
        }

    }

    fun updateTrip(trip: Trip?) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                trip?.let {
                    this@TripViewModel.database.update(trip)
                }
            }
        }

    }

    fun deleteTrip(idx: Long) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                this@TripViewModel.database.deleteByTripId(idx)
            }
        }
    }


    fun insertNewRandomTripRow() {
        uiScope.launch {
            val trip = Trip(
                pickupAddress= "${Fakeit.address().streetAddress()}",
                dropOffAddress= "${Fakeit.address().streetAddress()}"
            ).apply {
                setTripTimestamp(this)
            }

            withContext(Dispatchers.IO){
                this@TripViewModel.database.insert(trip)
            }

        }
    }

    /**
     * Deletes all Trips permanently
     */
    fun clearAllTrips() {
        uiScope.launch {
            withContext(Dispatchers.IO){
                this@TripViewModel.database.clear()
            }

        }
    }

}