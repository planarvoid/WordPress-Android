package com.soundcloud.android.main;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.collections.CollectionsFragment;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.explore.ExploreFragment;
import com.soundcloud.android.likes.TrackLikesFragment;
import com.soundcloud.android.main.LegacyNavigationFragment.NavItem;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.playlists.PlaylistsFragment;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stations.StationsHomeFragment;
import com.soundcloud.android.stream.SoundStreamFragment;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

@Deprecated // New top level navigation is in MainTabsPresenter
class LegacyMainPresenter extends NavigationPresenter {

    private static final String EXTRA_ACTIONBAR_TITLE = "actionbar_title";

    private static final String PLAYLISTS_FRAGMENT_TAG = "playlists_fragment";
    private static final String LIKES_FRAGMENT_TAG = "likes_fragment";
    private static final String EXPLORE_FRAGMENT_TAG = "explore_fragment";
    private static final String STREAM_FRAGMENT_TAG = "stream_fragment";
    private static final String STATIONS_FRAGMENT_TAG = "stations_fragment";
    private static final String COLLECTIONS_FRAGMENT_TAG = "collections_fragment";

    private static final NavItem NO_SELECTION = NavItem.NONE;
    private static final int DRAWER_SELECT_DELAY_MILLIS = 250;

    private final AccountOperations accountOperations;
    private final UserRepository userRepository;
    private final Resources resources;
    private final EventBus eventBus;
    private final Navigator navigator;
    private final BaseLayoutHelper layoutHelper;

    private final Handler drawerHandler = new Handler();
    private final CompositeSubscription userSubscription = new CompositeSubscription();

    private AppCompatActivity activity;
    private LegacyNavigationFragment navigationFragment;
    private FragmentManager fragmentManager;
    private ActionBar actionBar;

    private NavItem lastSelection = NO_SELECTION;
    private CharSequence lastTitle;
    private boolean refreshStream;

