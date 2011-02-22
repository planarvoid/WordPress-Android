package com.soundcloud.android.activity;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.CheckFollowingStatusTask;
import com.soundcloud.android.task.LoadDetailsTask;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.utils.WorkspaceView;
import com.soundcloud.utils.WorkspaceView.OnScrollListener;

import java.io.IOException;
import java.util.ArrayList;

public class UserBrowser extends ScActivity {
    private static String TAG = "UserBrowser";

    protected ImageView mIcon;

    protected FrameLayout mDetailsView;

    protected TextView mUser, mLocation, mTracks, mFollowers, mFullName, mWebsite, mDiscogsName,
            mMyspaceName, mDescription;

    protected boolean mFollowingChecked;

    protected ImageButton mFavorite;

    protected String _iconURL;

    private ScTabView mTracksView;
    private ScTabView mFavoritesView;
    private ScTabView mFollowersView;

    private WorkspaceView mWorkspaceView;

    protected Long mUserLoadId;

    protected boolean _isFollowing;
    protected LoadTask mLoadDetailsTask;

    protected int mFollowResult;

    private TabWidget mTabWidget;
    private TabHost mTabHost;

    protected int mLastTabIndex;

    private User mUserData;

    // TODO get rid od mIsOtherUser
    private boolean mIsOtherUser;

    private ImageLoader.BindResult avatarResult;

    public enum UserTabs {
        tracks
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.user_view);

        mDetailsView = (FrameLayout) getLayoutInflater().inflate(R.layout.user_details, null);

        mIcon = (ImageView) findViewById(R.id.user_icon);
        mUser = (TextView) findViewById(R.id.username);
        mLocation = (TextView) findViewById(R.id.location);
        mTracks = (TextView) mDetailsView.findViewById(R.id.tracks);
        mFollowers = (TextView) mDetailsView.findViewById(R.id.followers);

        mFullName = (TextView) mDetailsView.findViewById(R.id.fullname);
        mWebsite = (TextView) mDetailsView.findViewById(R.id.website);
        mDiscogsName = (TextView) mDetailsView.findViewById(R.id.discogs_name);
        mMyspaceName = (TextView) mDetailsView.findViewById(R.id.myspace_name);
        mDescription = (TextView) mDetailsView.findViewById(R.id.description);

        mIcon.setScaleType(ScaleType.CENTER_INSIDE);
        if (getResources().getDisplayMetrics().density > 1) {
            mIcon.getLayoutParams().width = 67;
            mIcon.getLayoutParams().height = 67;
        }

