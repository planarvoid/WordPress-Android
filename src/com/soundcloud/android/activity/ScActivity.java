package com.soundcloud.android.activity;

import static com.soundcloud.android.CloudUtils.getCurrentUserId;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.ICloudPlaybackService;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.utils.net.NetworkConnectivityListener;

import oauth.signpost.exception.OAuthCommunicationException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public abstract class ScActivity extends Activity {
    private Exception mException = null;
    private String mError = null;

    protected ICloudPlaybackService mPlaybackService;
    protected NetworkConnectivityListener connectivityListener;

    protected long mCurrentTrackId = -1;
    protected ArrayList<LazyBaseAdapter> mAdapters;

    boolean mIgnorePlaybackStatus;

    protected static final int CONNECTIVITY_MSG = 0;

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
            mPlaybackService = ICloudPlaybackService.Stub.asInterface(obj);
            onServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
            onServiceUnbound();
            mPlaybackService = null;
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

        Log.v(TAG, "KILLING THE TRACKER " + tracker);
        tracker.stop();
        tracker = null;
        Log.v(TAG, "KILLED THE TRACKER " + tracker);
        connectivityListener.stopListening();


        CloudUtils.unbindFromService(this);
        mPlaybackService = null;
        mIgnorePlaybackStatus = false;

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSoundCloudApplication().getState() != SoundCloudAPI.State.AUTHORIZED) {
            pause(true);

            onReauthenticate();

            Intent intent = new Intent(this, Authorize.class);
            intent.putExtra("reauthorize", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    protected void onReauthenticate() {
    }

    public void playTrack(final List<Parcelable> list, final int playPos, boolean goToPlayer) {
        Track t = null;

        // is this a track of a list
        if (list.get(playPos) instanceof Track)
            t = ((Track) list.get(playPos));
        else if (list.get(playPos) instanceof Event)
            t = ((Event) list.get(playPos)).getTrack();

        // find out if this track is already playing. If it is, just go to the
        // player
        try {
            if (t != null && mPlaybackService != null && mPlaybackService.getTrackId() != -1
                    && mPlaybackService.getTrackId() == (t.id)) {
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
        this.getSoundCloudApplication().cachePlaylist((ArrayList<Parcelable>) list);

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

    public void setFavoriteStatus(Track t, boolean favoriteStatus) {
        synchronized (this) {
            if (favoriteStatus)
                addFavorite(t);
            else
                removeFavorite(t);
        }

    }

    public void addFavorite(Track t) {
        FavoriteAddTask f = new FavoriteAddTask((SoundCloudApplication) this.getApplication());
        f.setOnFavoriteListener(new FavoriteTask.FavoriteListener() {
            @Override
            public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
                onFavoriteStatusSet(trackId, isFavorite);
            }

            @Override
            public void onException(long trackId, Exception e) {
                onFavoriteStatusSet(trackId, false); //failed, so it shouldn't be a favorite
            }

        });
        f.execute(t);
    }

    public void removeFavorite(Track t) {
        FavoriteRemoveTask f = new FavoriteRemoveTask((SoundCloudApplication) this.getApplication());
        f.setOnFavoriteListener(new FavoriteTask.FavoriteListener() {
            @Override
            public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
                onFavoriteStatusSet(trackId, isFavorite);
            }

            @Override
            public void onException(long trackId, Exception e) {
                onFavoriteStatusSet(trackId, true); //failed, so it should still be a favorite
            }

        });
        f.execute(t);
    }

    protected void onFavoriteStatusSet(long trackId, boolean isFavorite) {

    }


    public LazyListView buildList() {
        LazyListView lv = new LazyListView(this);
        lv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        lv.setOnItemClickListener(mOnItemClickListener);
        lv.setOnItemLongClickListener(mOnItemLongClickListener);
        lv.setOnItemSelectedListener(mOnItemSelectedListener);
        lv.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        lv.setFastScrollEnabled(true);
        lv.setTextFilterEnabled(true);
        lv.setDivider(getResources().getDrawable(R.drawable.list_separator));
        lv.setDividerHeight(1);
        // lv.setCacheColorHint(getResources().getColor(R.color.transparent));
        lv.setCacheColorHint(Color.TRANSPARENT);
        return lv;
    }


    protected AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {

        public void onItemClick(AdapterView<?> list, View row, int position, long id) {
            if (((LazyBaseAdapter) list.getAdapter()).getData().size() <= 0
                    || position >= ((LazyBaseAdapter) list.getAdapter()).getData().size())
                return; // bad list item clicked (possibly loading item)

            if (((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof Track
                    || ((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof Event) {
                ScActivity.this.playTrack(((LazyBaseAdapter) list.getAdapter()).getData(), position, true);

            } else if (((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof User) {
                Intent i = new Intent(ScActivity.this, UserBrowser.class);
                i.putExtra("user", ((LazyBaseAdapter) list.getAdapter()).getData().get(position));
                startActivity(i);

            }
        }

    };

    protected AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {

        public boolean onItemLongClick(AdapterView<?> list, View row, int position, long id) {
            if (((LazyBaseAdapter) list.getAdapter()).getData().size() <= 0
                    || position >= ((LazyBaseAdapter) list.getAdapter()).getData().size())
                return false; // bad list item clicked (possibly loading item)

            ((LazyBaseAdapter) list.getAdapter()).submenuIndex = ((LazyBaseAdapter) list.getAdapter()).animateSubmenuIndex = position;
            ((LazyBaseAdapter) list.getAdapter()).notifyDataSetChanged();
            return true;
        }

    };
    protected AdapterView.OnItemSelectedListener mOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> listView, View view, int position, long id) {
            if (((LazyBaseAdapter) listView.getAdapter()).submenuIndex == position)
                listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            else
                listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        }

        public void onNothingSelected(AdapterView<?> listView) {
            // This happens when you start scrolling, so we need to prevent it from staying
            // in the afterDescendants mode if the EditText was focused
            listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        }
    };


    public void addNewComment(final Track track, final long timestamp) {
        final EditText input = new EditText(this);
        final AlertDialog commentDialog = new AlertDialog.Builder(ScActivity.this)
                .setMessage(timestamp == -1 ? "Add an untimed comment" : "Add comment at " + CloudUtils.formatTimestamp(timestamp))
                .setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        sendComment(track.id, timestamp, input.getText().toString(), 0);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).create();

        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    commentDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        commentDialog.show();
    }

    private HttpResponse mAddCommentResult;
    private Comment mAddComment;

    void sendComment(final long track_id, long timestamp, final String commentBody, long replyTo) {

        mAddComment = new Comment();
        mAddComment.track_id = track_id;
        mAddComment.created_at = new Date(System.currentTimeMillis());
        mAddComment.user_id = getCurrentUserId(this);

        mAddComment.user = SoundCloudDB.getInstance().resolveUserById(this.getContentResolver(), mAddComment.user_id);
        mAddComment.timestamp = timestamp;
        mAddComment.body = commentBody;

        final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
        apiParams.add(new BasicNameValuePair("comment[body]", commentBody));
        if (timestamp > -1) apiParams.add(new BasicNameValuePair("comment[timestamp]", Long.toString(timestamp)));
        if (replyTo > 0) apiParams.add(new BasicNameValuePair("comment[reply_to]", Long.toString(replyTo)));


        // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    mAddCommentResult = getSoundCloudApplication().postContent(
                            CloudAPI.Enddpoints.TRACK_COMMENTS.replace("{track_id}", Long.toString(mAddComment.track_id)), apiParams);
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                    ScActivity.this.setException(e);
                }
                mHandler.post(mOnCommentAdd);
            }
        };
        t.start();
    }

    // Create runnable for posting
    final Runnable mOnCommentAdd = new Runnable() {
        public void run() {

            if (mAddCommentResult != null && mAddCommentResult.getStatusLine().getStatusCode() == 201) {
                onCommentAdded(mAddComment);
            } else {
                handleException();
            }
        }
    };

    protected void onCommentAdded(Comment c) {
        getSoundCloudApplication().uncacheComments(c.track_id);
    }


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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CloudUtils.OptionsMenu.SETTINGS:
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);

                return true;
            case CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK:
                intent = new Intent(this, ScPlayer.class);
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
}
