package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.net.Uri;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.*;
import com.soundcloud.android.model.*;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.ICloudPlaybackService;
import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.android.service.record.ICloudCreateService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.AddCommentDialog;
import com.soundcloud.android.view.ScListView;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import java.util.ArrayList;
import java.util.List;


public abstract class ScActivity extends android.app.Activity {
    private Boolean mIsConnected;

    protected Object[] mPreviousState;
    protected ICloudPlaybackService mPlaybackService;
    protected ICloudCreateService mCreateService;
    protected NetworkConnectivityListener connectivityListener;

    protected List<ScListView> mLists;

    private MenuItem menuCurrentUploadingItem;
    private boolean mIsForeground;
    private long mCurrentUserId;
    boolean mIgnorePlaybackStatus;

    protected static final int CONNECTIVITY_MSG = 0;

    // Need handler for callbacks to the UI thread
    protected final Handler mHandler = new Handler();

    public SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    public Handler getHandler(){
        return mHandler;
    }

    public boolean isConnected() {
        if (mIsConnected == null) {
            if (connectivityListener == null) {
                mIsConnected = true;
            } else {
                // mIsConnected not set yet
                NetworkInfo networkInfo = connectivityListener.getNetworkInfo();
                mIsConnected = networkInfo == null || networkInfo.isConnectedOrConnecting();
            }
        }
        return mIsConnected;
    }

    protected void onServiceBound() {
        if (getApp().getToken() == null) {
            pause();

        } else {
            try {
                final Track track = mPlaybackService != null ? mPlaybackService.getTrack() : null;
                if (track != null) {
                    setPlayingTrack(track.id, mPlaybackService.isPlaying());
                }
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }
        }
    }

    protected void onServiceUnbound() {
    }

    protected void onCreateServiceBound() {
        if (mLists == null || mLists.size() == 0 || !(this instanceof UserBrowser)) return;
        for (ScListView lv : mLists){
            if (lv.getBaseAdapter() instanceof MyTracksAdapter)
                try {
                    ((MyTracksAdapter) lv.getBaseAdapter()).checkUploadStatus(mCreateService.getUploadLocalId());
                } catch (RemoteException ignored) {}
        }
    }

