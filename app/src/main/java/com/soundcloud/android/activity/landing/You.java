package com.soundcloud.android.activity.landing;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.User;
import com.viewpagerindicator.TitlePageIndicator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;

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
                        R.id.nav_you);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    @Override
    protected void handleIntent(Intent intent) {
        super.handleIntent(intent);
        final String action = intent.getAction();
        if (!TextUtils.isEmpty(action)) {
            Tab t = Tab.fromAction(action);
            if (t != null){
                mPager.setCurrentItem(Tab.indexOf(t.tag));
                intent.setAction(null);
            }
        }
    }

    @Override
    protected int getSelectedMenuId() {
        final Intent intent = getIntent();
        if (intent.hasExtra(Tab.EXTRA)){
            final Tab tab = Tab.values()[Tab.indexOf(intent.getStringExtra(Tab.EXTRA))];
            if (tab == Tab.likes) {
                return R.id.nav_likes;
            }
        }
        return R.id.nav_you;
    }

    @Override
    protected boolean isYou() {
        return true;
    }
}
