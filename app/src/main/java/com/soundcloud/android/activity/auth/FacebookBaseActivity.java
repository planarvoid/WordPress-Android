package com.soundcloud.android.activity.auth;

public abstract class FacebookBaseActivity extends AbstractLoginActivity{

    /* package */ static final String VIA_SIGNUP_SCREEN = "via_signup_screen";

    @Override
    protected boolean wasAuthorizedViaSignupScreen() {
        return getIntent().getBooleanExtra(VIA_SIGNUP_SCREEN, false);
    }
}
