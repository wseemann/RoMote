package wseemann.media.romote.discovery

import org.junit.Assert
import org.junit.Test

class DeviceDescriptionTest {

    /** What a Roku Streambar SE actually serves, at both "/" and "/dial/dd.xml". */
    private val streambarDescription = """
        <?xml version="1.0"?>
        <root xmlns="urn:schemas-upnp-org:device-1-0">
        <specVersion><major>1</major><minor>0</minor></specVersion>
        <device>
        <deviceType>urn:roku-com:device:player:1-0</deviceType>
        <friendlyName>Roku Streambar SE</friendlyName>
        <manufacturer>Roku</manufacturer>
        <modelName>9104R</modelName>
        <serialNumber>X01800U24LW8</serialNumber>
        <UDN>uuid:28001200-0000-1000-8000-9cf1d4f78ff2</UDN>
        <iconList>
        <icon>
        <mimetype>image/png</mimetype>
        <width>360</width>
        <height>219</height>
        <depth>8</depth>
        <url>device-image.png</url>
        </icon>
        </iconList>
        <serviceList>
        <service>
        <serviceType>urn:dial-multiscreen-org:service:dial:1</serviceType>
        <SCPDURL>dial_SCPD.xml</SCPDURL>
        </service>
        </serviceList>
        </device>
        </root>
    """.trimIndent()

    /**
     * The document declares a default namespace, so a plain getChild("iconList") finds nothing.
     */
    @Test
    fun `reads the icon url out of a namespaced description`() {
        Assert.assertEquals("device-image.png", DeviceDescription.parseIconPath(streambarDescription))
    }

    @Test
    fun `returns null when the description carries no icon list`() {
        val xml = """
            <root xmlns="urn:schemas-upnp-org:device-1-0">
            <device><friendlyName>Roku</friendlyName></device>
            </root>
        """.trimIndent()

        Assert.assertNull(DeviceDescription.parseIconPath(xml))
    }

    @Test
    fun `returns null when the icon carries no url`() {
        val xml = """
            <root xmlns="urn:schemas-upnp-org:device-1-0">
            <device><iconList><icon><mimetype>image/png</mimetype></icon></iconList></device>
            </root>
        """.trimIndent()

        Assert.assertNull(DeviceDescription.parseIconPath(xml))
    }

    /** A device that answers with something other than a description must not abort the scan. */
    @Test
    fun `returns null for malformed xml`() {
        Assert.assertNull(DeviceDescription.parseIconPath("<root><device>"))
    }

    @Test
    fun `returns null for an empty document`() {
        Assert.assertNull(DeviceDescription.parseIconPath(""))
    }

    @Test
    fun `resolves the icon against the location a roku advertises`() {
        Assert.assertEquals(
            "http://192.168.50.80:8060/device-image.png",
            DeviceDescription.resolveIconUrl("http://192.168.50.80:8060/", "device-image.png")
        )
    }

    /**
     * The icon lives at the root even when the description was read from /dial/dd.xml:
     * http://host:8060/dial/device-image.png is a 404 on a real device. Resolving the relative url
     * the way URI.resolve() would is therefore wrong, and wrong silently - the row just keeps its
     * placeholder - so it is pinned here.
     */
    @Test
    fun `resolves the icon against the host rather than the dd xml directory`() {
        Assert.assertEquals(
            "http://192.168.50.80:8060/device-image.png",
            DeviceDescription.resolveIconUrl(
                "http://192.168.50.80:8060/dial/dd.xml",
                "device-image.png"
            )
        )
    }

    @Test
    fun `ignores a leading slash on the icon url`() {
        Assert.assertEquals(
            "http://192.168.50.80:8060/device-image.png",
            DeviceDescription.resolveIconUrl("http://192.168.50.80:8060/", "/device-image.png")
        )
    }

    @Test
    fun `keeps an icon url that is already absolute`() {
        Assert.assertEquals(
            "http://images.example.com/roku.png",
            DeviceDescription.resolveIconUrl(
                "http://192.168.50.80:8060/",
                "http://images.example.com/roku.png"
            )
        )
    }

    @Test
    fun `returns null when the description url is not a url`() {
        Assert.assertNull(DeviceDescription.resolveIconUrl("garbage", "device-image.png"))
    }

    /**
     * The document comes off the network, so an entity must never send the parser back out after
     * something else. This also stands in for the parser itself: Android's ExpatReader rejects the
     * disallow-doctype-decl feature that would otherwise be the way to do this, so the defence has
     * to be one every parser understands.
     */
    @Test
    fun `does not resolve an external entity`() {
        val xml = """
            <?xml version="1.0"?>
            <!DOCTYPE root [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
            <device><iconList><icon><url>&xxe;</url></icon></iconList></device>
            </root>
        """.trimIndent()

        Assert.assertNull(DeviceDescription.parseIconPath(xml))
    }

    @Test
    fun `returns null for a blank icon url`() {
        Assert.assertNull(DeviceDescription.resolveIconUrl("http://192.168.50.80:8060/", "  "))
    }
}
