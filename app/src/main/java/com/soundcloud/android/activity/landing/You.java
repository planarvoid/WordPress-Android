package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

public class You extends UserBrowser {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
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
                return R.id.nav_playlists;
            }
        }
        return R.id.nav_you;
    }

    @Override
    protected boolean isYou() {
        return true;
    }
}
