package wseemann.media.romote.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;

public class MyLinearLayout extends LinearLayout {
    private int heightMeasureSpec = 0;
    private OnSoftKeyboardListener onSoftKeyboardListener;
    private boolean redraw = true;
    private int widthMeasureSpec = 0;

    public interface OnSoftKeyboardListener {
        void onHidden();

        void onShown();
    }

    public MyLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyLinearLayout(Context context) {
        super(context);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.redraw) {
            this.widthMeasureSpec = widthMeasureSpec;
            this.heightMeasureSpec = heightMeasureSpec;
            if (this.onSoftKeyboardListener != null) {
                if (getMeasuredHeight() > MeasureSpec.getSize(heightMeasureSpec)) {
                    this.onSoftKeyboardListener.onShown();
                } else {
                    this.onSoftKeyboardListener.onHidden();
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        super.onMeasure(this.widthMeasureSpec, this.heightMeasureSpec);
    }

    public void setRedraw(boolean redraw) {
        this.redraw = redraw;
    }

    public final void setOnSoftKeyboardListener(OnSoftKeyboardListener listener) {
        this.onSoftKeyboardListener = listener;
    }
}