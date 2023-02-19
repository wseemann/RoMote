package wseemann.media.romote.utils;

import android.content.Context;
import android.os.Vibrator;
import android.view.View;

import wseemann.media.romote.di.CommonModule;

/**
 * Created by wseemann on 10/23/16.
 */
public class ViewUtils {

    private ViewUtils() {

    }

    public static void provideHapticFeedback(View view, int vibrateDurationMs) {
        if (CommonModule.PreferenceUtilsSingleton.preferenceUtils.shouldProvideHapticFeedback()) {
            Vibrator vibe = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibe.vibrate(vibrateDurationMs);
        }
    }
}