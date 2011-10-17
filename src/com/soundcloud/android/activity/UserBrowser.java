package com.soundcloud.android.activity;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.ImageView.ScaleType;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.adapter.*;
import com.soundcloud.android.cache.Connections;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.cache.ParcelCache;
import com.soundcloud.android.model.*;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.*;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** @noinspection unchecked*/
public class UserBrowser extends ScActivity implements ParcelCache.Listener<Connection>, FollowStatus.Listener {
    private User mUser;
    private TextView mUsername, mLocation, mFullName, mWebsite, mDiscogsName, mMyspaceName, mDescription, mFollowerCount, mTrackCount;
    private ImageView mIcon;
    private String mIconURL;
    private ImageLoader.BindResult avatarResult;

    private ScTabView mMyTracksView;
    private FrameLayout mDetailsView;
    private FriendFinderView mFriendFinderView;
    private ImageButton mFollowStateBtn;
    private Drawable mFollowDrawable, mUnfollowDrawable;
    private UserlistLayout mUserlistBrowser;
    private LoadUserTask mLoadDetailsTask;
    private boolean mUpdateInfo;

    private PrivateMessager mMessager;

    private List<Connection> mConnections;
    private Object mAdapterStates[];

    private static final CharSequence[] RECORDING_ITEMS = {"Edit", "Listen", "Upload", "Delete"};
    private static final CharSequence[] EXTERNAL_RECORDING_ITEMS = {"Edit", "Upload", "Delete"};


    public interface TabTags {
        String tracks = "tracks";
        String favorites = "favorites";
        String details = "details";
        String followings = "followings";
        String followers = "followers";
        String friend_finder = "friend_finder";
        String privateMessage = "private_message";
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

        mFollowerCount = (TextView) findViewById(R.id.followers);
        mTrackCount = (TextView) findViewById(R.id.tracks);

        mLocation = (TextView) mDetailsView.findViewById(R.id.location);
        mWebsite = (TextView) mDetailsView.findViewById(R.id.website);
        mDiscogsName = (TextView) mDetailsView.findViewById(R.id.discogs_name);
        mMyspaceName = (TextView) mDetailsView.findViewById(R.id.myspace_name);
        mDescription = (TextView) mDetailsView.findViewById(R.id.description);

        mIcon.setScaleType(ScaleType.CENTER_INSIDE);
        if (getResources().getDisplayMetrics().density > 1 || CloudUtils.isScreenXL(this)) {
            mIcon.getLayoutParams().width = 100;
            mIcon.getLayoutParams().height = 100;
        }

        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CloudUtils.checkIconShouldLoad(mIconURL)) {
                    new FullImageDialog(
                        UserBrowser.this,
                        ImageUtils.formatGraphicsUri(mIconURL, Consts.GraphicSize.CROP)
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

        Intent intent = getIntent();
        mUpdateInfo = intent.getBooleanExtra("updateInfo",true);

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mLoadDetailsTask = (LoadUserTask) mPreviousState[1];
            if (mLoadDetailsTask != null) mLoadDetailsTask.setActivity(this);

            setUser((User) mPreviousState[2]);

            if (isMe()) {
                mConnections = (List<Connection>) mPreviousState[3];
            }

            build();

            restoreAdapterStates((Object[]) mPreviousState[4]);

            if (mPreviousState[5] != null)
                mFriendFinderView.setState(Integer.parseInt(mPreviousState[5].toString()), false);

        } else {

            if (intent != null) {
                if (intent.hasExtra("user")) {
                    loadUserByObject((User) intent.getParcelableExtra("user"));
                } else if (intent.hasExtra("userId")) {
                    loadUserById(intent.getLongExtra("userId", -1));
                } else {
                    loadYou();
                }
                if (intent.hasExtra("recordingUri")) {
                    mMessager.setRecording(Uri.parse(intent.getStringExtra("recordingUri")), intent.getBooleanExtra("edit", false));
                }
            } else {
                loadYou();
            }

            if (isMe()) {
                Connections.get().requestUpdate(getApp(), false, this);
            }
        }

        mMyTracksView.onVisible();
        ((ScTabView) mUserlistBrowser.getCurrentWorkspaceView()).onVisible();
        loadDetails();
    }

    public void setTab(String tag) {
        mUserlistBrowser.setCurrentScreenByTag(tag);
    }

