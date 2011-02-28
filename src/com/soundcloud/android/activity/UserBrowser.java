package com.soundcloud.android.activity;


import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
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

import android.content.Intent;
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

import java.io.IOException;
import java.util.ArrayList;

public class UserBrowser extends ScActivity {
    private static String TAG = "UserBrowser";

    private ImageView mIcon;

    private FrameLayout mDetailsView;

    private TextView mUser;
    private TextView mLocation;
    private TextView mFullName;
    private TextView mWebsite;
    private TextView mDiscogsName;
    private TextView mMyspaceName;
    private TextView mDescription;

    private ImageButton mFollow;

    private String _iconURL;

    private ScTabView mTracksView;
    private ScTabView mFavoritesView;
    private ScTabView mFollowersView;

    private WorkspaceView mWorkspaceView;

    private long mUserLoadId;

    private boolean _isFollowing;
    private LoadTask mLoadDetailsTask;

    private int mFollowResult;

    private TabWidget mTabWidget;
    private TabHost mTabHost;

    private int mLastTabIndex;

    private User mUserData;

    private ImageLoader.BindResult avatarResult;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.user_view);

        mDetailsView = (FrameLayout) getLayoutInflater().inflate(R.layout.user_details, null);

        mIcon = (ImageView) findViewById(R.id.user_icon);
        mUser = (TextView) findViewById(R.id.username);
        mLocation = (TextView) findViewById(R.id.location);

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

        mFollow = (ImageButton) findViewById(R.id.btn_favorite);
        mFollow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                toggleFollowing();
            }
        });

        mFollow.setVisibility(View.GONE);
        mLastTabIndex = 0;

        Intent intent = getIntent();

        if (intent.hasExtra("user")) {
            loadUserByObject((User) intent.getParcelableExtra("user"));
            intent.removeExtra("user");
        } else if (intent.hasExtra("userId")) {
            loadUserById(intent.getLongExtra("userId", -1));
            intent.removeExtra("userId");
        } else {
            loadYou();
        }

        loadDetails();
    }


    @Override
    protected void onResume() {
        tracker.trackPageView("/profile");
        tracker.dispatch();

        checkFollowingStatus();

        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mWorkspaceView != null) {
            ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild())).onStart();
        } else if (mTabHost != null) {
            ((ScTabView) mTabHost.getCurrentView()).onStart();
        }
    }


    @Override
    public void onRefresh() {
        if (avatarResult == BindResult.ERROR)
            reloadAvatar();

        mTracksView.onRefresh();
        mFavoritesView.onRefresh();
        mFollowersView.onRefresh();
        mFavoritesView.onRefresh();

        if (mLoadDetailsTask != null) {
            if (!CloudUtils.isTaskFinished(mLoadDetailsTask)) {
                mLoadDetailsTask.cancel(true);
            }
        }

        loadDetails();

        if (mWorkspaceView != null) {
            ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild())).onRefresh();
        } else {
            ((ScTabView) mTabHost.getCurrentView()).onRefresh();
        }
    }

    private void loadYou() {
        if (getUserId() != -1) {
            mapUser(SoundCloudDB.getInstance().resolveUserById(
                    getContentResolver(),
                    getUserId()));

            mUserLoadId = getUserId();
        }
        build();
    }


    private void loadUserById(long userId) {
        mapUser(SoundCloudDB.getInstance().resolveUserById(getContentResolver(), userId));
        build();
    }

    private void loadUserByObject(User userInfo) {
        mUserLoadId = userInfo.id;
        mapUser(userInfo);
        build();
    }


    private void loadDetails() {
        mLoadDetailsTask = new LoadUserDetailsTask();
        mLoadDetailsTask.loadModel = CloudUtils.Model.user;
        mLoadDetailsTask.setActivity(this);
        mLoadDetailsTask.execute(getSoundCloudApplication().getRequest(getDetailsUrl(), null));
    }

    private class LoadUserDetailsTask extends LoadDetailsTask {
        @Override
        protected void mapDetails(Parcelable update) {
            mapUser((User) update);
        }
    }

    private void build() {
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
        if (isOtherUser()){
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_user_tracks_text).replace("{username}", mUserData.username));
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_tracks_text));
        }

        mTracksView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, mTracksView, adpWrap, CloudUtils.ListId.LIST_USER_TRACKS, null);
        CloudUtils.createTab(mTabHost, "tracks", getString(R.string.tab_tracks), null, emptyView);

        adp = new TracklistAdapter(this, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(this, adp, getFavoritesUrl(), CloudUtils.Model.track);
        if (isOtherUser()){
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_user_favorites_text).replace("{username}", mUserData.username));
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_favorites_text));
        }


        mFavoritesView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, mFavoritesView, adpWrap, CloudUtils.ListId.LIST_USER_FAVORITES, null);
        CloudUtils.createTab(mTabHost, "favorites", getString(R.string.tab_favorites), null, emptyView);

        final ScTabView detailsView = new ScTabView(this);
        detailsView.addView(mDetailsView);

        CloudUtils.createTab(mTabHost, "details", getString(R.string.tab_info), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(this, adp, getFollowingsUrl(), CloudUtils.Model.user);

        final ScTabView followingsView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, followingsView, adpWrap, CloudUtils.ListId.LIST_USER_FOLLOWINGS, null);
        CloudUtils.createTab(mTabHost, "followings", getString(R.string.tab_followings), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(this, adp, getFollowersUrl(), CloudUtils.Model.user);

        final ScTabView followersView = mFollowersView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, followersView, adpWrap, CloudUtils.ListId.LIST_USER_FOLLOWERS, null);
        CloudUtils.createTab(mTabHost, "followers", getString(R.string.tab_followers), null, emptyView);

        CloudUtils.configureTabs(this, mTabWidget, 30, -1, true);
        CloudUtils.setTabTextStyle(this, mTabWidget, true);

        if (!isOtherUser()) {
            mLastTabIndex = PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt(SoundCloudApplication.PROFILE_IDX, 0);
            mWorkspaceView.initWorkspace(mLastTabIndex);
            mTabHost.setCurrentTab(mLastTabIndex);
        } else {
            mWorkspaceView.initWorkspace(0);
        }

        mWorkspaceView.addView(mTracksView);
        mWorkspaceView.addView(mFavoritesView);
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

    private void setTabTextInfo() {
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
            if (!isOtherUser()) {
                PreferenceManager.getDefaultSharedPreferences(UserBrowser.this).edit()
                        .putInt(SoundCloudApplication.PROFILE_IDX, mLastTabIndex).commit();
            }
        }
    };

    private boolean isOtherUser() {
        return mUserLoadId != getUserId();
    }

    private void checkFollowingStatus() {
        if (isOtherUser()) {
            new CheckFollowingStatusTask(getSoundCloudApplication()) {
                @Override
                protected void onPostExecute(Boolean b) {
                    _isFollowing = (b == null) ? false : b;
                    setFollowingButtonText();
                }
            }.execute(mUserLoadId);
        }
    }

    private void toggleFollowing() {
        mFollow.setEnabled(false);
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

            if (!(mFollowResult == 200 || mFollowResult == 201 || mFollowResult == 404)) {
                _isFollowing = !_isFollowing;
                setFollowingButtonText();
            }
            mFollow.setEnabled(true);
        }
    };

    private void setFollowingButtonText() {
        if (isOtherUser()) {
            mFollow.setImageResource(_isFollowing ?
                    R.drawable.ic_unfollow_states : R.drawable.ic_follow_states);

            mFollow.setVisibility(View.VISIBLE);
        }
    }

    private void mapUser(User user) {
        if (user == null || user.id == null)
            return;

        mUserData = user;
        mUserLoadId = mUserData.id;

        mUser.setText(mUserData.username);
        mLocation.setText(CloudUtils.getLocationString(mUserData.country, mUserData.country));
        setTabTextInfo();

        String remoteUrl;
        if (getResources().getDisplayMetrics().density > 1) {
            remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.large);
        } else {
            remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.badge);
        }

        Log.i(TAG, "ICON URL " + remoteUrl);

        if (!remoteUrl.equals(_iconURL)) {
            Log.i(TAG, "Setting icon url");
            _iconURL = remoteUrl;
            reloadAvatar();
        }

        boolean _showTable = false;

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
            mDescription.setText(Html.fromHtml((mUserData).description.replace(System.getProperty("line.separator"), "<br/>")));
            mDescription.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if (_showTable) {
            mDetailsView.findViewById(R.id.txt_empty).setVisibility(View.GONE);
        } else {
            mDetailsView.findViewById(R.id.txt_empty).setVisibility(View.VISIBLE);
        }

    }

    private void reloadAvatar() {
        if (CloudUtils.checkIconShouldLoad(_iconURL)) {
            if ((avatarResult = ImageLoader.get(this).bind(mIcon, _iconURL, null)) != BindResult.OK) {
                mIcon.setImageDrawable(getResources().getDrawable(R.drawable.avatar_badge));
            }
        }
    }

    private String getDetailsUrl() {
        return CloudAPI.Enddpoints.USER_DETAILS.replace("{user_id}",
                Long.toString(mUserLoadId));
    }

    private String getUserTracksUrl() {
        return CloudUtils.buildRequestPath(
                CloudAPI.Enddpoints.USER_TRACKS.replace("{user_id}",
                        Long.toString(mUserLoadId)), getTrackOrder());
    }

    private String getFavoritesUrl() {
        return CloudUtils.buildRequestPath(
                CloudAPI.Enddpoints.USER_FAVORITES.replace("{user_id}",
                        Long.toString(mUserLoadId)), "favorited_at");
    }

    private String getFollowersUrl() {
        return CloudUtils.buildRequestPath(CloudAPI.Enddpoints.USER_FOLLOWERS.replace("{user_id}",
                Long.toString(mUserLoadId)), getUserOrder());
    }

    private String getFollowingsUrl() {
        return CloudUtils.buildRequestPath(CloudAPI.Enddpoints.USER_FOLLOWINGS.replace("{user_id}",
                Long.toString(mUserLoadId)), getUserOrder());
    }

    private String getUserOrder() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString("defaultUserSorting", "");

    }

    private String getTrackOrder() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString("defaultTrackSorting", "");
    }
}
