package wseemann.media.romote.model;

/**
 * Created by wseemann on 6/19/16.
 */
public class Device {

    private String udn;
    private String serialNumber;
    private String deviceId;
    private String vendorName;
    private String modelNumber;
    private String modelName;
    private String wifiMac;
    private String ethernetMac;
    private String networkType;
    private String userDeviceName;
    private String softwareVersion;
    private String softwareBuild;
    private String secureDevice;
    private String language;
    private String country;
    private String locale;
    private String timeZone;
    private String timeZoneOffset;
    private String powerMode;
    private String developerEnabled;
    private String keyedDeveloperId;
    private String searchEnabled;
    private String voiceSearchEnabled;
    private String notificationsEnabled;
    private String notificationsFirstUse;
    private String headphonesConnected;
    private String mHost;

    public Device() {

    }

    public String getHost() {
        return mHost;
    }

    public void setHost(String host) {
        this.mHost = host;
    }

    @Override
    public String toString() {
        return mHost;
    }

    public String getUdn() {
        return udn;
    }

    public void setUdn(String udn) {
        this.udn = udn;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getWifiMac() {
        return wifiMac;
    }

    public void setWifiMac(String wifiMac) {
        this.wifiMac = wifiMac;
    }

    public String getEthernetMac() {
        return ethernetMac;
    }

    public void setEthernetMac(String ethernetMac) {
        this.ethernetMac = ethernetMac;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getUserDeviceName() {
        return userDeviceName;
    }

    public void setUserDeviceName(String userDeviceName) {
        this.userDeviceName = userDeviceName;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getSoftwareBuild() {
        return softwareBuild;
    }

    public void setSoftwareBuild(String softwareBuild) {
        this.softwareBuild = softwareBuild;
    }

    public String getSecureDevice() {
        return secureDevice;
    }

    public void setSecureDevice(String secureDevice) {
        this.secureDevice = secureDevice;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTimeZoneOffset() {
        return timeZoneOffset;
    }

    public void setTimeZoneOffset(String timeZoneOffset) {
        this.timeZoneOffset = timeZoneOffset;
    }

    public String getPowerMode() {
        return powerMode;
    }

    public void setPowerMode(String powerMode) {
        this.powerMode = powerMode;
    }

    public String getDeveloperEnabled() {
        return developerEnabled;
    }

    public void setDeveloperEnabled(String developerEnabled) {
        this.developerEnabled = developerEnabled;
    }

    public String getKeyedDeveloperId() {
        return keyedDeveloperId;
    }

    public void setKeyedDeveloperId(String keyedDeveloperId) {
        this.keyedDeveloperId = keyedDeveloperId;
    }

    public String getSearchEnabled() {
        return searchEnabled;
    }

    public void setSearchEnabled(String searchEnabled) {
        this.searchEnabled = searchEnabled;
    }

    public String getVoiceSearchEnabled() {
        return voiceSearchEnabled;
    }

    public void setVoiceSearchEnabled(String voiceSearchEnabled) {
        this.voiceSearchEnabled = voiceSearchEnabled;
    }

    public String getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(String notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getNotificationsFirstUse() {
        return notificationsFirstUse;
    }

    public void setNotificationsFirstUse(String notificationsFirstUse) {
        this.notificationsFirstUse = notificationsFirstUse;
    }

    public String getHeadphonesConnected() {
        return headphonesConnected;
    }

    public void setHeadphonesConnected(String headphonesConnected) {
        this.headphonesConnected = headphonesConnected;
    }
}
