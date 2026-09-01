package wseemann.media.romote.network

import org.junit.Assert
import org.junit.Test

class LocalNetworkPolicyTest {

    @Test
    fun `wifi is a local network`() {
        Assert.assertTrue(LocalNetworkPolicy.isLocalNetwork(setOf(NetworkTransport.WIFI)))
    }

    @Test
    fun `ethernet is a local network`() {
        Assert.assertTrue(LocalNetworkPolicy.isLocalNetwork(setOf(NetworkTransport.ETHERNET)))
    }

    @Test
    fun `usb is a local network`() {
        Assert.assertTrue(LocalNetworkPolicy.isLocalNetwork(setOf(NetworkTransport.USB)))
    }

    @Test
    fun `cellular is not a local network`() {
        Assert.assertFalse(LocalNetworkPolicy.isLocalNetwork(setOf(NetworkTransport.CELLULAR)))
    }

    @Test
    fun `bluetooth is not a local network`() {
        Assert.assertFalse(LocalNetworkPolicy.isLocalNetwork(setOf(NetworkTransport.BLUETOOTH)))
    }

    @Test
    fun `an unrecognised transport is not a local network`() {
        Assert.assertFalse(LocalNetworkPolicy.isLocalNetwork(setOf(NetworkTransport.OTHER)))
    }

    /** A VPN reports itself alongside whatever it runs over. */
    @Test
    fun `a vpn over wifi is a local network`() {
        Assert.assertTrue(
            LocalNetworkPolicy.isLocalNetwork(
                setOf(NetworkTransport.VPN, NetworkTransport.WIFI)
            )
        )
    }

    @Test
    fun `a vpn over cellular is not a local network`() {
        Assert.assertFalse(
            LocalNetworkPolicy.isLocalNetwork(
                setOf(NetworkTransport.VPN, NetworkTransport.CELLULAR)
            )
        )
    }

    @Test
    fun `a network with no transports at all is not a local network`() {
        Assert.assertFalse(LocalNetworkPolicy.isLocalNetwork(emptySet()))
    }

    @Test
    fun `a phone holding no networks has no local network`() {
        Assert.assertFalse(LocalNetworkPolicy.hasLocalNetwork(emptyList()))
    }

    @Test
    fun `a phone on cellular alone has no local network`() {
        Assert.assertFalse(
            LocalNetworkPolicy.hasLocalNetwork(listOf(setOf(NetworkTransport.CELLULAR)))
        )
    }

    /** The reported bug: a hardwired phone was told it had no connection. */
    @Test
    fun `a hardwired phone has a local network`() {
        Assert.assertTrue(
            LocalNetworkPolicy.hasLocalNetwork(listOf(setOf(NetworkTransport.ETHERNET)))
        )
    }

    /**
     * Cellular is usually the default network when both are up, so asking the active network which
     * transport it is - what the old check did - answered "cellular" here.
     */
    @Test
    fun `wifi alongside cellular is a local network`() {
        Assert.assertTrue(
            LocalNetworkPolicy.hasLocalNetwork(
                listOf(setOf(NetworkTransport.CELLULAR), setOf(NetworkTransport.WIFI))
            )
        )
    }

    @Test
    fun `ethernet alongside cellular is a local network`() {
        Assert.assertTrue(
            LocalNetworkPolicy.hasLocalNetwork(
                listOf(setOf(NetworkTransport.CELLULAR), setOf(NetworkTransport.ETHERNET))
            )
        )
    }
}