        mFavorite = (ImageButton) findViewById(R.id.btn_favorite);
        mFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                toggleFollowing();
            }
        });

        mFavorite.setVisibility(View.GONE);
        mLastTabIndex = 0;


        Intent intent = getIntent();
        Bundle extras = intent.getExtras();


        if (extras != null) {
            if (extras.getParcelable("user") != null) {
                loadUserByObject((User) extras.getParcelable("user"));
                extras.remove("user");
            }
            if (extras.containsKey("userId")) {
                loadUserById(extras.getLong("userId"));
                extras.remove("userId");
            }
        } else {
            loadYou();
        }
        initLoadTasks();
    }


    @Override
    protected void onResume() {
        tracker.trackPageView("/profile");
        tracker.dispatch();

        checkFollowingStatus();

        super.onResume();
    }


    public void onRefresh(boolean all) {
        if (avatarResult == BindResult.ERROR)
            reloadAvatar();

        if (all) {
            mTracksView.onRefresh(all);
            mFavoritesView.onRefresh(all);
            mFollowersView.onRefresh(all);
            mFavoritesView.onRefresh(all);

            if (mLoadDetailsTask != null) {
                if (!CloudUtils.isTaskFinished(mLoadDetailsTask))
                    mLoadDetailsTask.cancel(true);

                mLoadDetailsTask = null;
            }
        } else if (mWorkspaceView != null) {

            Log.i(TAG, "ON REFRESH " + mWorkspaceView.getDisplayedChild());

            if (mWorkspaceView.getDisplayedChild() == 2 /* XXX */) {
                this.refreshDetailsTask();
            }
            if (mWorkspaceView != null) {
                Log.i(TAG, "REFRESHING WORKSPACE VIEW " + mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild()));
                ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild()))
                        .onRefresh(all);
            } else
                ((ScTabView) mTabHost.getCurrentView()).onRefresh(all);
        }
    }


    public void loadYou() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        User userInfo = null;

        if (getUserId() != -1) {
            try {
                if (userInfo != null && userInfo.id != null) mapUser(userInfo);
                userInfo = SoundCloudDB.getInstance().resolveUserById(
                        getContentResolver(),
                        getUserId()
                );

                if (userInfo != null && userInfo.id != null) mapUser(userInfo);
            } catch (NumberFormatException nfe) {
                // bad data - user has a corrupted value, and will be corrected on load
            }
        }
        build();
    }


    public void loadUserById(long userId) {
        mIsOtherUser = true;
        User userInfo;
        userInfo = SoundCloudDB.getInstance().resolveUserById(getContentResolver(), userId);
        mUserLoadId = userId;

        if (userInfo != null) mapUser(userInfo);
        build();
    }

    public void loadUserByObject(User userInfo) {
        mIsOtherUser = true;
        mUserLoadId = userInfo.id;
        mapUser(userInfo);
        build();
    }


    public void onStart() {
        super.onStart();

        Log.d(TAG, "UserBrowser onStart()");

        if (mWorkspaceView != null) {
            ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild())).onStart();
        } else if (mTabHost != null) {
            ((ScTabView) mTabHost.getCurrentView()).onStart();
        }

        // if this is the profile of the main user and there is no user id and a
        // task has already completed,
        // that means the task either failed, or they just revoked access, so
        // clear the details task so it will rerun

        if (mLoadDetailsTask == null) {
            initLoadTasks();

        } else if (!mIsOtherUser
                && getUserId() != -1 && mLoadDetailsTask != null
                && !CloudUtils.isTaskPending(mLoadDetailsTask)) {
            refreshDetailsTask();
        }

    }

    private void refreshDetailsTask() {
        mLoadDetailsTask = null;
        initLoadTasks();
    }

    public void initLoadTasks() {
        if (mLoadDetailsTask == null) {
            LoadTask lt = new LoadUserDetailsTask();
            lt.loadModel = CloudUtils.Model.user;
            lt.setActivity(this);
            mLoadDetailsTask = lt;
            mLoadDetailsTask.execute(getSoundCloudApplication().getRequest(
                    getDetailsUrl(), null));
        } else {
            mLoadDetailsTask.setActivity(this);

            if (CloudUtils.isTaskPending(mLoadDetailsTask)) {
                mLoadDetailsTask.execute();
            }
        }
    }

    protected class LoadUserDetailsTask extends LoadDetailsTask {
        @Override
        protected void mapDetails(Parcelable update) {
            mapUser((User) update);
        }
    }

    protected void build() {
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        FrameLayout frameLayout = (FrameLayout) findViewById(android.R.id.tabcontent);
        frameLayout.setPadding(0, 0, 0, 0);
        frameLayout.getLayoutParams().height = 0;

        tabHost.setup();

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabWidget = (TabWidget) findViewById(android.R.id.tabs);

        final HorizontalScrollView hsv = (HorizontalScrollView) findViewById(R.id.tab_scroller);
        hsv.setBackgroundColor(0xFF555555);

        mWorkspaceView = (WorkspaceView) findViewById(R.id.workspace_view);
        mWorkspaceView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollToView(int index) {
                mTabHost.setCurrentTab(index);
                hsv.scrollTo(mTabWidget.getChildTabViewAt(index).getLeft()
                        + mTabWidget.getChildTabViewAt(index).getWidth() / 2 - getWidth() / 2, 0);
            }

        });

        final ScTabView emptyView = new ScTabView(this);

        LazyBaseAdapter adp = new TracklistAdapter(this, new ArrayList<Parcelable>());
        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this, adp, getUserTracksUrl(), CloudUtils.Model.track);

        final ScTabView tracksView = mTracksView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, tracksView, adpWrap, CloudUtils.ListId.LIST_USER_TRACKS);
        CloudUtils.createTab(mTabHost, "tracks", getString(R.string.tab_tracks), null, emptyView);

        adp = new TracklistAdapter(this, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(this, adp, getFavoritesUrl(), CloudUtils.Model.track);

        final ScTabView favoritesView = mFavoritesView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, favoritesView, adpWrap, CloudUtils.ListId.LIST_USER_FAVORITES);
        CloudUtils.createTab(mTabHost, "favorites", getString(R.string.tab_favorites), null, emptyView);

        final ScTabView detailsView = new ScTabView(this);
        detailsView.addView(mDetailsView);

        CloudUtils.createTab(mTabHost, "details", getString(R.string.tab_info), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(this, adp, getFollowingsUrl(), CloudUtils.Model.user);

        final ScTabView followingsView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, followingsView, adpWrap, CloudUtils.ListId.LIST_USER_FOLLOWINGS);
        CloudUtils.createTab(mTabHost, "followings", getString(R.string.tab_followings), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(this, adp, getFollowersUrl(), CloudUtils.Model.user);

        final ScTabView followersView = mFollowersView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, followersView, adpWrap, CloudUtils.ListId.LIST_USER_FOLLOWERS);
        CloudUtils.createTab(mTabHost, "followers", getString(R.string.tab_followers), null, emptyView);

        CloudUtils.configureTabs(this, mTabWidget, 30, -1, true);
        CloudUtils.setTabTextStyle(this, mTabWidget, true);

        if (!mIsOtherUser) {
            mLastTabIndex = PreferenceManager.getDefaultSharedPreferences(this).getInt("lastProfileIndex", 0);
            mWorkspaceView.initWorkspace(0, mLastTabIndex);
            mTabHost.setCurrentTab(mLastTabIndex);

        } else {
            mWorkspaceView.initWorkspace(0, 0);
        }

        mWorkspaceView.addView(tracksView);
        mWorkspaceView.addView(favoritesView);
        mWorkspaceView.addView(detailsView);
        mWorkspaceView.addView(followingsView);
        mWorkspaceView.addView(followersView);

        mTabWidget.invalidate();
        setTabTextInfo();

        mTabHost.setOnTabChangedListener(tabListener);
    }


    private int getWidth() {
        return findViewById(R.id.user_details_root).getWidth();
    }

    protected void setTabTextInfo() {
        if (mTabWidget != null && mUserData != null) {
            if (!TextUtils.isEmpty(mUserData.track_count)) {
                CloudUtils.setTabText(mTabWidget, 0, getString(R.string.tab_tracks)
                        + " (" + mUserData.track_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 0, getString(
                        R.string.tab_tracks));
            }

            if (!TextUtils.isEmpty(mUserData.public_favorites_count)) {
                CloudUtils.setTabText(mTabWidget, 1, getString(R.string.tab_favorites)
                        + " (" + mUserData.public_favorites_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 1, getString(R.string.tab_favorites));
            }

            if (!TextUtils.isEmpty(mUserData.followings_count)) {
                CloudUtils.setTabText(mTabWidget, 3, getString(R.string.tab_followings)
                        + " (" + mUserData.followings_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 3, getString(R.string.tab_followings));
            }


            if (!TextUtils.isEmpty(mUserData.followers_count)) {
                CloudUtils.setTabText(mTabWidget, 4,
                        getString(R.string.tab_followers)
                                + " (" + mUserData.followers_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 4, getString(R.string.tab_followers));
            }


            findViewById(R.id.tab_scroller).scrollTo(mTabWidget.getChildTabViewAt(mTabHost.getCurrentTab()).getLeft()
                    + mTabWidget.getChildTabViewAt(mTabHost.getCurrentTab()).getWidth() / 2 - getWidth() / 2, 0);
        }
    }

    private OnTabChangeListener tabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String arg0) {
            if (mWorkspaceView != null)
                mWorkspaceView.setDisplayedChild(mTabHost.getCurrentTab(), (Math.abs(mLastTabIndex - mTabHost.getCurrentTab()) > 1));

            mLastTabIndex = mTabHost.getCurrentTab();
            if (!mIsOtherUser)
                PreferenceManager.getDefaultSharedPreferences(UserBrowser.this).edit()
                        .putInt("lastProfileIndex", mLastTabIndex).commit();


        }
    };

    private void checkFollowingStatus() {
        if (mIsOtherUser) {
            new CheckFollowingStatusTask(getSoundCloudApplication()) {
                @Override
                protected void onPostExecute(Boolean b) {
                    _isFollowing = b == null ? false : b;
                    setFollowingButtonText();
                }
            }.execute(mUserLoadId);
        }
    }

    private void toggleFollowing() {

        mFavorite.setEnabled(false);
        _isFollowing = !_isFollowing;
        setFollowingButtonText();
        mFollowResult = 0;

        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    if (_isFollowing) {
                        mFollowResult =
                                getSoundCloudApplication().putContent(
                                        CloudAPI.Enddpoints.MY_FOLLOWINGS + "/"
                                                + mUserData.id, null).getStatusLine().getStatusCode();
                    } else {
                        mFollowResult =
                                getSoundCloudApplication().deleteContent(
                                        CloudAPI.Enddpoints.MY_FOLLOWINGS + "/"
                                                + mUserData.id).getStatusLine().getStatusCode();
                    }

                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                    setException(e);
                }
                mHandler.post(mSetFollowingResult);
            }
        };
        t.start();
    }

    // Create runnable for posting since we update the following asynchronously
    final Runnable mSetFollowingResult = new Runnable() {
        public void run() {

            handleException();
            handleError();

            boolean success = false;
            if (mFollowResult != 0) {
                if (mFollowResult == 200 || mFollowResult == 201 || mFollowResult == 404) {
                    success = true;
                }
            }

            if (!success) {
                _isFollowing = !_isFollowing;
                setFollowingButtonText();
            }
            mFavorite.setEnabled(true);
        }
    };

    protected void setFollowingButtonText() {
        if (!mIsOtherUser)
            return;

        if (_isFollowing) {
            mFavorite.setImageResource(R.drawable.ic_unfollow_states);
        } else {
            mFavorite.setImageResource(R.drawable.ic_follow_states);
        }

        if (mUserData == null || getUserId() != mUserData.id) {
            mFavorite.setVisibility(View.VISIBLE);
        }
    }

    private void mapUser(User p) {
        mUserData = p; // save to details object for restoring state

        if (mUserData.id == null)
            return;

        mUserLoadId = mUserData.id;

        if (mUserData.user_following != null)
            if (mUserData.user_following.equalsIgnoreCase("true"))
                _isFollowing = true;

        mUser.setText(mUserData.username);
        mLocation.setText(CloudUtils.getLocationString(mUserData.country, mUserData.country));
        setTabTextInfo();

        // check for a local avatar and show it if it exists
        // String localAvatarPath =
        // CloudUtils.buildLocalAvatarUrl(mUserData.getPermalink());
        // File avatarFile = new File(localAvatarPath);
        String remoteUrl;
        if (getResources().getDisplayMetrics().density > 1)
            remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.large);
        else
            remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.badge);

        Log.i(TAG, "ICON URL " + remoteUrl);

        if (!remoteUrl.equals(_iconURL)) {
            Log.i(TAG, "Setting icon url");
            _iconURL = remoteUrl;
            reloadAvatar();
        }

        Boolean _showTable = false;

        if (!TextUtils.isEmpty(mUserData.full_name)) {
            _showTable = true;
            mFullName.setText(mUserData.full_name);
            mDetailsView.findViewById(R.id.fullname_row).setVisibility(View.VISIBLE);
        } else {

            mDetailsView.findViewById(R.id.fullname_row).setVisibility(View.GONE);
        }

        CharSequence styledText;
        if (!TextUtils.isEmpty(mUserData.website)) {
            _showTable = true;
            styledText = Html.fromHtml("<a href='" + mUserData.website + "'>"
                    + CloudUtils.stripProtocol(mUserData.website) + "</a>");
            mWebsite.setText(styledText);
            mWebsite.setMovementMethod(LinkMovementMethod.getInstance());
            mDetailsView.findViewById(R.id.website_row).setVisibility(View.VISIBLE);
        } else {
            mDetailsView.findViewById(R.id.website_row).setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mUserData.discogs_name)) {
            _showTable = true;
            styledText = Html.fromHtml("<a href='http://www.discogs.com/artist/"
                    + mUserData.discogs_name + "'>" + mUserData.discogs_name + "</a>");
            mDiscogsName.setText(styledText);
            mDiscogsName.setMovementMethod(LinkMovementMethod.getInstance());
            mDetailsView.findViewById(R.id.discogs_row).setVisibility(View.VISIBLE);
        } else {
            mDetailsView.findViewById(R.id.discogs_row).setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mUserData.myspace_name)) {
            _showTable = true;
            styledText = Html.fromHtml("<a href='http://www.myspace.com/"
                    + (mUserData).myspace_name + "'>" + (mUserData).myspace_name + "</a>");
            mMyspaceName.setText(styledText);
            mMyspaceName.setMovementMethod(LinkMovementMethod.getInstance());
            mDetailsView.findViewById(R.id.myspace_row).setVisibility(View.VISIBLE);
        } else {
            mDetailsView.findViewById(R.id.myspace_row).setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mUserData.description)) {
            _showTable = true;
            mDescription.setText((mUserData).description);
            mDescription.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if (_showTable) {
            mDetailsView.findViewById(R.id.txt_empty).setVisibility(View.GONE);
        } else
            mDetailsView.findViewById(R.id.txt_empty).setVisibility(View.VISIBLE);

    }

    private void reloadAvatar() {
        if (CloudUtils.checkIconShouldLoad(_iconURL)) {
            if ((avatarResult = ImageLoader.get(this).bind(mIcon, _iconURL, null)) != BindResult.OK) {
                mIcon.setImageDrawable(getResources().getDrawable(R.drawable.avatar_badge));
            }
        }
    }

    public Object[] saveLoadTasks() {
        return new Object[]{
                mLoadDetailsTask
        };
    }

    public void restoreLoadTasks(Object[] taskObject) {
        mLoadDetailsTask = (LoadTask) taskObject[0];
    }

    public Parcelable saveParcelable() {
        return mUserData;
    }

    public void restoreParcelable(Parcelable p) {
        if (p != null)
            mapUser((User) p);
    }

    protected String getDetailsUrl() {
        return mIsOtherUser ? CloudAPI.Enddpoints.USER_DETAILS.replace("{user_id}",
                Long.toString(mUserLoadId)) :
                CloudAPI.Enddpoints.MY_DETAILS;
    }

    protected String getUserTracksUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                CloudAPI.Enddpoints.USER_TRACKS.replace("{user_id}",
                        Long.toString(mUserLoadId)), getTrackOrder()) :
                CloudUtils.buildRequestPath(CloudAPI.Enddpoints.MY_TRACKS,
                        getTrackOrder());
    }

    private String getTrackOrder() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString("defaultTrackSorting", "");
    }

    protected String getFavoritesUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                CloudAPI.Enddpoints.USER_FAVORITES.replace("{user_id}",
                        Long.toString(mUserLoadId)), "favorited_at") :
                CloudUtils.buildRequestPath(CloudAPI.Enddpoints.MY_FAVORITES,
                        getTrackOrder());
    }

    protected String getFollowersUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                CloudAPI.Enddpoints.USER_FOLLOWERS.replace("{user_id}",
                        Long.toString(mUserLoadId)), getUserOrder()) :
                CloudUtils.buildRequestPath(CloudAPI.Enddpoints.MY_FOLLOWERS,
                        getTrackOrder());
    }

    private String getUserOrder() {

        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString("defaultUserSorting", "");

    }

    protected String getFollowingsUrl() {
        return mIsOtherUser ?
                CloudUtils.buildRequestPath(CloudAPI.Enddpoints.USER_FOLLOWINGS.replace("{user_id}",
                        Long.toString(mUserLoadId)), getUserOrder()) :
                CloudUtils.buildRequestPath(CloudAPI.Enddpoints.MY_FOLLOWINGS,
                        getTrackOrder());
    }


}
