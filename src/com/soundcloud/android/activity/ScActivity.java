
package com.soundcloud.android.activity;

import java.net.SocketException;
import java.net.UnknownHostException;

import oauth.signpost.exception.OAuthCommunicationException;

import org.json.JSONException;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.service.ICloudPlaybackService;
import com.soundcloud.utils.net.NetworkConnectivityListener;

public abstract class ScActivity extends Activity {
    private static final String TAG = "ScActivity";

    protected LinearLayout mHolder;

    protected Parcelable mDetailsData;

    protected String mCurrentTrackId;

    protected DBAdapter db;

    protected SharedPreferences mPreferences;

    private Exception mException = null;

    private String mError = null;

    protected Comment addComment;

    protected Parcelable menuParcelable;

    protected Parcelable dialogParcelable;

    protected String dialogUsername;

    private ProgressDialog mProgressDialog;

    protected ICloudPlaybackService mService = null;

    protected ICloudCreateService mCreateService = null;

    protected NetworkConnectivityListener connectivityListener;

    protected static final int CONNECTIVITY_MSG = 0;

    // Need handler for callbacks to the UI thread
    public final Handler mHandler = new Handler();

    protected GoogleAnalyticsTracker tracker;

    /**
     * Get an instance of our playback service
     * 
     * @return the playback service, or null if it doesn't exist for some reason
     */
    public ICloudPlaybackService getPlaybackService() {
        return mService;
    }

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

    /**
     * @param savedInstanceState
     * @param layoutResId
     */
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
        // Get goole tracker instance
        tracker = GoogleAnalyticsTracker.getInstance();

        // Start the tracker in manual dispatch mode...
        tracker.start("UA-2519404-11", this);

        connectivityListener.startListening(this);

        // start it so it is persistent outside this activity, then bind it
        startService(new Intent(this, CloudCreateService.class));
        bindService(new Intent(this, CloudCreateService.class), createOsc, 0);

        if (false == CloudUtils.bindToService(this, osc)) {
            Log.i(TAG, "BIND TO SERVICE FAILED");
        }
    }

    /**
     * Unbind our services
     */
    @Override
    protected void onStop() {
        super.onStop();

        Log.i(TAG, "KILLING THE TRACKER " + tracker);
        tracker.stop();
        tracker = null;
        Log.i(TAG, "KILLED THE TRACKER " + tracker);
        connectivityListener.stopListening();

        if (mCreateService != null) {
            this.unbindService(createOsc);
            mCreateService = null;
        }

        CloudUtils.unbindFromService(this);
        mService = null;

    }

    /**
	 * 
	 */
    @Override
    protected void onResume() {
        super.onResume();

        if (getSoundCloudApplication().getState() == SoundCloudAPI.State.AUTHORIZED) {
            onAuthenticated();
        } else {
            if (mHolder != null)
                mHolder.setVisibility(View.GONE);
            forcePause();

            if (!this.getClass().equals(Authorize.class)) {
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

    /**
	 * 
	 */
    public void forcePause() {
        try {
            if (mService != null) {
                if (mService.isPlaying()) {
                    mService.forcePause();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void showToast(String text) {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     * Get the current exception
     * 
     * @return the exception
     */
    public Exception getException() {
        return mException;
    }

    /**
     * Get the current error
     * 
     * @return the error
     */
    public String getError() {
        return mError;
    }

    public void setException(Exception e) {
        if (e != null)
            Log.i(TAG, "exception: " + e.toString());
        mException = e;
    }

    public void setError(String e) {
        if (e != null)
            Log.i(TAG, "error: " + e.toString());
        mError = e;

    }

    public void handleException() {

        if (getException() != null) {
            if (getException() instanceof UnknownHostException
                    || getException() instanceof SocketException
                    || getException() instanceof JSONException
                    || getException() instanceof OAuthCommunicationException) {
                showDialog(CloudUtils.Dialogs.DIALOG_ERROR_LOADING);
            } else {
                // don't show general errors :
                // showDialog(CloudUtils.Dialogs.DIALOG_GENERAL_ERROR);
            }
        }
        setException(null);
    }

    public void handleError() {
        if (mError != null) {
            if (mError.toString().toLowerCase().indexOf("unauthorized") != -1)
                showDialog(CloudUtils.Dialogs.DIALOG_UNAUTHORIZED);

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

                mProgressDialog = new ProgressDialog(this);
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

    /**
     * Handle options menu selections
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CloudUtils.OptionsMenu.SETTINGS:
                Intent intent = new Intent(this, Settings.class);
                startActivityForResult(intent, CloudUtils.RequestCodes.REUATHORIZE);

                return true;
            case CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK:
                intent = new Intent(this, ScPlayer.class);
                startActivityForResult(intent, CloudUtils.RequestCodes.REUATHORIZE);
                return true;
            case CloudUtils.OptionsMenu.CANCEL_CURRENT_UPLOAD:
                showDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Handler connHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTIVITY_MSG:

                    if (connectivityListener == null)
                        return;
                    NetworkInfo networkInfo = connectivityListener.getNetworkInfo();

                    if (networkInfo == null)
                        return;
                    ScActivity.this.onDataConnectionChanged(networkInfo.isConnected());
                    break;
            }
        }
    };

}
