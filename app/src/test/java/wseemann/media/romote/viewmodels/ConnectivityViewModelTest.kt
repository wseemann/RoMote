package wseemann.media.romote.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import wseemann.media.romote.event.ConnectivityUiEvent
import wseemann.media.romote.network.LocalNetworkMonitor

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelTest {

    private val monitor = FakeLocalNetworkMonitor()

    @Before
    fun setUp() {
        // viewModelScope is pinned to Dispatchers.Main, which has no implementation on the JVM.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows nothing before the monitor has reported`() {
        val viewModel = ConnectivityViewModel(monitor)

        Assert.assertFalse(viewModel.uiState.value.isDialogVisible)
        Assert.assertTrue(viewModel.uiState.value.isLocalNetworkAvailable)
    }

    @Test
    fun `stays quiet while a local network is up`() {
        val viewModel = ConnectivityViewModel(monitor)

        monitor.emit(true)

        Assert.assertFalse(viewModel.uiState.value.isDialogVisible)
        Assert.assertTrue(viewModel.uiState.value.isLocalNetworkAvailable)
    }

    @Test
    fun `shows the dialog once there is no local network`() {
        val viewModel = ConnectivityViewModel(monitor)

        monitor.emit(false)

        Assert.assertTrue(viewModel.uiState.value.isDialogVisible)
        Assert.assertFalse(viewModel.uiState.value.isLocalNetworkAvailable)
    }

    @Test
    fun `takes the dialog down by itself when the network comes back`() {
        val viewModel = ConnectivityViewModel(monitor)

        monitor.emit(false)
        monitor.emit(true)

        Assert.assertFalse(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun `keeps the dialog down after the user dismisses it`() {
        val viewModel = ConnectivityViewModel(monitor)
        monitor.emit(false)

        viewModel.onHandleEvent(ConnectivityUiEvent.DismissedEvent)

        Assert.assertFalse(viewModel.uiState.value.isDialogVisible)
        Assert.assertFalse(viewModel.uiState.value.isLocalNetworkAvailable)
    }

    /**
     * A dismissal answers for the outage it was made during. Without this the dialog would never
     * be seen again for the life of the ViewModel.
     */
    @Test
    fun `shows the dialog again on the next outage after a dismissal`() {
        val viewModel = ConnectivityViewModel(monitor)
        monitor.emit(false)
        viewModel.onHandleEvent(ConnectivityUiEvent.DismissedEvent)

        monitor.emit(true)
        monitor.emit(false)

        Assert.assertTrue(viewModel.uiState.value.isDialogVisible)
    }

    /** Reconnecting mid-outage clears the dismissal even though the dialog is already down. */
    @Test
    fun `clears the dismissal when the network returns`() {
        val viewModel = ConnectivityViewModel(monitor)
        monitor.emit(false)
        viewModel.onHandleEvent(ConnectivityUiEvent.DismissedEvent)

        monitor.emit(true)

        Assert.assertFalse(viewModel.uiState.value.isDismissed)
    }

    /**
     * A shared flow rather than a state flow: the real monitor reports nothing until the platform
     * answers, and consecutive emissions must not be conflated away by the tests below.
     */
    private class FakeLocalNetworkMonitor : LocalNetworkMonitor {

        override val isLocalNetworkAvailable = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)

        fun emit(isAvailable: Boolean) {
            Assert.assertTrue(isLocalNetworkAvailable.tryEmit(isAvailable))
        }
    }
}
