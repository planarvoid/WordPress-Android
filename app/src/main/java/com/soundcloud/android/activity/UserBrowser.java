package com.soundcloud.android.activity;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.EventAware;
import com.soundcloud.android.tracking.Level2;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.viewpagerindicator.TitlePageIndicator;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import java.util.List;

public class UserBrowser extends ScActivity implements
        FollowStatus.Listener,
        FetchUserTask.FetchUserListener,
        EventAware, ActionBar.OnNavigationListener {

    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_USER = "user";
    /* package */ @Nullable User mUser;

    private TextView mUsername, mLocation, mFullName, mWebsite, mDiscogsName, mMyspaceName, mDescription, mFollowerCount, mTrackCount;
    private View mVrStats;
    private ImageView mIcon;
    private String mIconURL;
    private ImageLoader.BindResult avatarResult;

    private EmptyCollection mEmptyInfoView;
    boolean mDisplayedInfo, mInfoError;

    private FrameLayout mInfoView;
    private FetchUserTask mLoadUserTask;
    private boolean mUpdateInfo;

    private List<Connection> mConnections;
    private Object[] mAdapterStates;

    private UserFragmentAdapter mAdapter;
    protected ViewPager mPager;
    private TitlePageIndicator mIndicator;


    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        return false;
    }

    public enum Tab {
        //details(Page.Users_info, Page.You_info, Content.USER, Content.ME, R.string.tab_title_user_info, R.string.tab_title_my_info),
        tracks(Page.Users_sounds, Page.You_sounds, Content.USER_TRACKS, Content.ME_TRACKS, R.string.tab_title_user_sounds, R.string.tab_title_my_sounds),
        likes(Page.Users_likes, Page.You_likes, Content.USER_LIKES, Content.ME_LIKES, R.string.tab_title_user_likes, R.string.tab_title_my_likes),
        followings(Page.Users_following, Page.You_following, Content.USER_FOLLOWINGS, Content.ME_FOLLOWINGS, R.string.tab_title_user_followings, R.string.tab_title_my_followings),
        followers(Page.Users_followers, Page.You_followers, Content.USER_FOLLOWERS, Content.ME_FOLLOWERS, R.string.tab_title_user_followers, R.string.tab_title_my_followers);

        public static final String EXTRA = "userBrowserTag";

        public final Page userPage, youPage;
        public final Content userContent, youContent;
        public final int userTitle, youTitle;
        public final String tag;

        Tab(Page userPage, Page youPage, Content userContent, Content youContent, int userTitle, int youTitle) {
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
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.user_browser);

        mInfoView = (FrameLayout) getLayoutInflater().inflate(R.layout.user_browser_details_view, null);

        mIcon = (ImageView) findViewById(R.id.user_icon);
        mUsername = (TextView) findViewById(R.id.username);
        mFullName = (TextView) findViewById(R.id.fullname);

        mFollowerCount = (TextView) findViewById(R.id.followers);
        mTrackCount = (TextView) findViewById(R.id.tracks);
        mVrStats = findViewById(R.id.vr_stats);

        AndroidUtils.setTextShadowForGrayBg(mUsername);
        AndroidUtils.setTextShadowForGrayBg(mFullName);
        AndroidUtils.setTextShadowForGrayBg(mFollowerCount);
        AndroidUtils.setTextShadowForGrayBg(mTrackCount);

        mLocation = (TextView) mInfoView.findViewById(R.id.location);
        mWebsite = (TextView) mInfoView.findViewById(R.id.website);
        mDiscogsName = (TextView) mInfoView.findViewById(R.id.discogs_name);
        mMyspaceName = (TextView) mInfoView.findViewById(R.id.myspace_name);
        mDescription = (TextView) mInfoView.findViewById(R.id.description);

        mIcon.setScaleType(ScaleType.CENTER_INSIDE);
        if (getResources().getDisplayMetrics().density > 1 || ImageUtils.isScreenXL(this)) {
            mIcon.getLayoutParams().width = 100;
            mIcon.getLayoutParams().height = 100;
        }

        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ImageUtils.checkIconShouldLoad(mIconURL)) {
                    new FullImageDialog(
                        UserBrowser.this,
                        Consts.GraphicSize.CROP.formatUri(mIconURL)
                    ).show();
                }

            }
        });

        mAdapter = new UserFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setBackgroundColor(Color.WHITE);

        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);


        Intent intent = getIntent();
        // XXX in case user is already loaded - should be handled here, not in caller
        mUpdateInfo = intent.getBooleanExtra("updateInfo",true);

        Configuration c = (Configuration) getLastCustomNonConfigurationInstance();
        if (c != null) {
            fromConfiguration(c);
        } else {
            if (intent.hasExtra(EXTRA_USER)) {
                loadUserByObject((User) intent.getParcelableExtra(EXTRA_USER));
            } else if (intent.hasExtra(EXTRA_USER_ID)) {
                loadUserById(intent.getLongExtra(EXTRA_USER_ID, -1));
            } else if (intent.getData() == null || !loadUserByUri(intent.getData())){
                loadYou();
            }

            if (!isYou()) FollowStatus.get(this).requestUserFollowings(this);

            if (intent.hasExtra(Tab.EXTRA)) {
                mPager.setCurrentItem(Tab.indexOf(intent.getStringExtra(Tab.EXTRA)));
                intent.removeExtra(Tab.EXTRA);
            }
        }

        loadDetails();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecordListener);
    }


    @Override
    protected int getSelectedMenuId() {
        return -1;
    }


    class UserFragmentAdapter extends FragmentPagerAdapter {
        public UserFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public ScListFragment getItem(int position) {
            return ScListFragment.newInstance(isYou() ? Tab.values()[position].youContent.uri : Tab.values()[position].userContent.forId(mUser.id));

        }

        @Override
        public int getCount() {
            return Tab.values().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getString(isYou() ? Tab.values()[position].youTitle : Tab.values()[position].userTitle);
        }
    }

    private void follow(User user) {
        getApp().track(Click.Follow, user, Level2.Users);
        toggleFollowing(user);
    }

    private void unfollow(User user) {
        getApp().track(Click.Unfollow, user, Level2.Users);
        toggleFollowing(user);
    }

    @Override
    protected void onResume() {
        trackScreen();
        super.onResume();
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
        if (!mUpdateInfo) return;

        if (mLoadUserTask == null) {
            mLoadUserTask = new FetchUserTask(getApp(), mUser.id);
            mLoadUserTask.addListener(this);
            mLoadUserTask.execute(Request.to(Endpoints.USER_DETAILS, mUser.id));
        }
    }

    public void onChange(boolean success, FollowStatus status) {
        invalidateOptionsMenu();
    }

    private void trackScreen() {
        track(getEvent(), mUser);
    }

    public Page getEvent() {
        //Tab current = Tab.valueOf(mUserlistBrowser.getCurrentTag());
        //return isYou() ? current.you : current.user;
        return Page.Users_sounds;
    }

    private boolean isOtherUser() {
        return !isYou();
    }

    protected boolean isYou() {
       return mUser != null && mUser.id == getCurrentUserId();
    }

    private void toggleFollowing(User user) {
        FollowStatus.get(this).toggleFollowing(user, getApp(), new Handler() {
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
        invalidateOptionsMenu();
    }

    @Override
    public void onSuccess(User user, String action) {
        mInfoError = false;
        setUser(user);
    }

    @Override
    public void onError(long userId) {
        mInfoError = true;
        if (!mDisplayedInfo) {
            configureEmptyView();
        }
    }

    private void setUser(final User user) {
        if (user == null || user.id < 0) return;
        mUser = user;

        if (!TextUtils.isEmpty(user.username)) mUsername.setText(user.username);
        if (TextUtils.isEmpty(user.full_name)){
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
            mTrackCount.setText(Integer.toString(user.track_count));
        }

        if (user.followers_count <= 0) {
            mFollowerCount.setVisibility(View.GONE);
        } else {
            mFollowerCount.setVisibility(View.VISIBLE);
            mFollowerCount.setText(Integer.toString(user.followers_count));
        }

        invalidateOptionsMenu();

        if (user.shouldLoadIcon()) {
            if (mIconURL == null
                || avatarResult == BindResult.ERROR
                || !user.avatar_url.substring(0, user.avatar_url.indexOf("?")).equals(mIconURL.substring(0, mIconURL.indexOf("?")))) {
                mIconURL = user.avatar_url;
                reloadAvatar();
            }
        }

        if (!TextUtils.isEmpty(user.website)) {
            mDisplayedInfo = true;
            mWebsite.setText(
                    TextUtils.isEmpty(user.website_title) ?
                    user.website.replace("http://www.", "").replace("http://", "") : user.website_title);
            mWebsite.setVisibility(View.VISIBLE);
            mWebsite.setFocusable(true);
            mWebsite.setClickable(true);
            mWebsite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.website));
                    startActivity(viewIntent);
                }
            });
        } else {
            mWebsite.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(user.discogs_name)) {
            mDisplayedInfo = true;
            mDiscogsName.setMovementMethod(LinkMovementMethod.getInstance());
            mDiscogsName.setVisibility(View.VISIBLE);
            mDiscogsName.setFocusable(true);
            mDiscogsName.setClickable(true);
            mDiscogsName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://www.discogs.com/artist/"+user.discogs_name));
                    startActivity(viewIntent);
                }
            });
        } else {
            mDiscogsName.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(user.myspace_name)) {
            mDisplayedInfo = true;
            mMyspaceName.setMovementMethod(LinkMovementMethod.getInstance());
            mMyspaceName.setVisibility(View.VISIBLE);
            mMyspaceName.setFocusable(true);
            mMyspaceName.setClickable(true);
            mMyspaceName.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent viewIntent =
                            new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://www.myspace.com/"+user.myspace_name));
                    startActivity(viewIntent);
                }
            });
        } else {
            mMyspaceName.setVisibility(View.GONE);
        }

        final String location = user.getLocation();
        if (!TextUtils.isEmpty(location)) {
            mDisplayedInfo = true;
            mLocation.setText(getString(R.string.from)+" "+location);
            mLocation.setVisibility(View.VISIBLE);
        } else {
            mLocation.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(user.description)) {
            mDisplayedInfo = true;
            mDescription.setText(ScTextUtils.fromHtml(user.description));
            mDescription.setMovementMethod(LinkMovementMethod.getInstance());
        }
        configureEmptyView();
    }

    private void configureEmptyView(){
        if (mDisplayedInfo && mEmptyInfoView != null && mEmptyInfoView.getParent() == mInfoView) {
            mInfoView.removeView(mEmptyInfoView);
        } else if (!mDisplayedInfo) {
            if (mEmptyInfoView == null) mEmptyInfoView = new EmptyCollection(this);
            if (mInfoError) {
                mEmptyInfoView.setMessageText(R.string.info_error);
                mEmptyInfoView.setImage(R.drawable.empty_connection);
                mEmptyInfoView.setActionText(-1);
            } else {
                if (isOtherUser()) {
                    mEmptyInfoView.setMessageText(R.string.info_empty_other_message);
                    mEmptyInfoView.setActionText(-1);
                } else {
                    mEmptyInfoView.setMessageText(R.string.info_empty_you_message);
                    mEmptyInfoView.setActionText(R.string.info_empty_you_action);
                    mEmptyInfoView.setButtonActionListener(new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/settings")));
                        }

                        @Override
                        public void onSecondaryAction() {
                        }
                    });
                }
            }

            if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                // won't fit in most landscape views
                mEmptyInfoView.setImageVisibility(false);
                mEmptyInfoView.setActionText(-1);
            }

            if (mEmptyInfoView.getParent() != mInfoView) {
                mInfoView.addView(mEmptyInfoView);
            }
        }

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
        c.connections = mConnections;
        c.pagerIndex = mPager.getCurrentItem();
        c.infoError = mInfoError;
        return c;
    }

    private void fromConfiguration(Configuration c){
        mInfoError = c.infoError;
        setUser(c.user);

        if (c.loadUserTask != null) {
            mLoadUserTask = c.loadUserTask;
        }
        if (isYou()) mConnections = c.connections;
        mPager.setCurrentItem(c.pagerIndex);
    }

    private static class Configuration {
        FetchUserTask loadUserTask;
        User user;
        List<Connection> connections;
        int pagerIndex;
        boolean infoError;
    }

    private boolean isFollowing(){
        return mUser != null && FollowStatus.get(this).isFollowing(mUser);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (!isYou()){
            MenuItem followItem = menu.findItem(R.id.action_bar_follow);
            final boolean following = isFollowing();
            followItem.setIcon(following ? R.drawable.ic_remove_user_white : R.drawable.ic_add_user_white);
            followItem.setTitle(getResources().getString(following ? R.string.action_bar_unfollow : R.string.action_bar_follow));
        } else {
            menu.removeItem(R.id.action_bar_follow);
        }
        return true;
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.user_browser;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bar_follow:
                if (mUser.user_following){
                    follow(mUser);
                } else {
                    unfollow(mUser);
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final BroadcastReceiver mRecordListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                invalidateOptionsMenu();
            }
    };
}
