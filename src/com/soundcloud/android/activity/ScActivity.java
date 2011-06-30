package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Recordings;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.service.ICloudPlaybackService;
import com.soundcloud.android.task.AddCommentTask.AddCommentListener;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.net.NetworkConnectivityListener;
import com.soundcloud.android.view.AddCommentDialog;
import com.soundcloud.android.view.LazyListView;
import org.json.JSONException;

import android.accounts.Account;
import android.app.Activity;
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
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public abstract class ScActivity extends Activity {

    private Exception mException;
    private String mError;

    protected Object[] mPreviousState;
    protected ICloudPlaybackService mPlaybackService;
    protected ICloudCreateService mCreateService;
    protected NetworkConnectivityListener connectivityListener;

    protected List<LazyListView> mLists;

    private MenuItem menuCurrentUploadingItem;
    boolean mIgnorePlaybackStatus;

    protected static final int CONNECTIVITY_MSG = 0;

    // Need handler for callbacks to the UI thread
    protected final Handler mHandler = new Handler();


    public SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    protected void onServiceBound() {
        //ImageLoader.get(ScActivity.this).pause();
        if (getApp().getToken() == null) {
            pause(true);

        } else {
            try {
                setPlayingTrack(mPlaybackService.getTrackId(), mPlaybackService.isPlaying());
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }
        }
    }

    protected void onServiceUnbound() {
    }

    protected void onCreateServiceBound() {
        if (mLists == null || mLists.size() == 0 || !(this instanceof UserBrowser)) return;
        for (LazyListView lv : mLists){
            if (lv.getAdapter() instanceof MyTracksAdapter)
                try {
                    ((MyTracksAdapter) lv.getAdapter()).checkUploadStatus(mCreateService.getUploadLocalId());
                } catch (RemoteException ignored) {}
        }
    }

    @Override
    public boolean onSearchRequested() {
        // just focus on the search tab, don't show default android search dialog
        startActivity(new Intent(this, Main.class).putExtra("tabTag", "search").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return false;
    }

    private ServiceConnection osc = new ServiceConnection() {
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


    private ServiceConnection createOsc = new ServiceConnection() {
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

        mLists = new ArrayList<LazyListView>();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectivityListener.unregisterHandler(connHandler);
        connectivityListener = null;
        unregisterReceiver(mPlaybackStatusListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        connectivityListener.startListening(this);

        CloudUtils.bindToService(this, CloudPlaybackService.class, osc);
        CloudUtils.bindToService(this, CloudCreateService.class, createOsc);

        if (mPlaybackService != null) {
            try {
                setPlayingTrack(mPlaybackService.getTrackId(), mPlaybackService.isPlaying());
            } catch (Exception e) {
                Log.e(TAG, "error", e);
            }
        }

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

        for (LazyListView l : mLists) {
            if (LazyBaseAdapter.class.isAssignableFrom(l.getAdapter().getClass())){
                ((LazyBaseAdapter) l.getAdapter()).notifyDataSetChanged();
            }
            if (l.getWrapper() != null) {
                if (l.getWrapper().isRefreshing()) {
                    l.prepareForRefresh();
                    l.setSelection(0);
                } else if (l.getWrapper().needsRefresh()) {
                    l.onRefresh();
                } else if (l.getFirstVisiblePosition() == 0) {
                    l.setSelection(1);
                }
            }



        }

        Account account = getApp().getAccount();
        if (account == null) {
            finish();
        }
    }

    public void playTrack(long trackId, final ArrayList<Parcelable> list, final int playPos, boolean goToPlayer) {
        // find out if this track is already playing. If it is, just go to the player
        try {
            if (mPlaybackService != null
                    && mPlaybackService.getTrackId() != -1
                    && mPlaybackService.getTrackId() == trackId) {
                if (goToPlayer) {
                    // skip the enqueuing, its already playing
                    launchPlayer();
                } else {
                    mPlaybackService.play();
                }
                return;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        // pass the tracklist to the application. This is the quickest way to get it to the service
        // another option would be to pass the parcelables through the intent, but that has the
        // unnecessary overhead of unmarshalling/marshalling them in to bundles. This way
        // we are just passing pointers
        // XXX ^^ this is assuming service and activity run in the same process
        getApp().cachePlaylist(list);
        if (goToPlayer) getApp().playerWaitForArtwork = true;

        try {
            mPlaybackService.playFromAppCache(playPos);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        if (goToPlayer) {
            launchPlayer();
            mIgnorePlaybackStatus = true;
        }
    }

    public void launchPlayer() {
        Intent i = new Intent(this, ScPlayer.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }


    public void pause(boolean force) {
        try {
            if (mPlaybackService != null) {
                if (mPlaybackService.isPlaying()) {
                    if (force) {
                        mPlaybackService.forcePause();
                    } else {
                        mPlaybackService.pause();
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    // WTF? why is this in ScActivity?
    // called from UserBrowser XXX replace with intent
    public boolean startUpload(Recording r) {
        if (mCreateService == null) return false;

        boolean uploading;
        try {
            uploading = mCreateService.isUploading();
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            uploading = true;
        }

        if (!uploading) {
            r.upload_status = Recording.UploadStatus.UPLOADING;
            getContentResolver().update(r.toUri(), r.buildContentValues(), null, null);
            try {
                mCreateService.uploadTrack(r.uploadData());
                return true;
            } catch (RemoteException ignored) {
                Log.e(TAG, "error", ignored);
            }

        } else {
            showToast(R.string.wait_for_upload_to_finish);
        }

        return false;
    }

    public void showToast(int stringId) {
        CloudUtils.showToast(this, getResources().getString(stringId));
    }

    protected void showToast(CharSequence text) {
        CloudUtils.showToast(this,text);
    }

    public LazyListView buildList() {
        return configureList(new LazyListView(this));
    }

    public LazyListView configureList(LazyListView lv) {
        return configureList(lv,mLists.size());
    }

    public LazyListView configureList(LazyListView lv, int addAtPosition) {
        lv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        lv.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        lv.setLazyListListener(mLazyListListener);
        lv.setFastScrollEnabled(false);
        lv.setTextFilterEnabled(true);
        lv.setDivider(getResources().getDrawable(R.drawable.list_separator));
        lv.setDividerHeight(1);
        lv.setCacheColorHint(Color.TRANSPARENT);
        mLists.add(addAtPosition < 0 || addAtPosition > mLists.size() ? mLists.size() : addAtPosition,lv);
        return lv;
    }

    public int removeList(LazyListView lazyListView){
        int i = 0;
        while (i < mLists.size()){
            if (mLists.get(i).equals(lazyListView)){
                mLists.remove(i);
                return i;
            }
            i++;
        }
        return -1;
    }

    public void addNewComment(final Comment comment, final AddCommentListener listener) {
        final AddCommentDialog dialog = new AddCommentDialog(this, comment, listener);
        dialog.show();
        dialog.getWindow().setGravity(Gravity.TOP);
    }

    public AddCommentListener mAddCommentListener = new AddCommentListener(){
        @Override
        public void onCommentAdd(boolean success, Comment c) {
        }

        @Override
        public void onException(Comment c, Exception e) {
            setException(e);
            handleException();
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

        for (LazyListView list : mLists) {
            if (TracklistAdapter.class.isAssignableFrom(list.getAdapter().getClass())) {
                ((TracklistAdapter) list.getAdapter()).setPlayingId(id, isPlaying);
                //((TracklistAdapter) list.getAdapter()).notifyDataSetChanged();
            }
        }
    }

    public Exception getException() {
        return mException;
    }

    public void setException(Exception e) {
        if (e != null) Log.i(TAG, "exception", e);
        mException = e;
    }

    // XXX why not pass Exception in?
    public void handleException() {
        if (getException() instanceof UnknownHostException
                || getException() instanceof SocketException
                || getException() instanceof JSONException) {
            safeShowDialog(Consts.Dialogs.DIALOG_ERROR_LOADING);
        }
        setException(null);
    }

    public void safeShowDialog(int dialogId) {
        if (!isFinishing()) {
            showDialog(dialogId);
        }
    }

    protected void onDataConnectionChanged(boolean isConnected) {
        if (isConnected) {
            // clear image loading errors
            ImageLoader.get(ScActivity.this).clearErrors();
        }
    }

    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_CANCEL_UPLOAD:
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
                                removeDialog(Consts.Dialogs.DIALOG_CANCEL_UPLOAD);
                            }
                        }).setNegativeButton(getString(R.string.btn_no),
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
            default:
                return super.onCreateDialog(which);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getParent() == null) {
            menu.add(menu.size(), Consts.OptionsMenu.INCOMING,
                menu.size(), R.string.menu_incoming).setIcon(R.drawable.ic_menu_incoming);
        }

        menu.add(menu.size(), Consts.OptionsMenu.VIEW_CURRENT_TRACK,
                menu.size(), R.string.menu_view_current_track).setIcon(R.drawable.ic_menu_player);

        menuCurrentUploadingItem = menu.add(menu.size(),
                Consts.OptionsMenu.CANCEL_CURRENT_UPLOAD, menu.size(),
                R.string.menu_cancel_current_upload).setIcon(R.drawable.ic_menu_delete);

        menu.add(menu.size(), Consts.OptionsMenu.FRIEND_FINDER, menu.size(), R.string.menu_friend_finder)
                .setIcon(R.drawable.ic_menu_friendfinder);

        menu.add(menu.size(), Consts.OptionsMenu.SETTINGS, menu.size(), R.string.menu_settings)
                .setIcon(android.R.drawable.ic_menu_preferences);

        if (this instanceof ScCreate) {
            menu.add(menu.size(), Consts.OptionsMenu.UPLOAD_FILE, 0, R.string.menu_upload_file).setIcon(
                android.R.drawable.ic_menu_upload);

        }  else {
            menu.add(menu.size(), Consts.OptionsMenu.REFRESH, 0, R.string.menu_refresh).setIcon(
                R.drawable.ic_menu_refresh);
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
            case Consts.OptionsMenu.REFRESH:
                onRefresh();
                return true;
            case Consts.OptionsMenu.VIEW_CURRENT_TRACK:
                startActivity(new Intent(this, ScPlayer.class));
                return true;
            case Consts.OptionsMenu.INCOMING:
                intent = new Intent(this, Main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("tabTag", "incoming");
                startActivity(intent);
                return true;
            case Consts.OptionsMenu.FRIEND_FINDER:
                intent = new Intent(this, Main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("userBrowserTag", UserBrowser.TabTags.friend_finder);
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
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (connectivityListener != null) {
                        NetworkInfo networkInfo = connectivityListener.getNetworkInfo();
                        if (networkInfo != null) {
                            if (networkInfo.isConnected()) ImageLoader.get(getApplicationContext()).clearErrors();
                            ScActivity.this.onDataConnectionChanged(networkInfo.isConnected());
                        }
                    }
                    break;
            }
        }
    };

    public long getCurrentUserId() {
        return getApp().getCurrentUserId();
    }

    public void onRefresh() {
    }

    public void pageTrack(String path) {
        getApp().pageTrack(path);
    }

    protected void handleRecordingClick(Recording recording) {
    }

    private LazyListView.LazyListListener mLazyListListener = new LazyListView.LazyListListener() {

        @Override
        public void onUserClick(ArrayList<Parcelable> users, int position) {
            Intent i = new Intent(ScActivity.this, UserBrowser.class);

            i.putExtra("user", users.get(position) instanceof
                    Friend ? ((Friend) users.get(position)).user : users.get(position));
            startActivity(i);
        }

        @Override
        public void onTrackClick(ArrayList<Parcelable> tracks, int position) {
            playTrack(((Track) tracks.get(position)).id, tracks, position, true);
        }

        @Override
        public void onEventClick(ArrayList<Parcelable> events, int position) {
            playTrack(((Event) events.get(position)).getTrack().id, events, position, true);
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
}
