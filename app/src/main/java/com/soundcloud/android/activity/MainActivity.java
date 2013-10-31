package com.soundcloud.android.activity;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.auth.EmailConfirm;
import com.soundcloud.android.activity.landing.You;
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
import android.support.v7.app.ActionBar;
import android.view.Menu;

public class MainActivity extends ScActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static final String EXTRA_ONBOARDING_USERS_RESULT = "onboarding_users_result";

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mLastTitle;

    private FetchUserTask mFetchUserTask;
    private AccountOperations mAccountOperations;

    private AndroidCloudAPI oldCloudAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mLastTitle = getTitle();
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // this must come after setting up the navigation drawer to configure the action bar properly
        supportInvalidateOptionsMenu();

        oldCloudAPI = new OldCloudAPI(this);
        mAccountOperations = new AccountOperations(this);
        ApplicationProperties mApplicationProperties = new ApplicationProperties(getResources());
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mNavigationDrawerFragment.handleIntent(intent);
        // the title/selection may have changed as a result of this intent, so store the new title to prevent overwriting
        mLastTitle = getTitle();
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
    public void onNavigationDrawerItemSelected(int position) {
        switch (NavigationDrawerFragment.NavItem.values()[position]) {
            case PROFILE:
                startActivity(new Intent(this, You.class));
                break;

            case STREAM:
                final Uri contentUri = getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true) ?
                        Content.ME_SOUND_STREAM.uri :
                        Content.ME_SOUND_STREAM.uri.buildUpon()
                                .appendQueryParameter(Consts.Keys.ONBOARDING, Consts.StringValues.ERROR).build();
                Fragment fragment = ScListFragment.newInstance(contentUri, R.string.side_menu_stream);
                attachFragment(fragment, "stream_fragment");
                break;

            case EXPLORE:
                fragment = getSupportFragmentManager().findFragmentByTag("explore_fragment");
                if (fragment == null) {
                    fragment = new ExploreFragment();
                }
                attachFragment(fragment, "explore_fragment");
                break;

            case LIKES:
                fragment = ScListFragment.newInstance(Content.ME_LIKES.uri, R.string.side_menu_likes);
                attachFragment(fragment, "likes_fragment");
                break;

            case PLAYLISTS:
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
        if (resourceId > 0) {
            mLastTitle = getString(resourceId);
        }
    }

    public boolean restoreActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setLogo(R.drawable.actionbar_logo);
        actionBar.setTitle(mLastTitle);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Keep null check. This might fire as a result of setContentView in which case this var won't be assigned
        if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
            return super.onCreateOptionsMenu(menu);
        }
        return true;
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
                        // FIXME: DB access on UI thread :(
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
