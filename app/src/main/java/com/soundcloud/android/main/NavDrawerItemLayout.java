package com.soundcloud.android.main;

import com.soundcloud.android.R;
import com.soundcloud.android.view.CheckableImageView;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class NavDrawerItemLayout extends LinearLayout implements Checkable {

    private boolean checked = false;
    private CheckableImageView mIcon;

    public NavDrawerItemLayout(Context context) {
        super(context);
    }

    public NavDrawerItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIcon = (CheckableImageView) findViewById(R.id.nav_item_image);
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
        mIcon.setChecked(checked);
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }
}
