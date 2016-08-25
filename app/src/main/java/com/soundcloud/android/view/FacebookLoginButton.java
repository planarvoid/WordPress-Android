package com.soundcloud.android.view;

import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import com.facebook.login.widget.LoginButton;

import android.content.Context;
import android.util.AttributeSet;

public class FacebookLoginButton extends LoginButton {

    public FacebookLoginButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        disableDefaultClickHandler();
        applyCustomFont(context, this, attrs);
    }

    public FacebookLoginButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        disableDefaultClickHandler();
        applyCustomFont(context, this, attrs);
    }

    private void disableDefaultClickHandler() {
        setInternalOnClickListener(null);
    }
}
