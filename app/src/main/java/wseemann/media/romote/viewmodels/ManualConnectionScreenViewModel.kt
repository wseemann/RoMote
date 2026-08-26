package wseemann.media.romote.viewmodels

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.wseemann.ecp.api.ResponseCallback
import com.wseemann.ecp.model.Device
import com.wseemann.ecp.request.QueryDeviceInfoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import wseemann.media.romote.event.ManualConnectionScreenUiEvent
import wseemann.media.romote.model.ManualConnectionScreenUiState
import wseemann.media.romote.tasks.ResponseCallbackWrapper
import wseemann.media.romote.utils.DBUtils
import wseemann.media.romote.utils.PreferenceUtils
import javax.inject.Inject

@HiltViewModel
class ManualConnectionScreenViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val preferenceUtils: PreferenceUtils,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualConnectionScreenUiState())
    val uiState = _uiState.asStateFlow()
    val uiStateLiveData = uiState.asLiveData()

    fun onHandleEvent(event: ManualConnectionScreenUiEvent) {
        when (event) {
            is ManualConnectionScreenUiEvent.ConnectClickedEvent -> onConnectClicked(event.host)
        }
    }

    private fun onConnectClicked(host: String) {
        val queryActiveAppRequest = QueryDeviceInfoRequest(host)
        queryActiveAppRequest.sendAsync(ResponseCallbackWrapper(object :
            ResponseCallback<Device?> {
            override fun onSuccess(device: Device?) {
                device?.host = host
                storeDevice(wseemann.media.romote.data.Device.fromDevice(device!!))
            }

            override fun onError(ex: Exception) {
            }
        }))
    }

    private fun storeDevice(device: wseemann.media.romote.data.Device) {
        DBUtils.insertDevice(context, device)
        preferenceUtils.setConnectedDevice(device.serialNumber)

        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("first_use", false)
        editor.commit()
    }
}