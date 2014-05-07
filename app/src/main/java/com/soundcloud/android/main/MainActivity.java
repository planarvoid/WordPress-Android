package com.soundcloud.android.main;

import static com.sothree.slidinguppanel.SlidingUpPanelLayout.*;
import static com.soundcloud.android.main.NavigationFragment.NavigationCallbacks;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;
import static rx.android.observables.AndroidObservable.bindActivity;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.UserOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.LikesListFragment;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.explore.ExploreFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.onboarding.auth.EmailConfirmationActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import net.hockeyapp.android.UpdateManager;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.View;

import javax.inject.Inject;

public class MainActivity extends ScActivity implements NavigationCallbacks {

    public static final String EXTRA_ONBOARDING_USERS_RESULT = "onboarding_users_result";

    private static final String EXTRA_ACTIONBAR_TITLE = "actionbar_title";
    private static final String EXTRA_ACTIONBAR_VISIBLE = "actionbar_visible";
    private static final String PLAYLISTS_FRAGMENT_TAG = "playlists_fragment";
    private static final String LIKES_FRAGMENT_TAG = "likes_fragment";
    private static final String EXPLORE_FRAGMENT_TAG = "explore_fragment";
    private static final String STREAM_FRAGMENT_TAG = "stream_fragment";
    private static final int NO_SELECTION = -1;

    private NavigationFragment navigationFragment;
    private CharSequence lastTitle;
    private int lastSelection = NO_SELECTION;

    @Inject
    ApplicationProperties applicationProperties;
    @Inject
    SoundCloudApplication application;
    @Inject
    UserOperations userOperations;
    @Inject
    StreamFragmentFactory streamFragmentFactory;
    @Inject
    FeatureFlags featureFlags;

    private final CompositeSubscription subscription = new CompositeSubscription();

    public MainActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navigationFragment = findNavigationFragment();
        navigationFragment.initState(savedInstanceState);

        if (savedInstanceState != null) {
            lastTitle = savedInstanceState.getCharSequence(EXTRA_ACTIONBAR_TITLE);
            actionBarController.setVisible(savedInstanceState.getBoolean(EXTRA_ACTIONBAR_VISIBLE, true));
        } else {
            lastTitle = getTitle();
            if (accountOperations.isUserLoggedIn()) {
                handleLoggedInUser(applicationProperties);
            }
            if (getIntent().hasExtra(EXTRA_ONBOARDING_USERS_RESULT)) {
                EmailOptInDialogFragment.show(this);
            }
        }

        // this must come after setting up the navigation drawer to configure the action bar properly
        supportInvalidateOptionsMenu();
        subscription.add(eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber()));
    }

    @Override
    protected void setContentView() {
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
            setContentView(R.layout.main_activity);
            SlidingUpPanelLayout playerPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
            playerPanel.setPanelSlideListener(new PlayerPanelListener());
        } else {
            setContentView(R.layout.main_activity_legacy);
        }
    }

    @Override
    public void onBackPressed() {
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            SlidingUpPanelLayout playerPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
            if (playerPanel.isExpanded()) {
                playerPanel.collapsePane();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    private NavigationFragment findNavigationFragment() {
        boolean isLayoutWithFixedNav = findViewById(R.id.navigation_fragment_id) == null;
        return (NavigationFragment) getSupportFragmentManager().findFragmentById(isLayoutWithFixedNav ?
            R.id.fixed_navigation_fragment_id :
            R.id.navigation_fragment_id);
    }

    private void handleLoggedInUser(ApplicationProperties appProperties) {
        boolean justAuthenticated = getIntent() != null && getIntent().hasExtra(AuthenticatorService.KEY_ACCOUNT_RESULT);
        User currentUser = accountOperations.getLoggedInUser();
        if (!justAuthenticated && accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)) {
            subscription.add(bindActivity(this, userOperations.refreshCurrentUser()).subscribe(new UserSubscriber()));
        }

        if (appProperties.isBetaBuildRunningOnDalvik()) {
            UpdateManager.register(this, getString(R.string.hockey_app_id));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

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
                eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SIDE_MENU_STREAM.get());
                break;
            case EXPLORE:
                // Publish event for default page in the explore fragment
                // Doesn't fire in onPageSelected() due to https://code.google.com/p/android/issues/detail?id=27526
                eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.EXPLORE_GENRES.get());
                break;
            case LIKES:
                eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SIDE_MENU_LIKES.get());
                break;
            case PLAYLISTS:
                eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SIDE_MENU_PLAYLISTS.get());
            default:
                break; // the remaining content fragments are tracked individually
        }
    }

    private void publishNavSelectedEvent() {
        final int position = navigationFragment.getCurrentSelectedPosition();
        switch (NavigationFragment.NavItem.values()[position]) {
            case STREAM:
                eventBus.publish(EventQueue.UI, UIEvent.fromStreamNav());
                break;
            case EXPLORE:
                eventBus.publish(EventQueue.UI, UIEvent.fromExploreNav());
                break;
            case LIKES:
                eventBus.publish(EventQueue.UI, UIEvent.fromLikesNav());
                break;
            case PLAYLISTS:
                eventBus.publish(EventQueue.UI, UIEvent.fromPlaylistsNav());
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        UpdateManager.unregister();
        subscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putCharSequence(EXTRA_ACTIONBAR_TITLE, lastTitle);
        savedInstanceState.putBoolean(EXTRA_ACTIONBAR_VISIBLE, actionBarController.isVisible());
        navigationFragment.storeState(savedInstanceState);
    }

    @Override
    public void onNavigationItemSelected(int position, boolean setTitle) {
        if (position == lastSelection) return;
        switch (NavigationFragment.NavItem.values()[position]) {
            case PROFILE:
                displayProfile();
                // This click is tracked separately since profile item is never selected
                eventBus.publish(EventQueue.UI, UIEvent.fromProfileNav());
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
         boolean onboardingResult = getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(STREAM_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = streamFragmentFactory.create(onboardingResult);
            attachFragment(fragment, STREAM_FRAGMENT_TAG, R.string.side_menu_stream);
        }
    }

    private void attachFragment(Fragment fragment, String tag, int titleResId) {
        getSupportFragmentManager().beginTransaction()
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
            if(userChangedEvent.getKind() == CurrentUserChangedEvent.USER_UPDATED) {
                updateUser(userChangedEvent.getCurrentUser());
            }
        }
    }

    private class UserSubscriber extends DefaultSubscriber<User> {
        @Override
        public void onNext(User user) {
            updateUser(user);
        }
    }


    private void updateUser(User user) {
                navigationFragment.updateProfileItem(user);
        if (!user.isPrimaryEmailConfirmed()) {
                    startActivityForResult(new Intent(this, EmailConfirmationActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
        }
    }

    private class PlayerPanelListener extends SimplePanelSlideListener {

        @Override
        public void onPanelSlide(View panel, float slideOffset) {
            actionBarController.setVisible(slideOffset > 0.5f);
        }

        @Override
        public void onPanelCollapsed(View panel) {
            setDrawerLocked(false);
        }

        @Override
        public void onPanelExpanded(View panel) {
            setDrawerLocked(true);
        }

        private void setDrawerLocked(boolean locked) {
            if (navigationFragment instanceof NavigationDrawerFragment) {
                ((NavigationDrawerFragment) navigationFragment).setLocked(locked);
            }
        }

    }
}
