package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SuggestedUserItemLayout extends FrameLayout implements Checkable {
    private boolean mChecked;
    private int mPadding;
    private ImageView mImageView;
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
        mPadding = (int) getResources().getDimension(R.dimen.onboarding_suggested_user_avatar_padding);
        mImageView = (ImageView) findViewById(R.id.suggested_user_image);
        mFollowButton = ((CompoundButton) findViewById(R.id.toggle_btn_follow));
        mImageView.setPadding(mPadding, mPadding, mPadding, mPadding);
    }

    @Override
    public void setChecked(boolean checked) {
        mFollowButton.setChecked(checked);
        if (checked){
            mImageView.setBackgroundResource(R.drawable.orange_outline);
        } else {
            mImageView.setBackground(null);
        }
        mImageView.setPadding(mPadding, mPadding, mPadding, mPadding);
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        mChecked = !mChecked;
    }
}
