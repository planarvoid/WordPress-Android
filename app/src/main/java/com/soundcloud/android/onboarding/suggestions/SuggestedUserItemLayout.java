package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

public class SuggestedUserItemLayout extends LinearLayout implements Checkable {
    private boolean mChecked;
    private CompoundButton mFollowButton;
    private View mSuggestedUserLayout;

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
        mSuggestedUserLayout = findViewById(R.id.suggested_user_selector);
    }

    @Override
    public void setChecked(boolean checked) {
        mFollowButton.setChecked(checked);
        if (checked){
            mSuggestedUserLayout.setBackgroundResource(R.drawable.suggested_user_grid_item_checked_selector);
        } else {
            mSuggestedUserLayout.setBackgroundResource(R.drawable.suggested_user_grid_item_selector);
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
        mSuggestedUserLayout.setPressed(pressed);
    }
}
