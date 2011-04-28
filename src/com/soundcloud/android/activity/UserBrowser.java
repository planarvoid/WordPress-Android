package com.soundcloud.android.activity;


import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.Recordings;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.CheckFollowingStatusTask;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.CloudUtils.GraphicsSizes;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.android.view.WorkspaceView;
import com.soundcloud.android.view.WorkspaceView.OnScrollListener;
import com.soundcloud.api.Endpoints;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
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

import java.io.File;
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
    private ScTabView mFollowingsView;

    private WorkspaceView mWorkspaceView;

    private long mUserLoadId;

    private CheckFollowingStatusTask mCheckFollowingTask;
    private LoadUserTask mLoadDetailsTask;

    private int mFollowResult;

    private TabWidget mTabWidget;
    private TabHost mTabHost;

    private int mLastTabIndex;

    private User mUserData;

    private ImageLoader.BindResult avatarResult;

    private static CharSequence[] RECORDING_ITEMS = {"Edit", "Listen", "Upload", "Delete"};
    private static CharSequence[] EXTERNAL_RECORDING_ITEMS = {"Edit", "Upload", "Delete"};

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.user_view);

        mDetailsView = (FrameLayout) getLayoutInflater().inflate(R.layout.user_details, null);

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
            mIcon.getLayoutParams().width = 67;
            mIcon.getLayoutParams().height = 67;
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
        if (mPreviousState != null){
            mLoadDetailsTask = (LoadUserTask) mPreviousState[1];
            mLoadDetailsTask.setActivity(this);

            mapUser((User) mPreviousState[2]);

            mCheckFollowingTask = (CheckFollowingStatusTask) mPreviousState[3];
            if (CloudUtils.isTaskFinished(mCheckFollowingTask)){
                setFollowingButtonText();
            } else {
                mCheckFollowingTask.setUserBrowser(this);
            }

            build();

            restoreAdapterStates((Object[]) mPreviousState[4]);

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

        loadDetails();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //this.unregisterReceiver(mUpdateAdapterListener);
    }


    @Override
    protected void onResume() {
        pageTrack("/profile");
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
    public Object onRetainNonConfigurationInstance() {
        return new Object[] {
                super.onRetainNonConfigurationInstance(),
                mLoadDetailsTask,
                mUserData,
                mCheckFollowingTask,
                getAdapterStates()
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

    private void restoreAdapterStates(Object[] adapterStates){
        int i = 0;
        for ( Object adapterState : adapterStates ){
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

        if (mWorkspaceView != null) {
            ((ScTabView) mWorkspaceView.getChildAt(mWorkspaceView.getDisplayedChild())).onRefresh();
        } else {
            ((ScTabView) mTabHost.getCurrentView()).onRefresh();
        }
    }

    private void loadYou() {
        if (getUserId() != -1) {
            User u = SoundCloudDB.getInstance().resolveUserById(getContentResolver(), getUserId());
            if (u == null) u = new User(getSoundCloudApplication());
            mapUser(u);
            mUserLoadId = u.id;
        }
        build();
    }


    private void loadUserById(long userId) {
        mapUser(SoundCloudDB.getInstance().resolveUserById(getContentResolver(), userId));
        if (mUserData == null) {
            mUserData = new User();
            mUserLoadId = mUserData.id = userId;
        }
        build();
        checkFollowingStatus();
    }

    private void loadUserByObject(User userInfo) {
        if (userInfo  == null)  return;

        mUserLoadId = userInfo.id;
        mapUser(userInfo);
        build();
        checkFollowingStatus();
    }


    private void loadDetails() {
        if (mLoadDetailsTask == null){
            mLoadDetailsTask = new LoadUserTask(getSoundCloudApplication());
            mLoadDetailsTask.setActivity(this);
        }

        if (CloudUtils.isTaskPending(mLoadDetailsTask)) {
            mLoadDetailsTask.execute(getDetailsUrl());
        }
    }

    private class LoadUserTask extends LoadTask<User> {
        public LoadUserTask(SoundCloudApplication api) {
            super(api, User.class);
        }

        @Override
        protected void onPostExecute(User user) {
            mapUser(user);
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

        LazyBaseAdapter adp = isOtherUser() ? new TracklistAdapter(this,
                new ArrayList<Parcelable>()) : new MyTracksAdapter(this,
                new ArrayList<Parcelable>());

        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this, adp, getUserTracksUrl(), Track.class);
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

        mTracksView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, mTracksView, adpWrap, CloudUtils.ListId.LIST_USER_TRACKS, null);
        CloudUtils.createTab(mTabHost, "tracks", getString(R.string.tab_tracks), null, emptyView);

        adp = new TracklistAdapter(this, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(this, adp, getFavoritesUrl(), Track.class);
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


        mFavoritesView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, mFavoritesView, adpWrap, CloudUtils.ListId.LIST_USER_FAVORITES, null);
        CloudUtils.createTab(mTabHost, "favorites", getString(R.string.tab_favorites), null, emptyView);

        final ScTabView detailsView = new ScTabView(this);
        detailsView.addView(mDetailsView);

        CloudUtils.createTab(mTabHost, "details", getString(R.string.tab_info), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(this, adp, getFollowingsUrl(), User.class);

        final ScTabView followingsView = mFollowingsView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, followingsView, adpWrap, CloudUtils.ListId.LIST_USER_FOLLOWINGS, null).disableLongClickListener();
        CloudUtils.createTab(mTabHost, "followings", getString(R.string.tab_followings), null, emptyView);

        adp = new UserlistAdapter(this, new ArrayList<Parcelable>());
        adpWrap = new LazyEndlessAdapter(this, adp, getFollowersUrl(), User.class);

        final ScTabView followersView = mFollowersView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, followersView, adpWrap, CloudUtils.ListId.LIST_USER_FOLLOWERS, null).disableLongClickListener();
        CloudUtils.createTab(mTabHost, "followers", getString(R.string.tab_followers), null, emptyView);

        CloudUtils.configureTabs(this, mTabWidget, 30, -1, true);
        CloudUtils.setTabTextStyle(this, mTabWidget, true);

        if (!isOtherUser()) {
            mLastTabIndex = getSoundCloudApplication().getAccountDataInt(User.DataKeys.PROFILE_IDX);
            mWorkspaceView.initWorkspace(mLastTabIndex);
            mTabHost.setCurrentTab(mLastTabIndex);
        } else {
            mWorkspaceView.initWorkspace(0);
        }

        mWorkspaceView.addView(mTracksView);
        mWorkspaceView.addView(mFavoritesView);
        mWorkspaceView.addView(detailsView);
        mWorkspaceView.addView(mFollowingsView);
        mWorkspaceView.addView(followersView);

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
            if (mWorkspaceView != null)
                mWorkspaceView.setDisplayedChild(mTabHost.getCurrentTab(), (Math.abs(mLastTabIndex - mTabHost.getCurrentTab()) > 1));

            mLastTabIndex = mTabHost.getCurrentTab();
            if (!isOtherUser()) {
                getSoundCloudApplication().setAccountData(User.DataKeys.PROFILE_IDX, Integer.toString(mLastTabIndex));
            }
        }
    };

    private boolean isOtherUser() {
        return mUserLoadId != getUserId();
    }

    private void checkFollowingStatus() {
        if (isOtherUser()) {
            mCheckFollowingTask = new CheckFollowingStatusTask(getSoundCloudApplication());
            mCheckFollowingTask.setUserBrowser(this);
            mCheckFollowingTask.execute(mUserLoadId);
        }
    }

    public void onCheckFollowingStatus(boolean isFollowing){
        mUserData.current_user_following = isFollowing;
        setFollowingButtonText();
    }

    private void toggleFollowing() {
        mFollow.setEnabled(false);
        mUserData.current_user_following = !mUserData.current_user_following;
        setFollowingButtonText();
        mFollowResult = 0;

        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    if (mUserData.current_user_following) {
                        mFollowResult =
                                getSoundCloudApplication().putContent(
                                        Endpoints.MY_FOLLOWINGS + "/"
                                                + mUserData.id, null).getStatusLine().getStatusCode();
                    } else {
                        mFollowResult =
                                getSoundCloudApplication().deleteContent(
                                        Endpoints.MY_FOLLOWINGS + "/"
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
    private final Runnable mSetFollowingResult = new Runnable() {
        public void run() {
            handleException();
            handleError();

            if (!(mFollowResult == 200 || mFollowResult == 201 || mFollowResult == 404)) {
                mUserData.current_user_following = !mUserData.current_user_following;
                setFollowingButtonText();
            }
            mFollow.setEnabled(true);
        }
    };

    private void setFollowingButtonText() {
        if (isOtherUser()) {
            mFollow.setImageResource(mUserData.current_user_following ?
                    R.drawable.ic_unfollow_states : R.drawable.ic_follow_states);

            mFollow.setVisibility(View.VISIBLE);
        }
    }

    private void mapUser(User user) {
        if (user == null || user.id <= 0)
            return;

        // need to maintain this variable in case we already checked following status
        if (mUserData != null) user.current_user_following = mUserData.current_user_following;

        mUserData = user;
        mUserLoadId = mUserData.id;

        mUser.setText(mUserData.username);
        mFullName.setText(mUserData.full_name);
        setTabTextInfo();

        if (CloudUtils.checkIconShouldLoad(mUserData.avatar_url)) {
            String remoteUrl;
            if (getResources().getDisplayMetrics().density > 1) {
                remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.LARGE);
            } else {
                remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.avatar_url, GraphicsSizes.BADGE);
            }

            if (_iconURL == null || avatarResult == BindResult.ERROR || !remoteUrl.substring(0,remoteUrl.indexOf("?")).equals(_iconURL.substring(0,_iconURL.indexOf("?")))) {
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
                    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.discogs.com/artist/"+mUserData.discogs_name));
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
                    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.myspace.com/"+mUserData.myspace_name));
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
        builder.setTitle(CloudUtils.generateRecordingSharingNote(recording.where_text,
                recording.what_text, recording.timestamp));
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.setItems(curr_items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (curr_items[item].equals(RECORDING_ITEMS[0])){
                    Intent i = new Intent(UserBrowser.this, ScUpload.class);
                    i.putExtra("recordingId", recording.id);
                    startActivity(i);
                }else if (curr_items[item].equals(RECORDING_ITEMS[1])){
                    Intent i = new Intent(UserBrowser.this, ScCreate.class);
                    i.putExtra("recordingId", recording.id);
                    startActivity(i);
                } else if (curr_items[item].equals(RECORDING_ITEMS[2])){
                    startUpload(recording);
                } else if (curr_items[item].equals(RECORDING_ITEMS[3])){
                    new AlertDialog.Builder(UserBrowser.this)
                    .setTitle(R.string.dialog_confirm_delete_recording_title)
                    .setMessage(R.string.dialog_confirm_delete_recording_message)
                    .setPositiveButton(getString(R.string.btn_yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    getContentResolver().delete(Recordings.CONTENT_URI,
                                            Recordings.ID + " = " + recording.id, null);
                                    if (!recording.external_upload){
                                        File f = new File(recording.audio_path);
                                        if (f.exists()) f.delete();
                                    }
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



    private String getDetailsUrl() {
        return  String.format(Endpoints.USER_DETAILS, mUserLoadId);
    }

    private String getUserTracksUrl() {
        return CloudUtils.buildRequestPath(
                String.format(Endpoints.USER_TRACKS, mUserLoadId),getTrackOrder());
    }

    private String getFavoritesUrl() {
        return CloudUtils.buildRequestPath(
                String.format(Endpoints.USER_FAVORITES, mUserLoadId), "favorited_at");
    }

    private String getFollowersUrl() {
        return CloudUtils.buildRequestPath(
                String.format(Endpoints.USER_FOLLOWERS, mUserLoadId), getUserOrder());
    }

    private String getFollowingsUrl() {
        return CloudUtils.buildRequestPath(
                String.format(Endpoints.USER_FOLLOWINGS, mUserLoadId), getUserOrder());
    }

    private String getUserOrder() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString("defaultUserSorting", "");

    }

    private String getTrackOrder() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString("defaultTrackSorting", "");
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
}
