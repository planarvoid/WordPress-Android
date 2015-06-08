package com.soundcloud.android.profile;

import android.content.Intent;
import android.text.TextUtils;

public class MeActivity extends LegacyProfileActivity {

    @Override
    protected void handleIntent(Intent intent) {
        super.handleIntent(intent);
        final String action = intent.getAction();
        if (!TextUtils.isEmpty(action)) {
            Tab t = Tab.fromAction(action);
            if (t != null){
                pager.setCurrentItem(Tab.indexOf(t.tag));
                intent.setAction(null);
            }
        }
    }

    @Override
    protected boolean isLoggedInUser() {
        return true;
    }
}
