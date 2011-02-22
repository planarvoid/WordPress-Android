
package com.soundcloud.android.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.ICloudPlaybackService;
import com.soundcloud.android.view.LazyList;
import com.soundcloud.utils.net.NetworkConnectivityListener;
import oauth.signpost.exception.OAuthCommunicationException;
import org.json.JSONException;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public abstract class ScActivity extends Activity {
    private Exception mException = null;
    private String mError = null;

    protected ICloudPlaybackService mPlaybackService;
    protected NetworkConnectivityListener connectivityListener;
    
    protected long mCurrentTrackId = -1;
    protected LazyList mList;
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
            forcePause();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectivityListener.unregisterHandler(connHandler);
        connectivityListener = null;
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

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSoundCloudApplication().getState() != SoundCloudAPI.State.AUTHORIZED) {
            forcePause();

            if (!(this instanceof Authorize)) {
                onReauthenticate();

                Intent intent = new Intent(this, Authorize.class);
                intent.putExtra("reauthorize", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }
    }

    protected void onReauthenticate() {
    }
    
    public void playTrack(final List<Parcelable> list, final int playPos) {
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
                // skip the enqueuing, its already playing
                Intent intent = new Intent(this, ScPlayer.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
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
            Log.i(TAG, "Play from app cache call");
            mPlaybackService.playFromAppCache(playPos);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        Intent intent = new Intent(this, ScPlayer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);


        mIgnorePlaybackStatus = true;
    }


    public void forcePause() {
        try {
            if (mPlaybackService != null) {
                if (mPlaybackService.isPlaying()) {
                    mPlaybackService.forcePause();
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
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
    
    public void safeShowDialog(int dialogId){
        if (!isFinishing()){
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


    protected void onDataConnectionChanged(Boolean isConnected) {
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

    public abstract void onRefresh(boolean b);
}
