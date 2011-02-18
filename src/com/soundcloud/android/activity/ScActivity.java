
package com.soundcloud.android.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.service.ICloudPlaybackService;
import com.soundcloud.utils.net.NetworkConnectivityListener;
import oauth.signpost.exception.OAuthCommunicationException;
import org.json.JSONException;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.net.SocketException;
import java.net.UnknownHostException;

public abstract class ScActivity extends Activity {
    private static final String TAG = "ScActivity";

    protected LinearLayout mHolder;

    private Exception mException = null;

    private String mError = null;

    protected String dialogUsername;

    protected ICloudPlaybackService mService = null;

    protected ICloudCreateService mCreateService = null;

    protected NetworkConnectivityListener connectivityListener;

    protected static final int CONNECTIVITY_MSG = 0;

    // Need handler for callbacks to the UI thread
    public final Handler mHandler = new Handler();

    protected GoogleAnalyticsTracker tracker;

    /**
     * Get an instance of our upload service
     * 
     * @return the upload service, or null if it doesn't exist for some reason
     */
    public ICloudCreateService getCreateService() {
        return mCreateService;
    }

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
            mService = ICloudPlaybackService.Stub.asInterface(obj);
            onServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
            onServiceUnbound();
            mService = null;
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

        CloudUtils.bindToService(this, CloudCreateService.class, createOsc);
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

        if (mCreateService != null) {
            //XXX this.unbindService(createOsc);
            mCreateService = null;
        }

        CloudUtils.unbindFromService(this);
        mService = null;

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSoundCloudApplication().getState() == SoundCloudAPI.State.AUTHORIZED) {
            onAuthenticated();
        } else {
            if (mHolder != null)
                mHolder.setVisibility(View.GONE);
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

    protected void onAuthenticated() {
    }

    protected void onReauthenticate() {
    }


    public void forcePause() {
        try {
            if (mService != null) {
                if (mService.isPlaying()) {
                    mService.forcePause();
                }
            }
        } catch (Exception e) {
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

    protected void cancelCurrentUpload() {
        try {
            mCreateService.cancelUpload();
        } catch (RemoteException e) {
            setException(e);
            handleException();
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
            case CloudUtils.Dialogs.DIALOG_ERROR_TRACK_ERROR:
                return new AlertDialog.Builder(this).setTitle(R.string.error_track_error_title)
                        .setMessage(R.string.error_track_error_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_TRACK_ERROR);
                                    }
                                }).create();

            case CloudUtils.Dialogs.DIALOG_UNAUTHORIZED:
                return new AlertDialog.Builder(this).setTitle(R.string.error_unauthorized_title)
                        .setMessage(R.string.error_unauthorized_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_UNAUTHORIZED);
                                    }
                                }).create();

            case CloudUtils.Dialogs.DIALOG_ERROR_TRACK_DOWNLOAD_ERROR:
                return new AlertDialog.Builder(this).setTitle(
                        R.string.error_track_download_error_title).setMessage(
                        R.string.error_track_download_error_message).setPositiveButton(
                        android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_TRACK_DOWNLOAD_ERROR);
                            }
                        }).create();

            case CloudUtils.Dialogs.DIALOG_GENERAL_ERROR:
                return new AlertDialog.Builder(this).setTitle(R.string.error_general_title)
                        .setMessage(R.string.error_general_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_GENERAL_ERROR);
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
            case CloudUtils.Dialogs.DIALOG_ERROR_STREAM_NOT_SEEKABLE:
                return new AlertDialog.Builder(this).setTitle(
                        R.string.error_stream_not_seekable_title).setMessage(
                        R.string.error_stream_not_seekable_message).setPositiveButton(
                        android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_LOADING);
                            }
                        }).create();
            case CloudUtils.Dialogs.DIALOG_SC_CONNECT_ERROR:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.error_sc_connect_error_title).setMessage(
                                R.string.error_sc_connect_error_message).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_SC_CONNECT_ERROR);
                                    }
                                }).create();
            case CloudUtils.Dialogs.DIALOG_ERROR_CHANGE_FAVORITE_STATUS_ERROR:
                return new AlertDialog.Builder(this).setTitle(
                        R.string.error_change_favorite_status_error_title).setMessage(
                        R.string.error_change_favorite_status_error_message).setPositiveButton(
                        android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_CHANGE_FAVORITE_STATUS_ERROR);
                            }
                        }).create();
            case CloudUtils.Dialogs.DIALOG_ERROR_CHANGE_FOLLOWING_STATUS_ERROR:
                return new AlertDialog.Builder(this).setTitle(
                        R.string.error_change_following_status_error_title).setMessage(
                        R.string.error_change_following_status_error_message).setPositiveButton(
                        android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_CHANGE_FOLLOWING_STATUS_ERROR);
                            }
                        }).create();

            case CloudUtils.Dialogs.DIALOG_FOLLOWING:
                String msgString = getString(R.string.alert_following_message).replace(
                        "{username}", dialogUsername);
                return new AlertDialog.Builder(this).setTitle(R.string.alert_following_title)
                        .setMessage(msgString).setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_FOLLOWING);
                                    }
                                }).create();

            case CloudUtils.Dialogs.DIALOG_ALREADY_FOLLOWING:
                msgString = getString(R.string.alert_already_following_message).replace(
                        "{username}", dialogUsername);
                return new AlertDialog.Builder(this).setTitle(
                        R.string.alert_already_following_title).setMessage(msgString)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_ALREADY_FOLLOWING);
                                    }
                                }).create();

            case CloudUtils.Dialogs.DIALOG_UNFOLLOWING:
                msgString = getString(R.string.alert_unfollowing_message).replace("{username}",
                        dialogUsername);
                return new AlertDialog.Builder(this).setTitle(R.string.alert_unfollowing_title)
                        .setMessage(msgString).setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_UNFOLLOWING);
                                    }
                                }).create();

            case CloudUtils.Dialogs.DIALOG_PROCESSING:

                ProgressDialog mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setTitle(R.string.processing_title);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);

                return mProgressDialog;

            case CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD:
                return new AlertDialog.Builder(this).setTitle(R.string.dialog_cancel_upload_title)
                        .setMessage(R.string.dialog_cancel_upload_message).setPositiveButton(
                                getString(R.string.btn_yes), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        cancelCurrentUpload();
                                        removeDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);

                                    }
                                }).setNegativeButton(getString(R.string.btn_no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
                                    }
                                }).create();
            case CloudUtils.Dialogs.DIALOG_ERROR_MAKING_CONNECTION:
                return new AlertDialog.Builder(this).setTitle(
                        R.string.error_making_connection_title).setMessage(
                        R.string.error_making_connection_message).setPositiveButton(
                        R.string.btn_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_MAKING_CONNECTION);
                                finish();
                            }
                        }).create();
            case CloudUtils.Dialogs.DIALOG_ERROR_RECORDING:
                return new AlertDialog.Builder(this).setTitle(R.string.error_recording_title)
                        .setMessage(R.string.error_recording_message).setPositiveButton(
                                R.string.btn_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_RECORDING);
                                        finish();
                                    }
                                }).create();
            case CloudUtils.Dialogs.DIALOG_AUTHENTICATION_CONTACTING:
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setTitle(R.string.authentication_contacting_title);
                mProgressDialog.setMessage(getResources().getString(
                        R.string.authentication_contacting_message));
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                return mProgressDialog;

        }
        return super.onCreateDialog(which);
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
        }
        return super.onOptionsItemSelected(item);
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
