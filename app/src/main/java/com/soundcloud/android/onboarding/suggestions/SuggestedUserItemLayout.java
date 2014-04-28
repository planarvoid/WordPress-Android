package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

public class SuggestedUserItemLayout extends LinearLayout implements Checkable {
    private boolean checked;
    private CompoundButton followButton;
    private View suggestedUserLayout;

    public SuggestedUserItemLayout(Context context) {
        super(context);
    }

    public SuggestedUserItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        followButton = ((CompoundButton) findViewById(R.id.toggle_btn_follow));
        suggestedUserLayout = findViewById(R.id.suggested_user_selector);
    }

    @Override
    public void setChecked(boolean checked) {
        followButton.setChecked(checked);
        if (checked){
            suggestedUserLayout.setBackgroundResource(R.drawable.suggested_user_grid_item_checked_selector);
        } else {
            suggestedUserLayout.setBackgroundResource(R.drawable.suggested_user_grid_item_selector);
        }
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void toggle() {
        checked = !checked;
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        suggestedUserLayout.setPressed(pressed);
    }
}
