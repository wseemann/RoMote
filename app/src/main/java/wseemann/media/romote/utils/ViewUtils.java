package wseemann.media.romote.utils;

import android.content.Context;
import android.os.Vibrator;
import android.view.View;

/**
 * Created by wseemann on 10/23/16.
 */
public class ViewUtils {

    private ViewUtils() {

    }

    public static void provideHapticFeedback(View view, int vibrateDurationMs) {
        if (PreferenceUtils.shouldProvideHapticFeedback(view.getContext())) {
            Vibrator vibe = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibe.vibrate(vibrateDurationMs);
        }
    }
}