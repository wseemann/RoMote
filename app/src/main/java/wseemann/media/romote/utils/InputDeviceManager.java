package wseemann.media.romote.utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import android.annotation.TargetApi;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;

import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.core.ECPRequest;
import com.wseemann.ecp.core.KeyPressKeyValues;
import com.wseemann.ecp.request.KeyupRequest;
import com.wseemann.ecp.request.KeydownRequest;

import wseemann.media.romote.utils.BroadcastUtils;
import wseemann.media.romote.utils.CommandHelper;

public class InputDeviceManager implements InputManager.InputDeviceListener {

    public interface HardwareDevicesListener {
        abstract void onIsEnabledWithDevices(boolean is_enabled, boolean has_devices);
    }

    private Context mContext;
    private CommandHelper mCommandHelper;
    private HashMap<String,ECPRequest> mRequestCache;
    private InputManager mInputManager;
    private HardwareDevicesListener mHardwareDevicesListener;
    private boolean mIsEnabled;

    public InputDeviceManager(Context context, CommandHelper command_helper) {
        mContext = context;
        mCommandHelper = command_helper;
        mRequestCache = new HashMap();
        mInputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        mIsEnabled = true;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }
    
    public boolean isEnabledWithDevices() {
        return mIsEnabled && isAnyDeviceExternal();
    }
    
    public void setEnabled(boolean is_enabled) {
        mIsEnabled = is_enabled;
        notifyEnabledWithDevices();
    }

    public void resume() {
        mInputManager.registerInputDeviceListener(this, new Handler(Looper.getMainLooper()));
    }

    public void pause() {
        mInputManager.unregisterInputDeviceListener(this);
    }

