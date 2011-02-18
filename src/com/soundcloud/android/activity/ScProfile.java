
package com.soundcloud.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import android.widget.TabHost;
import com.soundcloud.android.R;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.android.view.UserBrowser;

public class ScProfile extends ScActivity {

    private UserBrowser userBrowser;
    private TabHost mTabHost;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main_holder);
        build();
        userBrowser.initLoadTasks();
    }

    @Override
    protected void onResume() {
        tracker.trackPageView("/profile");
        tracker.dispatch();

        super.onResume();
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (mTabHost!=null) {
            ((ScTabView)mTabHost.getCurrentView()).onStart();
        }
    }

    protected void build() {
        LinearLayout mMainHolder = ((LinearLayout) findViewById(R.id.main_holder));
        userBrowser = new UserBrowser(this);
        mMainHolder.addView(userBrowser);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {

            if (extras.getParcelable("user") != null) {
                userBrowser.loadUserByObject((User) extras.getParcelable("user"));
                extras.remove("user");
            }
            if (extras.containsKey("userId")) {
                userBrowser.loadUserById(extras.getLong("userId"));
                extras.remove("serId");
            }

        } else {
            userBrowser.loadYou();
        }
    }

    public void setTabHost(TabHost tabHost) {
        mTabHost = tabHost;
    }
}
