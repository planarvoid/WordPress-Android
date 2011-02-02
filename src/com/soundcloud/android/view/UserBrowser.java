
package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.LoadDetailsTask;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.utils.WorkspaceView;
import com.soundcloud.utils.WorkspaceView.OnScrollListener;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.TabHost.OnTabChangeListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class UserBrowser extends ScTabView {

    private static String TAG = "UserBrowser";

    private LazyActivity mActivity;

    protected ImageView mIcon;

    protected TableLayout mUserTable;

    protected LinearLayout mUserTableHolder;

    protected FrameLayout mDetailsView;

    protected TextView mUser;

    protected TextView mLocation;

    protected TextView mTracks;

    protected TextView mFollowers;

    protected TextView mFullName;

    protected TextView mWebsite;

    protected TextView mDiscogsName;

    protected TextView mMyspaceName;

    protected TextView mDescription;

    protected Button mToggleView;

    protected Boolean mFollowingChecked = false;

    protected TextView mFavoriteStatus;

    protected ImageButton mFavorite;

    protected ListView lv_followers;

    protected ListView lv_followings;

    protected LoadTask mLoadFollowingsTask;

    protected LoadTask mLoadFollowersTask;

    protected String _permalink;

    protected String _username;

    protected String _location;

    protected String _tracks;

    protected String _followers;

    protected String _iconURL;

    private ScTabView mTracksView;

    private ScTabView mFavoritesView;

    private WorkspaceView mWorkspaceView;

    protected Long mUserLoadId = null;

    protected Boolean _isFollowing = false;

    protected LoadTask mLoadDetailsTask;

    protected String mFollowResult;

    private TabWidget mTabWidget;

    private TabHost mTabHost;

    protected ScTabView mLastTab;

    protected int mLastTabIndex;

    private User mUserData;

    private Boolean mIsOtherUser;

    private ImageLoader.BindResult avatarResult;

    protected DecimalFormat mSecondsFormat = new DecimalFormat("00");

    public enum UserTabs {
        tracks, favorites, info
    }

    public interface TabIndexes {
        public final static int TAB_TRACKS = 0;

        public final static int TAB_FAVORITES = 1;

        public final static int TAB_INFO = 2;

        public final static int TAB_FOLLOWINGS = 3;

        public final static int TAB_FOLLOWERS = 4;
    }

    public UserBrowser(LazyActivity c) {
        super(c);

        mActivity = c;

        LayoutInflater inflater = (LayoutInflater) c
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.user_view, this);
        mDetailsView = (FrameLayout) inflater.inflate(R.layout.user_details, null);

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
        if (getContext().getResources().getDisplayMetrics().density > 1) {
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
        mFavorite.setEnabled(false);
        mLastTabIndex = 0;
    }

    public void setUserTab(UserTabs tab) {
        switch (tab) {
            case tracks:
                mTabWidget.setCurrentTab(0);
                break;
            case favorites:
                mTabWidget.setCurrentTab(1);
                break;
            case info:
                mTabWidget.setCurrentTab(2);
                break;
        }

        if (mWorkspaceView != null)
            ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild())).onRefresh();
        else
            ((ScTabView) mTabHost.getCurrentView()).onRefresh();
    }

    @Override
    public void onRefresh(Boolean refreshAll) {
        if (avatarResult == BindResult.ERROR)
            reloadAvatar();

        if (refreshAll) {
            mTracksView.onRefresh();
            mFavoritesView.onRefresh();

            if (mLoadDetailsTask != null) {
                if (!CloudUtils.isTaskFinished(mLoadDetailsTask))
                    mLoadDetailsTask.cancel(true);

                mLoadDetailsTask = null;
            }
        } else {
            if (mWorkspaceView.getDisplayedChild() == TabIndexes.TAB_INFO) {
                this.refreshDetailsTask();
            }
            if (mWorkspaceView != null)
                ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild()))
                        .onRefresh();
            else
                ((ScTabView) mTabHost.getCurrentView()).onRefresh();
        }
    }

    public void refreshAll() {

    }

    public void loadYou() {
        mIsOtherUser = false;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        User userInfo = null;

        if (!preferences.getString("currentUserId", "-1").contentEquals("-1"))
            userInfo = CloudUtils.resolveUserById(mActivity.getSoundCloudApplication(), Integer
                    .parseInt(preferences.getString("currentUserId", "-1")), CloudUtils
                    .getCurrentUserId(mActivity));

        if (userInfo != null)
            mapUser(userInfo);

        build();
    }

    /*
     * public void loadUserByPermalink(String userPermalink){ mIsOtherUser =
     * true; mUserLoadId = userPermalink; User userInfo = null; userInfo =
     * CloudUtils.resolveUserByPermalink(mActivity, userPermalink,
     * CloudUtils.getCurrentUserId(mActivity)); if (userInfo != null)
     * mapUser(userInfo); build(); }
     */

    public void loadUserById(long userId) {
        mIsOtherUser = true;
        User userInfo = null;
        userInfo = CloudUtils.resolveUserById(mActivity.getSoundCloudApplication(), userId,
                CloudUtils.getCurrentUserId(mActivity));
        mUserLoadId = userId;

        if (userInfo != null)
            mapUser(userInfo);

        build();
    }

    public void loadUserByObject(User userInfo) {
        mIsOtherUser = true;
        mUserLoadId = userInfo.getId();
        mapUser(userInfo);
        build();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mWorkspaceView != null)
            ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild())).onStart();
        else
            ((ScTabView) mTabHost.getCurrentView()).onStart();

        // if this is the profile of th emain user and there is no user id and a
        // task has already completed,
        // that means the task either failed, or they just revoked access, so
        // clear the details task so it will rerun
        
        if (mLoadDetailsTask == null)
            initLoadTasks();
        else if (!mIsOtherUser
                && PreferenceManager.getDefaultSharedPreferences(mActivity).getString(
                        "currentUserId", "-1").contentEquals("-1") && mLoadDetailsTask != null
                && !CloudUtils.isTaskPending(mLoadDetailsTask))
            refreshDetailsTask();

    }

    private void refreshDetailsTask() {
        mLoadDetailsTask = null;
        initLoadTasks();
    }

    public void initLoadTasks() {

        if (mLoadDetailsTask == null) {
            try {
                mLoadDetailsTask = newLoadDetailsTask();
                mLoadDetailsTask.execute(mActivity.getSoundCloudApplication().getPreparedRequest(
                        getDetailsUrl()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mLoadDetailsTask.setActivity(mActivity);
            if (CloudUtils.isTaskPending(mLoadDetailsTask))
                mLoadDetailsTask.execute();
        }

        if (mFollowingChecked == false)
            checkFollowingStatus();
    }

    protected LoadTask newLoadDetailsTask() {
        LoadTask lt = new LoadUserDetailsTask();
        lt.loadModel = CloudUtils.Model.user;
        lt.setActivity(mActivity);
        return lt;
    }

    protected class LoadUserDetailsTask extends LoadDetailsTask {
        @Override
        protected void mapDetails(Parcelable update) {
            mapUser(update);
        }
    }

    protected void build() {
        /*
         * FrameLayout tabLayout = CloudUtils.createTabLayout(mActivity);
         * tabLayout.setLayoutParams(new
         * LayoutParams(android.view.ViewGroup.LayoutParams
         * .FILL_PARENT,android.view.ViewGroup.LayoutParams.FILL_PARENT));
         * ((FrameLayout) findViewById(R.id.tab_holder)).addView(tabLayout);
         * mTabHost = (TabHost) tabLayout.findViewById(android.R.id.tabhost);
         * mTabWidget = (TabWidget) tabLayout.findViewById(android.R.id.tabs);
         * if (mActivity instanceof ScProfile){ ((ScProfile)
         * mActivity).setTabHost(mTabHost); } LazyBaseAdapter adp = new
         * TracklistAdapter(mActivity, new ArrayList<Parcelable>());
         * LazyEndlessAdapter adpWrap = new
         * LazyEndlessAdapter(mActivity,adp,getUserTracksUrl
         * (),CloudUtils.Model.track); final ScTabView tracksView = mTracksView
         * = new ScTabView(mActivity,adpWrap);
         * CloudUtils.createTabList(mActivity, tracksView, adpWrap,
         * CloudUtils.ListId.LIST_USER_TRACKS); CloudUtils.createTab(mActivity,
         * mTabHost
         * ,"tracks",mActivity.getString(R.string.tab_tracks),null,tracksView,
         * false); adp = new TracklistAdapter(mActivity, new
         * ArrayList<Parcelable>()); adpWrap = new
         * LazyEndlessAdapter(mActivity,adp
         * ,getFavoritesUrl(),CloudUtils.Model.track); final ScTabView
         * favoritesView = mFavoritesView = new ScTabView(mActivity,adpWrap);
         * CloudUtils.createTabList(mActivity, favoritesView, adpWrap,
         * CloudUtils.ListId.LIST_USER_FAVORITES);
         * CloudUtils.createTab(mActivity,
         * mTabHost,"favorites",mActivity.getString
         * (R.string.tab_favorites),null,favoritesView, false); final ScTabView
         * detailsView = new ScTabView(mActivity);
         * detailsView.addView(mDetailsView); CloudUtils.createTab(mActivity,
         * mTabHost
         * ,"details",mActivity.getString(R.string.tab_info),null,detailsView,
         * false); CloudUtils.configureTabs(mActivity, mTabWidget, 30);
         * CloudUtils.setTabTextStyle(mActivity, mTabWidget, true);
         * mTabWidget.invalidate(); setTabTextInfo(); if (!mIsOtherUser)
         * mTabHost
         * .setCurrentTab(PreferenceManager.getDefaultSharedPreferences(
         * mActivity ).getInt("lastProfileIndex",0));
         * mTabHost.setOnTabChangedListener(tabListener);
         */

        // this is for the workspace view
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        FrameLayout frameLayout = (FrameLayout) findViewById(android.R.id.tabcontent);
        frameLayout.setPadding(0, 0, 0, 0);
        frameLayout.getLayoutParams().height = 0;

        tabHost.setup();

        // FrameLayout tabLayout = CloudUtils.createTabLayout(mActivity,true);
        // tabLayout.setLayoutParams(new
        // LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.FILL_PARENT));

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabWidget = (TabWidget) findViewById(android.R.id.tabs);

        final HorizontalScrollView hsv = (HorizontalScrollView) findViewById(R.id.tab_scroller);
        hsv.setBackgroundColor(0xFF555555);

        mWorkspaceView = (WorkspaceView) findViewById(R.id.workspace_view);
        mWorkspaceView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollToView(int index) {
                mTabHost.setCurrentTab(index);
                // if (!mIsOtherUser)
                // PreferenceManager.getDefaultSharedPreferences(mActivity).edit().putInt("lastProfileIndex",mLastTabIndex).commit();
                hsv.scrollTo(mTabWidget.getChildTabViewAt(index).getLeft()
                        + mTabWidget.getChildTabViewAt(index).getWidth() / 2 - getWidth() / 2, 0);
            }

        });
        // ((FrameLayout) findViewById(R.id.tab_holder)).addView(tabLayout);

        // ((ScrollView)
        // mDetailsView.findViewById(R.id.user_details_scrollview)).setOnTouchListener(gestureListener);

        // if (mActivity instanceof ScProfile){
        // ((ScProfile) mActivity).setTabHost(mTabHost);
        // }

        final ScTabView emptyView = new ScTabView(mActivity);

        LazyBaseAdapter adp = new TracklistAdapter(mActivity, new ArrayList<Parcelable>());
        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(mActivity, adp, getUserTracksUrl(),
                CloudUtils.Model.track);

        final ScTabView tracksView = mTracksView = new ScTabView(mActivity, adpWrap);
        CloudUtils
                .createTabList(mActivity, tracksView, adpWrap, CloudUtils.ListId.LIST_USER_TRACKS);
        // CloudUtils.createTab(mActivity,
        // mTabHost,"tracks",mActivity.getString(R.string.tab_tracks),null,tracksView,
        // true);
        CloudUtils.createTab(mActivity, mTabHost, "tracks", mActivity
                .getString(R.string.tab_tracks), null, emptyView, true);
        mWorkspaceView.addView(tracksView);

        adp = new TracklistAdapter(mActivity, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(mActivity, adp, getFavoritesUrl(), CloudUtils.Model.track);

        final ScTabView favoritesView = mFavoritesView = new ScTabView(mActivity, adpWrap);
        CloudUtils.createTabList(mActivity, favoritesView, adpWrap,
                CloudUtils.ListId.LIST_USER_FAVORITES);
        CloudUtils.createTab(mActivity, mTabHost, "favorites", mActivity
                .getString(R.string.tab_favorites), null, emptyView, true);
        mWorkspaceView.addView(favoritesView);

        final ScTabView detailsView = new ScTabView(mActivity);
        detailsView.addView(mDetailsView);
        mWorkspaceView.addView(detailsView);
        CloudUtils.createTab(mActivity, mTabHost, "details",
                mActivity.getString(R.string.tab_info), null, emptyView, true);

        adp = new UserlistAdapter(mActivity, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(mActivity, adp, getFollowingsUrl(), CloudUtils.Model.user);

        final ScTabView followingsView = new ScTabView(mActivity);
        CloudUtils.createTabList(mActivity, followingsView, adpWrap,
                CloudUtils.ListId.LIST_USER_FOLLOWINGS);
        CloudUtils.createTab(mActivity, mTabHost, "followings", mActivity
                .getString(R.string.tab_followings), null, emptyView, true);
        mWorkspaceView.addView(followingsView);

        adp = new UserlistAdapter(mActivity, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(mActivity, adp, getFollowersUrl(), CloudUtils.Model.user);

        final ScTabView followersView = new ScTabView(mActivity);
        CloudUtils.createTabList(mActivity, followersView, adpWrap,
                CloudUtils.ListId.LIST_USER_FOLLOWERS);
        CloudUtils.createTab(mActivity, mTabHost, "followers", mActivity
                .getString(R.string.tab_followers), null, emptyView, true);
        mWorkspaceView.addView(followersView);

        CloudUtils.configureTabs(mActivity, mTabWidget, 30, -1, true);
        CloudUtils.setTabTextStyle(mActivity, mTabWidget, true);
        mTabWidget.invalidate();

        setTabTextInfo();
        mTabHost.setOnTabChangedListener(tabListener);
        // Log.i(TAG,"SETTING");
        // if (mWorkspaceView != null && !mIsOtherUser)
        // mWorkspaceView.setDisplayedChild(PreferenceManager.getDefaultSharedPreferences(mActivity).getInt("lastProfileIndex",0));

        // mTabHost.setCurrentTab(PreferenceManager.getDefaultSharedPreferences(mActivity).getInt("lastProfileIndex",0));

    }

    protected void setTabTextInfo() {
        if (mTabWidget != null && mUserData != null) {
            if (!TextUtils.isEmpty(mUserData.getTrackCount()))
                CloudUtils.setTabText(mTabWidget, 0, mActivity.getResources().getString(
                        R.string.tab_tracks)
                        + " (" + mUserData.getTrackCount() + ")");
            else
                CloudUtils.setTabText(mTabWidget, 0, mActivity.getResources().getString(
                        R.string.tab_tracks));

            if (!TextUtils.isEmpty(mUserData.getPublicFavoritesCount()))
                CloudUtils.setTabText(mTabWidget, 1, mActivity.getResources().getString(
                        R.string.tab_favorites)
                        + " (" + mUserData.getPublicFavoritesCount() + ")");
            else
                CloudUtils.setTabText(mTabWidget, 1, mActivity.getResources().getString(
                        R.string.tab_favorites));
        }
    }

    private OnTabChangeListener tabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String arg0) {
            if (mLastTab != null) {
                mLastTab.onStop();
            }

            mLastTab = (ScTabView) mTabHost.getCurrentView();
            mLastTabIndex = mTabHost.getCurrentTab();
            if (!mIsOtherUser)
                PreferenceManager.getDefaultSharedPreferences(mActivity).edit().putInt(
                        "lastProfileIndex", mLastTabIndex).commit();
            if (mWorkspaceView != null)
                mWorkspaceView.setDisplayedChild(mTabHost.getCurrentTab());
        }
    };

    private void checkFollowingStatus() {
        if (!mIsOtherUser)
            return;

        mFollowingChecked = true;
        try {
            String url = mActivity.getSoundCloudApplication().getSignedUrl(
                    SoundCloudApplication.PATH_MY_FOLLOWINGS);
            new CheckFollowingStatusTask().execute(SoundCloudApplication.PATH_MY_FOLLOWINGS + "/"
                    + mUserLoadId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleFollowing() {

        mFavorite.setEnabled(false);
        _isFollowing = !_isFollowing;
        setFollowingButtonText();

        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    if (_isFollowing)
                        mFollowResult = CloudUtils.streamToString(mActivity
                                .getSoundCloudApplication().putContent(
                                        SoundCloudApplication.PATH_MY_USERS + "/"
                                                + mUserData.getId()));
                    else
                        mFollowResult = CloudUtils.streamToString(mActivity
                                .getSoundCloudApplication().deleteContent(
                                        SoundCloudApplication.PATH_MY_USERS + "/"
                                                + mUserData.getId()));

                } catch (Exception e) {
                    e.printStackTrace();
                    mActivity.setException(e);
                }

                if (mFollowResult != null) {

                    if (mFollowResult.indexOf("<user>") != -1
                            || mFollowResult.indexOf("200 - OK") != -1
                            || mFollowResult.indexOf("201 - Created") != -1) {
                        // _isFollowing = !_isFollowing;
                    }
                }
                mActivity.mHandler.post(mHandleErrors);
            }
        };
        t.start();
    }

    // Create runnable for posting since we update the following asynchronously
    final Runnable mHandleErrors = new Runnable() {
        public void run() {
            if (mActivity != null)
                mActivity.handleException();
            mActivity.handleError();
        }
    };

    // Create runnable for posting since we update the following asynchronously
    final Runnable mUpdateFollowing = new Runnable() {
        public void run() {
            setFollowingButtonText();
        }
    };

    protected void setFollowingButtonText() {
        if (!mIsOtherUser)
            return;

        if (_isFollowing) {
            mFavorite.setImageResource(R.drawable.ic_unfollow_states);
            // mFavoriteStatus.setText(R.string.favorited);
        } else {
            mFavorite.setImageResource(R.drawable.ic_follow_states);
            // mFavoriteStatus.setText(R.string.not_favorited);
        }

        if (mUserData != null
                && Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mActivity)
                        .getString("currentUserId", "-1")) == mUserData.getId()) {
            return;
        }

        mFavorite.setVisibility(View.VISIBLE);
        mFavorite.setEnabled(true);
    }

    public void mapUser(Parcelable p) {
        User mUserInfo = (User) p;

        // if (CloudUtils.getCurrentUserId(mActivity))
        // CloudUtils.resolveUser(mActivity, mUserInfo, true,
        // CloudUtils.getCurrentUserId(mActivity));

        mUserData = mUserInfo; // save to details object for restoring state

        if (mUserData.getId() == null)
            return;

        if (mUserLoadId == null && mFollowingChecked == false) {
            mUserLoadId = mUserData.getId();
            checkFollowingStatus();
        } else {
            mUserLoadId = mUserData.getId();
        }

        if (mUserData.getUserFollowing() != null)
            if (mUserData.getUserFollowing().equalsIgnoreCase("true"))
                _isFollowing = true;

        mUser.setText(mUserData.getUsername());
        mLocation.setText(CloudUtils.getLocationString(mUserData.getCountry(), mUserData
                .getCountry()));
        setTabTextInfo();

        // check for a local avatar and show it if it exists
        // String localAvatarPath =
        // CloudUtils.buildLocalAvatarUrl(mUserData.getPermalink());
        // File avatarFile = new File(localAvatarPath);
        String remoteUrl = "";
        if (getContext().getResources().getDisplayMetrics().density > 1)
            remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.getAvatarUrl(), GraphicsSizes.large);
        else
            remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.getAvatarUrl(), GraphicsSizes.badge);

        if (_iconURL != remoteUrl) {
            _iconURL = remoteUrl;
            reloadAvatar();
        }

        Boolean _showTable = false;

        if (TextUtils.isEmpty(mUserData.getTrackCount())
                || mUserData.getTrackCount().contentEquals("0")) {
            ((TableRow) mDetailsView.findViewById(R.id.tracks_row)).setVisibility(View.GONE);
        } else {
            mTracks.setText(mUserData.getTrackCount());
            ((TableRow) mDetailsView.findViewById(R.id.tracks_row)).setVisibility(View.VISIBLE);
            _showTable = true;
        }

        ((TableRow) mDetailsView.findViewById(R.id.followers_row)).setVisibility(View.GONE);
        // mFollowers.setText(mUserData.getData(User.key_followers_count));

        if (!TextUtils.isEmpty(mUserData.getFullName())) {
            _showTable = true;
            mFullName.setText(mUserData.getFullName());
            ((TableRow) mDetailsView.findViewById(R.id.fullname_row)).setVisibility(View.VISIBLE);
        } else {

            ((TableRow) mDetailsView.findViewById(R.id.fullname_row)).setVisibility(View.GONE);
        }

        CharSequence styledText;
        if (!TextUtils.isEmpty(mUserData.getWebsite())) {
            _showTable = true;
            styledText = Html.fromHtml("<a href='" + mUserData.getWebsite() + "'>"
                    + CloudUtils.stripProtocol(mUserData.getWebsite()) + "</a>");
            mWebsite.setText(styledText);
            mWebsite.setMovementMethod(LinkMovementMethod.getInstance());
            ((TableRow) mDetailsView.findViewById(R.id.website_row)).setVisibility(View.VISIBLE);
        } else {
            ((TableRow) mDetailsView.findViewById(R.id.website_row)).setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mUserData.getDiscogsName())) {
            _showTable = true;
            styledText = Html.fromHtml("<a href='http://www.discogs.com/artist/"
                    + mUserData.getDiscogsName() + "'>" + mUserData.getDiscogsName() + "</a>");
            mDiscogsName.setText(styledText);
            mDiscogsName.setMovementMethod(LinkMovementMethod.getInstance());
            ((TableRow) mDetailsView.findViewById(R.id.discogs_row)).setVisibility(View.VISIBLE);
        } else {
            ((TableRow) mDetailsView.findViewById(R.id.discogs_row)).setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mUserData.getMyspaceName())) {
            _showTable = true;
            styledText = Html.fromHtml("<a href='http://www.myspace.com/"
                    + (mUserData).getMyspaceName() + "'>" + (mUserData).getMyspaceName() + "</a>");
            mMyspaceName.setText(styledText);
            mMyspaceName.setMovementMethod(LinkMovementMethod.getInstance());
            ((TableRow) mDetailsView.findViewById(R.id.myspace_row)).setVisibility(View.VISIBLE);
        } else {
            ((TableRow) mDetailsView.findViewById(R.id.myspace_row)).setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mUserData.getDescription())) {
            _showTable = true;
            styledText = Html.fromHtml((mUserData).getDescription());
            mDescription.setText(styledText);
            mDescription.setMovementMethod(LinkMovementMethod.getInstance());
        }

        Log.i(TAG, "SHOW TABLE " + _showTable);

        if (_showTable) {
            ((TextView) mDetailsView.findViewById(R.id.txt_empty)).setVisibility(View.GONE);
        } else
            ((TextView) mDetailsView.findViewById(R.id.txt_empty)).setVisibility(View.VISIBLE);

    }

    private void reloadAvatar() {
        Log.i(TAG, "Reload Avatar " + _iconURL);
        if (CloudUtils.checkIconShouldLoad(_iconURL)) {
            if ((avatarResult = ImageLoader.get(mActivity).bind(mIcon, _iconURL, null)) != BindResult.OK) {
                mIcon.setImageDrawable(getResources().getDrawable(R.drawable.avatar_badge));
            }
        }
    }

    public Object[] saveLoadTasks() {
        Object[] ret = {
            mLoadDetailsTask
        };
        return ret;
    }

    public void restoreLoadTasks(Object[] taskObject) {
        mLoadDetailsTask = (LoadTask) taskObject[0];
    }

    public Parcelable saveParcelable() {
        return mUserData;
    }

    public void restoreParcelable(Parcelable p) {
        if (p != null)
            mapUser(p);
    }

    protected String getDetailsUrl() {
        return mIsOtherUser ? SoundCloudApplication.PATH_USER_DETAILS.replace("{user_id}", Long
                .toString(mUserLoadId)) : SoundCloudApplication.PATH_MY_DETAILS;
    }

    protected String getUserTracksUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                SoundCloudApplication.PATH_USER_TRACKS.replace("{user_id}", Long
                        .toString(mUserLoadId)), mActivity.getTrackOrder()).toString() : CloudUtils
                .buildRequestPath(SoundCloudApplication.PATH_MY_TRACKS, mActivity.getTrackOrder())
                .toString();
    }

    protected String getPlaylistsUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                SoundCloudApplication.PATH_USER_PLAYLISTS.replace("{user_id}", Long
                        .toString(mUserLoadId)), mActivity.getTrackOrder()).toString() : CloudUtils
                .buildRequestPath(SoundCloudApplication.PATH_MY_PLAYLISTS,
                        mActivity.getTrackOrder()).toString();
    }

    protected String getFavoritesUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                SoundCloudApplication.PATH_USER_FAVORITES.replace("{user_id}", Long
                        .toString(mUserLoadId)), "favorited_at").toString() : CloudUtils
                .buildRequestPath(SoundCloudApplication.PATH_MY_FAVORITES,
                        mActivity.getTrackOrder()).toString();
    }

    protected String getFollowersUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                SoundCloudApplication.PATH_USER_FOLLOWERS.replace("{user_id}", Long
                        .toString(mUserLoadId)), mActivity.getUserOrder()).toString() : CloudUtils
                .buildRequestPath(SoundCloudApplication.PATH_MY_FOLLOWERS,
                        mActivity.getTrackOrder()).toString();
    }

    protected String getFollowingsUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                SoundCloudApplication.PATH_USER_FOLLOWINGS.replace("{user_id}", Long
                        .toString(mUserLoadId)), mActivity.getUserOrder()).toString() : CloudUtils
                .buildRequestPath(SoundCloudApplication.PATH_MY_FOLLOWINGS,
                        mActivity.getTrackOrder()).toString();
    }

    private class CheckFollowingStatusTask extends AsyncTask<String, Integer, String> {
        private final HttpClient httpClient = new DefaultHttpClient();

        private String content = null;

        private String finalResult = null;

        @Override
        protected String doInBackground(String... urls) {
            try {
                return CloudUtils.streamToString(mActivity.getSoundCloudApplication()
                        .getSoundCloudAPI().get(urls[0]).getEntity().getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String response) {
            if (TextUtils.isEmpty(response) || response.contains("<error>")) {
                _isFollowing = false;
            } else {
                _isFollowing = true;
            }

            setFollowingButtonText();
        }
    }

    /*
     * protected String getUserTracksUrl() { return mIsOtherUser ?
     * CloudCommunicator.PATH_USER_TRACKS.replace("{user_id}",mUserLoadId):
     * CloudCommunicator.PATH_MY_TRACKS; } protected String getPlaylistsUrl() {
     * return mIsOtherUser ?
     * CloudCommunicator.PATH_USER_PLAYLISTS.replace("{user_id}",mUserLoadId):
     * CloudCommunicator.PATH_MY_PLAYLISTS; } protected String getFavoritesUrl()
     * { return mIsOtherUser ?
     * CloudCommunicator.PATH_USER_FAVORITES.replace("{user_id}",mUserLoadId):
     * CloudCommunicator.PATH_MY_FAVORITES; } protected String getFollowersUrl()
     * { return mIsOtherUser ?
     * CloudCommunicator.PATH_USER_FOLLOWERS.replace("{user_id}",mUserLoadId):
     * CloudCommunicator.PATH_MY_FOLLOWERS; } protected String
     * getFollowingsUrl() { return mIsOtherUser ?
     * CloudCommunicator.PATH_USER_FOLLOWINGS.replace("{user_id}",mUserLoadId):
     * CloudCommunicator.PATH_MY_FOLLOWINGS; }
     */

}