    private boolean sourceIsHardware(int source) {
        return
            ((source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
            ((source & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) ||
            ((source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK);
    }

    
    @TargetApi(29)
    private boolean deviceIsExternal(InputDevice device) {
        if (device == null ||
            device.isVirtual() ||
            !sourceIsHardware(device.getSources()) ||
            device.getVendorId() == 0 ||
            device.getProductId() == 0 ||
            device.getName().toLowerCase().matches(".*(virtual|touchpad|touchpanel|kpd|pmic|hall).*"))
            return false;

        try {
            Class<?> cls = device.getClass();
            Method method = cls.getMethod("isExternal");
            Boolean res = (Boolean) method.invoke(device);
            return res.booleanValue();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            return true;
        }
    }

    private boolean isAnyDeviceExternal() {
        int[] deviceIds = mInputManager.getInputDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice device = mInputManager.getInputDevice(deviceId);
            if (deviceIsExternal(device))
                return true;
        }
        return false;
    }

    public void setHardwareDevicesListener(HardwareDevicesListener listerner) {
        mHardwareDevicesListener = listerner;
    }

    private void notifyEnabledWithDevices() {
        if (mHardwareDevicesListener == null)
            return;
        mHardwareDevicesListener.onIsEnabledWithDevices(mIsEnabled, isAnyDeviceExternal());
    }

    public void onInputDeviceAdded(int deviceId) {
        notifyEnabledWithDevices();
    }

    public void onInputDeviceChanged(int deviceId) {
        notifyEnabledWithDevices();
    }

    public void onInputDeviceRemoved(int deviceId) {
        notifyEnabledWithDevices();
    }

    // TODO: implement mapping configuration
    private KeyPressKeyValues mapEventKeyCode(int keyCode) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BUTTON_A: return KeyPressKeyValues.SELECT;
        case KeyEvent.KEYCODE_BUTTON_B: return KeyPressKeyValues.BACK;
        case KeyEvent.KEYCODE_BUTTON_L1: return KeyPressKeyValues.REV;
        case KeyEvent.KEYCODE_BUTTON_L2: return KeyPressKeyValues.VOLUME_DOWN;
        case KeyEvent.KEYCODE_BUTTON_R1: return KeyPressKeyValues.FWD;
        case KeyEvent.KEYCODE_BUTTON_R2: return KeyPressKeyValues.VOLUME_UP;
        case KeyEvent.KEYCODE_BUTTON_START: return KeyPressKeyValues.HOME;
        case KeyEvent.KEYCODE_BUTTON_THUMBL: return KeyPressKeyValues.SELECT;
        case KeyEvent.KEYCODE_BUTTON_THUMBR: return KeyPressKeyValues.PLAY;
        case KeyEvent.KEYCODE_BUTTON_X: return KeyPressKeyValues.INTANT_REPLAY;
        case KeyEvent.KEYCODE_BUTTON_Y: return KeyPressKeyValues.INFO;
        case KeyEvent.KEYCODE_DEL: return KeyPressKeyValues.BACKSPACE;
        case KeyEvent.KEYCODE_ENTER: return KeyPressKeyValues.ENTER;
        case KeyEvent.KEYCODE_DPAD_UP: return KeyPressKeyValues.UP;
        case KeyEvent.KEYCODE_DPAD_DOWN: return KeyPressKeyValues.DOWN;
        case KeyEvent.KEYCODE_DPAD_LEFT: return KeyPressKeyValues.LEFT;
        case KeyEvent.KEYCODE_DPAD_RIGHT: return KeyPressKeyValues.RIGHT;
        case KeyEvent.KEYCODE_DPAD_CENTER: return KeyPressKeyValues.SELECT;
        }
        return null;
    }

    private String mapKeyEvent(int uc_char) {
        if (uc_char == 0)
            return null;

        return KeyPressKeyValues.LIT_ + Character.toString((char) uc_char);
    }

    private void requestSend(final ECPRequest<Void> request) {
        request.sendAsync(new ResponseCallback<>() {
            @Override
            public void onSuccess(@Nullable Void unused) { }

            @Override
            public void onError(@NonNull Exception ex) { }
        });
    }

    private void performRequest(String url_key, int action) {
        String url = mCommandHelper.getDeviceURL();

        ECPRequest<Void> request;
        String cache_key;
        switch(action) {
        case KeyEvent.ACTION_DOWN:
            cache_key = url + "/DOWN/" + url_key;
            request = mRequestCache.get(cache_key);
            if (request == null) {
                try {
                    request = new KeydownRequest(url, url_key);
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                    return;
                }
                mRequestCache.put(cache_key, request);
            }
            break;
        case KeyEvent.ACTION_UP:
            cache_key = url + "/UP/" + url_key;
            request = mRequestCache.get(cache_key);
            if (request == null) {
                try {
                    request = new KeyupRequest(url, url_key);
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                    return;
                }
                mRequestCache.put(cache_key, request);
            }
            break;
        default:
            throw new IllegalArgumentException("Unsupported keypress action " +
                                               Integer.toString(action));
        }

        requestSend(request);
    }

    private boolean processSpecialKeyAction(int key_code, int action) {
        if (key_code == KeyEvent.KEYCODE_BUTTON_SELECT &&
            action == KeyEvent.ACTION_DOWN) {
            setEnabled(!mIsEnabled);
            return true;
        }

        if (!mIsEnabled)
            return false;

        // Emulate diagonals, as ECP does not support them
        switch (key_code) {
        case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
            processKeyAction(KeyEvent.KEYCODE_DPAD_DOWN, 0, action);
            processKeyAction(KeyEvent.KEYCODE_DPAD_LEFT, 0, action);
            return true;
        case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
            processKeyAction(KeyEvent.KEYCODE_DPAD_DOWN, 0, action);
            processKeyAction(KeyEvent.KEYCODE_DPAD_RIGHT, 0, action);
            return true;
        case KeyEvent.KEYCODE_DPAD_UP_LEFT:
            processKeyAction(KeyEvent.KEYCODE_DPAD_UP, 0, action);
            processKeyAction(KeyEvent.KEYCODE_DPAD_LEFT, 0, action);
            return true;
        case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
            processKeyAction(KeyEvent.KEYCODE_DPAD_UP, 0, action);
            processKeyAction(KeyEvent.KEYCODE_DPAD_RIGHT, 0, action);
            return true;
        }
        return false;
    }

    private boolean processKeyAction(int key_code, int uc_char, int action) {
        if (processSpecialKeyAction (key_code, action))
            return true;

        if (!mIsEnabled)
            return false;

        String url_key;
        KeyPressKeyValues keypressKeyValue = mapEventKeyCode(key_code);
        if (keypressKeyValue != null) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (keypressKeyValue == KeyPressKeyValues.BACK ||
                    keypressKeyValue == KeyPressKeyValues.HOME ||
                    keypressKeyValue == KeyPressKeyValues.SELECT) {
                    BroadcastUtils.Companion.sendUpdateDeviceBroadcast(mContext);
                }
            }
            url_key = keypressKeyValue.getValue();
        } else {
            url_key = mapKeyEvent(uc_char);
            if (url_key == null)
                return false;
        }

        performRequest(url_key, action);
        return true;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!sourceIsHardware(event.getSource()))
            return false;

        int action = event.getAction();
        if (mIsEnabled &&
            action == KeyEvent.ACTION_DOWN &&
            event.getRepeatCount() > 0)
            return true;

        return processKeyAction(event.getKeyCode(), event.getUnicodeChar(), action);
    }
}
