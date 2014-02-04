package com.soundcloud.android.main;

import static rx.android.observables.AndroidObservable.fromActivity;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.UserOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.LikesListFragment;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.dagger.DependencyInjector;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventBus2;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.explore.ExploreFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.onboarding.auth.EmailConfirmationActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.storage.provider.Content;
import net.hockeyapp.android.UpdateManager;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;

import javax.inject.Inject;

public class MainActivity extends ScActivity implements NavigationFragment.NavigationCallbacks {

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
    EventBus2 mEventBus;
    @Inject
    ApplicationProperties mApplicationProperties;
    @Inject
    SoundCloudApplication mApplication;
    @Inject
    UserOperations mUserOperations;

    private final DependencyInjector mDependencyInjector;

    private CompositeSubscription mSubscription = new CompositeSubscription();

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

        mDependencyInjector.fromAppGraphWithModules(new MainModule()).inject(this);

        mNavigationFragment = findNavigationFragment();
        mNavigationFragment.initState(savedInstanceState);

        if (savedInstanceState != null) {
            mLastTitle = savedInstanceState.getCharSequence(EXTRA_ACTIONBAR_TITLE);
        } else {
            mLastTitle = getTitle();
            if (mAccountOperations.soundCloudAccountExists()) {
                handleLoggedInUser(mApplicationProperties);
            }
        }

        // this must come after setting up the navigation drawer to configure the action bar properly
        supportInvalidateOptionsMenu();
        mSubscription.add(EventBus.CURRENT_USER_CHANGED.subscribe(new CurrentUserChangedObserver()));
    }

    private NavigationFragment findNavigationFragment() {
        boolean isLayoutWithFixedNav = findViewById(R.id.navigation_fragment_id) == null;
        return (NavigationFragment) getSupportFragmentManager().findFragmentById(isLayoutWithFixedNav ?
            R.id.fixed_navigation_fragment_id :
            R.id.navigation_fragment_id);
    }

    private void handleLoggedInUser(ApplicationProperties appProperties) {
        boolean justAuthenticated = getIntent() != null && getIntent().hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
        User currentUser = mApplication.getLoggedInUser();
        if (!justAuthenticated && mAccountOperations.shouldCheckForConfirmedEmailAddress(currentUser)) {
            mSubscription.add(fromActivity(this, mUserOperations.refreshCurrentUser()).subscribe(new UserObserver()));
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
                EventBus.SCREEN_ENTERED.publish(Screen.SIDE_MENU_STREAM.get());
                break;
            case EXPLORE:
                // Publish event for default page in the explore fragment
                // Doesn't fire in onPageSelected() due to https://code.google.com/p/android/issues/detail?id=27526
                EventBus.SCREEN_ENTERED.publish(Screen.EXPLORE_GENRES.get());
                break;
            case LIKES:
                EventBus.SCREEN_ENTERED.publish(Screen.SIDE_MENU_LIKES.get());
                break;
            case PLAYLISTS:
                EventBus.SCREEN_ENTERED.publish(Screen.SIDE_MENU_PLAYLISTS.get());
            default:
                break; // the remaining content fragments are tracked individually
        }
    }

    private void publishNavSelectedEvent() {
        final int position = mNavigationFragment.getCurrentSelectedPosition();
        switch (NavigationFragment.NavItem.values()[position]) {
            case STREAM:
                mEventBus.publish(EventQueue.UI, UIEvent.fromStreamNav());
                break;
            case EXPLORE:
                mEventBus.publish(EventQueue.UI, UIEvent.fromExploreNav());
                break;
            case LIKES:
                mEventBus.publish(EventQueue.UI, UIEvent.fromLikesNav());
                break;
            case PLAYLISTS:
                mEventBus.publish(EventQueue.UI, UIEvent.fromPlaylistsNav());
            default:
                break;
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
        mNavigationFragment.storeState(savedInstanceState);
    }

    @Override
    public void onNavigationItemSelected(int position, boolean setTitle) {
        if (position == mLastSelection) return;
        switch (NavigationFragment.NavItem.values()[position]) {
            case PROFILE:
                displayProfile();
                // This click is tracked separately since profile item is never selected
                mEventBus.publish(EventQueue.UI, UIEvent.fromProfileNav());
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

        if (setTitle) {
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
            publishNavSelectedEvent();
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
            fragment = new LikesListFragment();
            attachFragment(fragment, LIKES_FRAGMENT_TAG, R.string.side_menu_likes);
        }
    }

    private void displayExplore() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(EXPLORE_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new ExploreFragment();
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

    private class CurrentUserChangedObserver extends DefaultObserver<CurrentUserChangedEvent> {
        @Override
        public void onNext(CurrentUserChangedEvent userChangedEvent) {
            if(userChangedEvent.getKind() == CurrentUserChangedEvent.USER_UPDATED) {
                updateUser(userChangedEvent.getCurrentUser());
            }
        }
    }

    private class UserObserver extends DefaultObserver<User> {
        @Override
        public void onNext(User user) {
            updateUser(user);
        }
    }

    private void updateUser(User user) {
        mNavigationFragment.updateProfileItem(user, mApplication.getResources());
        if (!user.isPrimaryEmailConfirmed()) {
            startActivityForResult(new Intent(this, EmailConfirmationActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
        }
    }
}
