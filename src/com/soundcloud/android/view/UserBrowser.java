
package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScProfile;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.LoadDetailsTask;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.utils.WorkspaceView;
import com.soundcloud.utils.WorkspaceView.OnScrollListener;

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
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;

import java.util.ArrayList;

public class UserBrowser extends ScTabView {

    private static String TAG = "UserBrowser";

    private ScActivity mActivity;

    protected ImageView mIcon;

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

    protected Boolean mFollowingChecked = false;

    protected ImageButton mFavorite;

    protected String _iconURL;

    private ScTabView mTracksView;

    private ScTabView mFavoritesView;
    
    private ScTabView mFollowersView;

    private WorkspaceView mWorkspaceView;

    protected Long mUserLoadId = null;

    protected Boolean _isFollowing = false;

    protected LoadTask mLoadDetailsTask;

    protected int mFollowResult;
    
    private TabWidget mTabWidget;

    private TabHost mTabHost;

    protected ScTabView mLastTab;

    protected int mLastTabIndex;

    private User mUserData;

    private Boolean mIsOtherUser;

    private ImageLoader.BindResult avatarResult;

    public enum UserTabs {
        tracks
    }

    public UserBrowser(ScActivity c) {
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
        mLastTabIndex = 0;
    }

    @Override
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
            if (mWorkspaceView.getDisplayedChild() == 2 /* XXX */) {
                this.refreshDetailsTask();
            }
            if (mWorkspaceView != null){
                Log.i(TAG,"ON Refresh " +  mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild()));
                ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild()))
                        .onRefresh(all);
            } else
                ((ScTabView) mTabHost.getCurrentView()).onRefresh(all);
        }
    }


    public void loadYou() {
        mIsOtherUser = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        User userInfo;

        if (!(preferences.getString("currentUserId", "-1").contentEquals("-1") ||
              preferences.getString("currentUserId", "-1").contentEquals(""))){

            try {
                userInfo = CloudUtils.resolveUserById(
                       mActivity.getSoundCloudApplication(),
                       Integer.parseInt(preferences.getString("currentUserId", "-1")),
                       CloudUtils.getCurrentUserId(mActivity));


                if (userInfo != null && userInfo.id != null) mapUser(userInfo);


            }
            catch (NumberFormatException nfe) {
                // bad data - user has a corrupted value, and will be corrected on load
            }
        }

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
        User userInfo;
        userInfo = CloudUtils.resolveUserById(mActivity.getSoundCloudApplication(), userId,
                CloudUtils.getCurrentUserId(mActivity));
        mUserLoadId = userId;

        if (userInfo != null)
            mapUser(userInfo);

        build();
    }

    public void loadUserByObject(User userInfo) {
        mIsOtherUser = true;
        mUserLoadId = userInfo.id;
        mapUser(userInfo);
        build();
    }

    @Override
    public void onStart() {
        super.onStart();


        Log.d(TAG, "UserBrowser onStart()");


        if (mWorkspaceView != null)
            ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild())).onStart();
        else  if (mTabHost != null)
            ((ScTabView) mTabHost.getCurrentView()).onStart();

        // if this is the profile of the main user and there is no user id and a
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
                Log.e(TAG, "error", e);
            }
        } else {
            mLoadDetailsTask.setActivity(mActivity);
            if (CloudUtils.isTaskPending(mLoadDetailsTask))
                mLoadDetailsTask.execute();
        }

        if (!mFollowingChecked)
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
                Log.i(TAG,"On Scroll to veiw " + index);
                mTabHost.setCurrentTab(index);
                // if (!mIsOtherUser)
                // PreferenceManager.getDefaultSharedPreferences(mActivity).edit().putInt("lastProfileIndex",mLastTabIndex).commit();
                hsv.scrollTo(mTabWidget.getChildTabViewAt(index).getLeft()
                        + mTabWidget.getChildTabViewAt(index).getWidth() / 2 - getWidth() / 2, 0);
            }

        });
        
        
        // ((FrameLayout) findViewById(R.id.tab_holder)).addView(tabLayout);

        final ScTabView emptyView = new ScTabView(mActivity);

        LazyBaseAdapter adp = new TracklistAdapter(mActivity, new ArrayList<Parcelable>());
        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(mActivity, adp, getUserTracksUrl(),
                CloudUtils.Model.track);

        final ScTabView tracksView = mTracksView = new ScTabView(mActivity, adpWrap);
        CloudUtils.createTabList(mActivity, tracksView, adpWrap, CloudUtils.ListId.LIST_USER_TRACKS);
        CloudUtils.createTab(mTabHost, "tracks", mActivity
                .getString(R.string.tab_tracks), null, emptyView);

        adp = new TracklistAdapter(mActivity, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(mActivity, adp, getFavoritesUrl(), CloudUtils.Model.track);

        final ScTabView favoritesView = mFavoritesView = new ScTabView(mActivity, adpWrap);
        CloudUtils.createTabList(mActivity, favoritesView, adpWrap,
                CloudUtils.ListId.LIST_USER_FAVORITES);
        CloudUtils.createTab(mTabHost, "favorites", mActivity
                .getString(R.string.tab_favorites), null, emptyView);

        final ScTabView detailsView = new ScTabView(mActivity);
        detailsView.addView(mDetailsView);
        
        CloudUtils.createTab(mTabHost, "details",
                mActivity.getString(R.string.tab_info), null, emptyView);

        adp = new UserlistAdapter(mActivity, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(mActivity, adp, getFollowingsUrl(), CloudUtils.Model.user);

        final ScTabView followingsView = new ScTabView(mActivity);
        CloudUtils.createTabList(mActivity, followingsView, adpWrap,
                CloudUtils.ListId.LIST_USER_FOLLOWINGS);
        CloudUtils.createTab(mTabHost, "followings", mActivity
                .getString(R.string.tab_followings), null, emptyView);

        adp = new UserlistAdapter(mActivity, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(mActivity, adp, getFollowersUrl(), CloudUtils.Model.user);

        final ScTabView followersView = mFollowersView = new ScTabView(mActivity);
        CloudUtils.createTabList(mActivity, followersView, adpWrap,
                CloudUtils.ListId.LIST_USER_FOLLOWERS);
        CloudUtils.createTab(mTabHost, "followers", mActivity
                .getString(R.string.tab_followers), null, emptyView);
        

        CloudUtils.configureTabs(mActivity, mTabWidget, 30, -1, true);
        CloudUtils.setTabTextStyle(mActivity, mTabWidget, true);
        

        if (!mIsOtherUser){
            mWorkspaceView.initWorkspace(0, PreferenceManager.getDefaultSharedPreferences(mActivity ).getInt("lastProfileIndex",0));
            mTabHost.setCurrentTab(PreferenceManager.getDefaultSharedPreferences(mActivity ).getInt("lastProfileIndex",0));
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
        
        if (mActivity instanceof ScProfile){

            // XXX
           ((ScProfile) mActivity).setTabHost(mTabHost);
        }
        
        mTabHost.setOnTabChangedListener(tabListener);
        
    }

    protected void setTabTextInfo() {
        if (mTabWidget != null && mUserData != null) {
            if (!TextUtils.isEmpty(mUserData.track_count))
                CloudUtils.setTabText(mTabWidget, 0, mActivity.getResources().getString(
                        R.string.tab_tracks)
                        + " (" + mUserData.track_count + ")");
            else
                CloudUtils.setTabText(mTabWidget, 0, mActivity.getResources().getString(
                        R.string.tab_tracks));

            if (!TextUtils.isEmpty(mUserData.public_favorites_count))
                CloudUtils.setTabText(mTabWidget, 1, mActivity.getResources().getString(
                        R.string.tab_favorites)
                        + " (" + mUserData.public_favorites_count + ")");
            else
                CloudUtils.setTabText(mTabWidget, 1, mActivity.getResources().getString(
                        R.string.tab_favorites));
            
            if (!TextUtils.isEmpty(mUserData.followings_count))
                CloudUtils.setTabText(mTabWidget, 3, mActivity.getResources().getString(
                        R.string.tab_followings)
                        + " (" + mUserData.followings_count + ")");
            else
                CloudUtils.setTabText(mTabWidget, 3, mActivity.getResources().getString(
                        R.string.tab_followings));
            
            if (!TextUtils.isEmpty(mUserData.followers_count))
                CloudUtils.setTabText(mTabWidget, 4, mActivity.getResources().getString(
                        R.string.tab_followers)
                        + " (" + mUserData.followers_count + ")");
            else
                CloudUtils.setTabText(mTabWidget, 4, mActivity.getResources().getString(
                        R.string.tab_followers));
            
            findViewById(R.id.tab_scroller).scrollTo(mTabWidget.getChildTabViewAt(mTabHost.getCurrentTab()).getLeft()
                    + mTabWidget.getChildTabViewAt(mTabHost.getCurrentTab()).getWidth() / 2 - getWidth() / 2, 0);
        }
    }

    private OnTabChangeListener tabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String arg0) {
            if (mWorkspaceView != null)
                mWorkspaceView.setDisplayedChild(mTabHost.getCurrentTab(),(Math.abs(mLastTabIndex - mTabHost.getCurrentTab()) > 1));
            
            mLastTab = (ScTabView) mTabHost.getCurrentView();
            mLastTabIndex = mTabHost.getCurrentTab();
            if (!mIsOtherUser)
                PreferenceManager.getDefaultSharedPreferences(mActivity).edit().putInt(
                        "lastProfileIndex", mLastTabIndex).commit();
            
            
        }
    };

    private void checkFollowingStatus() {
        if (!mIsOtherUser)
            return;

        mFollowingChecked = true;
        try {
            new CheckFollowingStatusTask().execute(CloudAPI.Enddpoints.MY_FOLLOWINGS + "/"
                    + mUserLoadId);

        } catch (Exception e) {
            Log.e(TAG, "error", e);
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
                    if (_isFollowing)
                        mFollowResult = mActivity
                                .getSoundCloudApplication().putContent(
                                        CloudAPI.Enddpoints.MY_USERS + "/"
                                                + mUserData.id, null).getStatusLine().getStatusCode();
                    else
                        mFollowResult = mActivity
                                .getSoundCloudApplication().deleteContent(
                                        CloudAPI.Enddpoints.MY_USERS + "/"
                                                + mUserData.id).getStatusLine().getStatusCode();

                } catch (Exception e) {
                    Log.e(TAG, "error", e);
                    mActivity.setException(e);
                }
                mActivity.mHandler.post(mSetFollowingResult);
            }
        };
        t.start();
    }

    // Create runnable for posting since we update the following asynchronously
    final Runnable mSetFollowingResult = new Runnable() {
        public void run() {
            if (mActivity != null){
                mActivity.handleException();
                mActivity.handleError();
            }
            boolean success = false;
            if (mFollowResult != 0) {
                if (mFollowResult == 200 || mFollowResult == 201 || mFollowResult == 404) {
                        success = true;
                }
            }
            
            if (!success){
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

        if (mUserData != null
                && Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mActivity)
                        .getString("currentUserId", "-1")) == mUserData.id) {
            return;
        }

        mFavorite.setVisibility(View.VISIBLE);
    }

    public void mapUser(Parcelable p) {

        mUserData = (User) p; // save to details object for restoring state

        if (mUserData.id == null)
            return;

        if (mUserLoadId == null && !mFollowingChecked) {
            mUserLoadId = mUserData.id;
            checkFollowingStatus();
        } else {
            mUserLoadId = mUserData.id;
        }

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
        if (getContext().getResources().getDisplayMetrics().density > 1)
            remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.large);
        else
            remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.badge);

        if (!remoteUrl.equals(_iconURL)) {
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
            if ((avatarResult = ImageLoader.get(mActivity).bind(mIcon, _iconURL, null)) != BindResult.OK) {
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
            mapUser(p);
    }

    protected String getDetailsUrl() {
        return mIsOtherUser ? CloudAPI.Enddpoints.USER_DETAILS.replace("{user_id}", Long
                .toString(mUserLoadId)) : CloudAPI.Enddpoints.MY_DETAILS;
    }

    protected String getUserTracksUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                CloudAPI.Enddpoints.USER_TRACKS.replace("{user_id}", Long
                        .toString(mUserLoadId)), getTrackOrder()) : CloudUtils
                .buildRequestPath(CloudAPI.Enddpoints.MY_TRACKS, getTrackOrder());
    }

    private String getTrackOrder() {
        return PreferenceManager.getDefaultSharedPreferences(mActivity)
                .getString("defaultTrackSorting", "");
    }

    protected String getFavoritesUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                CloudAPI.Enddpoints.USER_FAVORITES.replace("{user_id}", Long
                        .toString(mUserLoadId)), "favorited_at") : CloudUtils
                .buildRequestPath(CloudAPI.Enddpoints.MY_FAVORITES,
                        getTrackOrder());
    }

    protected String getFollowersUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                CloudAPI.Enddpoints.USER_FOLLOWERS.replace("{user_id}", Long
                        .toString(mUserLoadId)), getUserOrder()) : CloudUtils
                .buildRequestPath(CloudAPI.Enddpoints.MY_FOLLOWERS,
                       getTrackOrder());
    }

    private String getUserOrder() {

        return PreferenceManager.getDefaultSharedPreferences(mActivity)
                .getString("defaultUserSorting", "");

    }

    protected String getFollowingsUrl() {
        return mIsOtherUser ? CloudUtils.buildRequestPath(
                CloudAPI.Enddpoints.USER_FOLLOWINGS.replace("{user_id}", Long
                        .toString(mUserLoadId)), getUserOrder()) : CloudUtils
                .buildRequestPath(CloudAPI.Enddpoints.MY_FOLLOWINGS,
                        getTrackOrder());
    }

    private class CheckFollowingStatusTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return CloudUtils.streamToString(
                        mActivity.getSoundCloudApplication()
                                .executeRequest(urls[0]));
            } catch (Exception e) {
                Log.e(TAG, "error", e);
                return "";
            }
        }

        @Override
        protected void onPostExecute(String response) {
            _isFollowing = !(TextUtils.isEmpty(response) || response.contains("<error>"));
            setFollowingButtonText();
        }
    }
}
