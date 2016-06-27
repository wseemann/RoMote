package wseemann.media.romote.parser;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

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

            List<Element> children = rootNode.getChildren();

            device.setUdn(rootNode.getChild("udn").getValue());
            device.setSerialNumber(rootNode.getChild("serial-number").getValue());
            device.setDeviceId(rootNode.getChild("device-id").getValue());
            device.setVendorName(rootNode.getChild("vendor-name").getValue());
            device.setModelNumber(rootNode.getChild("model-number").getValue());
            device.setModelName(rootNode.getChild("model-name").getValue());
            device.setWifiMac(rootNode.getChild("wifi-mac").getValue());
            device.setEthernetMac(rootNode.getChild("ethernet-mac").getValue());
            device.setNetworkType(rootNode.getChild("network-type").getValue());
            device.setUserDeviceName(rootNode.getChild("user-device-name").getValue());
            device.setSoftwareVersion(rootNode.getChild("software-version").getValue());
            device.setSoftwareBuild(rootNode.getChild("software-build").getValue());
            device.setSecureDevice(rootNode.getChild("secure-device").getValue());
            device.setLanguage(rootNode.getChild("language").getValue());
            device.setCountry(rootNode.getChild("country").getValue());
            device.setLocale(rootNode.getChild("locale").getValue());
            device.setTimeZone(rootNode.getChild("time-zone").getValue());
            device.setTimeZoneOffset(rootNode.getChild("time-zone-offset").getValue());
            device.setPowerMode(rootNode.getChild("power-mode").getValue());
            device.setDeveloperEnabled(rootNode.getChild("developer-enabled").getValue());
            device.setKeyedDeveloperId(rootNode.getChild("keyed-developer-id").getValue());
            device.setSearchEnabled(rootNode.getChild("search-enabled").getValue());
            device.setVoiceSearchEnabled(rootNode.getChild("voice-search-enabled").getValue());
            device.setNotificationsEnabled(rootNode.getChild("notifications-enabled").getValue());
            device.setNotificationsFirstUse(rootNode.getChild("notifications-first-use").getValue());
            device.setHeadphonesConnected(rootNode.getChild("headphones-connected").getValue());

        } catch (JDOMException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return device;
    }
}
