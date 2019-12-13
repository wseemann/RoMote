package wseemann.media.romote.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;

import wseemann.media.romote.utils.ViewUtils;

public class VibratingImageButton extends AppCompatImageButton {

    private static final int VIBRATE_DURATION_MS = 100;

    private View.OnClickListener mListener;

    public VibratingImageButton(Context context) {
        this(context, null);
    }

    public VibratingImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.buttonStyle);
    }

    public VibratingImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnClickListener((View view) -> {
            ViewUtils.provideHapticFeedback(view, VIBRATE_DURATION_MS);

            if (mListener != null) {
                mListener.onClick(view);
            }
        });
    }

    @Override
    public void setOnClickListener(View.OnClickListener listener) {
        mListener = listener;
    }
}
