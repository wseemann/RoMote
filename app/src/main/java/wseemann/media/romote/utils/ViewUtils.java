package wseemann.media.romote.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;

import wseemann.media.romote.BuildConfig;
import wseemann.media.romote.di.CommonModule;

/**
 * Created by wseemann on 10/23/16.
 */
public class ViewUtils {

    private ViewUtils() {

    }

    public static void provideHapticFeedback(View view, int vibrateDurationMs) {
        if (CommonModule.PreferenceUtilsSingleton.preferenceUtils.shouldProvideHapticFeedback()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) view.getContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                Vibrator vibrator = vibratorManager.getDefaultVibrator();
                vibrator.vibrate(VibrationEffect.createOneShot(vibrateDurationMs,VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                Vibrator vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(vibrateDurationMs);
            }
        }
    }
}