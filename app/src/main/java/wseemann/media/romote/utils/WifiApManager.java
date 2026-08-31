/*
 * Copyright 2013 WhiteByte (Nick Russler, Ahmet Yueksektepe).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wseemann.media.romote.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import wseemann.media.romote.data.ClientScanResult;
import wseemann.media.romote.utils.Constants.WIFI_AP_STATE;

public class WifiApManager {
    private final WifiManager mWifiManager;

    public WifiApManager(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Gets the Wi-Fi enabled state.
     *
     * @return {@link WIFI_AP_STATE}
     * @see #isWifiApEnabled()
     */
    public WIFI_AP_STATE getWifiApState() {
        // getWifiApState is a hidden API behind ACCESS_WIFI_STATE, and the manifest stops asking
        // for that permission above API 27 because the non-SDK interface restrictions block the
        // call there anyway. Without this the reflection still runs and the platform answers with
        // a SecurityException stack trace on every scan.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
        }

        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApState");

            int tmp = ((Integer) method.invoke(mWifiManager));

            // Fix for Android 4
            if (tmp >= 10) {
                tmp = tmp - 10;
            }

            return WIFI_AP_STATE.class.getEnumConstants()[tmp];
        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
            return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
        }
    }

    /**
     * Return whether Wi-Fi AP is enabled or disabled.
     *
     * @return {@code true} if Wi-Fi AP is enabled
     * @hide Dont open yet
     * @see #getWifiApState()
     */
    public boolean isWifiApEnabled() {
        return getWifiApState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;
    }

    /**
     * Gets a list of the clients connected to the Hotspot
     *
     * @param onlyReachables   {@code false} if the list should contain unreachable (probably disconnected) clients, {@code true} otherwise
     * @param reachableTimeout Reachable Timout in miliseconds
     * @param finishListener,  Interface called when the scan method finishes
     */
    public ArrayList<ClientScanResult> getClientList(final boolean onlyReachables, final int reachableTimeout) {
        BufferedReader br = null;
        final ArrayList<ClientScanResult> result = new ArrayList<ClientScanResult>();

        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");

                if ((splitted != null) && (splitted.length >= 4)) {
                    // Basic sanity check
                    String mac = splitted[3];

                    if (mac.matches("..:..:..:..:..:..")) {
                        boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(reachableTimeout);

                        if (!onlyReachables || isReachable) {
                            result.add(new ClientScanResult(splitted[0], splitted[3], splitted[5], isReachable));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(this.getClass().toString(), e.toString());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                Log.e(this.getClass().toString(), e.getMessage());
            }
        }

        return result;
    }
}
