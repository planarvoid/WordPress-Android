package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;

public class You extends UserBrowser implements ScLandingPage {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                switch (Tab.values()[i]){
                    case likes : mRootView.setSelectedMenuId(R.id.nav_likes);   break;
                    case sets  : mRootView.setSelectedMenuId(R.id.nav_sets);    break;
                    default    : mRootView.setSelectedMenuId(R.id.nav_you);     break;
                }
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
            } else if (tab == Tab.sets) {
                return R.id.nav_sets;
            }
        }
        return R.id.nav_you;
    }

    @Override
    protected boolean isYou() {
        return true;
    }
}
