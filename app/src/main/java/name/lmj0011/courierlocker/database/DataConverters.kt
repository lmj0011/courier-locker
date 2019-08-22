package name.lmj0011.courierlocker.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataConverters {

    @TypeConverter
    fun fromStringList(value: MutableList<String>): String {
        val gson = Gson()
        val type = object : TypeToken<MutableList<String>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toStringList(value: String): MutableList<String> {
        val gson = Gson()
        val type = object : TypeToken<MutableList<String>>() {}.type
        return gson.fromJson(value, type)
    }
}