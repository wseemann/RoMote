package wseemann.media.romote.discovery

import org.jdom2.Element
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.jdom2.input.sax.XMLReaders
import org.xml.sax.InputSource
import timber.log.Timber
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the picture a Roku publishes of itself.
 *
 * Every Roku serves a UPnP description document at the LOCATION it advertises over SSDP, and that
 * document names its own image:
 *
 *     <iconList>
 *       <icon>
 *         <mimetype>image/png</mimetype><width>360</width><height>219</height>
 *         <url>device-image.png</url>
 *       </icon>
 *     </iconList>
 *
 * The filename is read out of the document rather than assumed, because nothing guarantees every
 * model spells it the same way.
 */
object DeviceDescription {

    private const val CONNECT_TIMEOUT_MILLIS = 3_000
    private const val READ_TIMEOUT_MILLIS = 3_000

    private const val TAG = "DeviceDescription"

    /**
     * @param descriptionUrl the device's description document, which for a Roku is the SSDP
     *        LOCATION verbatim.
     * @return an absolute URL for the device image, or null for any device that doesn't publish
     *         one or can't be reached. A device without a picture still belongs in the list, so
     *         nothing here throws.
     */
    fun fetchIconUrl(descriptionUrl: String): String? {
        val xml = fetch(descriptionUrl) ?: return null
        val iconPath = parseIconPath(xml) ?: return null

        return resolveIconUrl(descriptionUrl, iconPath)
    }

    private fun fetch(url: String): String? = try {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            requestMethod = "GET"
        }

        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Timber.tag(TAG).d("%s answered %s", url, connection.responseCode)
                null
            }
        } finally {
            connection.disconnect()
        }
    } catch (ex: Exception) {
        Timber.tag(TAG).e(ex, "Failed to read %s", url)
        null
    }

    /** The first icon's url as the document spells it, which on a Roku is relative. */
    internal fun parseIconPath(xml: String): String? = try {
        val root = hardenedBuilder().build(xml.reader()).rootElement

        // The document declares a default namespace (urn:schemas-upnp-org:device-1-0), so
        // getChild("iconList") finds nothing. Matching on the local name ignores it.
        // IteratorIterable is both an Iterator and an Iterable, so the type is spelled out to say
        // which asSequence() is meant.
        val descendants: Iterable<Element> = root.getDescendants(Filters.element())

        descendants.asSequence()
            .filter { element -> element.name == "icon" }
            .mapNotNull { icon -> icon.children.firstOrNull { it.name == "url" }?.textTrim }
            .firstOrNull { it.isNotEmpty() }
    } catch (ex: Exception) {
        Timber.tag(TAG).e(ex, "Failed to parse the device description")
        null
    }

    /**
     * Resolves the icon against the *authority* of [descriptionUrl] rather than against its
     * directory.
     *
     * This is deliberate and not an oversight: a Roku serves the same document at "/" and at
     * "/dial/dd.xml", and only http://host:8060/device-image.png exists -
     * http://host:8060/dial/device-image.png is a 404. Resolving the relative url the way
     * URI.resolve() would therefore breaks for anything discovered through the DIAL location.
     */
    internal fun resolveIconUrl(descriptionUrl: String, iconPath: String): String? {
        if (iconPath.startsWith("http://", ignoreCase = true) ||
            iconPath.startsWith("https://", ignoreCase = true)
        ) {
            return iconPath
        }

        val url = try {
            URL(descriptionUrl)
        } catch (ex: Exception) {
            Timber.tag(TAG).e(ex, "Not a url: %s", descriptionUrl)
            return null
        }

        if (url.host.isNullOrEmpty() || iconPath.isBlank()) {
            return null
        }

        val port = if (url.port == -1) "" else ":${url.port}"

        return "${url.protocol}://${url.host}$port/${iconPath.trimStart('/')}"
    }

    /**
     * A parser that will not reach back out to the network for whatever the document references.
     *
     * The obvious hardening - the disallow-doctype-decl feature - is not an option here: Android
     * parses with org.apache.harmony.xml.ExpatReader, which rejects the feature outright, and jdom2
     * does not apply it until build() is called, so asking for it turns every parse into a
     * JDOMException on device while still passing on the JVM. Refusing to resolve entities at all
     * closes the same hole and is understood by every parser.
     */
    private fun hardenedBuilder() = SAXBuilder(XMLReaders.NONVALIDATING).apply {
        expandEntities = false
        setEntityResolver { _, _ -> InputSource(StringReader("")) }
    }
}
