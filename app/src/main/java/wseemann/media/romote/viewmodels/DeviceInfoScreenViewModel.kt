package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import wseemann.media.romote.data.Device
import wseemann.media.romote.data.Device.Companion.fromDevice
import wseemann.media.romote.data.Entry
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.device.DeviceRepository
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.model.DeviceInfoScreenUiState
import javax.inject.Inject

@HiltViewModel
class DeviceInfoScreenViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val deviceManager: DeviceManager,
    private val deviceRepository: DeviceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceInfoScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // The extras DeviceInfoActivity was started with arrive here by way of the SavedStateHandle.
        // Loading from init rather than from an event means a configuration change re-creates the
        // Activity but not the ViewModel, so the device is queried exactly once.
        onLoadDeviceInfo(savedStateHandle[EXTRA_SERIAL_NUMBER])
    }

    private fun onLoadDeviceInfo(serialNumber: String?) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }

            // Paint what was stored when the device was paired, so there is something on screen
            // while the device itself is queried for its current details.
            serialNumber?.let {
                deviceRepository.getDevice(it)?.let { storedDevice ->
                    _uiState.update { it.copy(entries = parseDevice(storedDevice)) }
                }
            }

            deviceManager.getConnectedDevice()?.queryDeviceInfo()?.let { deviceInfo ->
                val device = fromDevice(deviceInfo)
                _uiState.update { it.copy(entries = parseDevice(device), isLoading = false) }
            } ?: run {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun parseDevice(device: Device): ImmutableList<Entry> = listOf(
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

    companion object {
        const val EXTRA_SERIAL_NUMBER = "serial_number"
    }
}
