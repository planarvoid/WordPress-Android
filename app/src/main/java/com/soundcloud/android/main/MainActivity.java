package com.soundcloud.android.main;

import static rx.android.AndroidObservables.fromActivity;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.onboarding.auth.EmailConfirmationActivity;
import com.soundcloud.android.explore.ExploreFragment;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.accounts.UserOperations;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.rx.Event;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import net.hockeyapp.android.UpdateManager;
import rx.Observer;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;

public class MainActivity extends ScActivity implements NavigationFragment.NavigationCallbacks {

    public static final String EXTRA_ONBOARDING_USERS_RESULT = "onboarding_users_result";
    private static final String EXTRA_ACTIONBAR_TITLE = "actionbar_title";

    private NavigationFragment mNavigationFragment;
    private CharSequence mLastTitle;
    private int mLastSelection = -1;

    private AccountOperations mAccountOperations;
    private CompositeSubscription mSubscription = new CompositeSubscription();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mNavigationFragment = (NavigationFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_fragment);

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

        mSubscription.add(Event.CURRENT_USER_UPDATED.subscribe(userObserver));
    }

    private void handleLoggedInUser(ApplicationProperties appProperties, Observer<User> observer) {
        boolean justAuthenticated = getIntent() != null && getIntent().hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
        User currentUser = SoundCloudApplication.instance.getLoggedInUser();
        if (!justAuthenticated && mAccountOperations.shouldCheckForConfirmedEmailAddress(currentUser)) {
            UserOperations userOperations = new UserOperations();
            mSubscription.add(fromActivity(this, userOperations.refreshCurrentUser()).subscribe(observer));
        }

        if (appProperties.isBetaBuildRunningOnDalvik()) {
            UpdateManager.register(this, getString(R.string.hockey_app_id));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mNavigationFragment.handleIntent(intent);
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
        mSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putCharSequence(EXTRA_ACTIONBAR_TITLE, mLastTitle);
    }

    @Override
    public void onNavigationItemSelected(int position, boolean setTitle) {
        if (position == mLastSelection) return;
        switch (NavigationFragment.NavItem.values()[position]) {
            case PROFILE:
                // Hi developer! If you're removing this line to replace the user profile activity with a fragment,
                // don't forget to search for the TODOs related to this in NavigationFragment.
                // --Your friend.
                getSupportActionBar().setDisplayShowTitleEnabled(false); // prevents title text change flashing
                startActivity(new Intent(this, MeActivity.class));
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

        if (setTitle){
            /**
             * In this case, restoreActionBar will not be called since it is already closed.
             * This probably came from {@link NavigationFragment#handleIntent(android.content.Intent)}
             */
            getSupportActionBar().setTitle(mLastTitle);
        }
        if (position != NavigationFragment.NavItem.PROFILE.ordinal()) {
            mLastSelection = position;
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
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mLastTitle);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Keep null check. This might fire as a result of setContentView in which case this var won't be assigned
        if (mNavigationFragment != null) {
            return super.onCreateOptionsMenu(menu);
        }
        return true;
    }

    private class UpdateUserProfileObserver extends DefaultObserver<User> {

        @Override
        public void onNext(User user) {
            mNavigationFragment.updateProfileItem(user, SoundCloudApplication.instance.getResources());
            if (!user.isPrimaryEmailConfirmed()) {
                startActivityForResult(new Intent(MainActivity.this, EmailConfirmationActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
            }
        }
    }
}
