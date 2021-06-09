package name.lmj0011.courierlocker.viewmodels

import name.lmj0011.courierlocker.database.Customer
import name.lmj0011.courierlocker.database.CustomerDao
import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import androidx.paging.*
import androidx.preference.PreferenceManager
import com.mooveit.library.Fakeit
import kotlinx.coroutines.*
import name.lmj0011.courierlocker.helpers.Util


class CustomerViewModel(
    val database: CustomerDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    var filterText = MutableLiveData<String>().apply { postValue(null) }

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication())

    val customers = database.getAllCustomers()

    val customer = MutableLiveData<Customer?>()

    var customersPaged: LiveData<PagingData<Customer>> = Transformations.switchMap(filterText) { query ->
        return@switchMap if (query.isNullOrEmpty()) {
            Pager(
                config = Util.getDefaultPagingConfig(),
                initialKey = null,
                database.getAllCustomersByThePage().asPagingSourceFactory()
            ).flow.cachedIn(viewModelScope).asLiveData()
        } else {
            Pager(
                config = Util.getDefaultPagingConfig(),
                initialKey = null,
                database.getAllCustomersByThePageFiltered("%$query%").asPagingSourceFactory()
            ).flow.cachedIn(viewModelScope).asLiveData()
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun setCustomer(idx: Int) {
        uiScope.launch {

            val customer = withContext(Dispatchers.IO){
                this@CustomerViewModel.database.get(idx.toLong())
            }

            this@CustomerViewModel.customer.postValue(customer)
        }
    }

    fun insertCustomer(
        name: String,
        address: String,
        addressLatitude: Double,
        addressLongitude: Double,
        impression: Int,
        note: String?
    ) {
        uiScope.launch {
            val customer = Customer().apply {
                this.name = name
                this.address = address
                this.addressLatitude = addressLatitude
                this.addressLongitude = addressLongitude
                this.impression = impression
                note?.let { this.note = it }
            }

            withContext(Dispatchers.IO){
                this@CustomerViewModel.database.insert(customer)
            }
        }

    }

    fun updateCustomer(customer: Customer?) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                customer?.let {
                    this@CustomerViewModel.database.update(it)
                }
            }
        }

    }

    fun deleteCustomer(idx: Long) {
        uiScope.launch {
            withContext(Dispatchers.IO){
                this@CustomerViewModel.database.deleteByCustomerId(idx)
            }
        }
    }


    fun insertNewRandomCustomerRow() {
        uiScope.launch {
            val customer = Customer(
                name = Fakeit.name().name(),
                address = Fakeit.address().streetAddress(),
                impression = (0..1).random()
            )

            when(customer.impression){
                0 -> customer.note = "nice, good tipper."
                else -> customer.note = "stiff game strong."
            }

            withContext(Dispatchers.IO){
                this@CustomerViewModel.database.insert(customer)
            }

        }
    }

    /**
     * Deletes all Customers permanently
     */
    fun clearAllCustomers() {
        uiScope.launch {
            withContext(Dispatchers.IO){
                this@CustomerViewModel.database.clear()
            }

        }
    }
}