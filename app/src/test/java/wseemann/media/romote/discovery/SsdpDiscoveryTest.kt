package wseemann.media.romote.discovery

import org.junit.Assert
import org.junit.Test

class SsdpDiscoveryTest {

    @Test
    fun `parses the location header of a roku reply`() {
        val response = """
            HTTP/1.1 200 OK
            Cache-Control: max-age=3600
            ST: roku:ecp
            USN: uuid:roku:ecp:1GH48D000000
            Ext:
            Server: Roku UPnP/1.0 MiniUPnPd/1.4
            LOCATION: http://192.168.1.9:8060/

        """.trimIndent()

        Assert.assertEquals("http://192.168.1.9:8060", SsdpDiscovery.parseLocation(response))
    }

    @Test
    fun `matches the header case insensitively`() {
        val response = "HTTP/1.1 200 OK\r\nlocation: http://192.168.1.9:8060/\r\n\r\n"

        Assert.assertEquals("http://192.168.1.9:8060", SsdpDiscovery.parseLocation(response))
    }

    @Test
    fun `keeps a location that carries no trailing path`() {
        val response = "HTTP/1.1 200 OK\r\nLOCATION: http://192.168.1.9:8060\r\n\r\n"

        Assert.assertEquals("http://192.168.1.9:8060", SsdpDiscovery.parseLocation(response))
    }

    @Test
    fun `drops the path beyond the host`() {
        val response = "HTTP/1.1 200 OK\r\nLOCATION: http://192.168.1.9:8060/dial/dd.xml\r\n\r\n"

        Assert.assertEquals("http://192.168.1.9:8060", SsdpDiscovery.parseLocation(response))
    }

    /**
     * Other SSDP responders share the network. The old implementation did
     * response.split("location:")[1] and threw ArrayIndexOutOfBoundsException here, which aborted
     * the whole scan rather than skipping the packet.
     */
    @Test
    fun `returns null for a reply with no location header`() {
        val response = "HTTP/1.1 200 OK\r\nST: upnp:rootdevice\r\nUSN: uuid:something\r\n\r\n"

        Assert.assertNull(SsdpDiscovery.parseLocation(response))
    }

    @Test
    fun `returns null for an empty response`() {
        Assert.assertNull(SsdpDiscovery.parseLocation(""))
    }

    @Test
    fun `returns null when the location is not a url`() {
        Assert.assertNull(SsdpDiscovery.parseLocation("HTTP/1.1 200 OK\r\nLOCATION: garbage\r\n\r\n"))
    }

    @Test
    fun `returns null when the location has no host`() {
        Assert.assertNull(SsdpDiscovery.parseLocation("HTTP/1.1 200 OK\r\nLOCATION: http:///\r\n\r\n"))
    }

    /**
     * The description url keeps the path the base url drops, because that path is where the device
     * says its own picture is named. A Roku answering ST roku:ecp points it at "/".
     */
    @Test
    fun `keeps the whole location as the description url`() {
        val response = """
            HTTP/1.1 200 OK
            ST: roku:ecp
            USN: uuid:roku:ecp:X01800U24LW8
            Server: Roku/15.3.4 UPnP/1.0 Roku/15.3.4
            LOCATION: http://192.168.50.80:8060/

        """.trimIndent()

        Assert.assertEquals(
            "http://192.168.50.80:8060/",
            SsdpDiscovery.parseDescriptionUrl(response)
        )
    }

    @Test
    fun `keeps a description url that points at dd xml`() {
        val response = "HTTP/1.1 200 OK\r\nLOCATION: http://192.168.50.80:8060/dial/dd.xml\r\n\r\n"

        Assert.assertEquals(
            "http://192.168.50.80:8060/dial/dd.xml",
            SsdpDiscovery.parseDescriptionUrl(response)
        )
    }

    @Test
    fun `returns no description url for a reply with no location header`() {
        val response = "HTTP/1.1 200 OK\r\nST: upnp:rootdevice\r\nUSN: uuid:something\r\n\r\n"

        Assert.assertNull(SsdpDiscovery.parseDescriptionUrl(response))
    }

    @Test
    fun `returns no description url when the location has no host`() {
        Assert.assertNull(
            SsdpDiscovery.parseDescriptionUrl("HTTP/1.1 200 OK\r\nLOCATION: http:///\r\n\r\n")
        )
    }
}