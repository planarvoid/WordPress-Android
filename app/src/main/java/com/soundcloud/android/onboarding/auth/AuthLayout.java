package com.soundcloud.android.onboarding.auth;

import butterknife.OnClick;
import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public abstract class AuthLayout extends RelativeLayout {

    public interface AuthHandler {
        void onGooglePlusAuth();

        void onFacebookAuth();
    }

    public AuthLayout(Context context) {
        super(context);
    }

    public AuthLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AuthLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected abstract AuthHandler getAuthHandler();

    @OnClick(R.id.google_plus_btn)
    public void onGooglePlusClick() {
        getAuthHandler().onGooglePlusAuth();
    }

    @OnClick(R.id.facebook_btn)
    public void onFacebookClick() {
        getAuthHandler().onFacebookAuth();
    }

    public void setGooglePlusVisibility(boolean visible) {
        findViewById(R.id.google_plus_btn).setVisibility(visible ? VISIBLE : GONE);
    }
}