    @Inject
    public LegacyMainPresenter(AccountOperations accountOperations, UserRepository userRepository, Resources resources,
                               EventBus eventBus, Navigator navigator, BaseLayoutHelper layoutHelper) {
        this.accountOperations = accountOperations;
        this.userRepository = userRepository;
        this.resources = resources;
        this.eventBus = eventBus;
        this.navigator = navigator;
        this.layoutHelper = layoutHelper;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        setupTitle(activity, bundle);

        this.activity = activity;
        fragmentManager = activity.getSupportFragmentManager();
        actionBar = activity.getSupportActionBar();

        navigationFragment = findNavigationFragment(activity);
        navigationFragment.initState(bundle);

        handleLoggedInUser(activity.getIntent());

        userSubscription.add(eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber()));
        activity.supportInvalidateOptionsMenu();
    }

    private void setupTitle(AppCompatActivity activity, @Nullable Bundle bundle) {
        if (bundle == null) {
            lastTitle = Strings.isNotBlank(lastTitle) ? lastTitle : activity.getTitle();
        } else {
            lastTitle = bundle.getCharSequence(EXTRA_ACTIONBAR_TITLE);
        }
    }

    private LegacyNavigationFragment findNavigationFragment(AppCompatActivity activity) {
        boolean isLayoutWithFixedNav = activity.findViewById(R.id.navigation_fragment_id) == null;
        return (LegacyNavigationFragment) activity.getSupportFragmentManager().findFragmentById(isLayoutWithFixedNav ?
                R.id.fixed_navigation_fragment_id :
                R.id.navigation_fragment_id);
    }

    @Override
    public void onSaveInstanceState(AppCompatActivity activity, Bundle bundle) {
        // Cannot swap content Fragment after onSaveInstanceState
        drawerHandler.removeCallbacksAndMessages(null);

        bundle.putCharSequence(EXTRA_ACTIONBAR_TITLE, lastTitle);
        navigationFragment.storeState(bundle);
    }

    private void handleLoggedInUser(@Nullable Intent intent) {
        if (accountOperations.isUserLoggedIn()) {
            boolean justAuthenticated = intent != null && intent.hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
            if (!justAuthenticated) {
                userSubscription.add(userRepository.syncedUserInfo(accountOperations.getLoggedInUserUrn())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new UserSubscriber()));
            }
        }
    }

    @Override
    public void onNewIntent(AppCompatActivity activity, Intent intent) {
        refreshStream = intent.getBooleanExtra(MainActivity.EXTRA_REFRESH_STREAM, false);

        final boolean setFragmentViaIntent = navigationFragment.handleIntent(intent);
        if (setFragmentViaIntent && Strings.isNotBlank(actionBar.getTitle())) {
            // the title/selection changed as a result of this intent, so store the new title to prevent overwriting
            lastTitle = actionBar.getTitle();
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        userSubscription.unsubscribe();
    }

    public void onInvalidateOptionsMenu() {
        actionBar.setTitle(lastTitle);
    }

    @Override
    public void setBaseLayout(AppCompatActivity activity) {
        layoutHelper.setBaseDrawerLayout(activity);
    }

    @Override
    public void onSelectItem(NavItem item) {
        displaySelectedItem(item);
    }

    @Override
    public void onSmoothSelectItem(NavItem item) {
        if (item != lastSelection) {
            displayContentDelayed(item);
        }
    }

    private void displayContentDelayed(final NavItem item) {
        drawerHandler.removeCallbacksAndMessages(null);
        drawerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (item != NavItem.PROFILE && lastSelection != NO_SELECTION) {
                    Fragment current = fragmentManager.findFragmentById(R.id.container);
                    if (current != null) {
                        fragmentManager.beginTransaction().remove(current).commit();
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

        actionBar.setTitle(lastTitle);
        trackSelection();
        if (NavItem.isSelectable(item)) {
            lastSelection = item;
        }
    }

    private void trackSelection() {
        if (lastSelection != NO_SELECTION) {
            // only publish content change events here that were user initiated,
            // not those coming from rotation changes etc.
            trackScreen();
            trackNavSelection();
        }
    }

    private void displayStations() {
        Fragment fragment = fragmentManager.findFragmentByTag(STATIONS_FRAGMENT_TAG);

        if (fragment == null) {
            fragment = new StationsHomeFragment();
            attachFragment(fragment, STATIONS_FRAGMENT_TAG, R.string.side_menu_stations);
        }
    }

    private void displayCollections() {
        Fragment fragment = fragmentManager.findFragmentByTag(COLLECTIONS_FRAGMENT_TAG);

        if (fragment == null) {
            fragment = new CollectionsFragment();
            attachFragment(fragment, COLLECTIONS_FRAGMENT_TAG, R.string.side_menu_collection);
        }
    }

    private void displayUpsell() {
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forNavClick());
        navigator.openUpgrade(activity);
    }

    private void displayProfile() {
        navigator.openMyProfile(activity, accountOperations.getLoggedInUserUrn());
    }

    private void displayPlaylists() {
        Fragment fragment = fragmentManager.findFragmentByTag(PLAYLISTS_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new PlaylistsFragment();
            attachFragment(fragment, PLAYLISTS_FRAGMENT_TAG, R.string.side_menu_playlists);
        }
    }

    private void displayLikes() {
        Fragment fragment = fragmentManager.findFragmentByTag(LIKES_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new TrackLikesFragment();
            attachFragment(fragment, LIKES_FRAGMENT_TAG, R.string.side_menu_likes);
        }
    }

    private void displayExplore() {
        Fragment fragment = fragmentManager.findFragmentByTag(EXPLORE_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new ExploreFragment();
            attachFragment(fragment, EXPLORE_FRAGMENT_TAG, R.string.side_menu_explore);
        }
    }

    private void displayStream() {
        Fragment fragment = fragmentManager.findFragmentByTag(STREAM_FRAGMENT_TAG);
        if (fragment == null || refreshStream) {
            refreshStream = false;
            fragment = new SoundStreamFragment();
            attachFragment(fragment, STREAM_FRAGMENT_TAG, R.string.side_menu_stream);
        }
    }

    private void attachFragment(Fragment fragment, String tag, @StringRes int titleResId) {
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.activity_open_enter, R.anim.hold)
                .replace(R.id.container, fragment, tag)
                .commit();
        lastTitle = resources.getString(titleResId);
    }

    public void trackScreen() {
        switch (navigationFragment.getCurrentSelectedItem()) {
            case STREAM:
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.STREAM));
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
            case STATIONS:
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.STATIONS_HOME));
                break;
            default:
                break; // the remaining content fragments are tracked individually
        }
    }

    private void trackNavSelection() {
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

    public boolean handleBackPressed() {
        return navigationFragment.handleBackPressed();
    }

    private class CurrentUserChangedSubscriber extends DefaultSubscriber<CurrentUserChangedEvent> {
        @Override
        public void onNext(CurrentUserChangedEvent userChangedEvent) {
            if (userChangedEvent.getKind() == CurrentUserChangedEvent.USER_UPDATED) {
                navigationFragment.updateProfileItem(userChangedEvent.getCurrentUser());
            }
        }
    }

    private class UserSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet user) {
            navigationFragment.updateProfileItem(user);
        }
    }

}
