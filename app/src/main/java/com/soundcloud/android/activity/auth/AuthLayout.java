package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
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

    abstract AuthHandler getAuthHandler();

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        findViewById(R.id.google_plus_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { getAuthHandler().onGooglePlusAuth();
            }
        });

        findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAuthHandler().onFacebookAuth();
            }
        });
    }
}
