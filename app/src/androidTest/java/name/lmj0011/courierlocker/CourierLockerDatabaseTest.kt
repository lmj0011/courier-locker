package name.lmj0011.courierlocker

import androidx.room.Room
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.GateCodesDao
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * This is not meant to be a full set of tests. For simplicity, most of your samples do not
 * include tests. However, when building the Room, it is helpful to make sure it works before
 * adding the UI.
 */

@RunWith(AndroidJUnit4ClassRunner::class)
class CourierLockerDatabaseTest {

    private lateinit var gateCodesDao: GateCodesDao
    private lateinit var db: CourierLockerDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        db = Room.inMemoryDatabaseBuilder(context, CourierLockerDatabase::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()

        gateCodesDao = db.gateCodesDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
//        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetGateCode() {
        var gateCode = GateCode()

        gateCode.address = "1500 Old Monrovia Rd."
        gateCode.codes.addAll(arrayOf("#1546","#6578","#3592"))

        gateCodesDao.insert(gateCode)

        val newGateCode = gateCodesDao.get(1)

        assertEquals(1L, newGateCode?.id)

        assertEquals("1500 Old Monrovia Rd.", newGateCode?.address)

        val sameList = mutableListOf("#1546","#6578","#3592")
        assertEquals(true, newGateCode?.codes?.equals(sameList))
    }
}