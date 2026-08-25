package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.model.Device
import com.wseemann.ecp.request.QueryDeviceInfoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wseemann.media.romote.model.Device.Companion.fromDevice
import wseemann.media.romote.model.DeviceInfoUiState
import wseemann.media.romote.model.Entry
import wseemann.media.romote.tasks.ResponseCallbackWrapper
import wseemann.media.romote.utils.CommandHelper
import wseemann.media.romote.utils.DBUtils
import javax.inject.Inject

@HiltViewModel
class DeviceInfoScreenViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val commandHelper: CommandHelper
) : ViewModel() {

    private val _deviceInfo = MutableStateFlow<DeviceInfoUiState>(DeviceInfoUiState.Loading)
    val deviceInfo = _deviceInfo.asStateFlow()
    val deviceInfoLiveData = deviceInfo.asLiveData()

    fun queryDeviceInfo(serialNumber: String?, host: String?) {
        viewModelScope.launch {
            val dbDevice = withContext(Dispatchers.IO) {
                DBUtils.getDevice(context, serialNumber)
            }
            if (dbDevice != null) {
                _deviceInfo.value = DeviceInfoUiState.Success(parseDevice(dbDevice))
            }

            val command = if (host == null) {
                commandHelper.getConnectedDeviceInfoURL()
            } else {
                commandHelper.getDeviceInfoURL(host)
            }
            sendCommand(command)
        }
    }

    private fun sendCommand(command: String) {
        val queryActiveAppRequest = QueryDeviceInfoRequest(command)
        queryActiveAppRequest.sendAsync(ResponseCallbackWrapper(object : ResponseCallback<Device?> {
            override fun onSuccess(data: Device?) {
                if (data == null) {
                    _deviceInfo.value = DeviceInfoUiState.Error(Exception("Device returned null"))
                } else {
                    _deviceInfo.value = DeviceInfoUiState.Success(parseDevice(fromDevice(data)))
                }
            }

            override fun onError(ex: Exception) {
                _deviceInfo.value = DeviceInfoUiState.Error(ex)
            }
        }))
    }

    private fun parseDevice(device: wseemann.media.romote.model.Device): MutableList<Entry> {
        val entries: MutableList<Entry> = ArrayList()

        entries.add(Entry("udn", device.udn))
        entries.add(Entry("serial-number", device.serialNumber))
        entries.add(Entry("device-id", device.deviceId))
        entries.add(Entry("vendor-name", device.vendorName))
        entries.add(Entry("model-number", device.modelNumber))
        entries.add(Entry("model-name", device.modelName))
        entries.add(Entry("wifi-mac", device.wifiMac))
        entries.add(Entry("ethernet-mac", device.ethernetMac))
        entries.add(Entry("network-type", device.networkType))
        entries.add(Entry("user-device-name", device.userDeviceName))
        entries.add(Entry("software-version", device.softwareVersion))
        entries.add(Entry("software-build", device.softwareBuild))
        entries.add(Entry("secure-device", device.secureDevice))
        entries.add(Entry("language", device.language))
        entries.add(Entry("country", device.country))
        entries.add(Entry("locale", device.locale))
        entries.add(Entry("time-zone", device.timeZone))
        entries.add(Entry("time-zone-offset", device.timeZoneOffset))
        entries.add(Entry("power-mode", device.powerMode))
        entries.add(Entry("supports-suspend", device.supportsSuspend))
        entries.add(Entry("supports-find-remote", device.supportsFindRemote))
        entries.add(Entry("supports-audio-guide", device.supportsAudioGuide))
        entries.add(Entry("developer-enabled", device.developerEnabled))
        entries.add(Entry("keyed-developer-id", device.keyedDeveloperId))
        entries.add(Entry("search-enabled", device.searchEnabled))
        entries.add(Entry("voice-search-enabled", device.voiceSearchEnabled))
        entries.add(Entry("notifications-enabled", device.notificationsEnabled))
        entries.add(Entry("notifications-first-use", device.notificationsFirstUse))
        entries.add(Entry("supports-private-listening", device.supportsPrivateListening))
        entries.add(Entry("headphones-connected", device.headphonesConnected))

        return entries
    }
}
