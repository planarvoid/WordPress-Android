package com.soundcloud.android.main;

import static com.soundcloud.android.main.NavigationFragment.NavigationCallbacks;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;
import static rx.android.observables.AndroidObservable.bindActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.UserOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.LikesListFragment;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.explore.ExploreFragment;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.onboarding.auth.EmailConfirmationActivity;
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
    public static final String EXPAND_PLAYER = "EXPAND_PLAYER";
    public static final String EXTRA_ONBOARDING_USERS_RESULT = "onboarding_users_result";

    private static final String EXTRA_ACTIONBAR_TITLE = "actionbar_title";
    private static final String PLAYLISTS_FRAGMENT_TAG = "playlists_fragment";
    private static final String LIKES_FRAGMENT_TAG = "likes_fragment";
    private static final String EXPLORE_FRAGMENT_TAG = "explore_fragment";
    private static final String STREAM_FRAGMENT_TAG = "stream_fragment";
    private static final int NO_SELECTION = -1;
    private static final int DRAWER_SELECT_DELAY_MILLIS = 250;

    private NavigationFragment navigationFragment;
    private CharSequence lastTitle;
    private int lastSelection = NO_SELECTION;

    @Inject ScreenPresenter presenter;
    @Inject UserOperations userOperations;
    @Inject SlidingPlayerController playerController;

    private final CompositeSubscription subscription = new CompositeSubscription();
    private Handler drawerHandler = new Handler();

    public MainActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponent(playerController);
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

        final boolean setFragmentViaIntent = navigationFragment.handleIntent(intent);
        if (setFragmentViaIntent && isNotBlank(getSupportActionBar().getTitle())) {
            // the title/selection changed as a result of this intent, so store the new title to prevent overwriting
            lastTitle = getSupportActionBar().getTitle();
        }

        if (intent.getBooleanExtra(EXPAND_PLAYER, false)) {
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
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
        // make sure we do not let "show fragment" messages propagate when the activity gets destroyed
        drawerHandler.removeCallbacksAndMessages(null);
        subscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putCharSequence(EXTRA_ACTIONBAR_TITLE, lastTitle);
        navigationFragment.storeState(savedInstanceState);
    }

    @Override
    public void onNavigationItemSelected(final int position, final boolean setTitle) {
        if (position == lastSelection) return;

        if (!isProfile(position) && lastSelection != NO_SELECTION) {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
            if (current != null) {
                getSupportFragmentManager().beginTransaction().remove(current).commit();
            }
        }

        // delay showing the fragment to make for smoother transitions when swapping content
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
        /*
         * Expand the player here to respect the following flow
         * - Display collapsed player
         * - Display content
         * - Expand player
         * */
        expandPlayerIfNeeded();
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
        if (fragment == null) {
            fragment = SoundStreamFragment.create(onboardingSucceeded);
            attachFragment(fragment, STREAM_FRAGMENT_TAG, R.string.side_menu_stream);
        }
    }

    private void expandPlayerIfNeeded() {
        if (getIntent().getBooleanExtra(EXPAND_PLAYER, false) && !playerController.isExpanded()) {
            playerController.expand();
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
            if(userChangedEvent.getKind() == CurrentUserChangedEvent.USER_UPDATED) {
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
        if (!user.isPrimaryEmailConfirmed()) {
                    startActivityForResult(new Intent(this, EmailConfirmationActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS), 0);
        }
    }

}
