package name.lmj0011.courierlocker.database

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

// ref: https://stackoverflow.com/a/51560124/2445763
@Dao
interface BaseDao {
//    @RawQuery
//    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery?): Int
}