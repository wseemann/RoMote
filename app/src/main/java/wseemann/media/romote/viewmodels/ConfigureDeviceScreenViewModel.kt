package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.data.DeviceDiscovery
import wseemann.media.romote.event.ConfigureDeviceScreenUiEvent
import wseemann.media.romote.model.ConfigureDeviceScreenUiState
import wseemann.media.romote.utils.DBUtils
import javax.inject.Inject

@HiltViewModel
class ConfigureDeviceScreenViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val deviceDiscovery: DeviceDiscovery
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigureDeviceScreenUiState())
    val uiState = _uiState.asStateFlow()
    val uiStateLiveData = uiState.asLiveData()

    fun onHandleEvent(event: ConfigureDeviceScreenUiEvent) {
        when (event) {
            is ConfigureDeviceScreenUiEvent.LoadAvailableDevicesEvent -> onLoadAvailableDevices()
        }
    }

    private fun onLoadAvailableDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val discovered = deviceDiscovery.discoverDevices()

                // Read what is paired after the scan, so a device unpaired mid-scan is still
                // offered here.
                val pairedSerialNumbers = DBUtils.getAllDevices(context)
                    .map { it.serialNumber }
                    .toSet()

                _uiState.update {
                    it.copy(
                        availableDevices = discovered
                            .filterNot { device -> device.serialNumber in pairedSerialNumbers }
                            .toPersistentList(),
                        isLoading = false
                    )
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private companion object {
        const val TAG = "ConfigureDeviceScreenViewModel"
    }
}
