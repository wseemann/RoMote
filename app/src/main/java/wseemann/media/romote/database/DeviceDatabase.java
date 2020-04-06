package wseemann.media.romote.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by wfseeman on 6/19/16.
 */
public class DeviceDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "devices";
    public static final String DEVICES_TABLE_NAME = "devices";

    public static final String HOST = "host";
    public static final String UDN = "udn";
    public static final String SERIAL_NUMBER = "serial_number";
    public static final String DEVICE_ID = "device_id";
    public static final String VENDOR_NAME = "vendor_name";
    public static final String MODEL_NUMBER = "model_number";
    public static final String MODEL_NAME = "model_name";
    public static final String WIFI_MAC = "wifi_mac";
    public static final String ETHERNET_MAC = "ethernet_mac";
    public static final String NETWORK_TYPE = "network_type";
    public static final String USER_DEVICE_NAME = "user_device_name";
    public static final String SOFTWARE_VERSION = "software_version";
    public static final String SOFTWARE_BUILD = "software_build";
    public static final String SECURE_DEVICE = "secure_device";
    public static final String LANGUAGE = "language";
    public static final String COUNTY = "country";
    public static final String LOCALE = "locale";
    public static final String TIME_ZONE = "time_zone";
    public static final String TIME_ZONE_OFFSET = "time_zone_offset";
    public static final String POWER_MODE = "power_mode";

    public static final String SUPPORTS_SUSPEND = "supports_suspend";
    public static final String SUPPORTS_FIND_REMOTE = "supports_find_remote";
    public static final String SUPPORTS_AUDIO_GUIDE = "supports_audio_guide";

    public static final String DEVELOPER_ENABLED = "developer_enabled";
    public static final String KEYED_DEVELOPER_ID = "keyed_developer_id";
    public static final String SEARCH_ENABLED = "search_enabled";
    public static final String VOICE_SEARCH_ENABLED = "voice_search_enabled";
    public static final String NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String NOTIFICATIONS_FIRST_USE = "notifications_first_use";
    public static final String SUPPORTS_PRIVATE_LISTENING = "supports_private_listening";
    public static final String HEADPHONES_CONNECTED = "headphones_connected";
    public static final String IS_TV = "is_tv";
    public static final String IS_STICK = "is_stick";

    private static final String DEVICES_TABLE_CREATE =
            "CREATE TABLE " + DEVICES_TABLE_NAME + " ("
                    //+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + HOST + " TEXT,"
                    + UDN + " TEXT,"
                    + SERIAL_NUMBER + " TEXT PRIMARY KEY,"
                    + DEVICE_ID + " TEXT,"
                    + VENDOR_NAME + " TEXT,"
                    + MODEL_NUMBER + " TEXT,"
                    + MODEL_NAME + " TEXT,"
                    + WIFI_MAC + " TEXT,"
                    + ETHERNET_MAC + " TEXT,"
                    + NETWORK_TYPE + " TEXT,"
                    + USER_DEVICE_NAME + " TEXT,"
                    + SOFTWARE_VERSION + " TEXT,"
                    + SOFTWARE_BUILD + " TEXT,"
                    + SECURE_DEVICE + " TEXT,"
                    + LANGUAGE + " TEXT,"
                    + COUNTY + " TEXT,"
                    + LOCALE + " TEXT,"
                    + TIME_ZONE + " TEXT,"
                    + TIME_ZONE_OFFSET + " TEXT,"
                    + POWER_MODE + " TEXT,"
                    + SUPPORTS_SUSPEND + " TEXT,"
                    + SUPPORTS_FIND_REMOTE + " TEXT,"
                    + SUPPORTS_AUDIO_GUIDE + " TEXT,"
                    + DEVELOPER_ENABLED + " TEXT,"
                    + KEYED_DEVELOPER_ID + " TEXT,"
                    + SEARCH_ENABLED + " TEXT,"
                    + VOICE_SEARCH_ENABLED + " TEXT,"
                    + NOTIFICATIONS_ENABLED + " TEXT,"
                    + NOTIFICATIONS_FIRST_USE + " TEXT,"
                    + SUPPORTS_PRIVATE_LISTENING + " TEXT,"
                    + HEADPHONES_CONNECTED + " TEXT,"
                    + IS_TV + " TEXT,"
                    + IS_STICK + " TEXT);";

    private static final String[] DEVICES_TABLE_ALTER_VERSION_TWO = {
            "ALTER TABLE " + DEVICES_TABLE_NAME + " ADD COLUMN " + SUPPORTS_SUSPEND + " TEXT;",
            "ALTER TABLE " + DEVICES_TABLE_NAME + " ADD COLUMN " + SUPPORTS_FIND_REMOTE + " TEXT;",
            "ALTER TABLE " + DEVICES_TABLE_NAME + " ADD COLUMN " + SUPPORTS_AUDIO_GUIDE + " TEXT;",
            "ALTER TABLE " + DEVICES_TABLE_NAME + " ADD COLUMN " + SUPPORTS_PRIVATE_LISTENING + " TEXT;"};

    private static final String[] DEVICES_TABLE_ALTER_VERSION_THREE = {
            "ALTER TABLE " + DEVICES_TABLE_NAME + " ADD COLUMN " + IS_TV + " TEXT;",
            "ALTER TABLE " + DEVICES_TABLE_NAME + " ADD COLUMN " + IS_STICK + " TEXT;"};

    public DeviceDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DEVICES_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 2) {
            for (int i = 0; i < DEVICES_TABLE_ALTER_VERSION_TWO.length; i++) {
                db.execSQL(DEVICES_TABLE_ALTER_VERSION_TWO[i]);
            }
        } else if (newVersion == 3) {
            for (int i = 0; i < DEVICES_TABLE_ALTER_VERSION_THREE.length; i++) {
                db.execSQL(DEVICES_TABLE_ALTER_VERSION_THREE[i]);
            }
        }
    }
}