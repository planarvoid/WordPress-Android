package com.soundcloud.android.activity;

import static android.text.TextUtils.isEmpty;
import static com.soundcloud.android.utils.AndroidUtils.setTextShadowForGrayBg;

import com.actionbarsherlock.app.ActionBar;
import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.OldCloudAPI;
import com.soundcloud.android.operations.following.FollowStatus;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.fragment.UserDetailsFragment;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.service.sync.SyncInitiator;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.EventAware;
import com.soundcloud.android.tracking.Level2;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.android.utils.images.ImageSize;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.android.view.EmptyListViewFactory;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.viewpagerindicator.TitlePageIndicator;
import org.jetbrains.annotations.Nullable;

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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class UserBrowser extends ScActivity implements
        FollowStatus.Listener,
        EventAware, ActionBar.OnNavigationListener, FetchModelTask.Listener<User> {

    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_USER = "user";

    /* package */ @Nullable User mUser;

    private TextView mUsername, mFullName, mFollowerCount, mTrackCount;
    private ToggleButton mToggleFollow;
    private View mVrStats;
    private ImageView mIcon;
    private String mIconURL;
    private ImageLoader.BindResult avatarResult;
    private FollowStatus mFollowStatus;
    private UserFragmentAdapter mAdapter;
    private FetchUserTask mLoadUserTask;
    protected ViewPager mPager;
    protected TitlePageIndicator mIndicator;

    private boolean mDelayContent;

    private UserDetailsFragment mUserDetailsFragment;
    private AndroidCloudAPI mOldCloudAPI;
    private AccountOperations mAccountOperations;
    private FollowingOperations mFollowingOperations;

    public static boolean startFromPlayable(Context context, Playable playable) {
        if (playable != null) {
            context.startActivity(
                    new Intent(context, UserBrowser.class)
                            .putExtra("userId", playable.getUserId()));
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.user_browser);
        mOldCloudAPI = new OldCloudAPI(this);
        mFollowStatus = FollowStatus.get();
        mFollowingOperations = new FollowingOperations();
        mAccountOperations = new AccountOperations(this);
        mIcon = (ImageView) findViewById(R.id.user_icon);
        mUsername = (TextView) findViewById(R.id.username);
        mFullName = (TextView) findViewById(R.id.fullname);

        mFollowerCount = (TextView) findViewById(R.id.followers);
        mTrackCount = (TextView) findViewById(R.id.tracks);
        mVrStats = findViewById(R.id.vr_stats);

        setTextShadowForGrayBg(mUsername, mFullName, mFollowerCount, mTrackCount);

        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ImageUtils.checkIconShouldLoad(mIconURL)) {
                    new FullImageDialog(UserBrowser.this, ImageSize.CROP.formatUri(mIconURL)).show();
                }

            }
        });
        mToggleFollow = (ToggleButton) findViewById(R.id.toggle_btn_follow);

        // if root view is expanded, wait to instantiate the fragments until it is closed as it causes severe jank
        mDelayContent = mRootView.isExpanded() && bundle == null;

        mAdapter = new UserFragmentAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mDelayContent ? new TempAdapter() : mAdapter);
        mPager.setCurrentItem(Tab.tracks.ordinal());
        mPager.setBackgroundColor(Color.WHITE);

        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);


        Intent intent = getIntent();
        Configuration c = (Configuration) getLastCustomNonConfigurationInstance();
        if (c != null) {
            fromConfiguration(c);
        } else {
            handleIntent(intent);
        }

        if (mUser != null) {
            mUserDetailsFragment = UserDetailsFragment.newInstance(mUser.getId());

            if (isYou()){
                mToggleFollow.setVisibility(View.GONE);
            } else {
                mToggleFollow.setChecked(mFollowStatus.isFollowing(mUser));
                mToggleFollow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleFollowing(mUser);
                        getApp().track(mUser.user_following ? Click.Follow : Click.Unfollow, mUser, Level2.Users);
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
    public void onMenuClosed() {
        super.onMenuClosed();

        if (mDelayContent){
            mDelayContent = false;
            // store selected item to restore on new adapter
            final int currentItem = mPager.getCurrentItem();
            mPager.setAdapter(mAdapter);
            mPager.setCurrentItem(currentItem, false);
        }
    }

    protected void handleIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_USER)) {
            loadUserByObject((User) intent.getParcelableExtra(EXTRA_USER));
        } else if (intent.hasExtra(EXTRA_USER_ID)) {
            loadUserById(intent.getLongExtra(EXTRA_USER_ID, -1));
        } else if (intent.getData() == null || !loadUserByUri(intent.getData())){
            loadYou();
        }

        if (!isYou()) mFollowStatus.requestUserFollowings(this);

        if (intent.hasExtra(Tab.EXTRA)) {
            mPager.setCurrentItem(Tab.indexOf(intent.getStringExtra(Tab.EXTRA)));
            intent.removeExtra(Tab.EXTRA);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // update action bar record option based on recorder status
        invalidateOptionsMenu();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SoundRecorder.RECORD_STARTED);
        filter.addAction(SoundRecorder.RECORD_ERROR);
        filter.addAction(SoundRecorder.RECORD_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRecordListener, filter);
    }

    @Override
    protected void onResume() {
        trackScreen();
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecordListener);
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
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
     protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (isConnected && avatarResult == BindResult.ERROR) reloadAvatar();
    }

    private void loadYou() {
        setUser(getApp().getLoggedInUser());
    }

    private void loadUserById(long userId) {
        if (userId != -1) {
            // check DB first as the cached user might be incomplete
            final User u = SoundCloudApplication.MODEL_MANAGER.getUser(userId);
            setUser(u != null ? u : SoundCloudApplication.MODEL_MANAGER.getCachedUser(userId));
        }
        if (mUser == null) {
            mUser = new User();
            mUser.setId(userId);
        }
    }

    private boolean loadUserByUri(Uri uri) {
        if (uri != null) {
            mUser = new UserStorage().getUserByUri(uri); //FIXME: DB access on UI thread
            if (mUser == null) {
                loadUserById(UriUtils.getLastSegmentAsLong(uri));
            }
        }
        return mUser != null;
    }

    private void loadUserByObject(User user) {
        if (user == null || user.getId() == -1) return;

        // show a user out of db if possible because he will be a complete user unlike
        // a parceled user that came from a track, list or comment
        final User dbUser = SoundCloudApplication.MODEL_MANAGER.getUser(user.getId());
        setUser(dbUser != null ? dbUser : user);
    }

    private void loadDetails() {
        if (mLoadUserTask == null && mUser != null) {
            mLoadUserTask = new FetchUserTask(mOldCloudAPI);
            mLoadUserTask.addListener(this);
            mLoadUserTask.execute(Request.to(Endpoints.USER_DETAILS, mUser.getId()));
        }
    }

    public void onFollowChanged() {
        mToggleFollow.setChecked(mFollowStatus.isFollowing(mUser));
    }

    private void trackScreen() {
        track(getEvent(), mUser);
    }

    public Page getEvent() {
        //Tab current = Tab.valueOf(mUserlistBrowser.getCurrentTag());
        //return isYou() ? current.you : current.user;
        return Page.Users_sounds;
    }

    protected boolean isYou() {
       return mUser != null && mUser.getId() == getCurrentUserId();
    }

    private void toggleFollowing(User user) {
        mFollowingOperations.toggleFollowing(user);
        SyncInitiator.pushFollowingsToApi(mAccountOperations.getSoundCloudAccount());
    }

    @Override
    public void onSuccess(User user) {
        user.last_updated = System.currentTimeMillis();
        setUser(user);

        // update user locally and ensure 1 instance
        mUser = SoundCloudApplication.MODEL_MANAGER.cache(user, ScResource.CacheUpdateMode.FULL);
        //FIXME: This will be handled/scheduled by an Observable when we're done refactoring storage
        new Thread(new Runnable() {
            @Override
            public void run() {
                new UserStorage().create(mUser);
            }
        }).start();
        mUserDetailsFragment.onSuccess(mUser);
    }

    @Override
    public void onError(Object context) {
        mUserDetailsFragment.onError();
    }

    private void setUser(final User user) {
        if (user == null || user.getId() < 0) return;
        mUser = user;

        if (!isEmpty(user.username)) mUsername.setText(user.username);

        if (isEmpty(user.full_name)) {
            mFullName.setVisibility(View.GONE);
        } else {
            mFullName.setText(user.full_name);
            mFullName.setVisibility(View.VISIBLE);
        }

        mVrStats.setVisibility((user.followers_count <= 0 || user.track_count <= 0) ? View.GONE : View.VISIBLE);

        if (user.track_count <= 0) {
            mTrackCount.setVisibility(View.GONE);
        } else {
            mTrackCount.setVisibility(View.VISIBLE);
            mTrackCount.setText(String.valueOf(user.track_count));
        }

        if (user.followers_count <= 0) {
            mFollowerCount.setVisibility(View.GONE);
        } else {
            mFollowerCount.setVisibility(View.VISIBLE);
            mFollowerCount.setText(String.valueOf(user.followers_count));
        }

        invalidateOptionsMenu();

        if (user.shouldLoadIcon()) {
            if (mIconURL == null
                || avatarResult == BindResult.ERROR
                || (user.avatar_url != null && !mIconURL.equals(user.avatar_url))) {
                mIconURL = user.avatar_url;

                reloadAvatar();
            }
        }

    }


    public User getUser() {
        return mUser;
    }

    private void reloadAvatar() {
        if (ImageUtils.checkIconShouldLoad(mIconURL)) {
            if ((avatarResult = ImageUtils.loadImageSubstitute(this,mIcon,mIconURL, ImageSize.LARGE,new ImageLoader.Callback() {
                @Override
                public void onImageLoaded(ImageView view, String url) {}

                @Override
                public void onImageError(ImageView view, String url, Throwable error) {
                    avatarResult = BindResult.ERROR;
                }
            }, null)) != BindResult.OK) {
                mIcon.setImageDrawable(getResources().getDrawable(R.drawable.avatar_badge_large));
            }
        }
    }

    private Configuration toConfiguration() {
        Configuration c = new Configuration();
        c.loadUserTask = mLoadUserTask;
        c.user = mUser;
        c.pagerIndex = mPager.getCurrentItem();
        return c;
    }

    private void fromConfiguration(Configuration c){
        setUser(c.user);

        if (c.loadUserTask != null) {
            mLoadUserTask = c.loadUserTask;
        }
        mPager.setCurrentItem(c.pagerIndex);
    }

    private static class Configuration {
        FetchUserTask loadUserTask;
        User user;
        int pagerIndex;
    }

    private final BroadcastReceiver mRecordListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                invalidateOptionsMenu();
            }
    };


    public enum Tab {
        details(Actions.YOUR_INFO, Page.Users_info, Page.You_info, Content.USER, Content.ME, R.string.tab_title_user_info, R.string.tab_title_my_info),
        tracks(Actions.YOUR_SOUNDS,Page.Users_sounds, Page.You_sounds, Content.USER_SOUNDS, Content.ME_SOUNDS, R.string.tab_title_user_sounds, R.string.tab_title_my_sounds),
        sets(Actions.YOUR_SETS,Page.Users_sets, Page.You_sets, Content.USER_PLAYLISTS, Content.ME_PLAYLISTS, R.string.tab_title_user_sets, R.string.tab_title_my_sets),
        likes(Actions.YOUR_LIKES,Page.Users_likes, Page.You_likes, Content.USER_LIKES, Content.ME_LIKES, R.string.tab_title_user_likes, R.string.tab_title_my_likes),
        followings(Actions.YOUR_FOLLOWINGS,Page.Users_following, Page.You_following, Content.USER_FOLLOWINGS, Content.ME_FOLLOWINGS, R.string.tab_title_user_followings, R.string.tab_title_my_followings),
        followers(Actions.YOUR_FOLLOWERS, Page.Users_followers, Page.You_followers, Content.USER_FOLLOWERS, Content.ME_FOLLOWERS, R.string.tab_title_user_followers, R.string.tab_title_my_followers);

        public static final String EXTRA = "userBrowserTag";

        public final Page userPage, youPage;
        public final Content userContent, youContent;
        public final int userTitle, youTitle;
        public final String tag;
        public final String action;

        Tab(String action, Page userPage, Page youPage, Content userContent, Content youContent, int userTitle, int youTitle) {
            this.action = action;
            this.userPage = userPage;
            this.youPage = youPage;
            this.userContent = userContent;
            this.youContent = youContent;
            this.userTitle = userTitle;
            this.youTitle = youTitle;
            this.tag = this.name();
        }

        public static int indexOf(String tag) {
            for (int i = 0; i < values().length; i++)
                if (values()[i].tag.equalsIgnoreCase(tag)) {
                    return i;
                }
            return -1;
        }

        public static Tab fromAction(String needle) {
            for (Tab t : values()){
                if (t.action.equals(needle)) return t;
            }
            return null;
        }

        public static String getTitle(Resources resources, int position, boolean isYou){
            return resources.getString(isYou ? Tab.values()[position].youTitle : Tab.values()[position].userTitle);
        }
    }

    class UserFragmentAdapter extends FragmentPagerAdapter {
        public UserFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Tab currentTab = Tab.values()[position];
            if (currentTab == Tab.details){
                return mUserDetailsFragment;
            } else {
                Content content;
                Uri contentUri;
                if (isYou()) {
                    content = currentTab.youContent;
                    contentUri = content.uri;
                } else {
                    content = currentTab.userContent;
                    contentUri = content.forId(mUser.getId());
                }
                ScListFragment listFragment = ScListFragment.newInstance(contentUri);
                listFragment.setEmptyViewFactory(new EmptyListViewFactory().forContent(UserBrowser.this, content, mUser));
                return listFragment;
            }
        }

        @Override
        public int getCount() {
            return Tab.values().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return Tab.getTitle(getResources(),position,isYou());
        }
    }

    class TempAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return Tab.values().length;
        }
        @Override
        public CharSequence getPageTitle(int position) {
            return Tab.getTitle(getResources(),position,isYou());
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return new View(UserBrowser.this);
        }
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }

        @Override
        public void destroyItem(View collection, int position, Object view) {
            ((ViewPager) collection).removeView((View) view);
        }
    }
}
