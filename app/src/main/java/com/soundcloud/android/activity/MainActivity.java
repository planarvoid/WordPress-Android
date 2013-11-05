package com.soundcloud.android.activity;

import static rx.android.AndroidObservables.fromActivity;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.auth.EmailConfirm;
import com.soundcloud.android.activity.landing.You;
import com.soundcloud.android.fragment.ExploreFragment;
import com.soundcloud.android.fragment.NavigationDrawerFragment;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.operations.UserOperations;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.service.auth.AuthenticatorService;
import net.hockeyapp.android.UpdateManager;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ScActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static final String EXTRA_ONBOARDING_USERS_RESULT = "onboarding_users_result";
    private static final String EXTRA_ACTIONBAR_TITLE = "actionbar_title";

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mLastTitle;

    private AccountOperations mAccountOperations;
    private Subscription mUpdateUserSubscription = Subscriptions.empty();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mAccountOperations = new AccountOperations(this);
        ApplicationProperties mApplicationProperties = new ApplicationProperties(getResources());

        final Observer<User> userObserver = new UpdateUserProfileObserver();
        if (savedInstanceState != null) {
            mLastTitle = savedInstanceState.getCharSequence(EXTRA_ACTIONBAR_TITLE);
        } else {
            mLastTitle = getTitle();

            if (mAccountOperations.soundCloudAccountExists()) {
                handleLoggedInUser(mApplicationProperties, userObserver);
            }
        }

        // this must come after setting up the navigation drawer to configure the action bar properly
        supportInvalidateOptionsMenu();
    }

    private void handleLoggedInUser(ApplicationProperties appProperties, Observer<User> observer) {
        boolean justAuthenticated = getIntent() != null && getIntent().hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
        User currentUser = SoundCloudApplication.instance.getLoggedInUser();
        if (!justAuthenticated && mAccountOperations.shouldCheckForConfirmedEmailAddress(currentUser)) {
            UserOperations userOperations = new UserOperations();
            mUpdateUserSubscription = fromActivity(this, userOperations.refreshCurrentUser()).subscribe(observer);
        }

        if (appProperties.isBetaBuildRunningOnDalvik()) {
            UpdateManager.register(this, getString(R.string.hockey_app_id));
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
        mUpdateUserSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putCharSequence(EXTRA_ACTIONBAR_TITLE, mLastTitle);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        switch (NavigationDrawerFragment.NavItem.values()[position]) {
            case PROFILE:
                // Hi developer! If you're removing this line to replace the user profile activity with a fragment,
                // don't forget to search for the TODOs related to this in NavigationDrawerFragment.
                // --Your friend.
                getSupportActionBar().setDisplayShowTitleEnabled(false); // prevents title text change flashing
                startActivity(new Intent(this, You.class));
                break;

            case STREAM:
                final Uri contentUri = getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true) ?
                        Content.ME_SOUND_STREAM.uri :
                        Content.ME_SOUND_STREAM.uri.buildUpon()
                                .appendQueryParameter(Consts.Keys.ONBOARDING, Consts.StringValues.ERROR).build();
                Fragment fragment = ScListFragment.newInstance(contentUri, R.string.side_menu_stream);
                attachFragment(fragment, "stream_fragment", R.string.side_menu_stream);
                break;

            case EXPLORE:
                fragment = getSupportFragmentManager().findFragmentByTag("explore_fragment");
                if (fragment == null) {
                    fragment = new ExploreFragment();
                }
                attachFragment(fragment, "explore_fragment", R.string.side_menu_explore);
                break;

            case LIKES:
                fragment = ScListFragment.newInstance(Content.ME_LIKES.uri, R.string.side_menu_likes);
                attachFragment(fragment, "likes_fragment", R.string.side_menu_likes);
                break;

            case PLAYLISTS:
                fragment = ScListFragment.newInstance(Content.ME_PLAYLISTS.uri, R.string.side_menu_playlists);
                attachFragment(fragment, "playlists_fragment", R.string.side_menu_playlists);
                break;
        }

    }

    private void attachFragment(Fragment fragment, String tag, int titleResId) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment, tag)
                .commit();
        mLastTitle = getString(titleResId);
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
        if (mNavigationDrawerFragment != null) {
            return super.onCreateOptionsMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mNavigationDrawerFragment.closeDrawer();
        return super.onOptionsItemSelected(item);
    }

    private class UpdateUserProfileObserver extends DefaultObserver<User> {

        @Override
        public void onNext(User user) {
            mNavigationDrawerFragment.updateProfileItem(user);
            if (!user.isPrimaryEmailConfirmed()) {
                startActivityForResult(new Intent(MainActivity.this, EmailConfirm.class)
                        .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
            }
        }
    }
}
