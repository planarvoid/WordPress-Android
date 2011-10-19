package com.soundcloud.android.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.util.StateSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.Button;
import com.soundcloud.android.R;

public class ColoredButton extends Button {

    private Context mContext;
    private boolean mDown = false;

    private int mIdleMult;
    private int mIdleText;

    private ColorStateList mDefaultTextColors;


    public ColoredButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(attrs);
        mContext = context;
        refreshDrawableState();

    }

    public ColoredButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(attrs);
        mContext = context;
        refreshDrawableState();
    }

    private void init(AttributeSet attrs){
        TypedArray a=getContext().obtainStyledAttributes(attrs,R.styleable.ColoredButton);
        mIdleMult = a.getInt(R.styleable.ColoredButton_idleMult, 0xFF494949);
        mIdleText = a.getColor(R.styleable.ColoredButton_idleText, 0xFFFFFFFF);
        a.recycle();

        mDefaultTextColors = getTextColors();
    }


    @Override
    protected void drawableStateChanged() {
        int[] states = getDrawableState();
        if (StateSet.stateSetMatches(new int[]{android.R.attr.state_pressed}, states) || StateSet.stateSetMatches(new int[] { android.R.attr.state_focused }, states)) {
            getBackground().setColorFilter(null);
            setTextColor(mDefaultTextColors);

        } else {
            getBackground().setColorFilter(mIdleMult, PorterDuff.Mode.MULTIPLY);
            setTextColor(mIdleText);

        }

        super.drawableStateChanged();
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
