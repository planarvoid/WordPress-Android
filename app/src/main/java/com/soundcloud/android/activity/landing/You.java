package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.User;
import com.viewpagerindicator.TitlePageIndicator;

import android.content.Intent;
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
                mRootView.setSelectedMenuId(
                        Tab.values()[i] == Tab.likes ? R.id.nav_likes :
                        Tab.values()[i] == Tab.followings ? R.id.nav_followings :
                        R.id.nav_you);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    @Override
    protected int getSelectedMenuId() {
        final Intent intent = getIntent();
        if (intent.hasExtra(Tab.EXTRA)){
            final Tab tab = Tab.values()[Tab.indexOf(intent.getStringExtra(Tab.EXTRA))];
            if (tab == Tab.likes) {
                return R.id.nav_likes;
            } else if (tab == Tab.followings) {
                return R.id.nav_followings;
            }
        }
        return R.id.nav_you;
    }

    @Override
    protected boolean isYou() {
        return true;
    }
}
