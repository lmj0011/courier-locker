package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.R
import name.lmj0011.courierlocker.database.*
import timber.log.Timber

class GigLabelViewModel(
    val database: GigLabelDao,
    application: Application
) : AndroidViewModel(application) {

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    var errorMsg = MutableLiveData("")

    var gigs = database.getAllGigs()

    init {
        uiScope.launch(Dispatchers.IO) {
            val list = database.getAllGigsBlocking()

            if (list.any()) // do nothing
            else createDefaultGigs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun insertGig(gigLabel: GigLabel?) {
        uiScope.launch(Dispatchers.IO) {
            var maxOrder = database.getMaxOrder()
            gigLabel?.let {
                it.order = maxOrder + 1
                database.insert(it)
                resetOrder()
            }
        }
    }

    fun updateGig(gigLabel: GigLabel?) {
        uiScope.launch(Dispatchers.IO) {
            gigLabel?.let {
                /**
                 * Here we're changing Trip.gigName of all
                 * matching trips to the updated GigLabel.name
                 */
                val oldGigLabelModel = database.get(gigLabel.id)

                val trips = database.getAllTrips().filter {trip ->
                    trip.gigName == oldGigLabelModel?.name
                }

                val tripsWithUpdateGigName = trips.map { trip ->
                    trip.gigName = it.name
                    trip
                }.toMutableList()

                database.update(it)
                database.updateAllTrips(tripsWithUpdateGigName)
            }
        }

    }

    fun updateAllGigs(gigLabels: MutableList<GigLabel>?) {
        uiScope.launch(Dispatchers.IO) {
            gigLabels?.let {
                /**
                 * We disallow altering GigLabel.name on batch Updating,
                 * since this would provide a way to undermine this.updateGig
                 */
                val oldGigs = database.getAllGigsBlocking()

                val refinedGigLabels = it.map { newerGigLabel ->
                    val og = oldGigs.find { og -> og.id == newerGigLabel.id }

                    if (og != null && og.name != newerGigLabel.name) {
                        newerGigLabel.name = og.name
                        Timber.w("Changing GigLabel.name is not allowed on batch Updates, keeping original name: ${og.name}")
                        newerGigLabel
                    } else newerGigLabel
                }.toMutableList()

                database.updateAll(refinedGigLabels)
            }
        }

    }

    fun deleteGig(idx: Long) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                database.deleteByGigId(idx)
                resetOrder()
            }
        }
    }


    private fun createDefaultGigs() {
        uiScope.launch(Dispatchers.IO) {
            val list = mutableListOf(
                GigLabel(name = "Doordash", order = 0),
                GigLabel(name = "Grubhub", order = 1),
                GigLabel(name = "Postmates", order = 2),
                GigLabel(name = "UberEats", order = 3),
                GigLabel(name = "Lyft", order = 4),
                GigLabel(name = "Uber", order = 5),
                GigLabel(name = "Roadie", order = 6)
            )

            database.insertAll(list)
        }
    }

    /**
     * Ensures that the all gigs' order matches it's position
     * in the recycler view, which is necessary for reordering
     * to be accurate.
     */
    private fun resetOrder() {
        uiScope.launch(Dispatchers.IO) {
            val list = database.getAllGigsByOrder()
            
            val newList = list.mapIndexed { index, gigLabel ->
                gigLabel.order = index
                gigLabel
            }.toMutableList()
            
            database.updateAll(newList)
        }
    }

    fun clearAllGigs() {
        uiScope.launch(Dispatchers.IO) {
            database.clear()
        }
    }


}