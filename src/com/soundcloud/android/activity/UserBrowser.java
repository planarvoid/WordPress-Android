package com.soundcloud.android.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.widget.ImageView.ScaleType;
import android.widget.TabHost.OnTabChangeListener;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.cache.Connections;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.cache.ParcelCache;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.*;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.ArrayList;
import java.util.List;

/** @noinspection unchecked*/
public class UserBrowser extends ScActivity implements WorkspaceView.OnScreenChangeListener, ParcelCache.Listener<Connection>, FollowStatus.Listener {
    private ImageView mIcon;

    private FrameLayout mDetailsView;
    private FriendFinderView mFriendFinderView;

    private TextView mUsername, mLocation, mFullName, mWebsite, mDiscogsName, mMyspaceName, mDescription;
    private HorizontalScrollView hsv;

    private ImageButton mFollowStateBtn;
    private Drawable mFollowDrawable, mUnfollowDrawable;

    private String mIconURL;

    private WorkspaceView mWorkspaceView;
    private LoadUserTask mLoadDetailsTask;

    private TabWidget mTabWidget;
    private TabHost mTabHost;

    private int mLastTabIndex;

    private User mUser;

    private ImageLoader.BindResult avatarResult;

    private List<Connection> mConnections;

    private Object mAdapterStates[];

    private static CharSequence[] RECORDING_ITEMS = {"Edit", "Listen", "Upload", "Delete"};
    private static CharSequence[] EXTERNAL_RECORDING_ITEMS = {"Edit", "Upload", "Delete"};

    public interface TabTags {
        String tracks = "tracks";
        String favorites = "favorites";
        String details = "details";
        String followings = "followings";
        String followers = "followers";
        String friend_finder = "friend_finder";
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.user_browser);

        mDetailsView = (FrameLayout) getLayoutInflater().inflate(R.layout.user_browser_details_view, null);

