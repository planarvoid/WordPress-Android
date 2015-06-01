package com.soundcloud.android.profile;

import static android.text.TextUtils.isEmpty;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.associations.ToggleFollowSubscriber;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tasks.FetchModelTask;
import com.soundcloud.android.tasks.FetchUserTask;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.android.view.SlidingTabLayout;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

@SuppressWarnings("PMD.TooManyFields")
public class ProfileActivity extends ScActivity implements
        FollowingOperations.FollowStatusChangedListener,
        ActionBar.OnNavigationListener, FetchModelTask.Listener<PublicApiUser>, ViewPager.OnPageChangeListener {

    public static final String EXTRA_USER_URN = "userUrn";
    public static final String EXTRA_USER = "user";
    public static final String EXTRA_QUERY_SOURCE_INFO = "searchQuerySourceInfo";
    private final BroadcastReceiver recordListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            supportInvalidateOptionsMenu();
        }
    };
    protected ViewPager pager;
    protected SlidingTabLayout indicator;
    /* package */ @Nullable PublicApiUser user;
    @Inject ImageOperations imageOperations;
    @Inject PublicCloudAPI oldCloudAPI;
    @Inject FollowingOperations followingOperations;
    @Inject LegacyUserStorage userStorage;
    @Inject @LightCycle SlidingPlayerController playerController;
    @Inject @LightCycle AdPlayerController adPlayerController;
    @Inject ProfileFragmentCreator profileListFragmentCreator;

    private TextView username, followerCount, followerMessage, location;
    private ToggleButton toggleFollow;
    private ImageView userImage;
    private FetchUserTask loadUserTask;
    private UserInfoFragment userInfoFragment;
    private int initialOtherFollowers;
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userImage = (ImageView) findViewById(R.id.user_image);
        username = (TextView) findViewById(R.id.username);
        location = (TextView) findViewById(R.id.location);

        followerCount = (TextView) findViewById(R.id.followers);
        followerMessage = (TextView) findViewById(R.id.followers_message);

        setTitle(isLoggedInUser() ? R.string.side_menu_you : R.string.side_menu_profile);

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user != null) {
                    new FullImageDialog(ProfileActivity.this, user.getUrn(), imageOperations).show();
                }

            }
        });
        toggleFollow = (ToggleButton) findViewById(R.id.toggle_btn_follow);

        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new UserFragmentAdapter(getSupportFragmentManager()));
        pager.setBackgroundColor(Color.WHITE);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        indicator = (SlidingTabLayout) findViewById(R.id.indicator);
        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(this);

        // make sure to call this only after we fully set up the view pager and the indicator, so as to receive
        // the callback to the page changed listener
        if (savedInstanceState == null) {
            pager.setCurrentItem(Tab.tracks.ordinal());
        }

        Intent intent = getIntent();
        Configuration c = (Configuration) getLastCustomNonConfigurationInstance();
        if (c != null) {
            fromConfiguration(c);
        } else {
            handleIntent(intent);
        }

        if (user != null) {
            userInfoFragment = UserInfoFragment.newInstance(user.getId());

            if (isLoggedInUser()) {
                toggleFollow.setVisibility(View.GONE);
            } else {
                toggleFollow.setChecked(followingOperations.isFollowing(user.getUrn()));
                setFollowersMessage();
                toggleFollow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleFollowing(user);
                        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleFollow(toggleFollow.isChecked(),
                                Screen.USER_HEADER.get(), user.getId()));
                    }
                });
            }

            loadDetails();
        } else {
            // if the user is null at this stage there is nothing we can do, except finishing
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // update action bar record option based on recorder status
        supportInvalidateOptionsMenu();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SoundRecorder.RECORD_STARTED);
        filter.addAction(SoundRecorder.RECORD_ERROR);
        filter.addAction(SoundRecorder.RECORD_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(recordListener, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(recordListener);
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        return false;
    }

    @Override
    public Configuration onRetainCustomNonConfigurationInstance() {
        return toConfiguration();
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {
    }

    @Override
    public void onPageSelected(int position) {
        Tab currentTab = Tab.values()[position];
        eventBus.publish(EventQueue.TRACKING, isLoggedInUser() ? ScreenEvent.create(currentTab.youScreen) : ScreenEvent.create(currentTab.userScreen));
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    @Override
    public void onFollowChanged() {
        toggleFollow.setChecked(followingOperations.isFollowing(user.getUrn()));
        setFollowersMessage();
    }

    @Override
    public void onSuccess(PublicApiUser user) {
        user.last_updated = System.currentTimeMillis();
        setUser(user);

        // update user locally and ensure 1 instance
        this.user = SoundCloudApplication.sModelManager.cache(user, PublicApiResource.CacheUpdateMode.FULL);

        // TODO: move to a *Operations class to decouple from storage layer
        fireAndForget(userStorage.storeAsync(this.user));
        userInfoFragment.onSuccess(this.user);
    }

    @Override
    public void onError(Object context) {
        userInfoFragment.onError();
    }

    public PublicApiUser getUser() {
        return user;
    }

    private void setUser(final PublicApiUser user) {
        if (user == null || user.getId() < 0) {
            return;
        }
        this.user = user;

        // Initial count prevents fluctuations from being reflected in followers message
        initialOtherFollowers = user.followers_count;
        if (followingOperations.isFollowing(this.user.getUrn())) {
            initialOtherFollowers--;
        }

        if (!isEmpty(user.username)) {
            username.setText(user.username);
        }

        if (followerCount != null) {
            if (user.followers_count <= 0) {
                followerCount.setVisibility(View.GONE);
            } else {
                followerCount.setVisibility(View.VISIBLE);
                followerCount.setText(ScTextUtils.formatNumberWithCommas(user.followers_count));
            }
        }

        setFollowersMessage();

        if (location != null) {
            if (ScTextUtils.isBlank(user.getLocation())) {
                location.setVisibility(View.GONE);
            } else {
                location.setVisibility(View.VISIBLE);
                location.setText(String.valueOf(user.getLocation()));
            }
        }

        imageOperations.displayWithPlaceholder(this.user.getUrn(),
                ApiImageSize.getFullImageSize(getResources()),
                userImage);

        supportInvalidateOptionsMenu();
    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayoutWithContent(R.layout.profile_content);
    }

    protected void handleIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_USER)) {
            loadUserByObject((PublicApiUser) intent.getParcelableExtra(EXTRA_USER));
        } else if (intent.hasExtra(EXTRA_USER_URN)) {
            Urn urn = intent.getParcelableExtra(EXTRA_USER_URN);
            loadUserById(urn.getNumericId());
        } else if (intent.getData() == null || !loadUserByUri(intent.getData())) {
            loadYou();
        }

        if (intent.hasExtra(EXTRA_QUERY_SOURCE_INFO)) {
            searchQuerySourceInfo = intent.getParcelableExtra(EXTRA_QUERY_SOURCE_INFO);
        }

        if (!isLoggedInUser()) {
            followingOperations.requestUserFollowings(this);
        }

        if (intent.hasExtra(Tab.EXTRA)) {
            pager.setCurrentItem(Tab.indexOf(intent.getStringExtra(Tab.EXTRA)));
            intent.removeExtra(Tab.EXTRA);
        }
    }

    protected boolean isLoggedInUser() {
        return user != null && user.getId() == accountOperations.getLoggedInUserUrn().getNumericId();
    }

    private void loadYou() {
        setUser(accountOperations.getLoggedInUser());
    }

    private void loadUserById(long userId) {
        if (userId != -1) {
            // check DB first as the cached user might be incomplete
            final PublicApiUser u = SoundCloudApplication.sModelManager.getUser(userId);
            setUser(u != null ? u : SoundCloudApplication.sModelManager.getCachedUser(userId));
        }
        if (user == null) {
            user = new PublicApiUser();
            user.setId(userId);
        }
    }

    private boolean loadUserByUri(Uri uri) {
        if (uri != null) {
            user = userStorage.getUserByUri(uri); //FIXME: DB access on UI thread
            if (user == null) {
                loadUserById(UriUtils.getLastSegmentAsLong(uri));
            }
        }
        return user != null;
    }

    private void loadUserByObject(PublicApiUser user) {
        if (user == null || user.getId() == -1) {
            return;
        }

        // show a user out of db if possible because he will be a complete user unlike
        // a parceled user that came from a track, list or comment
        final PublicApiUser dbUser = SoundCloudApplication.sModelManager.getUser(user.getId());
        setUser(dbUser != null ? dbUser : user);
    }

    private void loadDetails() {
        if (loadUserTask == null && user != null) {
            loadUserTask = new FetchUserTask(oldCloudAPI);
            loadUserTask.addListener(this);
            loadUserTask.execute(Request.to(Endpoints.USER_DETAILS, user.getId()));
        }
    }

    private void toggleFollowing(PublicApiUser user) {
        followingOperations.toggleFollowing(user)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ToggleFollowSubscriber(toggleFollow));
        setFollowersMessage();
    }

    private void setFollowersMessage() {
        if (followerMessage != null) {
            if (isLoggedInUser()) {
                followerMessage.setVisibility(View.GONE);
            } else {
                followerMessage.setVisibility(View.VISIBLE);
                followerMessage.setText(generateFollowersMessage());
            }
        }
    }

    private String generateFollowersMessage() {
        if (toggleFollow.isChecked()) {
            return ScTextUtils.formatFollowingMessage(getResources(), initialOtherFollowers);
        } else {
            return ScTextUtils.formatFollowersMessage(getResources(), initialOtherFollowers);
        }
    }

    private Configuration toConfiguration() {
        Configuration c = new Configuration();
        c.loadUserTask = loadUserTask;
        c.user = user;
        c.pagerIndex = pager.getCurrentItem();
        return c;
    }

    private void fromConfiguration(Configuration c) {
        setUser(c.user);

        if (c.loadUserTask != null) {
            loadUserTask = c.loadUserTask;
        }
        pager.setCurrentItem(c.pagerIndex);
    }

    public enum Tab {
        details(Actions.YOUR_INFO, Screen.USER_INFO, Screen.YOUR_INFO, Content.USER, Content.ME, R.string.tab_title_user_info),
        tracks(Actions.YOUR_SOUNDS, Screen.USER_POSTS, Screen.YOUR_POSTS, Content.USER_SOUNDS, Content.ME_SOUNDS, R.string.tab_title_user_sounds),
        sets(Actions.YOUR_SETS, Screen.USER_PLAYLISTS, Screen.YOUR_PLAYLISTS, Content.USER_PLAYLISTS, Content.ME_PLAYLISTS, R.string.tab_title_user_playlists),
        likes(Actions.YOUR_LIKES, Screen.USER_LIKES, Screen.YOUR_LIKES, Content.USER_LIKES, Content.ME_LIKES, R.string.tab_title_user_likes),
        followings(Actions.YOUR_FOLLOWINGS, Screen.USER_FOLLOWINGS, Screen.YOUR_FOLLOWINGS, Content.USER_FOLLOWINGS, Content.ME_FOLLOWINGS, R.string.tab_title_user_followings),
        followers(Actions.YOUR_FOLLOWERS, Screen.USER_FOLLOWERS, Screen.YOUR_FOLLOWERS, Content.USER_FOLLOWERS, Content.ME_FOLLOWERS, R.string.tab_title_user_followers);

        public static final String EXTRA = "userBrowserTag";

        public final Screen userScreen, youScreen;
        public final Content userContent, youContent;
        public final int userTitle;
        public final String tag;
        public final String action;

        Tab(String action, Screen userScreen, Screen youScreen, Content userContent, Content youContent, int userTitle) {
            this.action = action;
            this.userScreen = userScreen;
            this.youScreen = youScreen;
            this.userContent = userContent;
            this.youContent = youContent;
            this.userTitle = userTitle;
            this.tag = this.name();
        }

        public static int indexOf(String tag) {
            for (int i = 0; i < values().length; i++) {
                if (values()[i].tag.equalsIgnoreCase(tag)) {
                    return i;
                }
            }
            return -1;
        }

        public static Tab fromAction(String needle) {
            for (Tab t : values()) {
                if (t.action.equals(needle)) {
                    return t;
                }
            }
            return null;
        }

        public static String getTitle(Resources resources, int position, boolean isYou) {
            return resources.getString(Tab.values()[position].userTitle);
        }
    }

    private static class Configuration {
        FetchUserTask loadUserTask;
        PublicApiUser user;
        int pagerIndex;
    }

    class UserFragmentAdapter extends FragmentPagerAdapter {
        public UserFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Tab currentTab = Tab.values()[position];
            if (currentTab == Tab.details) {
                return userInfoFragment;
            } else {
                // don't ask me why the user could be null here. No time for this dead code - JS
                final long id = user == null ? Consts.NOT_SET : user.getId();
                final Content content = isLoggedInUser() ? currentTab.youContent : currentTab.userContent;
                final SearchQuerySourceInfo searchQuerySourceForTab = searchQuerySourceForTab(currentTab);
                final Uri uri = isLoggedInUser() ? content.uri : currentTab.userContent.forId(id);
                final Screen youScreen = isLoggedInUser() ? currentTab.youScreen : currentTab.userScreen;
                final String username = user == null ? ScTextUtils.EMPTY_STRING : user.username;
                return profileListFragmentCreator.create(ProfileActivity.this, content, user.getUrn(), username, uri, youScreen, searchQuerySourceForTab);
            }
        }

        @Override
        public int getCount() {
            return Tab.values().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return Tab.getTitle(getResources(), position, isLoggedInUser());
        }

        private SearchQuerySourceInfo searchQuerySourceForTab(Tab tab) {
            if (tab == Tab.followers || tab == Tab.followings) {
                return null;
            }
            return searchQuerySourceInfo;
        }
    }
}
