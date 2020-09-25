package name.lmj0011.courierlocker.helpers

import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.Trip
import timber.log.Timber
import java.math.RoundingMode
import java.text.NumberFormat

object Util {
    val numberFormatInstance: NumberFormat = NumberFormat.getCurrencyInstance()

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

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
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

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }


    /**
     * set a Trip.timestamp
     */
    fun setTripTimestamp(trip: Trip) {
        val iso8061Date = org.threeten.bp.ZonedDateTime.now().toOffsetDateTime().toString()
        trip.timestamp = iso8061Date
    }

    /**
     * return today's date in the format MM/DD/YY
     */
    fun getTripDate(trip: Trip): String {
        return try {
            val now = org.threeten.bp.ZonedDateTime.parse(trip.timestamp)
            val month = now.month.value
            val dayOfMonth = now.dayOfMonth
            val year = now.year.toString().substring(2)

            "${month}/${dayOfMonth}/${year}"
        } catch (ex: Exception) {
            Timber.e(ex)
            "n/a"
        }
    }

    fun isTripOfToday(trip: Trip): Boolean {
        return try {
            val now = org.threeten.bp.ZonedDateTime.now()
            val month = now.month.value
            val dayOfMonth = now.dayOfMonth
            val year = now.year.toString().substring(2)

            val tripNow = org.threeten.bp.ZonedDateTime.parse(trip.timestamp)
            val tripMonth = tripNow.month.value
            val tripDayOfMonth = tripNow.dayOfMonth
            val tripYear = tripNow.year.toString().substring(2)

            ("${month}/${dayOfMonth}/${year}" == "${tripMonth}/${tripDayOfMonth}/${tripYear}")
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
        sb.appendln("Date,Distance,Job,Origin,Destination,Notes")

        if (trips == null) {
            return sb.toString()
        }

        for (trip in trips) {
            val stopsSb = StringBuilder()
            stopsSb.append("${trip.stops.size} stops")
            trip.stops.map { stop ->
                stopsSb.append("|${stop.address}")
            }

            sb.appendln("${getTripDate(trip)},${metersToMiles(trip.distance)},${trip.gigName},\"${trip.pickupAddress}\",\"${trip.dropOffAddress}\",\"${trip.notes};${stopsSb} \",\"\"")
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


