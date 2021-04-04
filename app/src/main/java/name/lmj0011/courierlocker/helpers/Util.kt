package name.lmj0011.courierlocker.helpers

import android.text.Html
import android.text.Spanned
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.Trip
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.ZonedDateTime
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
                    append("${metersToMiles(trip.distance)} mi | ${numberFormatInstance.format(it.payAmount.toDouble())} | ${it.gigName}")
                } else {
                    append("<br>")
                    append("pay: ${numberFormatInstance.format(it.payAmount.toDouble())} | ${it.gigName}")
                }
            }
        }

        return Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
    }


    /**
     * set a Trip.timestamp
     */
    fun setTripTimestamp(trip: Trip) {
        val instant =  ZonedDateTime.now().toInstant()

        val iso8061Date = ZonedDateTime
            .ofInstant(instant, ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        trip.timestamp = iso8061Date
    }

    /**
     * return today's date
     */
    fun getTripDate(trip: Trip): String {
        return try {
            val date =  OffsetDateTime.parse(trip.timestamp)
            val formatter = DateTimeFormatter.ofPattern("MM/d/yy h:mma")

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
            val formatter = DateTimeFormatter.ofPattern("MM/d/yy")
            val instant =  ZonedDateTime.now().toInstant()

            val nowDate = ZonedDateTime
                .ofInstant(instant, ZoneId.systemDefault())

            val tripDate = ZonedDateTime.parse(trip.timestamp)


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
        val dt = ZonedDateTime.now()
        val milliOfDay = DateTimeFormatter.ofPattern("A").format(dt).takeLast(4)
        
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dt) + "-$milliOfDay"
    }

    /**
     * returns a string that can be prepended to a filename, it uses 2 unix epochs
     * example output: "20200712_20201025"
     *
     */
    fun getDateRangeFileNamePrefix(range: androidx.core.util.Pair<Long, Long>): String {
        val dateStr1 = DateTimeFormatter.ofPattern("yyyyMMdd")
            .format(Instant.ofEpochMilli(range.first!!).atZone(ZoneId.of("UTC")))

        val dateStr2 = DateTimeFormatter.ofPattern("yyyyMMdd")
            .format(Instant.ofEpochMilli(range.second!!).atZone(ZoneId.of("UTC")))

        return "${dateStr1}_${dateStr2}"
    }
}


