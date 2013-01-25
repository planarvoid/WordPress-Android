package com.soundcloud.android.activity;

import static android.text.TextUtils.isEmpty;
import static com.soundcloud.android.utils.AndroidUtils.setTextShadowForGrayBg;

import com.actionbarsherlock.app.ActionBar;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.fragment.UserDetailsFragment;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.EventAware;
import com.soundcloud.android.tracking.Level2;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.EmptyListView;
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
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.user_browser);

        mFollowStatus = FollowStatus.get(this);

        mIcon = (ImageView) findViewById(R.id.user_icon);
        mUsername = (TextView) findViewById(R.id.username);
        mFullName = (TextView) findViewById(R.id.fullname);

        mFollowerCount = (TextView) findViewById(R.id.followers);
        mTrackCount = (TextView) findViewById(R.id.tracks);
        mVrStats = findViewById(R.id.vr_stats);

        setTextShadowForGrayBg(mUsername, mFullName, mFollowerCount, mTrackCount);

        if (getResources().getDisplayMetrics().density > 1 || ImageUtils.isScreenXL(this)) {
            mIcon.getLayoutParams().width = 100;
            mIcon.getLayoutParams().height = 100;
        }

        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ImageUtils.checkIconShouldLoad(mIconURL)) {
                    new FullImageDialog(UserBrowser.this, Consts.GraphicSize.CROP.formatUri(mIconURL)).show();
                }

            }
        });
        mToggleFollow = (ToggleButton) findViewById(R.id.toggle_btn_follow);

        // if root view is expanded, wait to instantiate the fragments until it is closed as it causes severe jank
        mDelayContent = mRootView.isExpanded();

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
            mUserDetailsFragment = UserDetailsFragment.newInstance(mUser.id);

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
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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


    class UserFragmentAdapter extends FragmentPagerAdapter {
        public UserFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (Tab.values()[position] == Tab.details){
                return mUserDetailsFragment;
            } else {
                ScListFragment listFragment = ScListFragment.newInstance(isYou() ?
                        Tab.values()[position].youContent.uri : Tab.values()[position].userContent.forId(mUser.id));
                listFragment.setEmptyCollection(getEmptyScreenFromContent(position));
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
            final View v = View.inflate(UserBrowser.this, R.layout.empty_list,null);
            container.addView(v);
            return v;
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

    private EmptyListView getEmptyScreenFromContent(int position) {
        switch (isYou() ? Tab.values()[position].youContent : Tab.values()[position].userContent){
            case ME_SOUNDS:
                return new EmptyListView(this, new Intent(Actions.RECORD))
                        .setMessageText(R.string.list_empty_user_sounds_message)
                        .setActionText(R.string.list_empty_user_sounds_action)
                        .setImage(R.drawable.empty_rec);

            case USER_SOUNDS:
                return new EmptyListView(this).setMessageText(getString(R.string.empty_user_tracks_text,
                        mUser == null || mUser.username == null ? getString(R.string.this_user)
                                : mUser.username));

            case ME_LIKES:
                return new EmptyListView(this,
                        new Intent(Actions.WHO_TO_FOLLOW),
                        new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101"))
                ).setMessageText(R.string.list_empty_user_likes_message)
                        .setActionText(R.string.list_empty_user_likes_action)
                        .setImage(R.drawable.empty_like);

            case USER_LIKES:
                return new EmptyListView(this).setMessageText(getString(R.string.empty_user_likes_text,
                        mUser == null || mUser.username == null ? getString(R.string.this_user)
                                : mUser.username));

            case ME_FOLLOWERS:
                User loggedInUser = getApp().getLoggedInUser();
                if (loggedInUser == null || loggedInUser.track_count > 0) {
                    return new EmptyListView(this, new Intent(Actions.YOUR_SOUNDS))
                            .setMessageText(R.string.list_empty_user_followers_message)
                            .setActionText(R.string.list_empty_user_followers_action)
                            .setImage(R.drawable.empty_rec);
                } else {
                    return new EmptyListView(this, new Intent(Actions.RECORD))
                            .setMessageText(R.string.list_empty_user_followers_nosounds_message)
                            .setActionText(R.string.list_empty_user_followers_nosounds_action)
                            .setImage(R.drawable.empty_share);
                }

            case USER_FOLLOWERS:
                return new EmptyListView(this)
                        .setMessageText(getString(R.string.empty_user_followers_text,
                                mUser == null || mUser.username == null ? getString(R.string.this_user)
                                        : mUser.username));

            case ME_FOLLOWINGS:
                return new EmptyListView(this, new Intent(Actions.WHO_TO_FOLLOW))
                        .setMessageText(R.string.list_empty_user_following_message)
                        .setActionText(R.string.list_empty_user_following_action)
                        .setImage(R.drawable.empty_follow_3row);

            case USER_FOLLOWINGS:
                return new EmptyListView(this).setMessageText(getString(R.string.empty_user_followings_text,
                        mUser == null || mUser.username == null ? getString(R.string.this_user)
                                : mUser.username));
            default:
                return new EmptyListView(this);
        }
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
            mUser.id = userId;
        }
    }

    private boolean loadUserByUri(Uri uri) {
        if (uri != null) mUser = User.fromUri(uri, getContentResolver(), true);
        return (mUser != null);
    }

    private void loadUserByObject(User user) {
        if (user == null || user.id == -1) return;

        // show a user out of db if possible because he will be a complete user unlike
        // a parceled user that came from a track, list or comment
        final User dbUser = SoundCloudApplication.MODEL_MANAGER.getUser(user.id);
        setUser(dbUser != null ? dbUser : user);
    }

    private void loadDetails() {
        if (mLoadUserTask == null && mUser != null) {
            mLoadUserTask = new FetchUserTask(getApp());
            mLoadUserTask.addListener(this);
            mLoadUserTask.execute(Request.to(Endpoints.USER_DETAILS, mUser.id));
        }
    }

    public void onFollowChanged(boolean success) {
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
       return mUser != null && mUser.id == getCurrentUserId();
    }

    private void toggleFollowing(User user) {
        mFollowStatus.toggleFollowing(user, getApp(), new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what != FollowStatus.FOLLOW_STATUS_SUCCESS) {
                    invalidateOptionsMenu();

                    if (msg.what == FollowStatus.FOLLOW_STATUS_SPAM) {
                        AndroidUtils.showToast(UserBrowser.this, R.string.following_spam_warning);
                    } else {
                        AndroidUtils.showToast(UserBrowser.this, R.string.error_change_following_status);
                    }
                }
            }
        });
        mToggleFollow.setChecked(mFollowStatus.isFollowing(mUser));
    }

    @Override
    public void onSuccess(User user) {

        user.last_updated = System.currentTimeMillis();
        // update user locally and ensure 1 instance
        mUser = SoundCloudApplication.MODEL_MANAGER.cacheAndWrite(user, ScResource.CacheUpdateMode.FULL);

        setUser(user);
        mUserDetailsFragment.onSuccess(mUser);
    }

    @Override
    public void onError(Object context) {
        mUserDetailsFragment.onError();
    }

    private void setUser(final User user) {
        if (user == null || user.id < 0) return;
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
            if ((avatarResult = ImageUtils.loadImageSubstitute(this,mIcon,mIconURL, Consts.GraphicSize.LARGE,new ImageLoader.Callback() {
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
}
