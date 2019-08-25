package name.lmj0011.courierlocker

import androidx.room.Room
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import name.lmj0011.courierlocker.database.CourierLockerDatabase
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.GateCodeDao
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

    private lateinit var gateCodeDao: GateCodeDao
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

        gateCodeDao = db.gateCodeDao
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

        gateCodeDao.insert(gateCode)

        val newGateCode = gateCodeDao.get(1)

        assertEquals(1L, newGateCode?.id)

        assertEquals("1500 Old Monrovia Rd.", newGateCode?.address)

        val sameList = mutableListOf("#1546","#6578","#3592")
        assertEquals(true, newGateCode?.codes?.equals(sameList))
    }
}