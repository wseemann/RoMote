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

    private static Vibrator getVibrator(View view) {
        if (!CommonModule.PreferenceUtilsSingleton.preferenceUtils.shouldProvideHapticFeedback()) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) view.getContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vibratorManager.getDefaultVibrator();
        }
        return (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    // effect: use one of android.os.VibrationEffect
    public static void provideHapticEffect(View view, int effect_id, int fallbackVibrateDurationMs) {
        Vibrator vibrator = getVibrator(view);
        if (vibrator == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibrationEffect effect;
            if (effect_id == VibrationEffect.DEFAULT_AMPLITUDE) {
                effect = VibrationEffect.createOneShot(fallbackVibrateDurationMs, effect_id);
            } else {
                effect = VibrationEffect.createPredefined(effect_id);
            }
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(fallbackVibrateDurationMs); 
       }
    }

    public static void provideHapticFeedback(View view, int vibrateDurationMs) {
        provideHapticEffect(view, VibrationEffect.DEFAULT_AMPLITUDE, vibrateDurationMs);
    }
}
