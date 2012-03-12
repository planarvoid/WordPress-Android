package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.Upload;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.ICloudPlaybackService;
import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.android.service.record.ICloudCreateService;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
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
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public abstract class ScActivity extends android.app.Activity implements Tracker {
    private Boolean mIsConnected;

    protected Object[] mPreviousState;
    protected ICloudPlaybackService mPlaybackService;
    protected ICloudCreateService mCreateService;
    protected NetworkConnectivityListener connectivityListener;

    protected List<ScListView> mLists;

    private MenuItem menuCurrentUploadingItem;
    private long mCurrentUserId;
    private boolean mIgnorePlaybackStatus;

    private static final int CONNECTIVITY_MSG = 0;

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
            setPlayingTrackFromService();
        }
    }

    protected void onServiceUnbound() {
    }

    protected void onCreateServiceBound() {
        if (mLists == null || mLists.size() == 0 || !(this instanceof UserBrowser)) return;
        for (ScListView lv : mLists){
            if (lv.getBaseAdapter() instanceof MyTracksAdapter && mCreateService != null)
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
        generalIntentFilter.addAction(Actions.CONNECTION_ERROR);
        generalIntentFilter.addAction(Actions.LOGGING_OUT);
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
        setPlayingTrackFromService();
    }

    /**
     * Unbind our services
     */
    @Override
    protected void onStop() {
        super.onStop();

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


    public static class PlayInfo {
        public List<Playable> playables;
        public int position;
        public Uri uri;

        Track getTrack() {
            return playables.get(Math.max(0,Math.min(playables.size() -1 ,position))).getTrack();
        }

        public static PlayInfo forTracks(Track... t) {
            PlayInfo info = new PlayInfo();
            info.playables = Arrays.<Playable>asList(t);
            return info;
        }

    }

    public void playTrack(PlayInfo info) {
        playTrack(info, true, false);
    }

    public void playTrack(PlayInfo info, boolean goToPlayer, boolean commentMode) {
        final Track t = info.getTrack();
        if (getCurrentTrackId() != t.id) {
            Intent intent = new Intent(this, CloudPlaybackService.class)
                    .putExtra(CloudPlaybackService.PlayExtras.trackId, t.id)
                    .setAction(CloudPlaybackService.PLAY);

            if (info.uri != null) {
                SoundCloudApplication.TRACK_CACHE.put(info.getTrack(), false);
                intent.putExtra(CloudPlaybackService.PlayExtras.trackId, info.getTrack().id)
                      .putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                      .setData(info.uri);
            } else {
                CloudPlaybackService.playlistXfer = info.playables;
                    intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                    .putExtra(CloudPlaybackService.PlayExtras.playFromXferCache, true);
            }
            startService(intent);
        } else if (!goToPlayer) {
            try {
                mPlaybackService.play();
            } catch (RemoteException ignored) {
            }
        }
        if (goToPlayer) {
            launchPlayer(commentMode);
        }

    }

    private long getCurrentTrackId() {
        if (mPlaybackService != null) {
            try {
                return mPlaybackService.getCurrentTrackId();
            } catch (RemoteException ignore) {
            }
        }
        return -1;
    }

    private void launchPlayer(boolean commentMode) {
        Intent i = new Intent(this, ScPlayer.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra("commentMode",commentMode);
        startActivity(i);
        mIgnorePlaybackStatus = true;
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
        CloudUtils.showToast(this, stringId);
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
            if (Actions.CONNECTION_ERROR.equals(intent.getAction())) {
                safeShowDialog(Consts.Dialogs.DIALOG_ERROR_LOADING);
            } else if (Actions.LOGGING_OUT.equals(intent.getAction())) {
                if (mLists != null) {
                    for (final ScListView lv : mLists) {
                        if (lv.getWrapper() != null) lv.getWrapper().onLogout();
                    }
                }
            }
        }
    };

    private BroadcastReceiver mPlaybackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIgnorePlaybackStatus)
                return;

            final String action = intent.getAction();
            if (CloudPlaybackService.META_CHANGED.equals(action)) {
                setPlayingTrack(intent.getLongExtra("id", -1), true);
            } else if (CloudPlaybackService.PLAYBACK_COMPLETE.equals(action)) {
                setPlayingTrack(-1, false);
            } else if (CloudPlaybackService.PLAYSTATE_CHANGED.equals(action)) {
                setPlayingTrack(intent.getLongExtra("id", -1), intent.getBooleanExtra("isPlaying", false));
            }
        }
    };

    private void setPlayingTrack(long id, boolean isPlaying) {
        if (mLists == null || mLists.size() == 0)
            return;

        for (ScListView list : mLists) {
            if (list.getBaseAdapter() instanceof TracklistAdapter) {
                ((TracklistAdapter) list.getBaseAdapter()).setPlayingId(id, isPlaying);
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
                return Settings.createLogoutDialog(this);

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
                startActivity(new Intent(this, ScPlayer.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case Consts.OptionsMenu.STREAM:
                intent = new Intent(Actions.STREAM);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            case Consts.OptionsMenu.FRIEND_FINDER:
                intent = new Intent(Actions.MY_PROFILE)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("userBrowserTag", UserBrowser.Tab.friend_finder.name());
                startActivity(intent);
                return true;
            case Consts.OptionsMenu.CANCEL_CURRENT_UPLOAD:
                safeShowDialog(Consts.Dialogs.DIALOG_CANCEL_UPLOAD);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Handler connHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final ScActivity ctxt = ScActivity.this;
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (msg.obj instanceof NetworkInfo) {
                        NetworkInfo networkInfo = (NetworkInfo) msg.obj;
                        final boolean connected = networkInfo.isConnectedOrConnecting();
                        if (connected) {
                            ImageLoader.get(getApplicationContext()).clearErrors();

                            // announce potential proxy change
                            sendBroadcast(new Intent(Actions.CHANGE_PROXY_ACTION)
                                            .putExtra(Actions.EXTRA_PROXY, IOUtils.getProxy(ctxt, networkInfo)));
                        }
                        ctxt.onDataConnectionChanged(connected);
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

    protected void handleRecordingClick(Recording recording) {
    }

    private void setPlayingTrackFromService(){
        if (mPlaybackService == null) return;
        try {
            final long trackId = getCurrentTrackId();
            if (trackId != -1) {
                setPlayingTrack(trackId, mPlaybackService.isPlaying());
            }
        } catch (RemoteException ignore) {
        }
    }

    private ScListView.LazyListListener mLazyListListener = new ScListView.LazyListListener() {
        @Override
        public void onEventClick(EventsAdapterWrapper wrapper, int position) {
            final Activity e = (Activity) wrapper.getItem(position);
            if (e.type == Activity.Type.FAVORITING) {
                SoundCloudApplication.TRACK_CACHE.put(e.getTrack(), false);
                startActivity(new Intent(ScActivity.this, TrackFavoriters.class)
                    .putExtra("track_id", e.getTrack().id));
            } else {
                playTrack(wrapper.getPlayInfo(position));
            }
        }

        @Override
        public void onTrackClick(LazyEndlessAdapter wrapper, int position) {
            playTrack(wrapper.getPlayInfo(position));
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

        @Override
        public void onRecordingClick(final Recording recording) {
            handleRecordingClick(recording);
        }
    };

    public ICloudCreateService getCreateService() {
        return mCreateService;
    }

    // tracking shizzle
    public void track(Event event, Object... args) {
        getApp().track(event, args);
    }

    public void track(Class<?> klazz, Object... args) {
        getApp().track(klazz, args);
    }
}
