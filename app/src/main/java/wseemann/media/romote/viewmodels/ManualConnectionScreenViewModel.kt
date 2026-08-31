package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.model.Device
import com.wseemann.ecp.request.QueryDeviceInfoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.device.DeviceRepository
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.di.MainDispatcher
import wseemann.media.romote.event.ManualConnectionScreenUiEvent
import wseemann.media.romote.model.ManualConnectionScreenUiState
import wseemann.media.romote.tasks.ResponseCallbackWrapper
import javax.inject.Inject

@HiltViewModel
class ManualConnectionScreenViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val deviceManager: DeviceManager,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualConnectionScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onHandleEvent(event: ManualConnectionScreenUiEvent) {
        when (event) {
            is ManualConnectionScreenUiEvent.IpAddressChangedEvent -> onIpAddressChanged(event.ipAddress)
            is ManualConnectionScreenUiEvent.ConnectClickedEvent -> onConnectClicked()
        }
    }

    private fun onIpAddressChanged(ipAddress: String) {
        // Editing the address is the user's answer to the error, so retract it as they type.
        _uiState.update { it.copy(ipAddress = ipAddress, hasError = false) }
    }

    private fun onConnectClicked() {
        val host = "http://" + _uiState.value.ipAddress + ":8060"

        _uiState.update { it.copy(isLoading = true, hasError = false) }

        val queryDeviceInfoRequest = QueryDeviceInfoRequest(host)
        queryDeviceInfoRequest.sendAsync(
            ResponseCallbackWrapper(
                mainDispatcher,
                object :
                    ResponseCallback<Device?> {
                    override fun onSuccess(data: Device?) {
                        if (data == null) {
                            onConnectionFailed(null)
                            return
                        }

                        // The device only knows the host it was reached at because we tell it: the
                        // response itself carries no address.
                        data.host = host
                        storeDevice(wseemann.media.romote.data.Device.fromDevice(data))
                    }

                    override fun onError(ex: Exception) {
                        onConnectionFailed(ex)
                    }
                },
            ),
        )
    }

    private fun onConnectionFailed(ex: Exception?) {
        ex?.let { Timber.e(it) }
        _uiState.update { it.copy(isLoading = false, hasError = true) }
    }

    /**
     * [ResponseCallbackWrapper] hands the response back on the main thread, so the SQLite write and
     * the preference commit go to the IO dispatcher before the screen is told it is done.
     */
    private fun storeDevice(device: wseemann.media.romote.data.Device) {
        viewModelScope.launch(ioDispatcher) {
            deviceRepository.insertDevice(device)
            deviceManager.setConnectedDevice(device.serialNumber)

            _uiState.update { it.copy(isLoading = false, isConnected = true) }
        }
    }
}
