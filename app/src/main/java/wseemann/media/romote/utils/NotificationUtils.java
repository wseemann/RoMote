package wseemann.media.romote.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import com.jaku.core.KeypressKeyValues;

import wseemann.media.romote.R;
import wseemann.media.romote.activity.MainActivity;
import wseemann.media.romote.service.CommandService;

/**
 * Created by wseemann on 6/19/16.
 */
public class NotificationUtils {

    private NotificationUtils() {

    }

    public static Notification buildNotification(Context context, String title, String text, Bitmap bitmap) {

        String contentTitle = "Roku";
        String contentText = "";

        if (title != null) {
            contentTitle = title;
        }

        if (text != null) {
            contentText = text;
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        PendingIntent contentIntent = stackBuilder.getPendingIntent((int) System.currentTimeMillis(), 0);

        NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();

        NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setWhen(0)
                .setSmallIcon(R.mipmap.ic_launcher);

        if (bitmap != null) {
            builder.setLargeIcon(bitmap);
        }

        builder.addAction(GenerateActionCompat(context, R.drawable.ic_action_rewind, "Previous", 0, KeypressKeyValues.REV));
        builder.addAction(GenerateActionCompat(context, R.drawable.ic_action_pause, "Pause", 1, KeypressKeyValues.PLAY));
        builder.addAction(GenerateActionCompat(context, R.drawable.ic_action_fast_forward, "Next", 2, KeypressKeyValues.FWD));

        style.setShowActionsInCompactView(0, 1, 2);
        style.setShowCancelButton(true);

        builder.setStyle(style);

        return builder.build();
    }

    private static NotificationCompat.Action GenerateActionCompat(Context context, int icon, String title, int requestCode, KeypressKeyValues keypressKeyValue) {
        Intent intent = new Intent(context, CommandService.class);
        intent.putExtra("keypress", keypressKeyValue);
        PendingIntent pendingIntent = PendingIntent.getService(context, requestCode, intent, 0);

        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }
}
