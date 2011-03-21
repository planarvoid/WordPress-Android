package com.soundcloud.android.activity;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.app.SearchManager;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TabHost;
import android.widget.TabWidget;

public class Main extends TabActivity {
    private TabHost mTabHost;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);


        setContentView(R.layout.cloudtabs);

        mTabHost = buildTabHost();
        mTabHost.setCurrentTab(PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(SoundCloudApplication.Prefs.DASHBOARD_IDX, 0));

        CloudUtils.setTabTextStyle(this, (TabWidget) findViewById(android.R.id.tabs));

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                PreferenceManager.getDefaultSharedPreferences(Main.this).edit()
                        .putInt(SoundCloudApplication.Prefs.DASHBOARD_IDX, mTabHost.getCurrentTab())
                        .commit();
            }
        });

        handleIntent(getIntent());
    }


    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
        super.onNewIntent(intent);
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

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                mTabHost.setCurrentTabByTag("search");
                ((ScSearch) getCurrentActivity()).doSearch(intent.getStringExtra(SearchManager.QUERY));
            } else if (intent.hasExtra("tabIndex")) {
                mTabHost.setCurrentTab(intent.getIntExtra("tabIndex", 0));
                intent.removeExtra("tabIndex");
            } else if (intent.hasExtra("tabTag")) {
                mTabHost.setCurrentTabByTag(intent.getStringExtra("tabTag"));
                intent.removeExtra("tabTag");
            }
        }
    }

    private TabHost buildTabHost() {
        TabHost host = getTabHost();
        TabHost.TabSpec spec;

        spec = host.newTabSpec("incoming").setIndicator(
                getString(R.string.tab_incoming),
                getResources().getDrawable(R.drawable.ic_tab_incoming));
        spec.setContent(new Intent(this, Dashboard.class).putExtra("tab", "incoming"));
        host.addTab(spec);

        spec = host.newTabSpec("exclusive").setIndicator(
                getString(R.string.tab_exclusive),
                getResources().getDrawable(R.drawable.ic_tab_incoming));
        spec.setContent(new Intent(this, Dashboard.class).putExtra("tab", "exclusive"));
        host.addTab(spec);

        spec = host.newTabSpec("profile").setIndicator(
                getString(R.string.tab_you),
                getResources().getDrawable(R.drawable.ic_tab_you));
        spec.setContent(new Intent(this, UserBrowser.class));
        host.addTab(spec);

        spec = host.newTabSpec("record").setIndicator(
                getString(R.string.tab_record),
                getResources().getDrawable(R.drawable.ic_tab_record));
        spec.setContent(new Intent(this, ScCreate.class));
        host.addTab(spec);

        spec = host.newTabSpec("search").setIndicator(
                getString(R.string.tab_search),
                getResources().getDrawable(R.drawable.ic_tab_search));
        spec.setContent(new Intent(this, ScSearch.class));
        host.addTab(spec);

        return host;
    }
}
