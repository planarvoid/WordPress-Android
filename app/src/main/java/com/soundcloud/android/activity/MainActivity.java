package com.soundcloud.android.activity;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.auth.EmailConfirm;
import com.soundcloud.android.activity.landing.ScSearch;
import com.soundcloud.android.activity.landing.You;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.api.OldCloudAPI;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.fragment.ExploreFragment;
import com.soundcloud.android.fragment.NavigationDrawerFragment;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.auth.AuthenticatorService;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import net.hockeyapp.android.UpdateManager;

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

    private FetchUserTask mFetchUserTask;
    private AccountOperations mAccountOperations;

    private AndroidCloudAPI oldCloudAPI;
    private ApplicationProperties mApplicationProperties;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLastTitle = getTitle();

        getSupportActionBar().setLogo(R.drawable.ic_launcher);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));


        oldCloudAPI = new OldCloudAPI(this);
        mAccountOperations = new AccountOperations(this);
        mApplicationProperties = new ApplicationProperties(getResources());
        if (mAccountOperations.soundCloudAccountExists()) {

            if (IOUtils.isConnected(this) &&
                    mAccountOperations.soundCloudAccountExists() &&
                    mAccountOperations.getSoundCloudToken().valid() &&
                    !getApp().getLoggedInUser().isPrimaryEmailConfirmed() &&
                    !justAuthenticated(getIntent())) {
                checkEmailConfirmed();
            }

            if (mApplicationProperties.isBetaBuildRunningOnDalvik()) {
                UpdateManager.register(this, getString(R.string.hockey_app_id));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mAccountOperations.soundCloudAccountExists()) {
            mAccountOperations.addSoundCloudAccountManually(this);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        UpdateManager.unregister();
        super.onDestroy();
    }

    @Override
    protected int getSelectedMenuId() {
        return 0;
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        Fragment fragment = null;
        String fragmentTag = null;
        switch(position){
            case 0:
                startActivity(new Intent(this, You.class));
                break;

            case 1:
                final Uri contentUri = getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true) ?
                        Content.ME_SOUND_STREAM.uri :
                        Content.ME_SOUND_STREAM.uri.buildUpon()
                                .appendQueryParameter(Consts.Keys.ONBOARDING, Consts.StringValues.ERROR).build();
                fragment = ScListFragment.newInstance(contentUri, R.string.side_menu_stream);
                attachFragment(fragment, "stream_fragment");
                break;

            case 2:
                fragment = getSupportFragmentManager().findFragmentByTag("explore_fragment");
                if (fragment ==  null){
                    fragment = new ExploreFragment();
                }
                attachFragment(fragment, "explore_fragment");
                break;

            case 3:
                fragment = ScListFragment.newInstance(Content.ME_LIKES.uri, R.string.side_menu_likes);
                attachFragment(fragment, "likes_fragment");
                break;

            case 4:
                fragment = ScListFragment.newInstance(Content.ME_PLAYLISTS.uri, R.string.side_menu_playlists);
                attachFragment(fragment, "playlists_fragment");
                break;
        }

    }

    private void attachFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment, tag)
                .commit();
    }

    public void onSectionAttached(int resourceId) {
        if (resourceId > 0){
            mLastTitle = getString(resourceId);
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mLastTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Keep null check. This might fire as a result of setContentView in which case this var won't be assigned
        if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
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

    private boolean justAuthenticated(Intent intent) {
        return intent != null && intent.hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
    }

    // TODO, move this to a UserOperations class
    private void checkEmailConfirmed() {
        mFetchUserTask = new FetchUserTask(oldCloudAPI) {
            @Override
            protected void onPostExecute(User user) {
                if (user == null || user.isPrimaryEmailConfirmed()) {
                    if (user != null) {
                        new UserStorage().createOrUpdate(user);
                        mNavigationDrawerFragment.updateProfileItem(user);
                    }
                } else {
                    startActivityForResult(new Intent(MainActivity.this, EmailConfirm.class)
                            .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
                }
                mFetchUserTask = null;
            }
        };
        mFetchUserTask.execute(Request.to(Endpoints.MY_DETAILS));
    }
}
