package com.soundcloud.android.activity;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TabHost;
import android.widget.TabWidget;

public class Main extends TabActivity {

    protected boolean mIgnorePlaybackStatus = false;

    private TabHost mTabHost;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.cloudtabs);

        mTabHost = getTabHost();
        TabHost.TabSpec spec;

        spec = mTabHost.newTabSpec("incoming").setIndicator(
                getString(R.string.tab_incoming),
                getResources().getDrawable(R.drawable.ic_tab_incoming));

        spec.setContent(new Intent(this, Dashboard.class).putExtra("tab", "incoming"));
        mTabHost.addTab(spec);

        spec = mTabHost.newTabSpec("exclusive").setIndicator(
                getString(R.string.tab_exclusive),
                getResources().getDrawable(R.drawable.ic_tab_incoming));

        spec.setContent(new Intent(this, Dashboard.class).putExtra("tab", "exclusive"));
        mTabHost.addTab(spec);


        spec = mTabHost.newTabSpec("profile").setIndicator(
                getString(R.string.tab_you),
                getResources().getDrawable(R.drawable.ic_tab_you));
        spec.setContent(new Intent(this, UserBrowser.class));

        mTabHost.addTab(spec);

        spec = mTabHost.newTabSpec("record").setIndicator(
                getString(R.string.tab_record),
                getResources().getDrawable(R.drawable.ic_tab_record));

        spec.setContent(new Intent(this, ScCreate.class));

        mTabHost.addTab(spec);


        spec = mTabHost.newTabSpec("search").setIndicator(
                getString(R.string.tab_search),
                getResources().getDrawable(R.drawable.ic_tab_search));

        spec.setContent(new Intent(this, ScSearch.class));

        mTabHost.addTab(spec);



        mTabHost.setCurrentTab(PreferenceManager.getDefaultSharedPreferences(this).getInt(SoundCloudApplication.DASHBOARD_IDX,0));

        CloudUtils.setTabTextStyle(this, (TabWidget) findViewById(android.R.id.tabs));

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                PreferenceManager.getDefaultSharedPreferences(Main.this).edit()
                        .putInt(SoundCloudApplication.DASHBOARD_IDX, mTabHost.getCurrentTab())
                        .commit();
            }
        });
        handleIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent();
        super.onNewIntent(intent);
    }

    private void handleIntent() {
        if (getIntent() != null) {
            if (getIntent().hasExtra("tabIndex")) {
                mTabHost.setCurrentTab(getIntent().getIntExtra("tabIndex", 0));
                getIntent().removeExtra("tabIndex");
            } else if (getIntent().hasExtra("tabTag")) {
                mTabHost.setCurrentTabByTag(getIntent().getStringExtra("tabTag"));
                getIntent().removeExtra("tabTag");

            }
        }
    }

     @Override
    protected void onDestroy() {
        super.onDestroy();

    }


    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if (state.containsKey("tabTag")) {
            mTabHost.setCurrentTabByTag(state.getString("tabTag"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString("tabTag", mTabHost.getCurrentTabTag());
        super.onSaveInstanceState(state);
    }
}
