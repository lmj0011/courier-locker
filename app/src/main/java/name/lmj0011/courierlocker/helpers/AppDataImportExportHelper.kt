package name.lmj0011.courierlocker.helpers

import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.JsonObject
import name.lmj0011.courierlocker.database.*


object AppDataImportExportHelper {

    fun appModelsToJson(
        trips: List<Trip>,
        apartments: List<Apartment>,
        gatecodes: List<GateCode>,
        customers: List<Customer>,
        gigLabels: List<GigLabel>
    ): JsonObject {
        return jsonObject(
            "trips" to  jsonArray(
                trips.map {
                    jsonObject(
                        "id" to it.id,
                        "timestamp" to it.timestamp,
                        "pickupAddress" to it.pickupAddress,
                        "pickupAddressLatitude" to it.pickupAddressLatitude,
                        "pickupAddressLongitude" to it.pickupAddressLongitude,
                        "dropOffAddress" to it.dropOffAddress,
                        "dropOffAddressLatitude" to it.dropOffAddressLatitude,
                        "dropOffAddressLongitude" to it.dropOffAddressLongitude,
                        "distance" to it.distance,
                        "payAmount" to it.payAmount,
                        "gigName" to it.gigName,
                        "stops" to jsonArray(
                            it.stops.map {stop ->
                                jsonObject(
                                    "address" to stop.address,
                                    "latitude" to stop.latitude,
                                    "longitude" to stop.longitude
                                )
                            }
                        ),
                        "notes" to it.notes
                    )
                }
            ),

            "apartments" to jsonArray(
                apartments.map {
                    jsonObject(
                        "id" to it.id,
                        "gateCodeId" to it.gateCodeId,
                        "uid" to it.uid,
                        "name" to it.name,
                        "address" to it.address,
                        "latitude" to it.latitude,
                        "longitude" to it.longitude,
                        "aboveGroundFloorCount" to it.aboveGroundFloorCount,
                        "belowGroundFloorCount" to it.belowGroundFloorCount,
                        "floorOneAsBlueprint" to it.floorOneAsBlueprint,
                        "mapImageUrl" to it.mapImageUrl,
                        "sourceUrl" to it.sourceUrl,
                        "buildings" to jsonArray(
                            it.buildings.map {bldg ->
                                jsonObject(
                                    "number" to bldg.number,
                                    "latitude" to bldg.latitude,
                                    "longitude" to bldg.longitude
                                )
                            }
                        ),
                        "buildingUnits" to jsonArray(
                            it.buildingUnits.map { bldgUnit ->
                                jsonObject(
                                    "number" to bldgUnit.number,
                                    "floorNumber" to bldgUnit.floorNumber,
                                    "latitude" to bldgUnit.latitude,
                                    "longitude" to bldgUnit.longitude
                                )
                            }
                        )
                    )
                }
            ),

            "gatecodes" to jsonArray(
                gatecodes.map {
                    jsonObject(
                        "id" to it.id,
                        "address" to it.address,
                        "latitude" to it.latitude,
                        "longitude" to it.longitude,
                        "codes" to it.codes.toJsonArray() // can't call this on a non-primitive object :(
                    )
                }
            ),

            "customers" to jsonArray(
                customers.map {
                    jsonObject(
                        "id" to it.id,
                        "name" to it.name,
                        "address" to it.address,
                        "addressLatitude" to it.addressLatitude,
                        "addressLongitude" to it.addressLongitude,
                        "impression" to it.impression,
                        "note" to it.note
                    )
                }
            ),

            "gigLabels" to jsonArray(
                gigLabels.map {
                    jsonObject(
                        "id" to it.id,
                        "name" to it.name,
                        "order" to it.order,
                        "visible" to it.visible
                    )
                }
            )
        )
    }

}