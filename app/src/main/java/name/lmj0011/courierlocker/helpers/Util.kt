package name.lmj0011.courierlocker.helpers

import android.annotation.TargetApi
import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import name.lmj0011.courierlocker.database.GateCode
import name.lmj0011.courierlocker.database.Trip
import java.math.RoundingMode
import java.text.NumberFormat

class Util {
    companion object {
        val numberFormatInstance: NumberFormat = NumberFormat.getCurrencyInstance()
    }
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
            append("<br>")
            append("\t<h4>${it.address}</h4>")
            append("\tcode: <b>${it.codes[0]}</b><br>")

            append("\t<small><i>alternatives:<i>")
            it.codes.forEach { str ->
                append(" $str")
            }
            append("<br></small>")

            append("<br>")
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
@TargetApi(26)
fun setTripTimestamp(trip: Trip) {
    val iso8061Date = java.time.ZonedDateTime.now().toOffsetDateTime().toString()
    trip.timestamp = iso8061Date
}

/**
 * return today's date in the format MM/DD/YY
 */
@TargetApi(26)
fun getTripDate(trip: Trip): String {
    val now = java.time.ZonedDateTime.parse(trip.timestamp)
    val month = now.month.value
    val dayOfMonth = now.dayOfMonth
    val year = now.year.toString().substring(2)

    return "${month}/${dayOfMonth}/${year}"
}

@TargetApi(26)
fun isTripOfToday(trip: Trip): Boolean {
    val now = java.time.ZonedDateTime.now()
    val month = now.month.value
    val dayOfMonth = now.dayOfMonth
    val year = now.year.toString().substring(2)

    val tripNow = java.time.ZonedDateTime.parse(trip.timestamp)
    val tripMonth = tripNow.month.value
    val tripDayOfMonth = tripNow.dayOfMonth
    val tripYear = tripNow.year.toString().substring(2)

    return ("${month}/${dayOfMonth}/${year}" == "${tripMonth}/${tripDayOfMonth}/${tripYear}")
}

@TargetApi(26)
fun isTripOfMonth(trip: Trip): Boolean {
    val now = java.time.ZonedDateTime.now()
    val month = now.month.value
    val dayOfMonth = now.dayOfMonth
    val year = now.year.toString().substring(2)

    val tripNow = java.time.ZonedDateTime.parse(trip.timestamp)
    val tripMonth = tripNow.month.value
    val tripDayOfMonth = tripNow.dayOfMonth
    val tripYear = tripNow.year.toString().substring(2)

    return ("${month}/xx/${year}" == "${tripMonth}/xx/${tripYear}")
}

fun getCsvFromTripList(trips: List<Trip>?): String {
    /**
     * TODO use a StringBuilder to create a .csv formatted string of all Trips in the DB
     */
    if (trips == null) return ""

    val sb = StringBuilder()

    sb.appendln("Date,Distance,Job,Origin,Destination,Notes")
    for (trip in trips) {
       sb.appendln("${getTripDate(trip)},${metersToMiles(trip.distance)},${trip.gigName},\"${trip.pickupAddress}\",\"${trip.dropOffAddress}\",\"\"")
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
