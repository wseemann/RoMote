package wseemann.media.romote.model

class Device : com.wseemann.ecp.model.Device() {

    private var customUserDeviceName: String? = null

    companion object {
        fun fromDevice(jakuDevice: com.wseemann.ecp.model.Device): Device {
            val device = Device()

            device.host = jakuDevice.host
            device.udn = jakuDevice.udn
            device.serialNumber = jakuDevice.serialNumber
            device.deviceId = jakuDevice.deviceId
            device.vendorName = jakuDevice.vendorName
            device.modelNumber = jakuDevice.modelNumber
            device.modelName = jakuDevice.modelName
            device.wifiMac = jakuDevice.wifiMac
            device.ethernetMac = jakuDevice.ethernetMac
            device.networkType = jakuDevice.networkType
            device.userDeviceName = jakuDevice.userDeviceName
            device.softwareVersion = jakuDevice.softwareVersion
            device.softwareBuild = jakuDevice.softwareBuild
            device.secureDevice = jakuDevice.secureDevice
            device.language = jakuDevice.language
            device.country = jakuDevice.country
            device.locale = jakuDevice.locale
            device.timeZone = jakuDevice.timeZone
            device.timeZoneOffset = jakuDevice.timeZoneOffset
            device.powerMode = jakuDevice.powerMode
            device.supportsSuspend = jakuDevice.supportsSuspend
            device.supportsFindRemote = jakuDevice.supportsFindRemote
            device.supportsAudioGuide = jakuDevice.supportsAudioGuide
            device.developerEnabled = jakuDevice.developerEnabled
            device.keyedDeveloperId = jakuDevice.keyedDeveloperId
            device.searchEnabled = jakuDevice.searchEnabled
            device.voiceSearchEnabled = jakuDevice.voiceSearchEnabled
            device.notificationsEnabled = jakuDevice.notificationsEnabled
            device.notificationsFirstUse = jakuDevice.notificationsFirstUse
            device.supportsPrivateListening = jakuDevice.supportsPrivateListening
            device.headphonesConnected = jakuDevice.headphonesConnected
            device.isTv = jakuDevice.isTv
            device.isStick = jakuDevice.isStick

            return device
        }
    }

    fun getCustomUserDeviceName(): String? {
        return customUserDeviceName
    }

    fun setCustomUserDeviceName(customUserDeviceName: String?) {
        this.customUserDeviceName = customUserDeviceName
    }
}