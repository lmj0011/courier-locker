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

    @TypeConverter
    fun fromStopList(value: MutableList<Stop>): String {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Stop>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toStopList(value: String): MutableList<Stop> {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Stop>>() {}.type

        return when {
            gson.fromJson<Stop>(value, type) == null -> {
                mutableListOf()
            }
            else -> {
                gson.fromJson(value, type)
            }
        }
    }

    @TypeConverter
    fun fromBuildingList(value: MutableList<Building>): String {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Building>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toBuildingList(value: String): MutableList<Building> {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Building>>() {}.type

        return when {
            gson.fromJson<Building>(value, type) == null -> {
                mutableListOf()
            }
            else -> {
                gson.fromJson(value, type)
            }
        }
    }
}