package wseemann.media.romote.utils;

/**
 * Created by wseemann on 6/28/16.
 */
public class Constants {

    private Constants() {

    }

    public enum WIFI_AP_STATE {
        WIFI_AP_STATE_DISABLING,
        WIFI_AP_STATE_DISABLED,
        WIFI_AP_STATE_ENABLING,
        WIFI_AP_STATE_ENABLED,
        WIFI_AP_STATE_FAILED
    }

    public static final String UPDATE_DEVICE_BROADCAST = "wseemann.media.romote.UPDATE_DEVICE";
    public static final String PAYPAL_DONATION_LINK = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=C4RNUUK83P3E2";
    public static final String DISMISS_CONECTIVITY_DIALOG = "wseemann.media.romote.DISMISS_CONECTIVITY_DIALOG";
    public static final String PRIVATE_LISTENING_URL = "https://github.com/wseemann";
}
