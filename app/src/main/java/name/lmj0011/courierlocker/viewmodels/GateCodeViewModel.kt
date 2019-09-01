package name.lmj0011.courierlocker.viewmodels

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.mooveit.library.Fakeit
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.GateCodeDao
import name.lmj0011.courierlocker.helpers.formatGateCodes
import kotlin.random.Random


class GateCodeViewModel(
    val database: GateCodeDao,
    application: Application
) : AndroidViewModel(application) {

    val gateCodes = database.getAllGateCodes()

    /**
     * Converted persons to Spanned for displaying.
     */
    val personsString: LiveData<Spanned> = Transformations.map(gateCodes) { gateCodes ->
        formatGateCodes(gateCodes)
    }

    fun getGateCode(idx: Int): GateCode? {
        return database.get(idx.toLong())
    }

    fun insertGateCode(address: String, codes: Array<String>) {

        val gateCode = GateCode().apply {
            this.address = address
            this.codes.addAll(codes)
        }

        database.insert(gateCode)
    }

    fun updateGateCode(gateCode: GateCode?) {

        gateCode?.let {
            database.update(gateCode)
        }
    }

    fun deleteGateCode(idx: Long): Int {
        return database.deleteByGateCodeId(idx)
    }


    fun insertNewRandomGateCodeRow() {

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

        database.insert(gateCode)
    }
}