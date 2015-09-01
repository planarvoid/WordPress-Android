package com.soundcloud.android.main;

import static com.soundcloud.android.main.NavigationFragment.NavItem;
import static com.soundcloud.android.main.NavigationFragment.NavigationCallbacks;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;
import static rx.android.app.AppObservable.bindActivity;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.campaigns.InAppCampaignController;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.collections.CollectionsFragment;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.explore.ExploreFragment;
import com.soundcloud.android.gcm.GcmManager;
import com.soundcloud.android.likes.TrackLikesFragment;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playlists.PlaylistsFragment;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stations.StationsHomeFragment;
import com.soundcloud.android.stream.SoundStreamFragment;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.LightCycle;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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
    private static final String STATIONS_FRAGMENT_TAG = "stations_fragment";
    private static final String COLLECTIONS_FRAGMENT_TAG = "collections_fragment";
    private static final NavItem NO_SELECTION = NavItem.NONE;
    private static final int DRAWER_SELECT_DELAY_MILLIS = 250;

    private final CompositeSubscription subscription = new CompositeSubscription();
    private final Handler drawerHandler = new Handler();

    private NavigationFragment navigationFragment;
    private CharSequence lastTitle;
    private NavItem lastSelection = NO_SELECTION;
    private boolean refreshStream;

    @Inject UserRepository userRepository;
    @Inject FeatureFlags featureFlags;
    @Inject PlayQueueManager playQueueManager;
    @Inject CastConnectionHelper castConnectionHelper;
    @Inject Navigator navigator;

    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle InAppCampaignController inAppCampaignController;
    @Inject @LightCycle ActionBarHelper actionBarHelper;
    @Inject @LightCycle GcmManager gcmManager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navigationFragment = findNavigationFragment();
        navigationFragment.initState(savedInstanceState);

        if (savedInstanceState == null) {
            restoreTitle();
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

        castConnectionHelper.reconnectSessionIfPossible();

        if (playQueueManager.shouldReloadQueue()) {
            playQueueManager.loadPlayQueueAsync(true);
        }
    }

    private void restoreTitle() {
        lastTitle = isNotBlank(lastTitle) ? lastTitle : getTitle();
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
        if (accountOperations.isCrawler() || !(playerController.handleBackPressed() || navigationFragment.handleBackPressed())) {
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
        if (!justAuthenticated) {
            subscription.add(bindActivity(this, userRepository.syncedUserInfo(accountOperations.getLoggedInUserUrn())).subscribe(new UserSubscriber()));
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

        trackForegroundEvent(intent);
        setIntent(intent);
    }

    private void trackForegroundEvent(Intent intent) {
        if (Referrer.hasReferrer(intent) && Screen.hasScreen(intent)) {
            eventBus.publish(EventQueue.TRACKING, ForegroundEvent.open(Screen.fromIntent(intent), Referrer.fromIntent(intent)));
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
        switch (navigationFragment.getCurrentSelectedItem()) {
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
                break;
            default:
                break; // the remaining content fragments are tracked individually
        }
    }

    private void publishNavSelectedEvent() {
        switch (navigationFragment.getCurrentSelectedItem()) {
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
                break;
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
    public void onSmoothSelectItem(final NavItem item) {
        if (item == lastSelection) {
            return;
        }

        displayContentDelayed(item);
    }

    @Override
    public void onSelectItem(NavItem item) {
        displaySelectedItem(item);
    }

    private void displayContentDelayed(final NavItem item) {
        drawerHandler.removeCallbacksAndMessages(null);
        drawerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (item != NavItem.PROFILE && lastSelection != NO_SELECTION) {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
                    if (current != null) {
                        getSupportFragmentManager().beginTransaction().remove(current).commit();
                    }
                }

                displaySelectedItem(item);
            }
        }, DRAWER_SELECT_DELAY_MILLIS);
    }

    protected void displaySelectedItem(NavItem item) {
        switch (item) {
            case PROFILE:
                displayProfile();
                // This click is tracked separately since profile item is never selected
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromProfileNav());
                return;
            case STATIONS:
                displayStations();
                break;
            case COLLECTIONS:
                displayCollections();
                break;
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
                break;
            case UPSELL:
                displayUpsell();
                break;
            default:
                throw new IllegalArgumentException("Unknown navItem: " + item);
        }

        getSupportActionBar().setTitle(lastTitle);

        if (lastSelection != NO_SELECTION) {
            // only publish content change events here that were user initiated, not those coming from rotation changes
            // and stuff.
            publishContentChangeEvent();
            publishNavSelectedEvent();
        }
        if (NavItem.isSelectable(item)) {
            lastSelection = item;
        }
    }

    private void displayStations() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(STATIONS_FRAGMENT_TAG);

        if (fragment == null) {
            fragment = new StationsHomeFragment();
            attachFragment(fragment, STATIONS_FRAGMENT_TAG, R.string.side_menu_stations);
        }
    }

    private void displayCollections() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(COLLECTIONS_FRAGMENT_TAG);

        if (fragment == null) {
            fragment = new CollectionsFragment();
            attachFragment(fragment, COLLECTIONS_FRAGMENT_TAG, R.string.side_menu_collections);
        }
    }

    private void displayUpsell() {
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forNavClick());
        navigator.openUpgrade(this);
    }

    private void displayProfile() {
        navigator.openMyProfile(this, accountOperations.getLoggedInUserUrn());
    }

    private void displayPlaylists() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(PLAYLISTS_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new PlaylistsFragment();
            attachFragment(fragment, PLAYLISTS_FRAGMENT_TAG, R.string.side_menu_playlists);
        }
    }

    private void displayLikes() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LIKES_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new TrackLikesFragment();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        configureMainOptionMenuItems(menu);
        getSupportActionBar().setTitle(lastTitle);
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

    private class UserSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet user) {
            updateUser(user);
        }
    }

    private void updateUser(PropertySet user) {
        navigationFragment.updateProfileItem(user);
    }

}
