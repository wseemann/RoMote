package wseemann.media.romote.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        assertEquals("http://192.168.1.9:8060", SsdpDiscovery.parseLocation(response))
    }

    @Test
    fun `matches the header case insensitively`() {
        val response = "HTTP/1.1 200 OK\r\nlocation: http://192.168.1.9:8060/\r\n\r\n"

        assertEquals("http://192.168.1.9:8060", SsdpDiscovery.parseLocation(response))
    }

    @Test
    fun `keeps a location that carries no trailing path`() {
        val response = "HTTP/1.1 200 OK\r\nLOCATION: http://192.168.1.9:8060\r\n\r\n"

        assertEquals("http://192.168.1.9:8060", SsdpDiscovery.parseLocation(response))
    }

    @Test
    fun `drops the path beyond the host`() {
        val response = "HTTP/1.1 200 OK\r\nLOCATION: http://192.168.1.9:8060/dial/dd.xml\r\n\r\n"

        assertEquals("http://192.168.1.9:8060", SsdpDiscovery.parseLocation(response))
    }

    /**
     * Other SSDP responders share the network. The old implementation did
     * response.split("location:")[1] and threw ArrayIndexOutOfBoundsException here, which aborted
     * the whole scan rather than skipping the packet.
     */
    @Test
    fun `returns null for a reply with no location header`() {
        val response = "HTTP/1.1 200 OK\r\nST: upnp:rootdevice\r\nUSN: uuid:something\r\n\r\n"

        assertNull(SsdpDiscovery.parseLocation(response))
    }

    @Test
    fun `returns null for an empty response`() {
        assertNull(SsdpDiscovery.parseLocation(""))
    }

    @Test
    fun `returns null when the location is not a url`() {
        assertNull(SsdpDiscovery.parseLocation("HTTP/1.1 200 OK\r\nLOCATION: garbage\r\n\r\n"))
    }

    @Test
    fun `returns null when the location has no host`() {
        assertNull(SsdpDiscovery.parseLocation("HTTP/1.1 200 OK\r\nLOCATION: http:///\r\n\r\n"))
    }
}
