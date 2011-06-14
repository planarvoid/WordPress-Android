package com.soundcloud.android.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.widget.ImageView.ScaleType;
import android.widget.TabHost.OnTabChangeListener;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.adapter.*;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.LoadConnectionsTask;
import com.soundcloud.android.task.LoadConnectionsTask.ConnectionsListener;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.CloudUtils.GraphicsSizes;
import com.soundcloud.android.view.*;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class UserBrowser extends ScActivity implements WorkspaceView.OnScreenChangeListener, ConnectionsListener, FollowStatus.Listener {
    private ImageView mIcon;

    private FrameLayout mDetailsView;
    private FriendFinderView mFriendFinderView;

    private TextView mUser;
    private TextView mLocation;
    private TextView mFullName;
    private TextView mWebsite;
    private TextView mDiscogsName;
    private TextView mMyspaceName;
    private TextView mDescription;
    private HorizontalScrollView hsv;

    private ImageButton mFollow;
    private Drawable mFollowDrawable;
    private Drawable mUnfollowDrawable;

    private String _iconURL;

    private WorkspaceView mWorkspaceView;

    private long mUserLoadId;

    private LoadUserTask mLoadDetailsTask;

    private int mFollowResult;

    private TabWidget mTabWidget;
    private TabHost mTabHost;

    private int mLastTabIndex;

    private User mUserData;

    private ImageLoader.BindResult avatarResult;

    private LoadConnectionsTask mConnectionsTask;
    private List<Connection> mConnections;

    private static CharSequence[] RECORDING_ITEMS = {"Edit", "Listen", "Upload", "Delete"};
    private static CharSequence[] EXTERNAL_RECORDING_ITEMS = {"Edit", "Upload", "Delete"};

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.user_view);

        mDetailsView = (FrameLayout) getLayoutInflater().inflate(R.layout.user_browser, null);

        mIcon = (ImageView) findViewById(R.id.user_icon);
        mUser = (TextView) findViewById(R.id.username);
        mFullName = (TextView) findViewById(R.id.fullname);

        mLocation = (TextView) mDetailsView.findViewById(R.id.location);
        mWebsite = (TextView) mDetailsView.findViewById(R.id.website);
        mDiscogsName = (TextView) mDetailsView.findViewById(R.id.discogs_name);
        mMyspaceName = (TextView) mDetailsView.findViewById(R.id.myspace_name);
        mDescription = (TextView) mDetailsView.findViewById(R.id.description);

        mIcon.setScaleType(ScaleType.CENTER_INSIDE);
        if (getResources().getDisplayMetrics().density > 1) {
            mIcon.getLayoutParams().width = 100;
            mIcon.getLayoutParams().height = 100;
        }

        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CloudUtils.checkIconShouldLoad(_iconURL)) {
                    new FullImageDialog(UserBrowser.this, CloudUtils.formatGraphicsUrl(_iconURL,
                            GraphicsSizes.CROP)).show();
                }

            }
        });

        mFollow = (ImageButton) findViewById(R.id.btn_favorite);
        mFollow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                toggleFollowing();
            }
        });

        mFollow.setVisibility(View.GONE);
        mLastTabIndex = 0;

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mLoadDetailsTask = (LoadUserTask) mPreviousState[1];
            mLoadDetailsTask.setActivity(this);

            mapUser((User) mPreviousState[2]);

            if (!isOtherUser()) {
                mConnectionsTask = (LoadConnectionsTask) mPreviousState[3];
                mConnections = (List<Connection>) mPreviousState[4];
            }

            build();

            restoreAdapterStates((Object[]) mPreviousState[5]);

            if (mPreviousState[6] != null) mFriendFinderView.setState(Integer.parseInt(mPreviousState[6].toString()), false);

        } else {
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
        }

         if (!isOtherUser()) {
            if (mConnectionsTask == null) {
                mConnectionsTask = new LoadConnectionsTask(getApp());
            }

            if (!CloudUtils.isTaskFinished(mConnectionsTask)) {
                mConnectionsTask.setListener(this);
                if (CloudUtils.isTaskPending(mConnectionsTask)) mConnectionsTask.execute();
            }
        }
        loadDetails();
    }

    @Override
    protected void onResume() {
        pageTrack("/profile");
        super.onResume();
    }


    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{
                super.onRetainNonConfigurationInstance(),
                mLoadDetailsTask,
                mUserData,
                mConnectionsTask,
                mConnections,
                getAdapterStates(),
                mFriendFinderView != null ? mFriendFinderView.getCurrentState() : null
        };
    }

    private Object[] getAdapterStates() {
        Object[] states = new Object[mLists.size()];
        int i = 0;
        for (LazyListView list : mLists) {
            states[i] = LazyEndlessAdapter.class.isAssignableFrom(list.getWrapper().getClass()) ?
                    (list.getWrapper()).saveState() : null;
            i++;
        }
        return states;
    }

    private void restoreAdapterStates(Object[] adapterStates) {
        int i = 0;
        for (Object adapterState : adapterStates) {
            if (adapterState != null) (mLists.get(i).getWrapper()).restoreState((Object[]) adapterState);
            i++;
        }
    }


    @Override
    public void onRefresh() {
        if (avatarResult == BindResult.ERROR)
            reloadAvatar();

        if (mLoadDetailsTask != null) {
            if (!CloudUtils.isTaskFinished(mLoadDetailsTask)) {
                mLoadDetailsTask.cancel(true);
            }
            mLoadDetailsTask = null;
        }

        loadDetails();

        if (!isOtherUser() && CloudUtils.isTaskFinished(mConnectionsTask)) {
            mConnectionsTask = new LoadConnectionsTask(getApp());
            mConnectionsTask.setListener(this);
            mConnectionsTask.execute();
            if (mFriendFinderView != null) mFriendFinderView.setState(FriendFinderView.States.LOADING,false);
        }

        if (!(mWorkspaceView.getChildAt(mWorkspaceView.getCurrentScreen()) instanceof FriendFinderView)) {
            ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getCurrentScreen())).onRefresh(true);
        }
    }

    @Override
    public void onConnections(List<Connection> connections) {
        mConnections = connections;
        mFriendFinderView.onConnections(connections, true);
    }

    public void onScreenChanged(View newScreen, int newScreenIndex) {
        mTabHost.setCurrentTab(newScreenIndex);
        if (hsv != null) {
            hsv.scrollTo(mTabWidget.getChildTabViewAt(newScreenIndex).getLeft()
                    + mTabWidget.getChildTabViewAt(newScreenIndex).getWidth() / 2 - getWidth() / 2, 0);
        }
    }

    public void onScreenChanging(View newScreen, int newScreenIndex) {
        // do nothing
    }

    private void loadYou() {
        if (getUserId() != -1) {
            User u = SoundCloudDB.getUserById(getContentResolver(), getUserId());
            if (u == null) u = new User(getApp());
            mapUser(u);
            mUserLoadId = u.id;
        }

        build();
    }


    private void loadUserById(long userId) {
        mapUser(SoundCloudDB.getUserById(getContentResolver(), userId));
        if (mUserData == null) {
            mUserData = new User();
            mUserLoadId = mUserData.id = userId;
        }
        build();
        FollowStatus.get().requestUserFollowings(getApp(),this, false);
    }

    private void loadUserByObject(User userInfo) {
        if (userInfo == null) return;

        mUserLoadId = userInfo.id;
        mapUser(userInfo);
        build();
        FollowStatus.get().requestUserFollowings(getApp(), this, false);
    }


    private void loadDetails() {
        if (mLoadDetailsTask == null) {
            mLoadDetailsTask = new LoadUserTask(getApp());
            mLoadDetailsTask.setActivity(this);
        }

        if (CloudUtils.isTaskPending(mLoadDetailsTask)) {
            mLoadDetailsTask.execute(Request.to(Endpoints.USER_DETAILS, mUserLoadId));
        }
    }

    public void onFollowings(boolean success, FollowStatus status) {
        setFollowingButtonText();
    }

    private class LoadUserTask extends LoadTask<User> {
        public LoadUserTask(SoundCloudApplication api) {
            super(api, User.class);
        }

        @Override
        protected void onPostExecute(User user) {
            if (user != null) {
                SoundCloudDB.writeUser(getContentResolver(), user, WriteState.all,
                        getApp().getCurrentUserId());
                mapUser(user);
            }
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
        mWorkspaceView = (WorkspaceView) findViewById(R.id.workspace_view);

        hsv = (HorizontalScrollView) findViewById(R.id.tab_scroller);
        hsv.setBackgroundColor(0xFF555555);

        final ScTabView emptyView = new ScTabView(this);

        LazyBaseAdapter adp = isOtherUser() ? new TracklistAdapter(this,
                new ArrayList<Parcelable>(), Track.class) : new MyTracksAdapter(this,
                new ArrayList<Parcelable>(), Track.class);

        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_TRACKS, mUserLoadId));
        if (isOtherUser()) {
            if (mUserData != null) {
                adpWrap.setEmptyViewText(getResources().getString(
                        R.string.empty_user_tracks_text,
                        mUserData.username == null ? getResources().getString(R.string.this_user)
                                : mUserData.username));
            }
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_tracks_text));
        }

        ScTabView tracksView = new ScTabView(this, adpWrap);
        CloudUtils.configureTabList(this, buildList(), tracksView, adpWrap, CloudUtils.ListId.LIST_USER_TRACKS, null);
        CloudUtils.createTab(mTabHost, "tracks", getString(R.string.tab_tracks), null, emptyView);

        adp = new TracklistAdapter(this, new ArrayList<Parcelable>(), Track.class);
        adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_FAVORITES, mUserLoadId));
        if (isOtherUser()){
            if (mUserData != null) {
                adpWrap.setEmptyViewText(getResources().getString(
                        R.string.empty_user_favorites_text,
                        mUserData.username == null ? getResources().getString(R.string.this_user)
                                : mUserData.username));
            }
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_favorites_text));
        }


        ScTabView favoritesView = new ScTabView(this, adpWrap);
        CloudUtils.configureTabList(this, buildList(), favoritesView, adpWrap, CloudUtils.ListId.LIST_USER_FAVORITES, null);
        CloudUtils.createTab(mTabHost, "favorites", getString(R.string.tab_favorites), null, emptyView);

        final ScTabView detailsView = new ScTabView(this);
        detailsView.addView(mDetailsView);

        CloudUtils.createTab(mTabHost, "details", getString(R.string.tab_info), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class);
        adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_FOLLOWINGS, mUserLoadId));

        if (isOtherUser()) {
            if (mUserData != null) {
                adpWrap.setEmptyViewText(getResources().getString(
                        R.string.empty_user_followings_text,
                        mUserData.username == null ? getResources().getString(R.string.this_user)
                                : mUserData.username));
            }
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_followings_text));
        }

        final ScTabView followingsView = new ScTabView(this, adpWrap);
        CloudUtils.configureTabList(this, buildList(), followingsView, adpWrap, CloudUtils.ListId.LIST_USER_FOLLOWINGS, null).disableLongClickListener();
        CloudUtils.createTab(mTabHost, "followings", getString(R.string.tab_followings), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class);
        adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_FOLLOWERS, mUserLoadId));

        if (isOtherUser()) {
            if (mUserData != null) {
                adpWrap.setEmptyViewText(getResources().getString(
                        R.string.empty_user_followers_text,
                        mUserData.username == null ? getResources().getString(R.string.this_user)
                                : mUserData.username));
            }
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_followers_text));
        }

        final ScTabView followersView = new ScTabView(this, adpWrap);
        CloudUtils.configureTabList(this, buildList(), followersView, adpWrap, CloudUtils.ListId.LIST_USER_FOLLOWERS, null).disableLongClickListener();
        CloudUtils.createTab(mTabHost, "followers", getString(R.string.tab_followers), null, emptyView);

        if (!isOtherUser()) {
            FriendFinderAdapter ffAdp = new FriendFinderAdapter(this);
            SectionedEndlessAdapter ffAdpWrap = new SectionedEndlessAdapter(this, ffAdp);

            mFriendFinderView = new FriendFinderView(this, ffAdpWrap);
            mFriendFinderView.friendList = CloudUtils.configureTabList(this, configureList(new SectionedListView(this)),
                    mFriendFinderView, ffAdpWrap, CloudUtils.ListId.LIST_USER_SUGGESTED, null);
            mFriendFinderView.friendList.disableLongClickListener();
            CloudUtils.createTab(mTabHost, "friendFinder", getString(R.string.tab_suggested), null, emptyView);

            if (mConnectionsTask == null || !CloudUtils.isTaskFinished(mConnectionsTask)) {
                mFriendFinderView.setState(FriendFinderView.States.LOADING, false);
            } else {
                mFriendFinderView.onConnections(mConnections, false);
            }
        }

        CloudUtils.configureTabs(this, mTabWidget, 30, -1, true);
        CloudUtils.setTabTextStyle(this, mTabWidget, true);

        if (!isOtherUser()) {
            mLastTabIndex = getApp().getAccountDataInt(User.DataKeys.PROFILE_IDX);
            mWorkspaceView.initWorkspace(mLastTabIndex);
            mTabHost.setCurrentTab(mLastTabIndex);
        } else {
            mWorkspaceView.initWorkspace(0);
        }

        mWorkspaceView.addView(tracksView);
        mWorkspaceView.addView(favoritesView);
        mWorkspaceView.addView(detailsView);
        mWorkspaceView.addView(followingsView);
        mWorkspaceView.addView(followersView);
        if (mFriendFinderView != null) mWorkspaceView.addView(mFriendFinderView);

        mWorkspaceView.setOnScreenChangeListener(this);

        mTabWidget.invalidate();
        setTabTextInfo();

        mTabHost.setOnTabChangedListener(tabListener);
        hsv.setFillViewport(true);
    }

    private int getWidth() {
        return findViewById(R.id.user_details_root).getWidth();
    }

    private void setTabTextInfo() {
        if (mTabWidget != null && mUserData != null) {

            CloudUtils.setTabText(mTabWidget, 2, getString(R.string.tab_info));

            if (mUserData.track_count != 0) {
                CloudUtils.setTabText(mTabWidget, 0, getString(R.string.tab_tracks)
                        + " (" + mUserData.track_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 0, getString(
                        R.string.tab_tracks));
            }

            if (mUserData.public_favorites_count != 0) {
                CloudUtils.setTabText(mTabWidget, 1, getString(R.string.tab_favorites)
                        + " (" + mUserData.public_favorites_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 1, getString(R.string.tab_favorites));
            }

            if (mUserData.followings_count != 0) {
                CloudUtils.setTabText(mTabWidget, 3, getString(R.string.tab_followings)
                        + " (" + mUserData.followings_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 3, getString(R.string.tab_followings));
            }


            if (mUserData.followers_count != 0) {
                CloudUtils.setTabText(mTabWidget, 4,
                        getString(R.string.tab_followers)
                                + " (" + mUserData.followers_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 4, getString(R.string.tab_followers));
            }

            ((HorizontalScrollView) findViewById(R.id.tab_scroller)).setFillViewport(true);
            mTabWidget.setCurrentTab(mTabHost.getCurrentTab()); //forces the tab lines to redraw

        }
    }

    private OnTabChangeListener tabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String arg0) {
            if (mWorkspaceView != null) {
                if (Math.abs(mLastTabIndex - mTabHost.getCurrentTab()) > 1) {
                    mWorkspaceView.setCurrentScreenNow(mTabHost.getCurrentTab());
                } else {
                    mWorkspaceView.setCurrentScreen(mTabHost.getCurrentTab());
                }
            }

            mLastTabIndex = mTabHost.getCurrentTab();
            if (!isOtherUser()) {
                getApp().setAccountData(User.DataKeys.PROFILE_IDX, Integer.toString(mLastTabIndex));
            }
        }
    };

    private boolean isOtherUser() {
        return mUserLoadId != getUserId();
    }

    public void setTab(int screen) {
        mWorkspaceView.setCurrentScreen(screen);
    }

    private void toggleFollowing() {
        mFollow.setEnabled(false);
        final boolean following = FollowStatus.get().toggleFollowing(mUserData.id);
        setFollowingButtonText();

        mFollowResult = 0;

        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        Thread t = new Thread() {
            @Override
            public void run() {
                final Request request = Request.to(Endpoints.MY_FOLLOWING, mUserData.id);
                try {
                    if (following) {
                        mFollowResult =
                                getApp().put(request).getStatusLine().getStatusCode();
                    } else {
                        mFollowResult =
                                getApp().delete(request).getStatusLine().getStatusCode();
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
    private final Runnable mSetFollowingResult = new Runnable() {
        public void run() {
            handleException();
            handleError();

            if (!(mFollowResult == 200 || mFollowResult == 201 || mFollowResult == 404)) {
                FollowStatus.get().toggleFollowing(mUserData.id);
                setFollowingButtonText();
            }
            mFollow.setEnabled(true);
        }
    };

    private void setFollowingButtonText() {
        if (isOtherUser()) {
            mFollow.setImageDrawable(FollowStatus.get().following(mUserData) ?
                    createDrawableIfNecessary(mUnfollowDrawable,R.drawable.ic_unfollow_states) :
                    createDrawableIfNecessary(mFollowDrawable,R.drawable.ic_follow_states));

            mFollow.setVisibility(View.VISIBLE);
        }
    }

    private Drawable ensureDrawable(Drawable d, int resId) {
        if (d == null) d = getResources().getDrawable(resId);
        return d;
    }

    private void mapUser(User user) {
        if (user == null || user.id <= 0)
            return;

        mUserData = user;
        mUserLoadId = mUserData.id;

        mUser.setText(mUserData.username);
        mFullName.setText(mUserData.full_name);
        setTabTextInfo();

        setFollowingButtonText();

        if (CloudUtils.checkIconShouldLoad(mUserData.avatar_url)) {
            String remoteUrl;
            if (getResources().getDisplayMetrics().density > 1) {
                remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.LARGE);
            } else {
                remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.BADGE);
            }

            if (_iconURL == null || avatarResult == BindResult.ERROR || !remoteUrl.substring(0, remoteUrl.indexOf("?")).equals(_iconURL.substring(0, _iconURL.indexOf("?")))) {
                _iconURL = remoteUrl;
                reloadAvatar();
            }
        }

        boolean _showTable = false;

        if (!TextUtils.isEmpty(mUserData.website)) {
            _showTable = true;
            mWebsite.setText(TextUtils.isEmpty(mUserData.website_title) ? CloudUtils
                    .stripProtocol(mUserData.website) : mUserData.website_title);
            mWebsite.setVisibility(View.VISIBLE);
            mWebsite.setFocusable(true);
            mWebsite.setClickable(true);
            mWebsite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(mUserData.website));
                    startActivity(viewIntent);
                }
            });
        } else {
            mWebsite.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mUserData.discogs_name)) {
            _showTable = true;
            mDiscogsName.setMovementMethod(LinkMovementMethod.getInstance());
            mDiscogsName.setVisibility(View.VISIBLE);
            mDiscogsName.setFocusable(true);
            mDiscogsName.setClickable(true);
            mDiscogsName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.discogs.com/artist/" + mUserData.discogs_name));
                    startActivity(viewIntent);
                }
            });
        } else {
            mDiscogsName.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mUserData.myspace_name)) {
            _showTable = true;
            mMyspaceName.setMovementMethod(LinkMovementMethod.getInstance());
            mMyspaceName.setVisibility(View.VISIBLE);
            mMyspaceName.setFocusable(true);
            mMyspaceName.setClickable(true);
            mMyspaceName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.myspace.com/" + mUserData.myspace_name));
                    startActivity(viewIntent);
                }
            });
        } else {
            mMyspaceName.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(mUserData.city) || !TextUtils.isEmpty(mUserData.country)) {
            _showTable = true;
            mLocation.setText(getString(R.string.from) + " " + CloudUtils.getLocationString(mUserData.city, mUserData.country));
            mLocation.setVisibility(View.VISIBLE);
        } else {
            mLocation.setVisibility(View.GONE);
        }


        if (!TextUtils.isEmpty(mUserData.description)) {
            _showTable = true;
            mDescription.setText(Html.fromHtml((mUserData).description.replace(System.getProperty("line.separator"), "<br/>")));
            mDescription.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if (_showTable) {
            mDetailsView.findViewById(R.id.txt_empty).setVisibility(View.GONE);
        } else {
            TextView txtEmpty = (TextView) mDetailsView.findViewById(R.id.txt_empty);
            txtEmpty.setText(isOtherUser() ? R.string.info_empty_other : R.string.info_empty_you);
            txtEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void reloadAvatar() {
        if (CloudUtils.checkIconShouldLoad(_iconURL)) {
            if ((avatarResult = ImageLoader.get(this).bind(mIcon, _iconURL, null)) != BindResult.OK) {
                mIcon.setImageDrawable(getResources().getDrawable(R.drawable.avatar_badge_large));
            }
        }
    }

    @Override
    protected void handleRecordingClick(Recording recording) {
        if (recording.upload_status == Recording.UploadStatus.UPLOADING)
            safeShowDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
        else {
            showRecordingDialog(recording);
        }
    }

    private void showRecordingDialog(final Recording recording) {
        final CharSequence[] curr_items = recording.external_upload ? EXTERNAL_RECORDING_ITEMS : RECORDING_ITEMS;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setInverseBackgroundForced(true);
        builder.setTitle(recording.sharingNote());
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.setItems(curr_items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (curr_items[item].equals(RECORDING_ITEMS[0])) {
                    startActivity(new Intent(UserBrowser.this, ScUpload.class).setData(recording.toUri()));
                } else if (curr_items[item].equals(RECORDING_ITEMS[1])) {
                    startActivity(new Intent(UserBrowser.this, ScCreate.class).setData(recording.toUri()));
                } else if (curr_items[item].equals(RECORDING_ITEMS[2])) {
                    startUpload(recording);
                } else if (curr_items[item].equals(RECORDING_ITEMS[3])) {
                    new AlertDialog.Builder(UserBrowser.this)
                    .setTitle(R.string.dialog_confirm_delete_recording_title)
                    .setMessage(R.string.dialog_confirm_delete_recording_message)
                    .setPositiveButton(getString(R.string.btn_yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    recording.delete(getContentResolver());
                                }
                            })
                    .setNegativeButton(getString(R.string.btn_no),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                }
                            }).create().show();
                }
            }
        });
        builder.create().show();
    }

    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD:
                return new AlertDialog.Builder(this).setTitle(R.string.dialog_cancel_upload_title)
                        .setMessage(R.string.dialog_cancel_upload_message).setPositiveButton(
                                getString(R.string.btn_yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    // XXX this should be handled by ScCreate
                                    mCreateService.cancelUpload();
                                } catch (RemoteException ignored) {
                                    Log.w(TAG, ignored);
                                }
                                removeDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
                            }
                        }).setNegativeButton(getString(R.string.btn_no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
                                    }
                                }).create();
            default:
                return super.onCreateDialog(which);
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

                    if (success && !isOtherUser()) {
                        if (mConnectionsTask != null) mConnectionsTask.setListener(null);

                        mConnectionsTask = new LoadConnectionsTask(getApp());
                        mConnectionsTask.setListener(this);
                        mConnectionsTask.execute();
                        if (mFriendFinderView != null) {
                            mFriendFinderView.setState(FriendFinderView.States.LOADING,false);
                        }
                    }
                }
        }
    }

}
