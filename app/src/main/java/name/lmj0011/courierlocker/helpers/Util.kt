package name.lmj0011.courierlocker.helpers

import android.text.Html
import android.text.Spanned
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.Trip
import timber.log.Timber
import java.math.RoundingMode
import java.text.NumberFormat

object Util {
    val numberFormatInstance: NumberFormat = NumberFormat.getCurrencyInstance()

    /**
     * Shortens a Postal Address to only the street name if possible
     *
     * ex.) "1994 Waddell Dr., Huntsville, AL 35806, USA" -> "1994 Waddell Dr."
     */
    fun addressShortener(address: String, min: Int = 5, delimiter: Char = ','): String {
        val parts = address.split(delimiter)
        var str = ""

        for (idx in parts.indices) {
            str = parts.take(idx + 1).joinToString(delimiter.toString())

            if(str.length > min) break
        }

        return str
    }

    /**
     * Takes a list of GateCodes and converts and formats it into one string for display.
     *
     * For display in a TextView, we have to supply one string, and styles are per TextView, not
     * applicable per word. So, we build a formatted string using HTML. This is handy, but we will
     * learn a better way of displaying this data in a future lesson.
     *
     * @param   gateCodes - List of all GateCodes in the database.
     *
     * @return  Spanned - An interface for text that has formatting attached to it.
     *           See: https://developer.android.com/reference/android/text/Spanned
     *
     */

    fun formatGateCodes(gateCodes: List<GateCode>): Spanned {
        val sb = StringBuilder()
        sb.apply {
            gateCodes.forEach {
                if (it.codes.isNotEmpty()) {
                    append("<br>")

                    append("\t<b>${it.codes[0]}</b><br>")
                    append("\t${it.address}")

                    append("<br>")
                }
            }
        }

        return Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
    }

    fun formatRecentTripMessage(trip: Trip?): Spanned {
        val sb = StringBuilder()
        sb.apply {
            trip?.let {
                append("start: ${it.pickupAddress}")
                append("<br>")
                append("end: ${it.dropOffAddress}")

                if (trip.distance > 0) {
                    append("<br>")
                    append("${metersToMiles(trip.distance)} mi | ${Util.numberFormatInstance.format(it.payAmount.toDouble())} | ${it.gigName}")
                } else {
                    append("<br>")
                    append("pay: ${Util.numberFormatInstance.format(it.payAmount.toDouble())} | ${it.gigName}")
                }
            }
        }

        return Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
    }


    /**
     * set a Trip.timestamp
     */
    fun setTripTimestamp(trip: Trip) {
        val instant =  org.threeten.bp.ZonedDateTime.now().toInstant()

        val iso8061Date = org.threeten.bp.ZonedDateTime
            .ofInstant(instant, org.threeten.bp.ZoneId.systemDefault())
            .format(org.threeten.bp.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        trip.timestamp = iso8061Date
    }

    /**
     * return today's date
     */
    fun getTripDate(trip: Trip): String {
        return try {
            val date =  org.threeten.bp.OffsetDateTime.parse(trip.timestamp)
            val formatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("MM/d/yy h:mma")

            formatter.format(date)
                .replace("PM","pm")
                .replace("AM","am")
        } catch (ex: Exception) {
            Timber.e(ex)
            "n/a"
        }
    }

    fun isTripOfToday(trip: Trip): Boolean {
        return try {
            val formatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("MM/d/yy")
            val instant =  org.threeten.bp.ZonedDateTime.now().toInstant()

            val nowDate = org.threeten.bp.ZonedDateTime
                .ofInstant(instant, org.threeten.bp.ZoneId.systemDefault())

            val tripDate = org.threeten.bp.ZonedDateTime.parse(trip.timestamp)


            (formatter.format(nowDate) == formatter.format(tripDate))
        } catch (ex: Exception) {
            Timber.e(ex)
            false
        }
    }

    /**
     * a StringBuilder to create a .csv formatted string of all Trips in the DB
     */
    fun getCsvFromTripList(trips: List<Trip>?): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Distance,Job,Origin,Destination,Notes")

        if (trips == null) {
            return sb.toString()
        }

        for (trip in trips) {
            val stopsSb = StringBuilder()
            stopsSb.append("${trip.stops.size} stops")
            trip.stops.map { stop ->
                stopsSb.append("|${stop.address}")
            }

            sb.appendLine("${getTripDate(trip)},${metersToMiles(trip.distance)},${trip.gigName},\"${trip.pickupAddress}\",\"${trip.dropOffAddress}\",\"${trip.notes};${stopsSb} \",\"\"")
        }
        return sb.toString()
    }

    fun metersToMiles(meters: Double?): Double {
        if(meters == null) return 0.0
        val value = meters / 1609.344
        return value.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    fun metersToKilometers(meters: Double?): Double {
        if(meters == null) return 0.0
        val value = meters / 1000.0
        return value.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }


    /**
     * returns a string that can be prepended to a filename to make it unique
     * example output: "2020-07-12_9783"
     *
     * ref: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
     */
    fun getUniqueFileNamePrefix(): String {
        val dt = org.threeten.bp.ZonedDateTime.now()
        val milliOfDay = org.threeten.bp.format.DateTimeFormatter.ofPattern("A").format(dt).takeLast(4)
        
        return org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dt) + "-$milliOfDay"
    }
}


