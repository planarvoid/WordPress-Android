package com.soundcloud.android.main;

import static rx.android.AndroidObservables.fromActivity;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.UserOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.dagger.DependencyInjector;
import com.soundcloud.android.dagger.ObjectGraphProvider;
import com.soundcloud.android.explore.ExploreFragment;
import com.soundcloud.android.explore.ExploreModule;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.onboarding.auth.EmailConfirmationActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.rx.RxModule;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.storage.provider.Content;
import dagger.ObjectGraph;
import net.hockeyapp.android.UpdateManager;
import rx.Observer;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;

import javax.inject.Inject;
import javax.inject.Provider;

public class MainActivity extends ScActivity implements NavigationFragment.NavigationCallbacks,
        ObjectGraphProvider {

    public static final String EXTRA_ONBOARDING_USERS_RESULT = "onboarding_users_result";

    private static final String EXTRA_ACTIONBAR_TITLE = "actionbar_title";
    private static final String PLAYLISTS_FRAGMENT_TAG = "playlists_fragment";
    private static final String LIKES_FRAGMENT_TAG = "likes_fragment";
    private static final String EXPLORE_FRAGMENT_TAG = "explore_fragment";
    private static final String STREAM_FRAGMENT_TAG = "stream_fragment";
    private static final int NO_SELECTION = -1;

    private NavigationFragment mNavigationFragment;
    private CharSequence mLastTitle;
    private int mLastSelection = NO_SELECTION;

    @Inject
    CompositeSubscription mSubscription;
    @Inject
    ApplicationProperties mApplicationProperties;
    @Inject
    SoundCloudApplication application;
    @Inject
    UserOperations mUserOperations;
    @Inject
    Provider<ExploreFragment> mExploreFragmentProvider;

    private final DependencyInjector mDependencyInjector;
    private ObjectGraph mObjectGraph;

    @SuppressWarnings("unused")
    public MainActivity() {
        this(new DaggerDependencyInjector());
    }

    @VisibleForTesting
    protected MainActivity(DependencyInjector objectGraphCreator) {
        this.mDependencyInjector = objectGraphCreator;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mObjectGraph = mDependencyInjector.fromAppGraphWithModules(
                new ExploreModule(),
                new StorageModule(),
                new RxModule(),
                new ApiModule()
        );
        mObjectGraph.inject(this);
        mNavigationFragment = findNavigationFragment();
        mNavigationFragment.initState(savedInstanceState);


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

    private NavigationFragment findNavigationFragment() {
        boolean isLayoutWithFixedNav = findViewById(R.id.navigation_fragment_id) == null;
        return (NavigationFragment) getSupportFragmentManager().findFragmentById(isLayoutWithFixedNav ?
            R.id.fixed_navigation_fragment_id :
            R.id.navigation_fragment_id);
    }

    private void handleLoggedInUser(ApplicationProperties appProperties, Observer<User> observer) {
        boolean justAuthenticated = getIntent() != null && getIntent().hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
        User currentUser = application.getLoggedInUser();
        if (!justAuthenticated && mAccountOperations.shouldCheckForConfirmedEmailAddress(currentUser)) {

            mSubscription.add(fromActivity(this, mUserOperations.refreshCurrentUser()).subscribe(observer));
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
        mLastTitle = getSupportActionBar().getTitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mAccountOperations.soundCloudAccountExists()) {
            mAccountOperations.addSoundCloudAccountManually(this);
            finish();
        }

        if (shouldTrackScreen()) {
            publishContentChangeEvent();
        }
    }

    private void publishContentChangeEvent() {
        final int position = mNavigationFragment.getCurrentSelectedPosition();
        switch (NavigationFragment.NavItem.values()[position]) {
            case STREAM:
                Event.SCREEN_ENTERED.publish(Screen.SIDE_MENU_STREAM.get());
                break;
            case LIKES:
                Event.SCREEN_ENTERED.publish(Screen.SIDE_MENU_LIKES.get());
                break;
            case PLAYLISTS:
                Event.SCREEN_ENTERED.publish(Screen.SIDE_MENU_PLAYLISTS.get());
            default:
                break; // the remaining content fragments are tracked individually
        }
    }

    @Override
    protected void onDestroy() {
        UpdateManager.unregister();
        mSubscription.unsubscribe();
        mObjectGraph = null;
        super.onDestroy();
    }

    public ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putCharSequence(EXTRA_ACTIONBAR_TITLE, mLastTitle);
        mNavigationFragment.storeState(savedInstanceState);
    }

    @Override
    public void onNavigationItemSelected(int position, boolean setTitle) {
        if (position == mLastSelection) return;
        switch (NavigationFragment.NavItem.values()[position]) {
            case PROFILE:
                displayProfile();
                return;
            case STREAM:
                displayStream();
                break;
            case EXPLORE:
                displayExplore();
                break;
            case LIKES:
                displayLikes();
                break;
            case PLAYLISTS:
                displayPlaylists();
        }

        if (setTitle){
            /**
             * In this case, restoreActionBar will not be called since it is already closed.
             * This probably came from {@link NavigationFragment#handleIntent(android.content.Intent)}
             */
            getSupportActionBar().setTitle(mLastTitle);
        }
        if (mLastSelection != NO_SELECTION) {
            // only publish content change events here that were user initiated, not those coming from rotation changes
            // and stuff.
            publishContentChangeEvent();
        }
        if (position != NavigationFragment.NavItem.PROFILE.ordinal()) {
            mLastSelection = position;
        }
    }

    private void displayProfile() {
        // Hi developer! If you're removing this line to replace the user profile activity with a fragment,
        // don't forget to search for the TODOs related to this in NavigationFragment.
        // --Your friend.
        getSupportActionBar().setDisplayShowTitleEnabled(false); // prevents title text change flashing
        startActivity(new Intent(this, MeActivity.class));
    }

    private void displayPlaylists() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(PLAYLISTS_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = ScListFragment.newInstance(Content.ME_PLAYLISTS.uri, R.string.side_menu_playlists, Screen.SIDE_MENU_PLAYLISTS);
            attachFragment(fragment, PLAYLISTS_FRAGMENT_TAG, R.string.side_menu_playlists);
        }
    }

    private void displayLikes() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LIKES_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = ScListFragment.newInstance(Content.ME_LIKES.uri, R.string.side_menu_likes, Screen.SIDE_MENU_LIKES);
            attachFragment(fragment, LIKES_FRAGMENT_TAG, R.string.side_menu_likes);
        }
    }

    private void displayExplore() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(EXPLORE_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = mExploreFragmentProvider.get();
            attachFragment(fragment, EXPLORE_FRAGMENT_TAG, R.string.side_menu_explore);
        }
    }

    private void displayStream() {
        final Uri contentUri = getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true) ?
                Content.ME_SOUND_STREAM.uri :
                Content.ME_SOUND_STREAM.uri.buildUpon()
                        .appendQueryParameter(Consts.Keys.ONBOARDING, Consts.StringValues.ERROR).build();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(STREAM_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = ScListFragment.newInstance(contentUri, R.string.side_menu_stream, Screen.SIDE_MENU_STREAM);
            attachFragment(fragment, STREAM_FRAGMENT_TAG, R.string.side_menu_stream);
        }
    }

    private void attachFragment(Fragment fragment, String tag, int titleResId) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment, tag)
                .commit();
        mLastTitle = getString(titleResId);
    }

    public void restoreActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mLastTitle);
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
