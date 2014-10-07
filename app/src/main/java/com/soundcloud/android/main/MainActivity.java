package com.soundcloud.android.main;

import static com.soundcloud.android.main.NavigationFragment.NavigationCallbacks;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;
import static rx.android.observables.AndroidObservable.bindActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.UserOperations;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.LikesListFragment;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.explore.ExploreFragment;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.stream.SoundStreamFragment;
import com.soundcloud.android.view.screen.ScreenPresenter;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;

import javax.inject.Inject;

public class MainActivity extends ScActivity implements NavigationCallbacks {
    public static final String EXTRA_ONBOARDING_USERS_RESULT = "onboarding_users_result";
    public static final String EXTRA_REFRESH_STREAM = "refresh_stream";

    private static final String EXTRA_ACTIONBAR_TITLE = "actionbar_title";
    private static final String PLAYLISTS_FRAGMENT_TAG = "playlists_fragment";
    private static final String LIKES_FRAGMENT_TAG = "likes_fragment";
    private static final String EXPLORE_FRAGMENT_TAG = "explore_fragment";
    private static final String STREAM_FRAGMENT_TAG = "stream_fragment";
    private static final int NO_SELECTION = -1;
    private static final int DRAWER_SELECT_DELAY_MILLIS = 250;

    private final CompositeSubscription subscription = new CompositeSubscription();
    private final Handler drawerHandler = new Handler();

    private NavigationFragment navigationFragment;
    private CharSequence lastTitle;
    private int lastSelection = NO_SELECTION;
    private boolean refreshStream;

    @Inject ScreenPresenter presenter;
    @Inject UserOperations userOperations;
    @Inject SlidingPlayerController playerController;
    @Inject AdPlayerController adPlayerController;

    public MainActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponent(playerController);
        addLifeCycleComponent(adPlayerController);
        presenter.attach(this);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navigationFragment = findNavigationFragment();
        navigationFragment.initState(savedInstanceState);

        if (savedInstanceState == null) {
            lastTitle = getTitle();
            if (accountOperations.isUserLoggedIn()) {
                handleLoggedInUser();
            }
            setupEmailOptIn();
        } else {
            lastTitle = savedInstanceState.getCharSequence(EXTRA_ACTIONBAR_TITLE);
        }

