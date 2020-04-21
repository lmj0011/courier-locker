package name.lmj0011.courierlocker.helpers

import com.google.android.libraries.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import name.lmj0011.courierlocker.database.Building

// ref: https://github.com/googlemaps/android-maps-utils/blob/master/demo/src/main/java/com/google/maps/android/utils/demo/model/MyItem.java
class AptBldgClusterItem(val bldg: Building) : ClusterItem {
    override fun getPosition(): LatLng {
        return LatLng(bldg.latitude, bldg.longitude)
    }

    override fun getTitle(): String {
        return "building ${bldg.number}"
    }

    override fun getSnippet(): String {
        return ""
    }
}