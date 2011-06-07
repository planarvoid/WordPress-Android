package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.LightingColorFilter;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.Button;

public class DarkButton extends Button {

    private Context mContext;
    private boolean mDown = false;

    public DarkButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }


    @Override
    protected void drawableStateChanged() {
        if (mDown){
            getBackground().setColorFilter(null);
            setTextColor(0xFF000000);
        } else {
            getBackground().setColorFilter(new LightingColorFilter(0xFF494949, 0xFF000000));
            setTextColor(0xFFFFFFFF);
        }
        super.drawableStateChanged();
        invalidate();
    }


    @Override
    public void setPressed(boolean pressed) {
       mDown = pressed;
        super.setPressed(pressed);
    }

    /**
     * Implement this method to handle touch screen motion events.
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDown = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    final int x = (int) event.getX();
                    final int y = (int) event.getY();

                    // Be lenient about moving outside of buttons
                    int slop = ViewConfiguration.get(mContext).getScaledTouchSlop();
                    if ((x < 0 - slop) || (x >= getWidth() + slop) ||
                            (y < 0 - slop) || (y >= getHeight() + slop)) {
                        mDown = false;
                    }
                    break;
            }
            return super.onTouchEvent(event);
    }

}
