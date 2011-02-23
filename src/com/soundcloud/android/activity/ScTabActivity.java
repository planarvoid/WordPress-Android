package com.soundcloud.android.activity;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabWidget;

public class ScTabActivity extends TabActivity {

    protected boolean mIgnorePlaybackStatus = false;

    private MenuItem menuCurrentPlayingItem;

    private MenuItem menuCurrentUploadingItem;

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




        mTabHost.setCurrentTab(0);

        CloudUtils.setTabTextStyle(this, (TabWidget) findViewById(android.R.id.tabs));

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                PreferenceManager.getDefaultSharedPreferences(ScTabActivity.this).edit()
                        .putInt("lastDashboardIndex", mTabHost.getCurrentTab())
                        .commit();
            }
        });


//        tabHost.setCurrentTab(PreferenceManager.getDefaultSharedPreferences(Dashboard.this)
//                .getInt("lastDashboardIndex", 0));


        handleIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent();
        super.onNewIntent(intent);
    }

    private void handleIntent() {
        if (getIntent() != null && getIntent().getExtras() != null
                && getIntent().getIntExtra("tabIndex", -1) != -1) {
            mTabHost.setCurrentTab(getIntent().getIntExtra("tabIndex", 0));
            getIntent().getExtras().clear();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menuCurrentPlayingItem = menu.add(menu.size(), CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK,
                menu.size(), R.string.menu_view_current_track).setIcon(
                R.drawable.ic_menu_info_details);
        menuCurrentUploadingItem = menu.add(menu.size(),
                CloudUtils.OptionsMenu.CANCEL_CURRENT_UPLOAD, menu.size(),
                R.string.menu_cancel_current_upload).setIcon(R.drawable.ic_menu_delete);

        menu.add(menu.size(), CloudUtils.OptionsMenu.SETTINGS, menu.size(), R.string.menu_settings)
                .setIcon(R.drawable.ic_menu_preferences);

        menu.add(menu.size(), CloudUtils.OptionsMenu.REFRESH, 0, R.string.menu_refresh).setIcon(
                R.drawable.context_refresh);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Prepare the options menu based on the current class and current play
     * state
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            /* if (mCreateService.isUploading()) { XXX */
            if (false) {
                menuCurrentUploadingItem.setVisible(true);
            } else {
                menuCurrentUploadingItem.setVisible(false);
            }
        } catch (Exception e) {
            menuCurrentUploadingItem.setVisible(false);
        }

        return true;
    }

@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CloudUtils.OptionsMenu.REFRESH:

                ((ScActivity)getCurrentActivity()).onRefresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

     @Override
    protected void onDestroy() {
        super.onDestroy();

    }


    @Override
    protected void onRestoreInstanceState(Bundle state) {

//        if (setTabIndex == -1) {
//            String setTabIndexString = state.getString("currentTabIndex");
//            if (!TextUtils.isEmpty(setTabIndexString)) {
//                setTabIndex = Integer.parseInt(setTabIndexString);
//            } else
//                setTabIndex = 0;
//        }
//        if (tabHost != null)
//            tabHost.setCurrentTab(setTabIndex);

        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
//        if (tabHost != null) {
//                 state.putString("currentTabIndex", Integer.toString(tabHost.getCurrentTab()));
//             }

    }
}