        // this must come after setting up the navigation drawer to configure the action bar properly
        supportInvalidateOptionsMenu();
        subscription.add(eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber()));
    }

    private void setupEmailOptIn() {
        if (getIntent().hasExtra(EXTRA_ONBOARDING_USERS_RESULT)) {
            EmailOptInDialogFragment.show(this);
        }
    }

    @Override
    protected void setContentView() {
        presenter.setBaseDrawerLayout();
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }

    private NavigationFragment findNavigationFragment() {
        boolean isLayoutWithFixedNav = findViewById(R.id.navigation_fragment_id) == null;
        return (NavigationFragment) getSupportFragmentManager().findFragmentById(isLayoutWithFixedNav ?
                R.id.fixed_navigation_fragment_id :
                R.id.navigation_fragment_id);
    }

    private void handleLoggedInUser() {
        boolean justAuthenticated = getIntent() != null && getIntent().hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
        PublicApiUser currentUser = accountOperations.getLoggedInUser();
        if (!justAuthenticated && accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)) {
            subscription.add(bindActivity(this, userOperations.refreshCurrentUser()).subscribe(new UserSubscriber()));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        refreshStream = intent.getBooleanExtra(EXTRA_REFRESH_STREAM, false);
        final boolean setFragmentViaIntent = navigationFragment.handleIntent(intent);
        if (setFragmentViaIntent && isNotBlank(getSupportActionBar().getTitle())) {
            // the title/selection changed as a result of this intent, so store the new title to prevent overwriting
            lastTitle = getSupportActionBar().getTitle();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!accountOperations.isUserLoggedIn()) {
            accountOperations.triggerLoginFlow(this);
            finish();
        }

        if (shouldTrackScreen()) {
            publishContentChangeEvent();
        }
    }

    private void publishContentChangeEvent() {
        final int position = navigationFragment.getCurrentSelectedPosition();
        switch (NavigationFragment.NavItem.values()[position]) {
            case STREAM:
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SIDE_MENU_STREAM));
                break;
            case EXPLORE:
                // Publish event for default page in the explore fragment
                // Doesn't fire in onPageSelected() due to https://code.google.com/p/android/issues/detail?id=27526
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.EXPLORE_GENRES));
                break;
            case LIKES:
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SIDE_MENU_LIKES));
                break;
            case PLAYLISTS:
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SIDE_MENU_PLAYLISTS));
            default:
                break; // the remaining content fragments are tracked individually
        }
    }

    private void publishNavSelectedEvent() {
        final int position = navigationFragment.getCurrentSelectedPosition();
        switch (NavigationFragment.NavItem.values()[position]) {
            case STREAM:
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromStreamNav());
                break;
            case EXPLORE:
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromExploreNav());
                break;
            case LIKES:
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromLikesNav());
                break;
            case PLAYLISTS:
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlaylistsNav());
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Cannot swap content Fragment after onSaveInstanceState
        drawerHandler.removeCallbacksAndMessages(null);

        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putCharSequence(EXTRA_ACTIONBAR_TITLE, lastTitle);
        navigationFragment.storeState(savedInstanceState);
    }

    @Override
    public void onSmoothSelectItem(final int position, final boolean setTitle) {
        if (position == lastSelection) {
            return;
        }

        if (!isProfile(position) && lastSelection != NO_SELECTION) {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
            if (current != null) {
                getSupportFragmentManager().beginTransaction().remove(current).commit();
            }
        }

        displayContentDelayed(position, setTitle);
    }

    @Override
    public void onSelectItem(int position, boolean setTitle) {
        displayFragment(position, setTitle);
    }

    private void displayContentDelayed(final int position, final boolean setTitle) {
        drawerHandler.removeCallbacksAndMessages(null);
        drawerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                displayFragment(position, setTitle);
            }
        }, DRAWER_SELECT_DELAY_MILLIS);
    }

    private boolean isProfile(int position) {
        return NavigationFragment.NavItem.values()[position] == NavigationFragment.NavItem.PROFILE;
    }

    protected void displayFragment(int position, boolean setTitle) {
        final NavigationFragment.NavItem navItem = NavigationFragment.NavItem.values()[position];
        switch (navItem) {
            case PROFILE:
                displayProfile();
                // This click is tracked separately since profile item is never selected
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromProfileNav());
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
            getSupportActionBar().setTitle(lastTitle);
        }
        if (lastSelection != NO_SELECTION) {
            // only publish content change events here that were user initiated, not those coming from rotation changes
            // and stuff.
            publishContentChangeEvent();
            publishNavSelectedEvent();
        }
        if (position != NavigationFragment.NavItem.PROFILE.ordinal()) {
            lastSelection = position;
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
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(STREAM_FRAGMENT_TAG);
        boolean onboardingSucceeded = getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true);
        if (fragment == null || refreshStream) {
            refreshStream = false;
            fragment = SoundStreamFragment.create(onboardingSucceeded);
            attachFragment(fragment, STREAM_FRAGMENT_TAG, R.string.side_menu_stream);
        }
    }

    private void attachFragment(Fragment fragment, String tag, int titleResId) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.activity_open_enter, R.anim.hold)
                .replace(R.id.container, fragment, tag)
                .commit();
        lastTitle = getString(titleResId);
    }

    public void restoreActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(lastTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Keep null check. This might fire as a result of setContentView in which case this var won't be assigned
        if (navigationFragment != null) {
            return super.onCreateOptionsMenu(menu);
        }
        return true;
    }

    private class CurrentUserChangedSubscriber extends DefaultSubscriber<CurrentUserChangedEvent> {
        @Override
        public void onNext(CurrentUserChangedEvent userChangedEvent) {
            if (userChangedEvent.getKind() == CurrentUserChangedEvent.USER_UPDATED) {
                updateUser(userChangedEvent.getCurrentUser());
            }
        }
    }

    private class UserSubscriber extends DefaultSubscriber<PublicApiUser> {
        @Override
        public void onNext(PublicApiUser user) {
            updateUser(user);
        }
    }

    private void updateUser(PublicApiUser user) {
        navigationFragment.updateProfileItem(user);
    }

}
