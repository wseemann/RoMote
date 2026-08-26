package wseemann.media.romote.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import wseemann.media.romote.data.Device

class WakeOnLanTest {

    @Test
    fun parseMac_acceptsColonSeparated() {
        assertArrayEquals(MAC, WakeOnLan.parseMac("a8:4e:3f:11:22:33"))
    }

    @Test
    fun parseMac_acceptsDashSeparatedAndBareHex() {
        assertArrayEquals(MAC, WakeOnLan.parseMac("a8-4e-3f-11-22-33"))
        assertArrayEquals(MAC, WakeOnLan.parseMac("A84E3F112233"))
    }

    @Test
    fun parseMac_rejectsMissingAndMalformedValues() {
        assertNull(WakeOnLan.parseMac(null))
        assertNull(WakeOnLan.parseMac(""))
        assertNull(WakeOnLan.parseMac("a8:4e:3f:11:22"))
        assertNull(WakeOnLan.parseMac("zz:4e:3f:11:22:33"))
    }

    @Test
    fun buildMagicPacket_isSixOnesFollowedBySixteenRepeats() {
        val packet = WakeOnLan.buildMagicPacket(MAC)

        assertEquals(102, packet.size)

        for (i in 0 until 6) {
            assertEquals(0xFF.toByte(), packet[i])
        }

        for (i in 0 until 16) {
            assertArrayEquals(MAC, packet.copyOfRange(6 + i * 6, 12 + i * 6))
        }
    }

    @Test
    fun resolveMac_prefersEthernetWhenNetworkTypeIsEthernet() {
        val device = device(networkType = "ethernet")

        assertArrayEquals(ETHERNET_MAC, WakeOnLan.resolveMac(device))
    }

    @Test
    fun resolveMac_prefersWifiOtherwise() {
        val device = device(networkType = "wifi")

        assertArrayEquals(MAC, WakeOnLan.resolveMac(device))
    }

    @Test
    fun resolveMac_fallsBackWhenThePreferredMacIsBlank() {
        assertArrayEquals(MAC, WakeOnLan.resolveMac(device(networkType = "ethernet", ethernetMac = "")))
        assertArrayEquals(ETHERNET_MAC, WakeOnLan.resolveMac(device(networkType = "wifi", wifiMac = null)))
    }

    @Test
    fun resolveMac_returnsNullWhenTheDeviceHasNoMac() {
        assertNull(WakeOnLan.resolveMac(device(wifiMac = "", ethernetMac = null)))
    }

    @Test
    fun broadcastAddressFor_derivesTheSubnetBroadcast() {
        assertEquals("192.168.1.255", WakeOnLan.broadcastAddressFor("192.168.1.42", 24)?.hostAddress)
        assertEquals("192.168.255.255", WakeOnLan.broadcastAddressFor("192.168.1.42", 16)?.hostAddress)
        assertEquals("10.0.7.255", WakeOnLan.broadcastAddressFor("10.0.4.9", 22)?.hostAddress)
    }

    @Test
    fun broadcastAddressFor_rejectsNonIpv4AndOutOfRangePrefixes() {
        assertNull(WakeOnLan.broadcastAddressFor("192.168.1.42", 32))
        assertNull(WakeOnLan.broadcastAddressFor("192.168.1.42", 0))
        assertNull(WakeOnLan.broadcastAddressFor("fe80::1", 64))
        assertNull(WakeOnLan.broadcastAddressFor("192.168.1.999", 24))
    }

    private fun device(
        networkType: String? = "wifi",
        wifiMac: String? = "a8:4e:3f:11:22:33",
        ethernetMac: String? = "b0:a7:37:44:55:66"
    ) = Device().apply {
        this.networkType = networkType
        this.wifiMac = wifiMac
        this.ethernetMac = ethernetMac
    }

    private companion object {
        val MAC = byteArrayOf(
            0xA8.toByte(), 0x4E, 0x3F, 0x11, 0x22, 0x33
        )
        val ETHERNET_MAC = byteArrayOf(
            0xB0.toByte(), 0xA7.toByte(), 0x37, 0x44, 0x55, 0x66
        )
    }
}
