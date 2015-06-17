package com.soundcloud.android.creators.upload;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

/**
 * Originally from https://gist.github.com/dmide/7506c7d9614eed90805d
 */

public class CircularProgressBar extends View {
    private CircularProgressDrawable mDrawable;

    public CircularProgressBar(Context context) {
        this(context, null);
    }

    public CircularProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mDrawable = new CircularProgressDrawable(12,
                getResources().getColor(R.color.ak_sc_orange));
        mDrawable.setCallback(this);
        if (getVisibility() == VISIBLE) {
            mDrawable.start();
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        if (indeterminate) {
            mDrawable.start();
        } else {
            mDrawable.stop();
        }
    }

    public void setProgress(int percentage) {
        mDrawable.setProgress(percentage);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        mDrawable.draw(canvas);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (mDrawable != null) {
            if (visibility == VISIBLE) {
                mDrawable.start();
            } else {
                mDrawable.stop();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mDrawable.setBounds(0, 0, w, h);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mDrawable || super.verifyDrawable(who);
    }
}