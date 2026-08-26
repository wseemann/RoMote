package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.QueryRequests
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.event.DeviceInfoScreenUiEvent
import wseemann.media.romote.data.Device
import wseemann.media.romote.data.Device.Companion.fromDevice
import wseemann.media.romote.model.DeviceInfoScreenUiState
import wseemann.media.romote.data.Entry
import wseemann.media.romote.utils.CommandHelper
import wseemann.media.romote.utils.DBUtils
import javax.inject.Inject

@HiltViewModel
class DeviceInfoScreenViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val commandHelper: CommandHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceInfoScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onHandleEvent(event: DeviceInfoScreenUiEvent) {
        when (event) {
            is DeviceInfoScreenUiEvent.LoadDeviceInfoEvent ->
                onLoadDeviceInfo(event.serialNumber, event.host)
        }
    }

    private fun onLoadDeviceInfo(serialNumber: String?, host: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            // Paint what was stored when the device was paired, so there is something on screen
            // while the device itself is queried for its current details.
            DBUtils.getDevice(context, serialNumber)?.let { storedDevice ->
                _uiState.update { it.copy(entries = parseDevice(storedDevice)) }
            }

            val command = if (host == null) {
                commandHelper.getConnectedDeviceInfoURL()
            } else {
                commandHelper.getDeviceInfoURL(host)
            }

            try {
                val device = fromDevice(QueryRequests.queryDeviceInfo(command))
                _uiState.update { it.copy(entries = parseDevice(device), isLoading = false) }
            } catch (ex: Exception) {
                // Any stored entries stay on screen; they are the best answer available.
                Timber.e(ex)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun parseDevice(device: Device): ImmutableList<Entry> {
        return listOf(
            Entry("udn", device.udn.orEmpty()),
            Entry("serial-number", device.serialNumber.orEmpty()),
            Entry("device-id", device.deviceId.orEmpty()),
            Entry("vendor-name", device.vendorName.orEmpty()),
            Entry("model-number", device.modelNumber.orEmpty()),
            Entry("model-name", device.modelName.orEmpty()),
            Entry("wifi-mac", device.wifiMac.orEmpty()),
            Entry("ethernet-mac", device.ethernetMac.orEmpty()),
            Entry("network-type", device.networkType.orEmpty()),
            Entry("user-device-name", device.userDeviceName.orEmpty()),
            Entry("software-version", device.softwareVersion.orEmpty()),
            Entry("software-build", device.softwareBuild.orEmpty()),
            Entry("secure-device", device.secureDevice.orEmpty()),
            Entry("language", device.language.orEmpty()),
            Entry("country", device.country.orEmpty()),
            Entry("locale", device.locale.orEmpty()),
            Entry("time-zone", device.timeZone.orEmpty()),
            Entry("time-zone-offset", device.timeZoneOffset.orEmpty()),
            Entry("power-mode", device.powerMode.orEmpty()),
            Entry("supports-suspend", device.supportsSuspend.orEmpty()),
            Entry("supports-find-remote", device.supportsFindRemote.orEmpty()),
            Entry("supports-audio-guide", device.supportsAudioGuide.orEmpty()),
            Entry("developer-enabled", device.developerEnabled.orEmpty()),
            Entry("keyed-developer-id", device.keyedDeveloperId.orEmpty()),
            Entry("search-enabled", device.searchEnabled.orEmpty()),
            Entry("voice-search-enabled", device.voiceSearchEnabled.orEmpty()),
            Entry("notifications-enabled", device.notificationsEnabled.orEmpty()),
            Entry("notifications-first-use", device.notificationsFirstUse.orEmpty()),
            Entry("supports-private-listening", device.supportsPrivateListening.orEmpty()),
            Entry("headphones-connected", device.headphonesConnected.orEmpty())
        ).toPersistentList()
    }
}