    @Override
    public boolean onSearchRequested() {
        // just focus on the search tab, don't show default android search dialog
        startActivity(new Intent(Actions.SEARCH)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return false;
    }

    private final ServiceConnection osc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mPlaybackService = ICloudPlaybackService.Stub.asInterface(obj);
            onServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
            onServiceUnbound();
            mPlaybackService = null;
        }
    };


    private final ServiceConnection createOsc = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mCreateService = (ICloudCreateService) binder;
            onCreateServiceBound();
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectivityListener = new NetworkConnectivityListener();
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);

        // Volume mode should always be music in this app
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(CloudPlaybackService.META_CHANGED);
        playbackFilter.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        playbackFilter.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        registerReceiver(mPlaybackStatusListener, new IntentFilter(playbackFilter));

        IntentFilter generalIntentFilter = new IntentFilter();
        generalIntentFilter.addAction(Consts.IntentActions.CONNECTION_ERROR);
        registerReceiver(mGeneralIntentListener, generalIntentFilter);

        mLists = new ArrayList<ScListView>();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectivityListener.unregisterHandler(connHandler);
        connectivityListener = null;
        unregisterReceiver(mPlaybackStatusListener);
        unregisterReceiver(mGeneralIntentListener);

        for (final ScListView l : mLists) {
            if (l.getWrapper() != null) {
                l.getWrapper().onDestroy();
            }
                if (l.getBaseAdapter() != null) {
                l.getBaseAdapter().onDestroy();
            }
        }
        if (findViewById(R.id.container) != null){
            unbindDrawables(findViewById(R.id.container));
            System.gc();
        }
    }

    protected void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            try {
                ((ViewGroup) view).removeAllViews();
            } catch (UnsupportedOperationException ignore){ }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        connectivityListener.startListening(this);

        CloudUtils.bindToService(this, CloudPlaybackService.class, osc);
        CloudUtils.bindToService(this, CloudCreateService.class, createOsc);
        try {
            final Track track = mPlaybackService != null ? mPlaybackService.getTrack() : null;
            if (track != null) {
                setPlayingTrack(track.id, mPlaybackService.isPlaying());
            }
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }
    }

    /**
     * Unbind our services
     */
    @Override
    protected void onStop() {
        super.onStop();

        mIsForeground = false;

        connectivityListener.stopListening();

        CloudUtils.unbindFromService(this, CloudPlaybackService.class);
        mPlaybackService = null;
        mIgnorePlaybackStatus = false;

        CloudUtils.unbindFromService(this, CloudCreateService.class);
        mCreateService = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsForeground = true;

        Account account = getApp().getAccount();
        if (account == null) {
            finish();
        } else {
            if (mLists != null) {
                for (final ScListView lv : mLists) {
                    if (lv.getWrapper() != null) lv.getWrapper().onResume();
                }
            }
        }
    }

    public void playTrack(final int position, final LazyEndlessAdapter wrapper, boolean goToPlayer, boolean commentMode) {
        // XXX this looks scary
        SoundCloudApplication.TRACK_CACHE.put(((Playable) wrapper.getItem(position)).getTrack());
        if (position > 0 && wrapper.getItem(position) instanceof Playable){
            SoundCloudApplication.TRACK_CACHE.put(((Playable) wrapper.getItem(position - 1)).getTrack());
        }
        if (position < wrapper.getWrappedAdapter().getCount() -1 && wrapper.getItem(position + 1) instanceof Playable){
            SoundCloudApplication.TRACK_CACHE.put(((Playable) wrapper.getItem(position + 1)).getTrack());
        }
        final Track t = ((Playable) wrapper.getItem(position)).getTrack();
        if (!handleTrackAlreadyPlaying(t, goToPlayer, commentMode)) {
            final Uri contentUri = wrapper.getContentUri();
            if (contentUri != null) {
                startService(new Intent(this, CloudPlaybackService.class)
                        .putExtra("playPos", position)
                        .setData(wrapper.getContentUri())
                        .setAction(CloudPlaybackService.PLAY));
            } else {
                CloudPlaybackService.playlistXfer = wrapper.getData();
                startService(new Intent(this, CloudPlaybackService.class)
                    .putExtra("playPos", position)
                    .putExtra("playFromXferCache", true)
                    .setAction(CloudPlaybackService.PLAY));
            }


            if (goToPlayer) {
                launchPlayer(commentMode);
                mIgnorePlaybackStatus = true;
            }
        }
    }

    public void playTrack(final Track track, boolean goToPlayer, boolean commentMode) {
        // find out if this track is already playing. If it is, just go to the player
        if (!handleTrackAlreadyPlaying(track, goToPlayer, commentMode)) {
            startService(new Intent(this, CloudPlaybackService.class)
                    .putExtra("track", track)
                    .setAction(CloudPlaybackService.PLAY));

            if (goToPlayer) {
                launchPlayer(commentMode);
                mIgnorePlaybackStatus = true;
            }
        }
    }

    private boolean handleTrackAlreadyPlaying(Track track, boolean goToPlayer, boolean commentMode) {
        // find out if this track is already playing. If it is, just go to the player
        try {
            final Track playingTrack = mPlaybackService != null ? mPlaybackService.getTrack() : null;
            if (playingTrack != null && playingTrack.id == track.id) {
                if (goToPlayer) {
                    // skip the enqueuing, its already playing
                    launchPlayer(commentMode);
                } else {
                    mPlaybackService.play();
                }
                return true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        return false;
    }

    public void launchPlayer(boolean commentMode) {
        Intent i = new Intent(this, ScPlayer.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra("commentMode",commentMode);
        startActivity(i);
    }


    public void pause() {
        try {
            if (mPlaybackService != null) {
                mPlaybackService.pause();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    // WTF? why is this in ScActivity?
    // called from UserBrowser XXX replace with intent
    public boolean startUpload(Recording r) {
        if (mCreateService == null) return false;

        try {
            if (mCreateService.startUpload(new Upload(r, getResources()))) return true;
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        showToast(R.string.wait_for_upload_to_finish);
        return false;
    }

    public void showToast(int stringId) {
        CloudUtils.showToast(this, getResources().getString(stringId));
    }

    public ScListView buildList() {
        return configureList(new ScListView(this), false);
    }

    public ScListView buildList(boolean longClickable) {
        return configureList(new ScListView(this), longClickable);
    }

    public ScListView configureList(ScListView lv) {
        return configureList(lv,false, mLists.size());
    }

    public ScListView configureList(ScListView lv, boolean longClickable) {
        return configureList(lv,longClickable, mLists.size());
    }

    public ScListView configureList(ScListView lv, boolean longClickable, int addAtPosition) {
        lv.setLazyListListener(mLazyListListener);
        lv.getRefreshableView().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        lv.getRefreshableView().setFastScrollEnabled(false);
        lv.getRefreshableView().setDivider(getResources().getDrawable(R.drawable.list_separator));
        lv.getRefreshableView().setDividerHeight(1);
        lv.getRefreshableView().setCacheColorHint(Color.TRANSPARENT);
        lv.getRefreshableView().setLongClickable(longClickable);
        mLists.add(addAtPosition < 0 || addAtPosition > mLists.size() ? mLists.size() : addAtPosition,lv);
        return lv;
    }

    public int removeList(ScListView lazyListView){
        int index = mLists.indexOf(lazyListView);
        if (index != -1) {
            mLists.remove(index);
        }
        return index;
    }

    public void addNewComment(final Comment comment) {
        getApp().pendingComment = comment;
        safeShowDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);
    }

    private BroadcastReceiver mGeneralIntentListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Consts.IntentActions.CONNECTION_ERROR)) {
                safeShowDialog(Consts.Dialogs.DIALOG_ERROR_LOADING);
            }
        }
    };

    private BroadcastReceiver mPlaybackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIgnorePlaybackStatus)
                return;

            String action = intent.getAction();
            if (action.equals(CloudPlaybackService.META_CHANGED)) {
                setPlayingTrack(intent.getLongExtra("id", -1), true);
            } else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
                setPlayingTrack(-1, false);
            } else if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
                setPlayingTrack(intent.getLongExtra("id", -1), intent.getBooleanExtra("isPlaying", false));
            }
        }
    };

    private void setPlayingTrack(long id, boolean isPlaying) {
        if (mLists == null || mLists.size() == 0)
            return;

        for (ScListView list : mLists) {
            if (TracklistAdapter.class.isAssignableFrom(list.getBaseAdapter().getClass())) {
                ((TracklistAdapter) list.getBaseAdapter()).setPlayingId(id, isPlaying);
                list.getBaseAdapter().notifyDataSetChanged();
            }
        }
    }

    public void safeShowDialog(int dialogId) {
        if (!isFinishing()) {
            showDialog(dialogId);
        }
    }

    public void safeShowDialog(Dialog dialog) {
        if (!isFinishing()) {
            dialog.show();
        }
    }

    protected void onDataConnectionChanged(boolean isConnected) {
        mIsConnected = isConnected;
        if (isConnected) {
            // clear image loading errors
            ImageLoader.get(ScActivity.this).clearErrors();
            if (getApp().getAccount() != null){
                for (ScListView lv : mLists) {
                    if (lv.getWrapper() != null) lv.getWrapper().onConnected();
                }
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_CANCEL_UPLOAD:
                return new AlertDialog.Builder(this)
                        .setTitle(null)
                        .setMessage(R.string.dialog_cancel_upload_message).setPositiveButton(
                                android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    // XXX this should be handled by ScCreate
                                    mCreateService.cancelUpload();
                                } catch (RemoteException ignored) {
                                    Log.w(TAG, ignored);
                                }
                                removeDialog(Consts.Dialogs.DIALOG_CANCEL_UPLOAD);
                            }
                        }).setNegativeButton(android.R.string.no,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        removeDialog(Consts.Dialogs.DIALOG_CANCEL_UPLOAD);
                                    }
                                }).create();

            case Consts.Dialogs.DIALOG_UNAUTHORIZED:
                return new AlertDialog.Builder(this).setTitle(R.string.error_unauthorized_title)
                        .setMessage(R.string.error_unauthorized_message).setNegativeButton(
                                R.string.menu_settings, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(ScActivity.this, Settings.class));
                            }
                        }).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
                                    }
                                }).create();
            case Consts.Dialogs.DIALOG_ERROR_LOADING:
                return new AlertDialog.Builder(this).setTitle(R.string.error_loading_title)
                        .setMessage(R.string.error_loading_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(Consts.Dialogs.DIALOG_ERROR_LOADING);
                                    }
                                }).create();
            case Consts.Dialogs.DIALOG_LOGOUT:
                return CloudUtils.createLogoutDialog(this);

            case Consts.Dialogs.DIALOG_ADD_COMMENT:
                final AddCommentDialog dialog = new AddCommentDialog(this);
                dialog.getWindow().setGravity(Gravity.TOP);
                return dialog;
            default:
                return super.onCreateDialog(which);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getParent() == null) {
            menu.add(menu.size(), Consts.OptionsMenu.STREAM,
                menu.size(), R.string.menu_incoming).setIcon(R.drawable.ic_menu_incoming);
        }

        menuCurrentUploadingItem = menu.add(menu.size(),
                Consts.OptionsMenu.CANCEL_CURRENT_UPLOAD, menu.size(),
                R.string.menu_cancel_current_upload).setIcon(R.drawable.ic_menu_delete);

        menu.add(menu.size(), Consts.OptionsMenu.FRIEND_FINDER, menu.size(), R.string.menu_friend_finder)
                .setIcon(R.drawable.ic_menu_friendfinder);

         if (this instanceof ScPlayer) {
            menu.add(menu.size(), Consts.OptionsMenu.REFRESH, 0, R.string.menu_refresh).setIcon(
                R.drawable.ic_menu_refresh);
        } else {
             menu.add(menu.size(), Consts.OptionsMenu.VIEW_CURRENT_TRACK,
                menu.size(), R.string.menu_view_current_track).setIcon(R.drawable.ic_menu_player);
        }

        menu.add(menu.size(), Consts.OptionsMenu.SETTINGS, menu.size(), R.string.menu_settings)
                .setIcon(android.R.drawable.ic_menu_preferences);

        if (SoundCloudApplication.DEV_MODE){
            menu.add(menu.size(), Consts.OptionsMenu.SECRET_DEV_BUTTON, menu.size(), "Super Secret Dev Button")
                .setIcon(android.R.drawable.ic_menu_compass);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean uploading = false;
        try {
            if (mCreateService != null) uploading = mCreateService.isUploading();
        } catch (RemoteException ignored) {}
        menuCurrentUploadingItem.setVisible(uploading);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.SETTINGS:
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
                return true;
            case Consts.OptionsMenu.VIEW_CURRENT_TRACK:
                startActivity(new Intent(this, ScPlayer.class));
                return true;
            case Consts.OptionsMenu.STREAM:
                intent = new Intent(Actions.STREAM);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            case Consts.OptionsMenu.FRIEND_FINDER:
                trackPage(Consts.Tracking.PEOPLE_FINDER);
                intent = new Intent(Actions.USER_BROWSER)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("userBrowserTag", UserBrowser.TabTags.friend_finder);
                startActivity(intent);
                return true;
            case Consts.OptionsMenu.CANCEL_CURRENT_UPLOAD:
                safeShowDialog(Consts.Dialogs.DIALOG_CANCEL_UPLOAD);
                return true;
            case Consts.OptionsMenu.SECRET_DEV_BUTTON:
                //startActivity(new Intent(this,TestActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Handler connHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (connectivityListener != null) {
                        NetworkInfo networkInfo = connectivityListener.getNetworkInfo();
                        if (networkInfo != null) {
                            if (networkInfo.isConnected()) ImageLoader.get(getApplicationContext()).clearErrors();
                            ScActivity.this.onDataConnectionChanged(networkInfo.isConnectedOrConnecting());
                        }
                    }
                    break;
            }
        }
    };

    public long getCurrentUserId() {
        if (mCurrentUserId == 0) {
            mCurrentUserId = getApp().getCurrentUserId();
        }
        return mCurrentUserId;
    }

    public void trackPage(String path) {
        getApp().trackPage(path);
    }

    public void trackEvent(String category, String action) {
        getApp().trackEvent(category, action);
    }

    protected void handleRecordingClick(Recording recording) {
    }

    private ScListView.LazyListListener mLazyListListener = new ScListView.LazyListListener() {
        @Override
        public void onEventClick(EventsAdapterWrapper wrapper, int position) {
            final Activity e = (Activity) wrapper.getItem(position);
            if (e.type == Activity.Type.FAVORITING) {
                SoundCloudApplication.TRACK_CACHE.put(e.getTrack());
                startActivity(new Intent(ScActivity.this, TrackFavoriters.class)
                    .putExtra("track_id", e.getTrack().id));
            } else {
                playTrack(position, wrapper, true, false);
            }
        }

        @Override
        public void onTrackClick(LazyEndlessAdapter wrapper, int position) {
            playTrack(position, wrapper, true, false);
        }

        @Override
        public void onUserClick(User user) {
            Intent i = new Intent(ScActivity.this, UserBrowser.class);
            i.putExtra("user", user);
            startActivity(i);
        }

        @Override
        public void onCommentClick(Comment comment) {
            Intent i = new Intent(ScActivity.this, UserBrowser.class);
            i.putExtra("user", comment.user);
            startActivity(i);
        }

        public void onFling() {
            ImageLoader.get(ScActivity.this).pause();
        }

        @Override
        public void onFlingDone() {
            ImageLoader.get(ScActivity.this).unpause();
        }

        @Override
        public void onRecordingClick(final Recording recording) {
            handleRecordingClick(recording);
        }
    };

    public ICloudCreateService getCreateService() {
        return mCreateService;
    }
}
