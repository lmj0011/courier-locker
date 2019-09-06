package name.lmj0011.courierlocker.helpers

/**
 *
 * Represents a point on the surface of a sphere. (The Earth is almost
 * spherical.)
 *
 *
 * To create an instance, call one of the static methods fromDegrees() or
 * fromRadians().
 *
 *
 * This code was originally published at
 * [
 * http://JanMatuschek.de/LatitudeLongitudeBoundingCoordinates#Java](http://JanMatuschek.de/LatitudeLongitudeBoundingCoordinates#Java).
 *
 * @author Jan Philip Matuschek
 * @version 22 September 2010
 */
class GeoLocation private constructor() {

    /**
     * @return the latitude, in radians.
     */
    var latitudeInRadians: Double = 0.toDouble()
        private set  // latitude in radians
    /**
     * @return the longitude, in radians.
     */
    var longitudeInRadians: Double = 0.toDouble()
        private set  // longitude in radians

    /**
     * @return the latitude, in degrees.
     */
    var latitudeInDegrees: Double = 0.toDouble()
        private set  // latitude in degrees
    /**
     * @return the longitude, in degrees.
     */
    var longitudeInDegrees: Double = 0.toDouble()
        private set  // longitude in degrees

    private fun checkBounds() {
        require(
            !(latitudeInRadians < MIN_LAT || latitudeInRadians > MAX_LAT ||
                    longitudeInRadians < MIN_LON || longitudeInRadians > MAX_LON)
        )
    }

    override fun toString(): String {
        return "(" + latitudeInDegrees + "\u00B0, " + longitudeInDegrees + "\u00B0) = (" +
                latitudeInRadians + " rad, " + longitudeInRadians + " rad)"
    }

    /**
     * Computes the great circle distance between this GeoLocation instance
     * and the location argument.
     * @param radius the radius of the sphere, e.g. the average radius for a
     * spherical approximation of the figure of the Earth is approximately
     * 6371.01 kilometers.
     * @return the distance, measured in the same unit as the radius
     * argument.
     */
    fun distanceTo(location: GeoLocation, radius: Double): Double {
        return Math.acos(
            Math.sin(latitudeInRadians) * Math.sin(location.latitudeInRadians) + Math.cos(
                latitudeInRadians
            ) * Math.cos(location.latitudeInRadians) *
                    Math.cos(longitudeInRadians - location.longitudeInRadians)
        ) * radius
    }

    /**
     *
     * Computes the bounding coordinates of all points on the surface
     * of a sphere that have a great circle distance to the point represented
     * by this GeoLocation instance that is less or equal to the distance
     * argument.
     *
     * For more information about the formulae used in this method visit
     * [
 * http://JanMatuschek.de/LatitudeLongitudeBoundingCoordinates](http://JanMatuschek.de/LatitudeLongitudeBoundingCoordinates).
     * @param distance the distance from the point represented by this
     * GeoLocation instance. Must me measured in the same unit as the radius
     * argument.
     * @param radius the radius of the sphere, e.g. the average radius for a
     * spherical approximation of the figure of the Earth is approximately
     * 6371.01 kilometers.
     * @return an array of two GeoLocation objects such that:
     *  * The latitude of any point within the specified distance is greater
     * or equal to the latitude of the first array element and smaller or
     * equal to the latitude of the second array element.
     *  * If the longitude of the first array element is smaller or equal to
     * the longitude of the second element, then
     * the longitude of any point within the specified distance is greater
     * or equal to the longitude of the first array element and smaller or
     * equal to the longitude of the second array element.
     *  * If the longitude of the first array element is greater than the
     * longitude of the second element (this is the case if the 180th
     * meridian is within the distance), then
     * the longitude of any point within the specified distance is greater
     * or equal to the longitude of the first array element
     * **or** smaller or equal to the longitude of the second
     * array element.
     *
     */
    fun boundingCoordinates(distance: Double, radius: Double): Array<GeoLocation> {

        require(!(radius < 0.0 || distance < 0.0))

        // angular distance in radians on a great circle
        val radDist = distance / radius

        var minLat = latitudeInRadians - radDist
        var maxLat = latitudeInRadians + radDist

        var minLon: Double
        var maxLon: Double
        if (minLat > MIN_LAT && maxLat < MAX_LAT) {
            val deltaLon = Math.asin(Math.sin(radDist) / Math.cos(latitudeInRadians))
            minLon = longitudeInRadians - deltaLon
            if (minLon < MIN_LON) minLon += 2.0 * Math.PI
            maxLon = longitudeInRadians + deltaLon
            if (maxLon > MAX_LON) maxLon -= 2.0 * Math.PI
        } else {
            // a pole is within the distance
            minLat = Math.max(minLat, MIN_LAT)
            maxLat = Math.min(maxLat, MAX_LAT)
            minLon = MIN_LON
            maxLon = MAX_LON
        }

        return arrayOf(fromRadians(minLat, minLon), fromRadians(maxLat, maxLon))
    }

    companion object {

        private val MIN_LAT = Math.toRadians(-90.0)  // -PI/2
        private val MAX_LAT = Math.toRadians(90.0)   //  PI/2
        private val MIN_LON = Math.toRadians(-180.0) // -PI
        private val MAX_LON = Math.toRadians(180.0)  //  PI

        /**
         * @param latitude the latitude, in degrees.
         * @param longitude the longitude, in degrees.
         */
        fun fromDegrees(latitude: Double, longitude: Double): GeoLocation {
            val result = GeoLocation()
            result.latitudeInRadians = Math.toRadians(latitude)
            result.longitudeInRadians = Math.toRadians(longitude)
            result.latitudeInDegrees = latitude
            result.longitudeInDegrees = longitude
            result.checkBounds()
            return result
        }

        /**
         * @param latitude the latitude, in radians.
         * @param longitude the longitude, in radians.
         */
        fun fromRadians(latitude: Double, longitude: Double): GeoLocation {
            val result = GeoLocation()
            result.latitudeInRadians = latitude
            result.longitudeInRadians = longitude
            result.latitudeInDegrees = Math.toDegrees(latitude)
            result.longitudeInDegrees = Math.toDegrees(longitude)
            result.checkBounds()
            return result
        }
    }

}
