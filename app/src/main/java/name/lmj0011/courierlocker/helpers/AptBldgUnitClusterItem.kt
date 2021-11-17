package name.lmj0011.courierlocker.helpers

import com.google.android.libraries.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import name.lmj0011.courierlocker.database.BuildingUnit

// ref: https://github.com/googlemaps/android-maps-utils/blob/master/demo/src/main/java/com/google/maps/android/utils/demo/model/MyItem.java
class AptBldgUnitClusterItem(val bldgUnit: BuildingUnit) : ClusterItem {
    override fun getPosition(): LatLng {
        return LatLng(bldgUnit.latitude, bldgUnit.longitude)
    }

    override fun getTitle(): String {
        return "unit ${bldgUnit.number}"
    }

    override fun getSnippet(): String {
        return ""
    }
}