package wseemann.media.romote.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import wseemann.media.romote.R;

public class RemoteButtonLayout extends LinearLayout {

    private RepeatingImageButton mUpButton;
    private RepeatingImageButton mLeftButton;
    private RepeatingImageButton mRightButton;
    private RepeatingImageButton mDownButton;
    private ImageButton mOkButton;

    public RemoteButtonLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RemoteButtonLayout(Context context) {
        super(context);
        init();
    }

    private void init() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.remote_button_layout, this);

        mUpButton = (RepeatingImageButton) findViewById(R.id.up_button);
        mLeftButton = (RepeatingImageButton) findViewById(R.id.left_button);
        mRightButton = (RepeatingImageButton) findViewById(R.id.right_button);
        mDownButton = (RepeatingImageButton) findViewById(R.id.down_button);
        mOkButton = (ImageButton) findViewById(R.id.ok_button);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        int width = View.MeasureSpec.getSize(widthSpec);
        int height = View.MeasureSpec.getSize(heightSpec);

        if (View.MeasureSpec.getMode(widthSpec) == View.MeasureSpec.EXACTLY
                && View.MeasureSpec.getMode(heightSpec) == View.MeasureSpec.EXACTLY) {
            setMeasuredDimension(width, height);
        } else {
            int size = Math.min(width, height);
            setMeasuredDimension(size, size);
        }
    }

    public void setUpButtonListener(final View.OnClickListener listener) {
        mUpButton.setOnClickListener(listener);
    }

    public void setLeftButtonListener(final View.OnClickListener listener) {
        mLeftButton.setOnClickListener(listener);
    }

    public void setRightButtonListener(final View.OnClickListener listener) {
        mRightButton.setOnClickListener(listener);
    }

    public void setDownButtonListener(final View.OnClickListener listener) {
        mDownButton.setOnClickListener(listener);
    }

    public void setOkButtonListener(final View.OnClickListener listener) {
        mOkButton.setOnClickListener(listener);
    }
}