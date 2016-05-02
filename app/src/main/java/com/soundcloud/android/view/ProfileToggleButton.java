package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.StringRes;
import android.util.AttributeSet;

public class ProfileToggleButton extends CustomFontToggleButton {

    private int textOn;
    private int textOff;
    private final int rightTextPadding;
    private final int baseTextPadding;

    public ProfileToggleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProfileToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProfileToggleButton, defStyle, 0);
        this.textOn = a.getResourceId(R.styleable.ProfileToggleButton_textOn, Consts.NOT_SET);
        this.textOff = a.getResourceId(R.styleable.ProfileToggleButton_textOff, Consts.NOT_SET);
        this.rightTextPadding = a.getDimensionPixelSize(R.styleable.ProfileToggleButton_textPaddingRight, 0);
        this.baseTextPadding = getPaddingRight();
        a.recycle();

        setChecked(isChecked());
    }

    @Override
    public void setChecked(boolean checked) {
        updateButtonText(checked);
        super.setChecked(checked);
    }

    private void updateButtonText(boolean checked) {
        // Do not change this to != Consts.NOT_SET as it will default to 0x0 and crash in runtime
        // because setChecked is called as part of the super constructor call
        if (checked && textOn > 0) {
            updateText(textOn);
        } else if (!checked && textOff > 0) {
            updateText(textOff);
        } else {
            resetText();
        }
    }

    public void setTextOn(int textOn) {
        this.textOn = textOn;
    }

    public void setTextOff(int textOff) {
        this.textOff = textOff;
    }

    private void updateText(@StringRes Integer buttonText) {
        setText(buttonText);
        setPadding(getPaddingLeft(), getPaddingTop(), baseTextPadding + rightTextPadding, getPaddingBottom());
    }

    private void resetText() {
        setText(null);
        setPadding(getPaddingLeft(), getPaddingTop(), baseTextPadding, getPaddingBottom());
    }
}
