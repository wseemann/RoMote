package wseemann.media.romote.device

class DeviceManager(private val deviceRepository: DeviceRepository) {

    fun setConnectedDevice(serialNumber: String?) {
        deviceRepository.setConnectedDevice(serialNumber)
    }

    fun getConnectedDevice(): Device? = deviceRepository.getConnectedDevice()?.let {
        Device(it)
    }
}
