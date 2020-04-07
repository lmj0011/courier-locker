package name.lmj0011.courierlocker.helpers

import android.util.Xml
import name.lmj0011.courierlocker.database.Apartment
import name.lmj0011.courierlocker.database.Building
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

// ref: https://developer.android.com/training/basics/network-ops/xml

class PlexmapsXmlParser() {
    private val ns: String? = null

    // Downloads a given plexmap xml feed and parses it
    // Returns a List of Apartments.
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseFeeds(urlStrings: Array<String>): List<Apartment> {
        val allApts = mutableListOf<Apartment>()

        urlStrings.forEach { urlStr ->
            val apts = downloadUrl(urlStr)?.use { stream ->
                // Instantiate the parser
                this.parse(stream, urlStr)
            } ?: emptyList()

            allApts.addAll(apts)
        }

        return allApts.toList()
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream? {
        val url = URL(urlString)
        return (url.openConnection() as? HttpURLConnection)?.run {
            readTimeout = 10000
            connectTimeout = 15000
            requestMethod = "GET"
            doInput = true
            // Starts the query
            connect()
            inputStream
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream, urlString: String): List<Apartment> {
        inputStream.use { inputStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readFeed(parser, urlString)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser, urlString: String): List<Apartment> {
        val apartments = mutableListOf<Apartment>()

        parser.require(XmlPullParser.START_TAG, ns, "apartments")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the apartment tag
            when (parser.name){
                "apartment" -> {
                    apartments.add(readApartment(parser, urlString))
                }
                else -> {
                    skip(parser)
                }
            }
        }
        return apartments
    }


    // Parses the contents of an apartment. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readApartment(parser: XmlPullParser, urlString: String): Apartment {
        parser.require(XmlPullParser.START_TAG, ns, "apartment")
        var name = ""
        var address = ""
        var latitude = 0.toDouble()
        var longitude = 0.toDouble()
        var mapImageUrl = ""
        var buildings = listOf<Building>()

        var sourceRowId = parser.getAttributeValue(null, "id")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "name" -> name = readName(parser)
                "address" -> address = readAddress(parser)
                "latitude" -> latitude = readLatitude(parser)
                "longitude" -> longitude = readLongitude(parser)
                "mapImageUrl" -> mapImageUrl = readMapImageUrl(parser)
                "buildings" -> {
                    buildings = readApartmentBuildings(parser)
                }
                else -> skip(parser)
            }
        }

        return Apartment().apply {
            this.uid = "${urlString}#id=${sourceRowId}"
            this.name = name
            this.address = address
            this.latitude = latitude
            this.longitude = longitude
            this.mapImageUrl = mapImageUrl
            this.sourceUrl = urlString
            this.buildings = buildings
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readApartmentBuildings(parser: XmlPullParser): List<Building> {
        parser.require(XmlPullParser.START_TAG, ns, "buildings")
        val buildings = mutableListOf<Building>()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "building" -> buildings.add(readBuilding(parser))
                else -> skip(parser)
            }
        }

        return buildings.toList()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readBuilding(parser: XmlPullParser): Building {
        parser.require(XmlPullParser.START_TAG, ns, "building")
        var number = ""
        var latitude = 0.toDouble()
        var longitude = 0.toDouble()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "number" -> number = readNumber(parser)
                "latitude" -> latitude = readLatitude(parser)
                "longitude" -> longitude = readLongitude(parser)
                else -> skip(parser)
            }
        }

        return Building(number, latitude, longitude)
    }

    // Processes number tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readNumber(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "number")
        val number = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "number")
        return number
    }

    // Processes name tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readName(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "name")
        val name = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "name")
        return name
    }

    // Processes address tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readAddress(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "address")
        val address = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "address")
        return address
    }

    // Processes address tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLatitude(parser: XmlPullParser): Double {
        parser.require(XmlPullParser.START_TAG, ns, "latitude")
        val latitude = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "latitude")

        return try {
            latitude.toDouble()
        } catch (ex: NumberFormatException) {
            Timber.e("invalid input for Latitude: \"${latitude}\", returning 0")
            0.toDouble()
        }
    }

    // Processes address tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLongitude(parser: XmlPullParser): Double {
        parser.require(XmlPullParser.START_TAG, ns, "longitude")
        val longitude = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "longitude")

        return try {
            longitude.toDouble()
        } catch (ex: NumberFormatException) {
            Timber.e("invalid input for Longitude: \"${longitude}\", returning 0")
            0.toDouble()
        }
    }

    // Processes address tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readMapImageUrl(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "mapImageUrl")
        val mapImageUrl = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "mapImageUrl")
        return mapImageUrl
    }

    // For extracting text values from tags.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    // skips tags we're not interested in
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}