package com.soundcloud.android.activity;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.adapter.RemoteCollectionAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.cache.Connections;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.cache.ParcelCache;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.EventAware;
import com.soundcloud.android.tracking.Level2;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.FriendFinderView;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.android.view.UserlistLayout;
import com.soundcloud.android.view.WorkspaceView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class UserBrowser extends ScActivity implements
        ParcelCache.Listener<Connection>,
        FollowStatus.Listener,
        FetchUserTask.FetchUserListener,
        EventAware {

    /* package */ User mUser;

    private TextView mUsername, mLocation, mFullName, mWebsite, mDiscogsName, mMyspaceName, mDescription, mFollowerCount, mTrackCount;
    private View mVrStats;
    private ImageView mIcon;
    private String mIconURL;
    private ImageLoader.BindResult avatarResult;

    private EmptyCollection mEmptyInfoView;
    boolean mDisplayedInfo, mInfoError;

    private ScTabView mMyTracksView;
    private FrameLayout mInfoView;
    private FriendFinderView mFriendFinderView;
    private Button mFollowBtn, mFollowingBtn;
    private UserlistLayout mUserlistBrowser;
    private FetchUserTask mLoadUserTask;
    private boolean mUpdateInfo;

    private List<Connection> mConnections;
    private Object mAdapterStates[];

    public enum Tab {
        tracks(Page.Users_sounds, Page.You_sounds),
        favorites(Page.Users_likes, Page.You_likes),
        details(Page.Users_info, Page.You_info),
        followings(Page.Users_following, Page.You_following),
        followers(Page.Users_followers, Page.You_followers),
        friend_finder(null, Page.You_find_friends);

        public final Page user, you;

        Tab(Page user, Page you) {
            this.user = user;
            this.you = you;
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

        CloudUtils.setTextShadowForGrayBg(mUsername);
        CloudUtils.setTextShadowForGrayBg(mFullName);
        CloudUtils.setTextShadowForGrayBg(mFollowerCount);
        CloudUtils.setTextShadowForGrayBg(mTrackCount);

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

        mFollowBtn = (Button) findViewById(R.id.btn_followState);
        mFollowingBtn = (Button) findViewById(R.id.btn_followingState);

        mFollowBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                follow(mUser);
            }
        });

        mFollowingBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                unfollow(mUser);
            }
        });

        Intent intent = getIntent();
        // XXX in case user is already loaded - should be handled here, not in caller
        mUpdateInfo = intent.getBooleanExtra("updateInfo",true);

        Configuration c = (Configuration) getLastNonConfigurationInstance();
        if (c != null) {
            fromConfiguration(c);
        } else {

            if (intent.hasExtra("user")) {
                loadUserByObject((User) intent.getParcelableExtra("user"));
            } else if (intent.hasExtra("userId")) {
                loadUserById(intent.getLongExtra("userId", -1));
            } else {
                loadYou();
            }

            build();
            if (!isMe()) FollowStatus.get().requestUserFollowings(getApp(), this, false);

            if (intent.hasExtra("userBrowserTag")){
                mUserlistBrowser.initByTag(intent.getStringExtra("userBrowserTag"));
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

            if (isMe()) {
                mConnections = Connections.get().getObjectsOrNull();
                mFriendFinderView.onConnections(mConnections, true);
                Connections.get().requestUpdate(getApp(), false, this);
            }
        }

        mMyTracksView.onVisible();
        ((ScTabView) mUserlistBrowser.getCurrentWorkspaceView()).onVisible();
        loadDetails();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
        mUserlistBrowser.setCurrentScreenByTag(tag);
    }

    public boolean isShowingTab(Tab tabTag) {
        return mUserlistBrowser.getCurrentTag().equals(tabTag.name());
    }


    @Override
    protected void onResume() {
        if (getApp().getAccount() != null && mAdapterStates != null){
            restoreAdapterStates(mAdapterStates);
            mAdapterStates = null;
        }
        trackScreen();

        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        for (ScListView list : mLists) {
            list.checkForManualDetatch();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FollowStatus.get().removeListener(this);
        mAdapterStates = new Object[mLists.size()];
        int i = 0;
        for (ScListView list : mLists) {
            if (list.getWrapper() != null) {
                mAdapterStates[i] = list.getWrapper().saveState();
                list.getWrapper().cleanup();
                list.postDetach(); // detach from window to clear recycler
            }
            i++;
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return toConfiguration();
    }

    private void restoreAdapterStates(Object[] adapterStates) {
        int i = 0;
        for (Object adapterState : adapterStates) {
            if (adapterState != null) {
                mLists.get(i).getWrapper().restoreState((Object[]) adapterState);
            }
            i++;
        }
    }

    @Override
     protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (isConnected && avatarResult == BindResult.ERROR) reloadAvatar();
    }

    public void refreshConnections(){
        if (isMe()) {
            Connections.get().requestUpdate(getApp(), true, this);
            if (mFriendFinderView != null) mFriendFinderView.setState(FriendFinderView.States.LOADING, true);
        }
    }

    public void onChanged(List<Connection> connections, ParcelCache<Connection> cache) {
        mConnections = connections;
        mFriendFinderView.onConnections(connections, true);
    }

    private void loadYou() {
        setUser(getApp().getLoggedInUser());
    }

    private void loadUserById(long userId) {
        if (userId != -1) {
            // check DB first as the cached user might be incomplete
            final User u = SoundCloudDB.getUserById(getContentResolver(), userId);
            setUser(u != null ? u : SoundCloudApplication.USER_CACHE.get(userId));
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
        final User dbUser = SoundCloudDB.getUserById(getContentResolver(), user.id);
        setUser(dbUser != null ? dbUser : user);
    }

    private void loadDetails() {
        if (!mUpdateInfo) return;

        if (mLoadUserTask == null) {
            mLoadUserTask = new FetchUserTask(getApp(), mUser.id);
            mLoadUserTask.setActivity(this);
            mLoadUserTask.addListener(this);
            mLoadUserTask.execute(Request.to(Endpoints.USER_DETAILS, mUser.id));
        }
    }

    public void onChange(boolean success, FollowStatus status) {
        setFollowingButton();
    }

    private void trackScreen() {
        track(getEvent(), mUser);
    }

    public Page getEvent() {
        Tab current = Tab.valueOf(mUserlistBrowser.getCurrentTag());
        return isMe() ? current.you : current.user;
    }

    private void build() {

        mUserlistBrowser = (UserlistLayout) findViewById(R.id.userlist_browser);
        final boolean isMe = isMe();

        // Tracks View
        LazyBaseAdapter adp = isOtherUser() ? new TracklistAdapter(this,
                new ArrayList<Parcelable>(), Track.class) : new MyTracksAdapter(this,
                new ArrayList<Parcelable>(), Track.class);

        LazyEndlessAdapter adpWrap = new RemoteCollectionAdapter(this, adp,
                isMe ?  Content.ME_TRACKS.uri : null,
                isMe ?  null : Request.to(Endpoints.USER_TRACKS, mUser.id), false);


        if (isMe) {
            adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_sounds_message)
                    .setActionText(R.string.list_empty_user_sounds_action)
                    .setImage(R.drawable.empty_rec)
                    .setActionListener(new EmptyCollection.ActionListener() {
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
        adp = new TracklistAdapter(this, new ArrayList<Parcelable>(), Track.class);

        adpWrap = new RemoteCollectionAdapter(this, adp,
                isMe ?  Content.ME_FAVORITES.uri : null,//CloudUtils.replaceWildcard(Content.USER_FAVORITES.uri, mUser.id),
                isMe ?  null : Request.to(Endpoints.USER_FAVORITES, mUser.id), false);

        if (isMe) {
            adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_likes_message)
                    .setActionText(R.string.list_empty_user_likes_action)
                    .setImage(R.drawable.empty_like)
                    .setActionListener(new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            mUserlistBrowser.setCurrentScreenByTag(Tab.friend_finder.name());
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
        adp = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class);
        adpWrap = new RemoteCollectionAdapter(this, adp,
                isMe ?  Content.ME_FOLLOWINGS.uri : null, //CloudUtils.replaceWildcard(Content.USER_FOLLOWINGS.uri, mUser.id),
                isMe ?  null : Request.to(Endpoints.USER_FOLLOWINGS, mUser.id), false);

        if (isMe) {
            adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_following_message)
                    .setActionText(R.string.list_empty_user_following_action)
                    .setImage(R.drawable.empty_follow_3row)
                    .setActionListener(new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            mUserlistBrowser.setCurrentScreenByTag(Tab.friend_finder.name());
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
        adp = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class, isMe);
        adpWrap = new RemoteCollectionAdapter(this, adp,
                isMe ?  Content.ME_FOLLOWERS.uri : null,//CloudUtils.replaceWildcard(Content.USER_FOLLOWERS.uri, mUser.id),
                isMe ?  null : Request.to(Endpoints.USER_FOLLOWERS, mUser.id), false);
        if (isMe) {
            if (mUser.track_count > 0){
                adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_followers_message)
                    .setActionText(R.string.list_empty_user_followers_action)
                    .setImage(R.drawable.empty_rec)
                    .setActionListener(new EmptyCollection.ActionListener() {
                        @Override
                        public void onAction() {
                            mUserlistBrowser.setCurrentScreenByTag(Tab.tracks.name());
                        }

                        @Override
                        public void onSecondaryAction() {
                        }
                    }));
            } else {
                adpWrap.setEmptyView(new EmptyCollection(this).setMessageText(R.string.list_empty_user_followers_nosounds_message)
                    .setActionText(R.string.list_empty_user_followers_nosounds_action)
                    .setImage(R.drawable.empty_share)
                    .setActionListener(new EmptyCollection.ActionListener() {
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

        if (mFriendFinderView != null) mUserlistBrowser.addView(mFriendFinderView, getString(R.string.user_browser_tab_friend_finder), getResources().getDrawable(R.drawable.ic_user_tab_friendfinder), Tab.friend_finder.name());
        mUserlistBrowser.addView(mMyTracksView,  getString(R.string.user_browser_tab_sounds), getResources().getDrawable(R.drawable.ic_user_tab_sounds), Tab.tracks.name());
        mUserlistBrowser.addView(favoritesView, getString(R.string.user_browser_tab_likes), getResources().getDrawable(R.drawable.ic_user_tab_likes), Tab.favorites.name());
        mUserlistBrowser.addView(followingsView, getString(R.string.user_browser_tab_followings), getResources().getDrawable(R.drawable.ic_user_tab_following), Tab.followings.name());
        mUserlistBrowser.addView(followersView, getString(R.string.user_browser_tab_followers), getResources().getDrawable(R.drawable.ic_user_tab_followers), Tab.followers.name());
        mUserlistBrowser.addView(infoView, getString(R.string.user_browser_tab_info), getResources().getDrawable(R.drawable.ic_user_tab_info), Tab.details.name());

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
                ((ScTabView) newScreen).onVisible();
            }
        });
    }

    private boolean isOtherUser() {
        return !isMe();
    }

    private boolean isMe() {
       return mUser != null && mUser.id == getCurrentUserId();
    }

    private void toggleFollowing(User user) {
        mFollowBtn.setEnabled(false);
        mFollowingBtn.setEnabled(false);

        FollowStatus.get().toggleFollowing(user.id, getApp(), new Handler() {
            @Override public void handleMessage(Message msg) {
                mFollowBtn.setEnabled(true);
                mFollowingBtn.setEnabled(true);

                if (msg.what == 0) {
                    setFollowingButton();
                    CloudUtils.showToast(UserBrowser.this, R.string.error_change_following_status);
                }
            }
        });
        setFollowingButton();
    }

    private void setFollowingButton() {
        if (isOtherUser()) {
            if (FollowStatus.get().isFollowing(mUser)) {
                mFollowingBtn.setVisibility(View.VISIBLE);
                mFollowBtn.setVisibility(View.INVISIBLE);
            }  else {
                mFollowingBtn.setVisibility(View.INVISIBLE);
                mFollowBtn.setVisibility(View.VISIBLE);
            }
        } else {
            mFollowBtn.setVisibility(View.INVISIBLE);
            mFollowingBtn.setVisibility(View.INVISIBLE);
        }
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

        setFollowingButton();
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
            mDescription.setText(Html.fromHtml((user).description.replace(System.getProperty("line.separator"), "<br/>")));
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
                    mEmptyInfoView.setActionListener(new EmptyCollection.ActionListener() {
                        @Override public void onAction() {
                            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/settings")));
                        }
                        @Override public void onSecondaryAction() {}
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
            new AlertDialog.Builder(this)
                .setTitle(null)
                .setMessage(R.string.dialog_cancel_upload_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recording.cancelUpload(UserBrowser.this);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create()
                .show();
        } else {
            startActivity(new Intent(UserBrowser.this, ScCreate.class).setData(recording.toUri()));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case Connect.MAKE_CONNECTION:
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

                        if (mFriendFinderView != null) {
                            mFriendFinderView.setState(FriendFinderView.States.LOADING, false);
                        }
                    }
                }
            //noinspection fallthrough
            default:
        }
    }

    private Configuration toConfiguration(){
        Configuration c = new Configuration();
        c.loadUserTask = mLoadUserTask;
        c.user = mUser;
        c.connections = mConnections;
        c.workspaceIndex = mUserlistBrowser.getCurrentWorkspaceIndex();
        c.adapterStates = mAdapterStates;
        c.friendFinderState = mFriendFinderView != null ? mFriendFinderView.getCurrentState() : -1;
        c.infoError = mInfoError;
        return c;
    }

    private void fromConfiguration(Configuration c){
        mInfoError = c.infoError;
        setUser(c.user);
        build(); //build here because the rest of the state needs a constructed userlist browser

        if (c.loadUserTask != null) {
            mLoadUserTask = c.loadUserTask;
            mLoadUserTask.setActivity(this);
        }
        if (isMe()) mConnections = c.connections;
        mUserlistBrowser.initWorkspace(c.workspaceIndex);
        if (c.friendFinderState != -1) {
            if (c.friendFinderState == FriendFinderView.States.LOADING){
                refreshConnections();
            } else {
                mFriendFinderView.setState(c.friendFinderState, false);
            }
        }
        restoreAdapterStates(c.adapterStates);
    }

    private static class Configuration {
        FetchUserTask loadUserTask;
        User user;
        List<Connection> connections;
        int workspaceIndex;
        Object[] adapterStates;
        int friendFinderState;
        boolean infoError;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isMe()) {
            menu.add(menu.size(), Consts.OptionsMenu.PRIVATE_MESSAGE,
                menu.size(), R.string.menu_private_message).setIcon(R.drawable.ic_menu_friendfinder);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.PRIVATE_MESSAGE:
                Intent intent = new Intent(this, ScCreate.class);
                intent.putExtra(ScCreate.EXTRA_PRIVATE_MESSAGE_RECIPIENT,mUser);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