    @Override
    protected void onResume() {
        if (getApp().getAccount() != null && mAdapterStates != null){
            restoreAdapterStates(mAdapterStates);
            mAdapterStates = null;
        }
        trackCurrentScreen();
        super.onResume();
        if (mMessager != null) mMessager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMessager != null) mMessager.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mMessager != null) mMessager.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mMessager != null) mMessager.onStop();
        FollowStatus.get().removeListener(this);

        mAdapterStates = new Object[mLists.size()];
        int i = 0;
        for (ScListView list : mLists) {
            if (list.getWrapper() != null) {
                mAdapterStates[i] = list.getWrapper().saveState();
                list.getWrapper().cleanup();
            }
            i++;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMessager != null) mMessager.onDestroy();
    }

    public void onSaveInstanceState(Bundle state) {
        if (mMessager != null) mMessager.onSaveInstanceState(state);
    }

    public void onRestoreInstanceState(Bundle state) {
        if (mMessager != null) mMessager.onRestoreInstanceState(state);
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
            if (mFriendFinderView != null) mFriendFinderView.setState(FriendFinderView.States.LOADING, false);
        }
    }

    public void onChanged(List<Connection> connections, ParcelCache<Connection> cache) {
        mConnections = connections;
        mFriendFinderView.onConnections(connections, true);
    }

    private void loadYou() {
        setUser(getApp().getLoggedInUser());
        if (mUser == null) {
            mUser = new User();
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
        if (!mUpdateInfo) return;

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

    private void trackCurrentScreen(){
        trackPage(mUser.pageTrack(isMe(), mUserlistBrowser.getCurrentTag()));
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

        mUserlistBrowser = (UserlistLayout) findViewById(R.id.userlist_browser);

        // Details View
        final ScTabView detailsView = new ScTabView(this);
        detailsView.addView(mDetailsView);


        mMessager = isMe() ? null : new PrivateMessager(this, mUser);

        // Tracks View
        LazyBaseAdapter adp = isOtherUser() ? new TracklistAdapter(this,
                new ArrayList<Parcelable>(), Track.class) : new MyTracksAdapter(this,
                new ArrayList<Parcelable>(), Track.class);

        LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_TRACKS, mUser.id), false);
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

        mMyTracksView = new ScTabView(this);
        mMyTracksView.setLazyListView(buildList(), adpWrap, Consts.ListId.LIST_USER_TRACKS, true);

        // Favorites View
        adp = new TracklistAdapter(this, new ArrayList<Parcelable>(), Track.class);
        adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_FAVORITES, mUser.id).add("order","favorited_at"), false);
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

        // Followings View
        adp = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class);
        adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_FOLLOWINGS, mUser.id), false);

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
        followingsView.setLazyListView(buildList(false), adpWrap, Consts.ListId.LIST_USER_FOLLOWINGS, true).disableLongClickListener();

        // Followers View
        adp = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class);
        adpWrap = new LazyEndlessAdapter(this, adp, Request.to(Endpoints.USER_FOLLOWERS, mUser.id), false);

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
        followersView.setLazyListView(buildList(false), adpWrap, Consts.ListId.LIST_USER_FOLLOWERS, true).disableLongClickListener();

        for (ScListView list : mLists){
            list.setFadingEdgeLength(0);
        }

        // Friend Finder View
        if (isMe()) {
            mFriendFinderView = new FriendFinderView(this);
            if (mConnections == null) {
                mFriendFinderView.setState(FriendFinderView.States.LOADING, false);
            } else {
                mFriendFinderView.onConnections(mConnections, false);
            }
        }

        mUserlistBrowser.addView(detailsView, "Info", TabTags.details);
        if (mMessager != null) mUserlistBrowser.addView(mMessager, "Message", TabTags.privateMessage);

        if (mFriendFinderView != null) mUserlistBrowser.addView(mFriendFinderView, "Friend Finder", TabTags.friend_finder);
        mUserlistBrowser.addView(mMyTracksView, "Tracks", TabTags.tracks);
        mUserlistBrowser.addView(favoritesView, "Favorites", TabTags.favorites);
        mUserlistBrowser.addView(followingsView, "Following", TabTags.followings);
        mUserlistBrowser.addView(followersView, "Followers", TabTags.followers);


        mUserlistBrowser.setOnScreenChangedListener(new WorkspaceView.OnScreenChangeListener() {
            @Override public void onScreenChanged(View newScreen, int newScreenIndex) {
                trackCurrentScreen();
                if (isMe()) {
                    getApp().setAccountData(User.DataKeys.PROFILE_IDX, Integer.toString(newScreenIndex));
                }
            }
            @Override public void onScreenChanging(View newScreen, int newScreenIndex) {}

            @Override
            public void onNextScreenVisible(View newScreen, int newScreenIndex) {
                ((ScTabView) newScreen).onVisible();
            }
        });

        if (isMe()) {
            final int initialTab = getApp().getAccountDataInt(User.DataKeys.PROFILE_IDX);
            if (initialTab == -1){
                //tracks tab
                mUserlistBrowser.initWorkspace(2);
            } else {
                mUserlistBrowser.initWorkspace(initialTab);
            }

        } else {
            mUserlistBrowser.initWorkspace(1);
        }


    }

    private boolean isOtherUser() {
        return !isMe();
    }

    private boolean isMe() {
       return mUser != null && mUser.id == getCurrentUserId();
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
        if (TextUtils.isEmpty(user.full_name)){
            mFullName.setVisibility(View.GONE);
        } else {
            mFullName.setText(user.full_name);
            mFullName.setVisibility(View.VISIBLE);
        }
        mFollowerCount.setText(Integer.toString(user.followers_count));
        mTrackCount.setText(Integer.toString(user.track_count));

        setFollowingButtonText();
        if (CloudUtils.checkIconShouldLoad(user.avatar_url)) {
            if (mIconURL == null
                || avatarResult == BindResult.ERROR
                || !user.avatar_url.substring(0, user.avatar_url.indexOf("?")).equals(mIconURL.substring(0, mIconURL.indexOf("?")))) {
                mIconURL = user.avatar_url;
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

        final String location = user.getLocation();
        if (!TextUtils.isEmpty(location)) {
            displayedSomething = true;
            mLocation.setText(getString(R.string.from)+" "+location);
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
            mDetailsView.findViewById(R.id.empty_txt).setVisibility(View.GONE);
        } else {
            TextView txtEmpty = (TextView) mDetailsView.findViewById(R.id.empty_txt);
            txtEmpty.setText(Html.fromHtml(getString(isOtherUser() ? R.string.info_empty_other : R.string.info_empty_you)));
            txtEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void reloadAvatar() {
        if (CloudUtils.checkIconShouldLoad(mIconURL)) {
            if ((avatarResult = ImageUtils.loadImageSubstitute(this,mIcon,mIconURL, Consts.GraphicSize.LARGE,new ImageLoader.ImageViewCallback() {
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
    protected void handleRecordingClick(Recording recording) {
        if (recording.upload_status == Upload.UploadStatus.UPLOADING)
            safeShowDialog(Consts.Dialogs.DIALOG_CANCEL_UPLOAD);
        else {
            showRecordingDialog(recording);
        }
    }

    private void showRecordingDialog(final Recording recording) {
        final CharSequence[] curr_items = recording.external_upload ? EXTERNAL_RECORDING_ITEMS : RECORDING_ITEMS;

        new AlertDialog.Builder(this)
            .setInverseBackgroundForced(true)
            .setTitle(recording.sharingNote(getResources()))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setItems(curr_items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if (curr_items[item].equals(RECORDING_ITEMS[0])) {
                        if (recording.private_user_id <= 0){
                            startActivity(new Intent(UserBrowser.this, ScUpload.class).setData(recording.toUri()));
                        } else {
                            startActivity(new Intent(UserBrowser.this, UserBrowser.class).putExtra("userId", recording.private_user_id)
                                            .putExtra("edit",true)
                                            .putExtra("recordingUri", recording.toUri().toString())
                                            .putExtra("userBrowserTag", UserBrowser.TabTags.privateMessage));
                        }

                    } else if (curr_items[item].equals(RECORDING_ITEMS[1])) {
                        if (recording.private_user_id <= 0){
                            startActivity(new Intent(UserBrowser.this, ScCreate.class).setData(recording.toUri()));
                        } else {
                            startActivity(new Intent(UserBrowser.this, UserBrowser.class).putExtra("userId", recording.private_user_id)
                                    .putExtra("edit", false)
                                    .putExtra("recordingUri", recording.toUri().toString())
                                    .putExtra("userBrowserTag", UserBrowser.TabTags.privateMessage));
                        }



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
                                .setNegativeButton(getString(R.string.btn_no), null)
                                .create()
                                .show();
                    }
                }
            })
            .create()
            .show();
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
            default:
                mMessager.onActivityResult(requestCode,resultCode,result);
        }
    }

    @Override
    public void onCreateServiceBound() {
        super.onCreateServiceBound();
        if (mMessager != null) mMessager.onCreateServiceBound(mCreateService);
    }

    @Override
    protected Dialog onCreateDialog(int which) {
        Dialog created = null;
        if (mMessager != null) {
            created = mMessager.onCreateDialog(which);
        }
        return created == null ? super.onCreateDialog(which) : created;
    }

}
