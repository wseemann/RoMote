package wseemann.media.romote.view;

import android.content.Context;
import android.os.VibrationEffect;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageButton;

import wseemann.media.romote.utils.ViewUtils;

public class VibratingImageButton extends AppCompatImageButton {

    private static final int VIBRATE_DURATION_MS = 25;

    private View.OnClickListener mClickListener;
    private View.OnTouchListener mTouchListener;

    private boolean preventClick = false;

    public VibratingImageButton(Context context) {
        this(context, null);
    }

    public VibratingImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.buttonStyle);
    }

    public VibratingImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnClickListener((View view) -> {
                if (!preventClick) {
                    ViewUtils.provideHapticEffect(view, VibrationEffect.EFFECT_TICK, VIBRATE_DURATION_MS);
                    if (mClickListener != null)
                        mClickListener.onClick(view);
                }
                preventClick = false;
            });

        super.setOnTouchListener((View view, MotionEvent event) -> {
                if (mTouchListener == null)
                    return false;

                switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ViewUtils.provideHapticEffect(view, VibrationEffect.EFFECT_TICK, VIBRATE_DURATION_MS);
                    preventClick = true;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    preventClick = false;
                    break;
                }

                return mTouchListener.onTouch(view, event);
            });
    }

    @Override
    public void setOnClickListener(View.OnClickListener listener) {
        mClickListener = listener;
    }

    @Override
    public void setOnTouchListener(View.OnTouchListener listener) {
        mTouchListener = listener;
    }
}
