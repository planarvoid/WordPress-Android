package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

public class SuggestedUserItemLayout extends FrameLayout implements Checkable {
    private boolean mChecked;
    private CompoundButton mFollowButton;

    public SuggestedUserItemLayout(Context context) {
        super(context);
    }

    public SuggestedUserItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFollowButton = ((CompoundButton) findViewById(R.id.toggle_btn_follow));
        mFollowButton.setClickable(false);
        mFollowButton.setDuplicateParentStateEnabled(false);
    }

    @Override
    public void setChecked(boolean checked) {
        mFollowButton.setChecked(checked);
        if (checked){
            setBackgroundResource(R.drawable.suggested_user_grid_item_checked_selector);
        } else {
            setBackgroundResource(R.drawable.suggested_user_grid_item_selector);
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        mChecked = !mChecked;
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // nop
    }
}
