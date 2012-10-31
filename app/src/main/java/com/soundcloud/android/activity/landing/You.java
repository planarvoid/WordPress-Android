package com.soundcloud.android.activity.landing;

import com.soundcloud.android.activity.UserBrowser;

public class You extends UserBrowser implements ScLandingPage {
    @Override
    protected boolean isYou() {
        return true;
    }

    @Override
    public LandingPage getPageValue() {
        return LandingPage.You;
    }
}
