package wseemann.media.romote.parser;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.StringReader;

import wseemann.media.romote.model.Device;

/**
 * Created by wseemann on 6/19/16.
 */
public class DeviceInfoParser {

    public DeviceInfoParser() {

    }

    public Device parse(String xml) {
        Device device = new Device();

        SAXBuilder builder = new SAXBuilder();

        if (xml == null) {
            return device;
        }

        Document document;
        try {
            document = (Document) builder.build(new StringReader(xml));
            Element rootNode = document.getRootElement();

            device.setUdn(checkValue(rootNode.getChild("udn")));
            device.setSerialNumber(checkValue(rootNode.getChild("serial-number")));
            device.setDeviceId(checkValue(rootNode.getChild("device-id")));
            device.setVendorName(checkValue(rootNode.getChild("vendor-name")));
            device.setModelNumber(checkValue(rootNode.getChild("model-number")));
            device.setModelName(checkValue(rootNode.getChild("model-name")));
            device.setWifiMac(checkValue(rootNode.getChild("wifi-mac")));
            device.setEthernetMac(checkValue(rootNode.getChild("ethernet-mac")));
            device.setNetworkType(checkValue(rootNode.getChild("network-type")));
            device.setUserDeviceName(checkValue(rootNode.getChild("user-device-name")));
            device.setSoftwareVersion(checkValue(rootNode.getChild("software-version")));
            device.setSoftwareBuild(checkValue(rootNode.getChild("software-build")));
            device.setSecureDevice(checkValue(rootNode.getChild("secure-device")));
            device.setLanguage(checkValue(rootNode.getChild("language")));
            device.setCountry(checkValue(rootNode.getChild("country")));
            device.setLocale(checkValue(rootNode.getChild("locale")));
            device.setTimeZone(checkValue(rootNode.getChild("time-zone")));
            device.setTimeZoneOffset(checkValue(rootNode.getChild("time-zone-offset")));
            device.setPowerMode(checkValue(rootNode.getChild("power-mode")));
            device.setDeveloperEnabled(checkValue(rootNode.getChild("developer-enabled")));
            device.setKeyedDeveloperId(checkValue(rootNode.getChild("keyed-developer-id")));
            device.setSearchEnabled(checkValue(rootNode.getChild("search-enabled")));
            device.setVoiceSearchEnabled(checkValue(rootNode.getChild("voice-search-enabled")));
            device.setNotificationsEnabled(checkValue(rootNode.getChild("notifications-enabled")));
            device.setNotificationsFirstUse(checkValue(rootNode.getChild("notifications-first-use")));
            device.setHeadphonesConnected(checkValue(rootNode.getChild("headphones-connected")));

        } catch (JDOMException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return device;
    }

    private String checkValue(Element element) {
        if (element == null) {
            return null;
        }

        return element.getValue();
    }
}
