package com.soundcloud.android.view.adapter;

import com.soundcloud.android.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class MainMenuRow extends LinearLayout implements Checkable {
    private boolean mChecked;

    @SuppressWarnings("UnusedDeclaration")
    public MainMenuRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(11) @SuppressWarnings("UnusedDeclaration")
    public MainMenuRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mChecked){
            setBackgroundResource(R.drawable.sidebar_item_background_selected);
        } else {
            setBackgroundResource(R.drawable.sidebar_item_background);
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
