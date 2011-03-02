package com.soundcloud.android.activity;

import static com.soundcloud.android.CloudUtils.getCurrentUserId;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.service.ICloudPlaybackService;
import com.soundcloud.android.task.AddCommentTask;
import com.soundcloud.android.task.AddCommentTask.AddCommentListener;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.utils.net.NetworkConnectivityListener;

import oauth.signpost.exception.OAuthCommunicationException;

import org.json.JSONException;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public abstract class ScActivity extends Activity {
    private Exception mException = null;
    private String mError = null;

    protected ICloudPlaybackService mPlaybackService;
    protected ICloudCreateService mCreateService;
    protected NetworkConnectivityListener connectivityListener;

    protected ArrayList<LazyListView> mLists;
    protected ArrayList<LazyBaseAdapter> mAdapters;

    private MenuItem menuCurrentUploadingItem;
    boolean mIgnorePlaybackStatus;

    protected static final int CONNECTIVITY_MSG = 0;

    public boolean pendingIconsUpdate;

    // Need handler for callbacks to the UI thread
    public final Handler mHandler = new Handler();

    protected GoogleAnalyticsTracker tracker;

    /**
     * Get an instance of our communicator
     *
     * @return the Cloud Communicator singleton
     */
    public SoundCloudApplication getSoundCloudApplication() {
        return (SoundCloudApplication) this.getApplication();
    }

    public void showToast(int stringId) {
        showToast(getResources().getString(stringId));
    }

    public int getScrollState() {
        int scrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
        for (LazyListView lv : mLists){
            if (lv.getScrollState() > scrollState) scrollState = lv.getScrollState();
        }
        return scrollState;
    }


    protected void onServiceBound() {
        if (getSoundCloudApplication().getState() != SoundCloudAPI.State.AUTHORIZED) {
            pause(true);
            return;
        }

        try {
            setPlayingTrack(mPlaybackService.getTrackId(), mPlaybackService.isPlaying());
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    protected void onServiceUnbound() {
    }

    private ServiceConnection osc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            Log.i(TAG,"On Playback COnnected");
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
        this.registerReceiver(mPlaybackStatusListener, new IntentFilter(playbackFilter));

        mLists = new ArrayList<LazyListView>();
        mAdapters = new ArrayList<LazyBaseAdapter>();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectivityListener.unregisterHandler(connHandler);
        connectivityListener = null;
        this.unregisterReceiver(mPlaybackStatusListener);
    }

    protected void restoreState(Object[] saved) {
    }


    /**
     * Bind our services
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Get google tracker instance
        tracker = GoogleAnalyticsTracker.getInstance();

        // Start the tracker in manual dispatch mode...
        tracker.start("UA-2519404-11", this);

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

        tracker.stop();
        tracker = null;
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

        if (getSoundCloudApplication().getState() != SoundCloudAPI.State.AUTHORIZED) {
            pause(true);

            onReauthenticate();

            Intent intent = new Intent(this, Authorize.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    protected void onReauthenticate() {
    }

    public void playTrack(long trackId, final ArrayList<Parcelable> list, final int playPos, boolean goToPlayer) {

        // find out if this track is already playing. If it is, just go to the
        // player
        try {
            if (mPlaybackService != null && mPlaybackService.getTrackId() != -1
                    && mPlaybackService.getTrackId() == trackId) {
                if (goToPlayer) {
                    // skip the enqueuing, its already playing
                    Intent intent = new Intent(this, ScPlayer.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
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
        this.getSoundCloudApplication().cachePlaylist(list);

        try {
            mPlaybackService.playFromAppCache(playPos);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        if (goToPlayer) {
            Intent intent = new Intent(this, ScPlayer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            mIgnorePlaybackStatus = true;
        }
    }


    public void pause(boolean force) {
        try {
            if (mPlaybackService != null) {
                if (mPlaybackService.isPlaying()) {
                    if (force)
                        mPlaybackService.forcePause();
                    else
                        mPlaybackService.pause();
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }




    public LazyListView buildList() {
        LazyListView lv = new LazyListView(this);
        lv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        lv.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        lv.setLazyListListener(mLazyListListener);
        lv.setFastScrollEnabled(true);
        lv.setTextFilterEnabled(true);
        lv.setDivider(getResources().getDrawable(R.drawable.list_separator));
        lv.setDividerHeight(1);
        // lv.setCacheColorHint(getResources().getColor(R.color.transparent));
        lv.setCacheColorHint(Color.TRANSPARENT);
        mLists.add(lv);
        return lv;
    }


    public void addNewComment(final Comment comment, final AddCommentListener listener) {

        // set up dialog
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Dialog);
        dialog.setContentView(R.layout.add_comment_dialog);
        dialog.setCancelable(true);
        if (comment.reply_to_id > 0) {
            dialog.setTitle("Reply to " + comment.user.username + " at "
                    + CloudUtils.formatTimestamp(comment.timestamp));
        } else {
            dialog.setTitle((comment.timestamp == -1 ? "Add an untimed comment" : "Add comment at "
                    + CloudUtils.formatTimestamp(comment.timestamp)));
        }
        final EditText input = (EditText) dialog.findViewById(R.id.comment_input);

        ((Button) dialog.findViewById(R.id.positiveButton))
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        comment.body = input.getText().toString();
                        new AddCommentTask(ScActivity.this, comment,
                                listener == null ? mAddCommentListener : listener).execute();
                        dialog.dismiss();
                    }
                });

        ((Button) dialog.findViewById(R.id.negativeButton))
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
        // imm.showSoftInput(input,InputMethodManager.SHOW_FORCED);
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    Log.i(TAG, "HHHAS FOCUS");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                            InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
            }
        });

        // now that the dialog is set up, it's time to show it
        dialog.show();
    }

    private AddCommentListener mAddCommentListener = new AddCommentListener(){

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
        if (mAdapters == null || mAdapters.size() == 0)
            return;

        for (LazyBaseAdapter adp : mAdapters) {
            if (TracklistAdapter.class.isAssignableFrom(adp.getClass()))
                ((TracklistAdapter) adp).setPlayingId(id, isPlaying);
        }
    }

    protected void showToast(String text) {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public Exception getException() {
        return mException;
    }

    public void setException(Exception e) {
        if (e != null)
            Log.i(TAG, "exception: " + e.toString());
        mException = e;
    }

    public void handleException() {
        if (getException() instanceof UnknownHostException
                || getException() instanceof SocketException
                || getException() instanceof JSONException
                || getException() instanceof OAuthCommunicationException) {
            safeShowDialog(CloudUtils.Dialogs.DIALOG_ERROR_LOADING);
        } else {
            // don't show general errors :
            // safeShowDialog(CloudUtils.Dialogs.DIALOG_GENERAL_ERROR);
        }
        setException(null);
    }

    public void safeShowDialog(int dialogId) {
        if (!isFinishing()) {
            showDialog(dialogId);
        }
    }

    public void handleError() {
        if (mError != null) {
            if (mError.toLowerCase().indexOf("unauthorized") != -1)
                safeShowDialog(CloudUtils.Dialogs.DIALOG_UNAUTHORIZED);

            mError = null;
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
            case CloudUtils.Dialogs.DIALOG_UNAUTHORIZED:
                return new AlertDialog.Builder(this).setTitle(R.string.error_unauthorized_title)
                        .setMessage(R.string.error_unauthorized_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_UNAUTHORIZED);
                                    }
                                }).create();
            case CloudUtils.Dialogs.DIALOG_ERROR_LOADING:
                return new AlertDialog.Builder(this).setTitle(R.string.error_loading_title)
                        .setMessage(R.string.error_loading_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_LOADING);
                                    }
                                }).create();
            case CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD:
                return new AlertDialog.Builder(this).setTitle(R.string.dialog_cancel_upload_title)
                        .setMessage(R.string.dialog_cancel_upload_message).setPositiveButton(
                                getString(R.string.btn_yes), new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        //XXX cancelCurrentUpload();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG,"PPPARENT " + this.getParent());
        if (this.getParent() == null || this.getParent().getClass() != Main.class)
            menu.add(menu.size(), CloudUtils.OptionsMenu.INCOMING,
                menu.size(), R.string.menu_incoming).setIcon(
                R.drawable.ic_menu_incoming);


        menu.add(menu.size(), CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK,
                menu.size(), R.string.menu_view_current_track).setIcon(
                R.drawable.ic_menu_player);

        menuCurrentUploadingItem = menu.add(menu.size(),
                CloudUtils.OptionsMenu.CANCEL_CURRENT_UPLOAD, menu.size(),
                R.string.menu_cancel_current_upload).setIcon(R.drawable.ic_menu_delete);

        menu.add(menu.size(), CloudUtils.OptionsMenu.SETTINGS, menu.size(), R.string.menu_settings)
                .setIcon(R.drawable.ic_menu_preferences);

        menu.add(menu.size(), CloudUtils.OptionsMenu.REFRESH, 0, R.string.menu_refresh).setIcon(
                R.drawable.context_refresh);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Prepare the options menu based on the current class and current play
     * state
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            menuCurrentUploadingItem.setVisible(mCreateService.isUploading() ? true : false);
        } catch (Exception e) {
            menuCurrentUploadingItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CloudUtils.OptionsMenu.SETTINGS:
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
                return true;
            case CloudUtils.OptionsMenu.REFRESH:
                onRefresh();
                return true;
            case CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK:
                intent = new Intent(this, ScPlayer.class);
                startActivity(intent);
                return true;
            case CloudUtils.OptionsMenu.INCOMING:
                intent = new Intent(this, Main.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("tabTag", "incoming");
                startActivity(intent);
                return true;
            case CloudUtils.OptionsMenu.CANCEL_CURRENT_UPLOAD:
                safeShowDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
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
                            ScActivity.this.onDataConnectionChanged(networkInfo.isConnected());
                        }
                    }
                    break;
            }
        }
    };

    public long getUserId() {
        return getCurrentUserId(this);
    }

    public void onRefresh() {
    }

    private LazyListView.LazyListListener mLazyListListener = new LazyListView.LazyListListener() {

        @Override
        public void onUserClick(ArrayList<Parcelable> users, int position) {
            Intent i = new Intent(ScActivity.this, UserBrowser.class);
            i.putExtra("user", users.get(position));
            startActivity(i);
        }

        @Override
        public void onTrackClick(ArrayList<Parcelable> tracks, int position) {
            playTrack(((Track)tracks.get(position)).id, tracks, position, true);
        }

        @Override
        public void onEventClick(ArrayList<Parcelable> events, int position) {
            playTrack(((Event) events.get(position)).getTrack().id, events, position, true);
        }

        @Override
        public void onIconsShouldLoad() {

        }
    };

}
