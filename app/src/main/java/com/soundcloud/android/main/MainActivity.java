package com.soundcloud.android.main;

import static rx.android.AndroidObservables.fromActivity;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.UserOperations;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.dagger.DependencyInjector;
import com.soundcloud.android.dagger.ObjectGraphProvider;
import com.soundcloud.android.explore.ExploreFragment;
import com.soundcloud.android.explore.ExploreTracksCategoriesFragmentModule;
import com.soundcloud.android.explore.ExploreTracksFragmentModule;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.onboarding.auth.EmailConfirmationActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.Event;
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

public class MainActivity extends ScActivity implements NavigationFragment.NavigationCallbacks,
        ObjectGraphProvider {

    public static final String EXTRA_ONBOARDING_USERS_RESULT = "onboarding_users_result";
    private static final String EXTRA_ACTIONBAR_TITLE = "actionbar_title";

    private NavigationFragment mNavigationFragment;
    private CharSequence mLastTitle;
    private int mLastSelection = -1;
    @Inject
    public AccountOperations mAccountOperations;
    @Inject
    public CompositeSubscription mSubscription;
    @Inject
    public ApplicationProperties mApplicationProperties;
    @Inject
    public SoundCloudApplication application;
    @Inject
    public UserOperations mUserOperations;
    @Inject
    public ExploreFragment mExploreFragment;

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
                new ExploreTracksCategoriesFragmentModule(),
                new ExploreTracksFragmentModule(),
                new StorageModule(),
                new RxModule()
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
            case PROFILE:{
                // Hi developer! If you're removing this line to replace the user profile activity with a fragment,
                // don't forget to search for the TODOs related to this in NavigationFragment.
                // --Your friend.
                getSupportActionBar().setDisplayShowTitleEnabled(false); // prevents title text change flashing
                startActivity(new Intent(this, MeActivity.class));
                break;
            }
            case STREAM:{
                final Uri contentUri = getIntent().getBooleanExtra(EXTRA_ONBOARDING_USERS_RESULT, true) ?
                        Content.ME_SOUND_STREAM.uri :
                        Content.ME_SOUND_STREAM.uri.buildUpon()
                                .appendQueryParameter(Consts.Keys.ONBOARDING, Consts.StringValues.ERROR).build();
                Fragment fragment = ScListFragment.newInstance(contentUri, R.string.side_menu_stream);
                attachFragment(fragment, "stream_fragment", R.string.side_menu_stream);
                break;
            }
            case EXPLORE:{
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("explore_fragment");
                if (fragment == null) {
                    attachFragment(mExploreFragment, "explore_fragment", R.string.side_menu_explore);
                }
                break;
            }
            case LIKES:{
                Fragment fragment = ScListFragment.newInstance(Content.ME_LIKES.uri, R.string.side_menu_likes);
                attachFragment(fragment, "likes_fragment", R.string.side_menu_likes);
                break;
            }
            case PLAYLISTS:{
                Fragment fragment = ScListFragment.newInstance(Content.ME_PLAYLISTS.uri, R.string.side_menu_playlists);
                attachFragment(fragment, "playlists_fragment", R.string.side_menu_playlists);
                break;
            }
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
