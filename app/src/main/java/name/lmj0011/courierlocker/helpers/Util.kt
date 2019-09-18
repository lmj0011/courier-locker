package name.lmj0011.courierlocker.helpers

import android.annotation.TargetApi
import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import name.lmj0011.courierlocker.database.GateCode


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
 * return today's date in the format YYYY-MM-DD
 */
@TargetApi(26)
fun todaysDate(): String {
    val now = java.time.ZonedDateTime.now()
    val month = now.month.value
    val dayOfMonth = now.dayOfMonth
    val year = now.year.toString()

    return "${year}-${month}-${dayOfMonth}"
}