        mIcon = (ImageView) findViewById(R.id.user_icon);
        mUsername = (TextView) findViewById(R.id.username);
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
                if (CloudUtils.checkIconShouldLoad(mIconURL)) {
                    new FullImageDialog(
                        UserBrowser.this,
                        CloudUtils.formatGraphicsUrl(mIconURL, Consts.GraphicsSizes.CROP)
                    ).show();
                }

            }
        });

        mFollowStateBtn = (ImageButton) findViewById(R.id.btn_followState);
        mFollowStateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                toggleFollowing();
            }
        });

        mFollowDrawable = getResources().getDrawable(R.drawable.ic_follow_states);
        mUnfollowDrawable = getResources().getDrawable(R.drawable.ic_unfollow_states);

        mLastTabIndex = 0;

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mLoadDetailsTask = (LoadUserTask) mPreviousState[1];
            mLoadDetailsTask.setActivity(this);

            setUser((User) mPreviousState[2]);

            if (isMe()) {
                mConnections = (List<Connection>) mPreviousState[3];
            }

            build();

            restoreAdapterStates((Object[]) mPreviousState[4]);

            if (mPreviousState[5] != null)
                mFriendFinderView.setState(Integer.parseInt(mPreviousState[5].toString()), false);

        } else {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("user")) {
                loadUserByObject((User) intent.getParcelableExtra("user"));
            } else if (intent != null && intent.hasExtra("userId")) {
                loadUserById(intent.getLongExtra("userId", -1));
            } else {
                loadYou();
            }

            for (LazyListView list : mLists) {
                if (LazyEndlessAdapter.class.isAssignableFrom(list.getWrapper().getClass()))
                    list.onRefresh();
            }
        }

        if (isMe()) {
            Connections.get().requestUpdate(getApp(), this, false);
        }
        loadDetails();
    }

    @Override
    protected void onResume() {
        pageTrack("/profile");
        super.onResume();
    }


    @Override
    protected void onStart() {
        pageTrack("/profile");
        if (mAdapterStates != null){
            restoreAdapterStates(mAdapterStates);
            mAdapterStates = null;
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FollowStatus.get().removeListener(this);

        mAdapterStates = new Object[mLists.size()];
        int i = 0;
        for (LazyListView list : mLists) {
            if (list.getWrapper() != null){
                mAdapterStates[i] = list.getWrapper().saveState();
                list.getWrapper().cleanup();
            }
            i++;
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{
                super.onRetainNonConfigurationInstance(),
                mLoadDetailsTask,
                mUser,
                mConnections,
                mAdapterStates,
                mFriendFinderView != null ? mFriendFinderView.getCurrentState() : null
        };
    }

    private void restoreAdapterStates(Object[] adapterStates) {
        int i = 0;
        for (Object adapterState : adapterStates) {
            if (adapterState != null) {
                mLists.get(i).getWrapper().restoreState((Object[]) adapterState);
                if (mLists.get(i).getWrapper().isRefreshing()) {
                    mLists.get(i).prepareForRefresh();
                    mLists.get(i).setSelection(0);
                }
            }
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
        refreshConnections();

         if (!(mWorkspaceView.getChildAt(mWorkspaceView.getCurrentScreen()) instanceof FriendFinderView)) {
            ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getCurrentScreen())).onRefresh(true);
        }
    }


    public void refreshConnections(){
        if (isMe()) {
            Connections.get().requestUpdate(getApp(), this, true);
            if (mFriendFinderView != null) mFriendFinderView.setState(FriendFinderView.States.LOADING, false);
        }
    }

    public void onChanged(List<Connection> connections, ParcelCache<Connection> cache) {
        mConnections = connections;
        mFriendFinderView.onConnections(connections, true);
    }

    public void onScreenChanged(View newScreen, int newScreenIndex) { }

    public void onScreenChanging(View newScreen, int newScreenIndex) {
        mTabHost.setCurrentTab(newScreenIndex);
        if (hsv != null) {
            hsv.scrollTo(mTabWidget.getChildTabViewAt(newScreenIndex).getLeft()
                    + mTabWidget.getChildTabViewAt(newScreenIndex).getWidth() / 2 - getWidth() / 2, 0);
        }
    }

    private void loadYou() {
        if (getCurrentUserId() != -1) {
            User u = SoundCloudDB.getUserById(getContentResolver(), getCurrentUserId());
            if (u == null) u = new User(getApp());
            setUser(u);
        }
        build();
    }


    private void loadUserById(long userId) {
        setUser(SoundCloudDB.getUserById(getContentResolver(), userId));
        if (mUser == null) {
            mUser = new User();
        }
        build();
        FollowStatus.get().requestUserFollowings(getApp(), this, false);
    }

    private void loadUserByObject(User user) {
        if (user == null) return;
        setUser(user);
        build();
        FollowStatus.get().requestUserFollowings(getApp(), this, false);
    }


    private void loadDetails() {
        if (mLoadDetailsTask == null) {
            mLoadDetailsTask = new LoadUserTask(getApp());
            mLoadDetailsTask.setActivity(this);
        }

        if (CloudUtils.isTaskPending(mLoadDetailsTask)) {
            mLoadDetailsTask.execute(Request.to(Endpoints.USER_DETAILS, mUser.id));
        }
    }

    public void onChange(boolean success, FollowStatus status) {
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
                setUser(user);
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

        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_TRACKS, mUser.id));
        if (isOtherUser()) {
            if (mUser != null) {
                adpWrap.setEmptyViewText(getResources().getString(
                        R.string.empty_user_tracks_text,
                        mUser.username == null ? getResources().getString(R.string.this_user)
                                : mUser.username));
            }
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_tracks_text));
        }

        ScTabView tracksView = new ScTabView(this);
        tracksView.setLazyListView(buildList(), adpWrap, Consts.ListId.LIST_USER_TRACKS, true);
        CloudUtils.createTab(mTabHost, TabTags.tracks, getString(R.string.tab_tracks), null, emptyView);

        adp = new TracklistAdapter(this, new ArrayList<Parcelable>(), Track.class);
        adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_FAVORITES, mUser.id));
        if (isOtherUser()) {
            if (mUser != null) {
                adpWrap.setEmptyViewText(getResources().getString(
                        R.string.empty_user_favorites_text,
                        mUser.username == null ? getResources().getString(R.string.this_user)
                                : mUser.username));
            }
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_favorites_text));
        }


        ScTabView favoritesView = new ScTabView(this);
        favoritesView.setLazyListView(buildList(), adpWrap, Consts.ListId.LIST_USER_FAVORITES, true);
        CloudUtils.createTab(mTabHost, TabTags.favorites, getString(R.string.tab_favorites), null, emptyView);

        final ScTabView detailsView = new ScTabView(this);
        detailsView.addView(mDetailsView);

        CloudUtils.createTab(mTabHost, TabTags.details, getString(R.string.tab_info), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class);
        adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_FOLLOWINGS, mUser.id));

        if (isOtherUser()) {
            if (mUser != null) {
                adpWrap.setEmptyViewText(getResources().getString(
                        R.string.empty_user_followings_text,
                        mUser.username == null ? getResources().getString(R.string.this_user)
                                : mUser.username));
            }
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_followings_text));
        }

        final ScTabView followingsView = new ScTabView(this);
        followingsView.setLazyListView(buildList(), adpWrap, Consts.ListId.LIST_USER_FOLLOWINGS, true).disableLongClickListener();
        CloudUtils.createTab(mTabHost, TabTags.followings, getString(R.string.tab_followings), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class);
        adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_FOLLOWERS, mUser.id));

        if (isOtherUser()) {
            if (mUser != null) {
                adpWrap.setEmptyViewText(getResources().getString(
                        R.string.empty_user_followers_text,
                        mUser.username == null ? getResources().getString(R.string.this_user)
                                : mUser.username));
            }
        } else {
            adpWrap.setEmptyViewText(getResources().getString(R.string.empty_my_followers_text));
        }

        final ScTabView followersView = new ScTabView(this);
        followersView.setLazyListView(buildList(), adpWrap, Consts.ListId.LIST_USER_FOLLOWERS, true).disableLongClickListener();

        CloudUtils.createTab(mTabHost, TabTags.followers, getString(R.string.tab_followers), null, emptyView);

        if (isMe()) {
            mFriendFinderView = new FriendFinderView(this);
            CloudUtils.createTab(mTabHost, TabTags.friend_finder, getString(R.string.tab_friend_finder), null, emptyView);

            if (mConnections == null) {
                mFriendFinderView.setState(FriendFinderView.States.LOADING, false);
            } else {
                mFriendFinderView.onConnections(mConnections, false);
            }
        }

        CloudUtils.configureTabs(this, mTabWidget, 30, -1, true);
        CloudUtils.setTabTextStyle(this, mTabWidget, true);

        if (isMe()) {
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
        if (mTabWidget != null && mUser != null) {

            CloudUtils.setTabText(mTabWidget, 2, getString(R.string.tab_info));

            if (mUser.track_count != 0) {
                CloudUtils.setTabText(mTabWidget, 0, getString(R.string.tab_tracks)
                        + " (" + mUser.track_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 0, getString(
                        R.string.tab_tracks));
            }

            if (mUser.public_favorites_count != 0) {
                CloudUtils.setTabText(mTabWidget, 1, getString(R.string.tab_favorites)
                        + " (" + mUser.public_favorites_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 1, getString(R.string.tab_favorites));
            }

            if (mUser.followings_count != 0) {
                CloudUtils.setTabText(mTabWidget, 3, getString(R.string.tab_followings)
                        + " (" + mUser.followings_count + ")");
            } else {
                CloudUtils.setTabText(mTabWidget, 3, getString(R.string.tab_followings));
            }


            if (mUser.followers_count != 0) {
                CloudUtils.setTabText(mTabWidget, 4,
                        getString(R.string.tab_followers)
                                + " (" + mUser.followers_count + ")");
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
            if (isMe()) {
                getApp().setAccountData(User.DataKeys.PROFILE_IDX, Integer.toString(mLastTabIndex));
            }
        }
    };

    private boolean isOtherUser() {
        return !isMe();
    }

    private boolean isMe() {
       return mUser.id == getCurrentUserId();
    }

    public void setTab(String tag) {
        mTabHost.setCurrentTabByTag(tag);
        mWorkspaceView.setCurrentScreenNow(mTabHost.getCurrentTab());
    }

    private void toggleFollowing() {
        mFollowStateBtn.setEnabled(false);
        FollowStatus.get().toggleFollowing(mUser.id, getApp(), new Handler() {
            @Override public void handleMessage(Message msg) {
                mFollowStateBtn.setEnabled(true);
                if (msg.arg1 == 0) {
                    setFollowingButtonText();
                    CloudUtils.showToast(UserBrowser.this, R.string.error_change_following_status);
                }
            }
        });
        setFollowingButtonText();
    }

    private void setFollowingButtonText() {
        if (isOtherUser()) {
            mFollowStateBtn.setImageDrawable(FollowStatus.get().isFollowing(mUser) ? mUnfollowDrawable : mFollowDrawable);
            mFollowStateBtn.setVisibility(View.VISIBLE);
        } else {
            mFollowStateBtn.setVisibility(View.GONE);
        }
    }

    private void setUser(final User user) {
        if (user == null || user.id < 0) return;
        mUser = user;
        mUsername.setText(user.username);
        mFullName.setText(user.full_name);
        setTabTextInfo();

        setFollowingButtonText();
        if (CloudUtils.checkIconShouldLoad(user.avatar_url)) {
            String remoteUrl = CloudUtils.formatGraphicsUrl(user.avatar_url, Consts.GraphicsSizes.LARGE);

            if (mIconURL == null
                || avatarResult == BindResult.ERROR
                || !remoteUrl.substring(0, remoteUrl.indexOf("?")).equals(mIconURL.substring(0, mIconURL.indexOf("?")))) {
                mIconURL = remoteUrl;
                reloadAvatar();
            }
        }

        boolean displayedSomething = false;
        if (!TextUtils.isEmpty(user.website)) {
            displayedSomething = true;
            mWebsite.setText(
                    TextUtils.isEmpty(user.website_title) ?
                    CloudUtils.stripProtocol(user.website) : user.website_title);
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
            displayedSomething = true;
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
            displayedSomething = true;
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

        if (!TextUtils.isEmpty(user.city) || !TextUtils.isEmpty(user.country)) {
            displayedSomething = true;
            mLocation.setText(getString(R.string.from)+" "+CloudUtils.getLocationString(user.city, user.country));
            mLocation.setVisibility(View.VISIBLE);
        } else {
            mLocation.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(user.description)) {
            displayedSomething = true;
            mDescription.setText(Html.fromHtml((user).description.replace(System.getProperty("line.separator"), "<br/>")));
            mDescription.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if (displayedSomething) {
            mDetailsView.findViewById(R.id.txt_empty).setVisibility(View.GONE);
        } else {
            TextView txtEmpty = (TextView) mDetailsView.findViewById(R.id.txt_empty);
            txtEmpty.setText(isOtherUser() ? R.string.info_empty_other : R.string.info_empty_you);
            txtEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void reloadAvatar() {
        if (CloudUtils.checkIconShouldLoad(mIconURL)) {
            if ((avatarResult = ImageLoader.get(this).bind(mIcon, mIconURL, null)) != BindResult.OK) {
                mIcon.setImageDrawable(getResources().getDrawable(R.drawable.avatar_badge_large));
            }
        }
    }

    @Override
    protected void handleRecordingClick(Recording recording) {
        if (recording.upload_status == Recording.UploadStatus.UPLOADING)
            safeShowDialog(Consts.Dialogs.DIALOG_CANCEL_UPLOAD);
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
                        Connections.get().requestUpdate(getApp(), this, true);

                        if (mFriendFinderView != null) {
                            mFriendFinderView.setState(FriendFinderView.States.LOADING, false);
                        }
                    }
                }
        }
    }

}
