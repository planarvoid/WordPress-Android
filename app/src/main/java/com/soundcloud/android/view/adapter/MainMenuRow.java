package com.soundcloud.android.view.adapter;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class MainMenuRow extends LinearLayout implements Checkable {

    boolean mChecked;

    public MainMenuRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MainMenuRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mChecked){
            setBackgroundResource(R.drawable.sidebar_item_background_selected);
        } else {
            setBackgroundColor(Color.TRANSPARENT);
        }

        int paddingTopBottom = (int) getResources().getDimension(R.dimen.slm_item_padding_topbottom);
        int paddingLeft = (int) getResources().getDimension(R.dimen.slm_item_padding_left);
        int paddingRight = (int) getResources().getDimension(R.dimen.slm_item_padding_right);
        setPadding(paddingLeft, paddingTopBottom, paddingRight, paddingTopBottom);
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }
}
