package com.soundcloud.android.activity;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.ScSearch;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.fragment.ExploreTracksCategoriesFragment;
import com.soundcloud.android.fragment.NavigationDrawerFragment;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ScActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mLastTitle;

    public static final String EXTRA_ONBOARDING_USERS_RESULT  = "onboarding_users_result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLastTitle = getTitle();

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected int getSelectedMenuId() {
        return 0;
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        Fragment fragment = null;
        switch(position){
            case 0:
                fragment = ScListFragment.newInstance(Content.ME_SOUNDS.uri, R.string.side_menu_you);
                break;

            case 1:
                final Uri contentUri = getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true) ?
                        Content.ME_SOUND_STREAM.uri :
                        Content.ME_SOUND_STREAM.uri.buildUpon()
                                .appendQueryParameter(Consts.Keys.ONBOARDING, Consts.StringValues.ERROR).build();
                fragment = ScListFragment.newInstance(contentUri, R.string.side_menu_stream);
                break;

            case 2:
                fragment = new ExploreTracksCategoriesFragment();
                break;

            case 3:
                fragment = ScListFragment.newInstance(Content.ME_LIKES.uri, R.string.side_menu_likes);
                break;

            case 4:
                fragment = ScListFragment.newInstance(Content.ME_PLAYLISTS.uri, R.string.side_menu_playlists);
                break;
        }

        if (fragment != null){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }

    }

    public void onSectionAttached(int resourceId) {
        mLastTitle = getString(resourceId);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mLastTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_enter_search:
                startActivity(new Intent(this, ScSearch.class));
                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
