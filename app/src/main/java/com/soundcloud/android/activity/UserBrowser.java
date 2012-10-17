package com.soundcloud.android.activity;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.create.ScUpload;
import com.soundcloud.android.cache.Connections;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.cache.ParcelCache;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScModelManager;
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
import com.soundcloud.android.view.NowPlayingIndicator;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.viewpagerindicator.TitlePageIndicator;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class UserBrowser extends ScListActivity implements
        ParcelCache.Listener<Connection>,
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
    private ViewPager mPager;
    private TitlePageIndicator mIndicator;


    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        return false;
    }

    public enum Tab {
        tracks(Page.Users_sounds, Page.You_sounds),
        favorites(Page.Users_likes, Page.You_likes),
        details(Page.Users_info, Page.You_info),
        followings(Page.Users_following, Page.You_following),
        followers(Page.Users_followers, Page.You_followers),
        friend_finder(null, Page.You_find_friends);

        public static final String EXTRA = "userBrowserTag";

        public final Page user, you;
        public final String tag;

        Tab(Page user, Page you) {
            this.user = user;
            this.you = you;
            this.tag = this.name();
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
            } else {
                loadYou();
            }

            build();

            if (!isMe()) FollowStatus.get().requestUserFollowings(getApp(), this, false);

            /*
            if (intent.hasExtra(Tab.EXTRA)) {
                mUserlistBrowser.initByTag(intent.getStringExtra(Tab.EXTRA));
            } else if (isMe()) {
                final int initialTab = getApp().getAccountDataInt(User.DataKeys.PROFILE_IDX);
                if (initialTab == -1) {
                    mUserlistBrowser.initWorkspace(1);//tracks tab
                } else {
                    mUserlistBrowser.initWorkspace(initialTab);
                }
            } else {
                mUserlistBrowser.initWorkspace(1);//tracks tab
            }
                          */
            if (isMe()) {
                mConnections = Connections.get().getObjectsOrNull();
                //mFriendFinderView.onConnections(mConnections, true);
                Connections.get().requestUpdate(getApp(), false, this);
            }
        }

        loadDetails();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_you;
    }


    class UserFragmentAdapter extends FragmentPagerAdapter {
        protected final Content[] my_contents = new Content[]{Content.ME_TRACKS, Content.ME_FAVORITES, Content.ME_FOLLOWERS, Content.ME_FOLLOWINGS};
        protected final int[] myTitleIds = new int[]{R.string.tab_title_my_sounds, R.string.tab_title_my_likes, R.string.tab_title_my_followers, R.string.tab_title_my_followings};

        protected final Content[] user_contents = new Content[]{Content.USER_TRACKS, Content.USER_FAVORITES, Content.USER_FOLLOWERS, Content.USER_FOLLOWINGS};
        protected final int[] userTitleIds = new int[]{R.string.tab_title_user_sounds, R.string.tab_title_user_likes, R.string.tab_title_user_followers, R.string.tab_title_user_followings};

            public UserFragmentAdapter(FragmentManager fm) {
                super(fm);
            }

            @Override
            public ScListFragment getItem(int position) {
                return ScListFragment.newInstance(isMe() ? my_contents[position].uri : user_contents[position].forId(mUser.id));

            }

            @Override
            public int getCount() {
                return isMe() ? my_contents.length : user_contents.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return getResources().getString(isMe() ? myTitleIds[position] : userTitleIds[position]);
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

    public void setTab(String tag) {
        //mUserlistBrowser.setCurrentScreenByTag(tag);
    }

    public boolean isShowingTab(Tab tab) {
        return false;
        //return mUserlistBrowser.getCurrentTag().equals(tab.tag);
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

    public void refreshConnections(){
        if (isMe()) {
            Connections.get().requestUpdate(getApp(), true, this);
            //if (mFriendFinderView != null) mFriendFinderView.setState(FriendFinderView.States.LOADING, true);
        }
    }

    public void onChanged(List<Connection> connections, ParcelCache<Connection> cache) {
        mConnections = connections;
        //mFriendFinderView.onConnections(connections, true);
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
        //return isMe() ? current.you : current.user;
        return Page.Users_sounds;
    }

    private void build() {

        //mUserlistBrowser = (UserlistLayout) findViewById(R.id.userlist_browser);
        final boolean isMe = isMe();

        getSupportActionBar().setTitle(mUser.username);
        /*
        // Tracks View
        ScBaseAdapter adp = isOtherUser() ?
                new ScBaseAdapter(this, Content.TRACK) :
                new MyTracksAdapter(this, Content.TRACK);

        LazyEndlessAdapter adpWrap = new RemoteCollectionAdapter(this, adp,
                isMe ?  Content.ME_TRACKS.uri : null,
                isMe ?  null : Request.to(Endpoints.USER_TRACKS, mUser.id), false);


        if (isMe) {
            adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_sounds_message)
                    .setActionText(R.string.list_empty_user_sounds_action)
                    .setImage(R.drawable.empty_rec)
                    .setButtonActionListener(new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            startActivity(new Intent(Actions.RECORD)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                        }

                        @Override
                        public void onSecondaryAction() {
                        }
                    }));
        } else {
            if (mUser != null) {
                adpWrap.setEmptyViewText(R.string.empty_user_tracks_text,
                        mUser.username == null ? getResources().getString(R.string.this_user)
                                : mUser.username);
            }
        }

        mMyTracksView = new ScTabView(this);
        mMyTracksView.setLazyListView(buildList(), adpWrap, Consts.ListId.LIST_USER_TRACKS, true);

        // Favorites View
        adp = new ScBaseAdapter(this, Content.TRACK);

        adpWrap = new RemoteCollectionAdapter(this, adp,
                isMe ?  Content.ME_FAVORITES.uri : null,//CloudUtils.replaceWildcard(Content.USER_FAVORITES.uri, mUser.id),
                isMe ?  null : Request.to(Endpoints.USER_FAVORITES, mUser.id), false);

        if (isMe) {
            adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_likes_message)
                    .setActionText(R.string.list_empty_user_likes_action)
                    .setImage(R.drawable.empty_like)
                    .setButtonActionListener(new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            mUserlistBrowser.setCurrentScreenByTag(Tab.friend_finder.tag);
                        }

                        @Override
                        public void onSecondaryAction() {
                            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/settings/connections")));
                        }
                    }));
        } else {
            if (mUser != null) {
                adpWrap.setEmptyViewText(R.string.empty_user_favorites_text,
                        mUser.username == null ? getResources().getString(R.string.this_user)
                                : mUser.username);
            }

        }


        ScTabView favoritesView = new ScTabView(this);
        favoritesView.setLazyListView(buildList(), adpWrap, Consts.ListId.LIST_USER_FAVORITES, true);

        // Followings View
        adp = new ScBaseAdapter(this, Content.USER);
        adpWrap = new RemoteCollectionAdapter(this, adp,
                isMe ?  Content.ME_FOLLOWINGS.uri : null, //CloudUtils.replaceWildcard(Content.USER_FOLLOWINGS.uri, mUser.id),
                isMe ?  null : Request.to(Endpoints.USER_FOLLOWINGS, mUser.id), false);

        if (isMe) {
            adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_following_message)
                    .setActionText(R.string.list_empty_user_following_action)
                    .setImage(R.drawable.empty_follow_3row)
                    .setButtonActionListener(new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            mUserlistBrowser.setCurrentScreenByTag(Tab.friend_finder.tag);
                        }

                        @Override
                        public void onSecondaryAction() {
                        }
                    }));
        } else {
            if (mUser != null) {
                adpWrap.setEmptyViewText(R.string.empty_user_followings_text,
                        mUser.username == null ? getResources().getString(R.string.this_user)
                                : mUser.username);
            }
        }

        final ScTabView followingsView = new ScTabView(this);
        followingsView.setLazyListView(buildList(false), adpWrap, Consts.ListId.LIST_USER_FOLLOWINGS, true);


        // Followers View
        adp = new ScBaseAdapter(this, Content.USER);
        adpWrap = new RemoteCollectionAdapter(this, adp,
                isMe ?  Content.ME_FOLLOWERS.uri : null,//CloudUtils.replaceWildcard(Content.USER_FOLLOWERS.uri, mUser.id),
                isMe ?  null : Request.to(Endpoints.USER_FOLLOWERS, mUser.id), false);
        if (isMe) {
            if (mUser.track_count > 0){
                adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_followers_message)
                    .setActionText(R.string.list_empty_user_followers_action)
                    .setImage(R.drawable.empty_rec)
                    .setButtonActionListener(new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            mUserlistBrowser.setCurrentScreenByTag(Tab.tracks.tag);
                        }

                        @Override
                        public void onSecondaryAction() {
                        }
                    }));
            } else {
                adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_followers_nosounds_message)
                    .setActionText(R.string.list_empty_user_followers_nosounds_action)
                    .setImage(R.drawable.empty_share)
                    .setButtonActionListener(new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            startActivity(new Intent(Actions.RECORD).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                        }

                        @Override
                        public void onSecondaryAction() {
                        }
                    }));
            }
        } else {
            if (mUser != null) {
                adpWrap.setEmptyViewText(R.string.empty_user_followers_text,
                        mUser.username == null ? getResources().getString(R.string.this_user)
                                : mUser.username);
            }
        }
        final ScTabView followersView = new ScTabView(this);
        followersView.setLazyListView(buildList(false), adpWrap, Consts.ListId.LIST_USER_FOLLOWERS, true);

        // Details View
        final ScTabView infoView = new ScTabView(this);
        infoView.addView(mInfoView);

        for (ScListView list : mLists){
            list.setFadingEdgeLength(0);
        }

        // Friend Finder View
        if (isMe) {
            mFriendFinderView = new FriendFinderView(this);
            if (mConnections == null) {
                mFriendFinderView.setState(FriendFinderView.States.LOADING, false);
            } else {
                mFriendFinderView.onConnections(mConnections, false);
            }
        }


        if (mFriendFinderView != null) mUserlistBrowser.addView(mFriendFinderView, getString(R.string.user_browser_tab_friend_finder), getResources().getDrawable(R.drawable.ic_user_tab_friendfinder), Tab.friend_finder.tag);
        mUserlistBrowser.addView(mMyTracksView,  getString(R.string.user_browser_tab_sounds), getResources().getDrawable(R.drawable.ic_user_tab_sounds), Tab.tracks.tag);
        mUserlistBrowser.addView(favoritesView, getString(R.string.user_browser_tab_likes), getResources().getDrawable(R.drawable.ic_user_tab_likes), Tab.favorites.tag);
        mUserlistBrowser.addView(followingsView, getString(R.string.user_browser_tab_followings), getResources().getDrawable(R.drawable.ic_user_tab_following), Tab.followings.tag);
        mUserlistBrowser.addView(followersView, getString(R.string.user_browser_tab_followers), getResources().getDrawable(R.drawable.ic_user_tab_followers), Tab.followers.tag);
        mUserlistBrowser.addView(infoView, getString(R.string.user_browser_tab_info), getResources().getDrawable(R.drawable.ic_user_tab_info), Tab.details.tag);


        mUserlistBrowser.setOnScreenChangedListener(new WorkspaceView.OnScreenChangeListener() {
            @Override public void onScreenChanged(View newScreen, int newScreenIndex) {
                trackScreen();

                if (isMe) {
                    getApp().setAccountData(User.DataKeys.PROFILE_IDX, Integer.toString(newScreenIndex));
                }
            }
            @Override public void onScreenChanging(View newScreen, int newScreenIndex) {}

            @Override
            public void onNextScreenVisible(View newScreen, int newScreenIndex) {
                //((ScTabView) newScreen).onVisible();
            }
        });
        */
    }

    private boolean isOtherUser() {
        return !isMe();
    }

    private boolean isMe() {
       return mUser != null && mUser.id == getCurrentUserId();
    }

    private void toggleFollowing(User user) {
        FollowStatus.get().toggleFollowing(user.id, getApp(), new Handler() {
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

    @Override
    protected void handleRecordingClick(final Recording recording) {
        if (recording.upload_status == Recording.Status.UPLOADING) {
            startActivity(recording.getMonitorIntent());
        } else {
            startActivity(new Intent(UserBrowser.this,
                    (recording.external_upload ? ScUpload.class : ScCreate.class)).
                    setData(recording.toUri()));

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case Consts.RequestCodes.MAKE_CONNECTION:
                if (resultCode == RESULT_OK) {
                    boolean success = result.getBooleanExtra("success", false);
                    String msg = getString(
                            success ? R.string.connect_success : R.string.connect_failure,
                            result.getStringExtra("service"));
                    Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();

                    if (success && isMe()) {
                        Connections.get().requestUpdate(getApp(), true, this);
                    }
                }
            //noinspection fallthrough
            default:
        }
    }

    private Configuration toConfiguration() {
        Configuration c = new Configuration();
        c.loadUserTask = mLoadUserTask;
        c.user = mUser;
        c.connections = mConnections;
        //c.workspaceIndex = mUserlistBrowser.getCurrentWorkspaceIndex();
        c.infoError = mInfoError;
        return c;
    }

    private void fromConfiguration(Configuration c){
        mInfoError = c.infoError;
        setUser(c.user);
        build(); //build here because the rest of the state needs a constructed userlist browser

        if (c.loadUserTask != null) {
            mLoadUserTask = c.loadUserTask;
        }
        if (isMe()) mConnections = c.connections;
        //mUserlistBrowser.initWorkspace(c.workspaceIndex);
    }

    private static class Configuration {
        FetchUserTask loadUserTask;
        User user;
        List<Connection> connections;
        int workspaceIndex;
        boolean infoError;
    }

    private boolean isFollowing(){
        return mUser != null && FollowStatus.get().isFollowing(mUser);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem followItem = menu.findItem(R.id.action_bar_follow);

        final boolean following = isFollowing();
        followItem.setIcon(following ? R.drawable.ic_remove_user_white : R.drawable.ic_add_user_white);
        followItem.setTitle(getResources().getString(following ? R.string.action_bar_unfollow : R.string.action_bar_follow));

        SoundRecorder soundRecorder = SoundRecorder.getInstance(this);
        if (!isMe() && (!soundRecorder.isRecording() || soundRecorder.getRecording().getRecipient() == mUser)) {
            menu.add(menu.size(), Consts.OptionsMenu.PRIVATE_MESSAGE,
                    menu.size(), R.string.menu_private_message).setIcon(R.drawable.ic_options_menu_rec);
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
            case Consts.OptionsMenu.PRIVATE_MESSAGE:
                Intent intent = new Intent(this, ScCreate.class);
                intent.putExtra(ScCreate.EXTRA_PRIVATE_MESSAGE_RECIPIENT,mUser);
                startActivity(intent);
                return true;
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
}
