package wseemann.media.romote.utils;

/**
 * Created by wseemann on 10/6/17.
 */

public enum RokuRequestTypes {
    query_active_app("query/active-app"),
    query_device_info("query/device-info"),
    launch("launch"),
    keypress("keypress"),
    query_icon("query/icon");

    private final String method;

    RokuRequestTypes(String method) {
        this.method = method;
    }

    public String getValue() {
        return method;
    }
}

