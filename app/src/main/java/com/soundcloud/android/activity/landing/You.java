package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.User;
import com.viewpagerindicator.TitlePageIndicator;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class You extends UserBrowser implements ScLandingPage {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        ((TitlePageIndicator) findViewById(R.id.indicator)).setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                getApp().setAccountData(User.DataKeys.PROFILE_IDX, i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    @Override
    protected boolean isYou() {
        return true;
    }
}
