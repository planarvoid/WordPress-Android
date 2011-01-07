package com.soundcloud.android.activity;

import java.net.SocketException;
import java.net.UnknownHostException;

import oauth.signpost.exception.OAuthCommunicationException;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Comment;

public abstract class ScActivity extends Activity  {
	private static final String TAG = "LazyActivity";
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


	// Need handler for callbacks to the UI thread
    public final Handler mHandler = new Handler();
    
    protected GoogleAnalyticsTracker tracker;
	
	/**
	 * 
	 * @param savedInstanceState
	 * @param layoutResId
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Get goole tracker instance
		tracker = GoogleAnalyticsTracker.getInstance();

	    // Start the tracker in manual dispatch mode...
	    tracker.start("UA-2519404-11", this);
	 	
		
	    // Volume mode should always be music in this app
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	

	@Override
	protected void onDestroy() {
    	super.onDestroy();
    	tracker.stop();
    }
	
	
	/**
	 * Get an instance of our communicator
	 * @return the Cloud Communicator singleton
	 */
	public SoundCloudApplication getSoundCloudApplication(){
		return (SoundCloudApplication) this.getApplication();
	}
	
	public void showToast(int stringId) {
		showToast(getResources().getString(stringId));
	}
	
	protected void showToast(String text) {
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
	
	/**
	 * Get the current exception
	 * @return the exception
	 */
	public Exception getException() {
		return mException;
	}

	/**
	 * Get the current error
	 * @return the error
	 */
	public String getError() {
		return mError;
	}
	
	
	public void setException(Exception e) {
		if (e != null) Log.i(TAG, "exception: " + e.toString());
		mException = e;
	}
	
	public void setError(String e) {
		if (e != null) Log.i(TAG, "error" + e.toString());
		mError = e;
	}


	public void handleException() {
		
		if (getException() != null) {
			if (getException() instanceof UnknownHostException 
					|| getException() instanceof SocketException
					|| getException() instanceof JSONException
					|| getException() instanceof OAuthCommunicationException) {
				showDialog(CloudUtils.Dialogs.DIALOG_ERROR_LOADING);
			}  else {
				showDialog(CloudUtils.Dialogs.DIALOG_GENERAL_ERROR);
			}
		}
		setException(null);
	}
	
	
	protected void cancelCurrentUpload(){
		
	}
	 
	
	 @Override
		protected Dialog onCreateDialog(int which) {
			switch (which) {
			
				case CloudUtils.Dialogs.DIALOG_ERROR_TRACK_ERROR:
					return new AlertDialog.Builder(this).setTitle(R.string.error_track_error_title).setMessage(R.string.error_track_error_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_TRACK_ERROR);
						}
					}).create();
					
				case CloudUtils.Dialogs.DIALOG_ERROR_TRACK_DOWNLOAD_ERROR:
					return new AlertDialog.Builder(this).setTitle(R.string.error_track_download_error_title).setMessage(R.string.error_track_download_error_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_TRACK_DOWNLOAD_ERROR);
						}
					}).create();
					
				case CloudUtils.Dialogs.DIALOG_GENERAL_ERROR:
					return new AlertDialog.Builder(this).setTitle(R.string.error_general_title).setMessage(R.string.error_general_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_GENERAL_ERROR);
						}
					}).create();
					
				case CloudUtils.Dialogs.DIALOG_ERROR_LOADING:
					return new AlertDialog.Builder(this).setTitle(R.string.error_loading_title).setMessage(R.string.error_loading_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_LOADING);
						}
					}).create();
				case CloudUtils.Dialogs.DIALOG_ERROR_STREAM_NOT_SEEKABLE:
					return new AlertDialog.Builder(this).setTitle(R.string.error_stream_not_seekable_title).setMessage(R.string.error_stream_not_seekable_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_LOADING);
						}
					}).create();
				case CloudUtils.Dialogs.DIALOG_SC_CONNECT_ERROR:
					return new AlertDialog.Builder(this).setTitle(R.string.error_sc_connect_error_title).setMessage(R.string.error_sc_connect_error_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_SC_CONNECT_ERROR);
						}
					}).create();
				case CloudUtils.Dialogs.DIALOG_ERROR_CHANGE_FAVORITE_STATUS_ERROR:
					return new AlertDialog.Builder(this).setTitle(R.string.error_change_favorite_status_error_title).setMessage(R.string.error_change_favorite_status_error_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_CHANGE_FAVORITE_STATUS_ERROR);
						}
					}).create();
				case CloudUtils.Dialogs.DIALOG_ERROR_CHANGE_FOLLOWING_STATUS_ERROR:
					return new AlertDialog.Builder(this).setTitle(R.string.error_change_following_status_error_title).setMessage(R.string.error_change_following_status_error_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_CHANGE_FOLLOWING_STATUS_ERROR);
						}
					}).create();
				
				case CloudUtils.Dialogs.DIALOG_FOLLOWING:
					String msgString = getString(R.string.alert_following_message).replace("{username}", dialogUsername);
					return new AlertDialog.Builder(this).setTitle(R.string.alert_following_title).setMessage(msgString).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_FOLLOWING);
						}
					}).create();
					
				case CloudUtils.Dialogs.DIALOG_ALREADY_FOLLOWING:
					msgString = getString(R.string.alert_already_following_message).replace("{username}", dialogUsername);
					return new AlertDialog.Builder(this).setTitle(R.string.alert_already_following_title).setMessage(msgString).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_ALREADY_FOLLOWING);
						}
					}).create();
					
				case CloudUtils.Dialogs.DIALOG_UNFOLLOWING:
					msgString = getString(R.string.alert_unfollowing_message).replace("{username}", dialogUsername);
					return new AlertDialog.Builder(this).setTitle(R.string.alert_unfollowing_title).setMessage(msgString).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
					 return new AlertDialog.Builder(this)
		                .setTitle(R.string.dialog_cancel_upload_title)
		                .setMessage(R.string.dialog_cancel_upload_message)
		                .setPositiveButton(getString(R.string.btn_yes), new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
									cancelCurrentUpload();
								removeDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
		                       
		                    }
		                })
		                .setNegativeButton(getString(R.string.btn_no), new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
		                    	removeDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
		                    }
		                })
		                .create(); 
				case CloudUtils.Dialogs.DIALOG_AUTHENTICATION_CONTACTING:
					mProgressDialog = new ProgressDialog(this);
					mProgressDialog.setTitle(R.string.authentication_contacting_title);
					mProgressDialog.setMessage(getResources().getString(R.string.authentication_contacting_message));
					mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					mProgressDialog.setIndeterminate(true);
					mProgressDialog.setCancelable(false);
					return mProgressDialog;
					 
				case CloudUtils.Dialogs.DIALOG_AUTHENTICATION_RETRY:
					mProgressDialog = new ProgressDialog(this);
					mProgressDialog.setTitle(R.string.authentication_retrying_title);
					mProgressDialog.setMessage(getResources().getString(R.string.authentication_retrying_message));
					mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					mProgressDialog.setIndeterminate(true);
					mProgressDialog.setCancelable(false);
					return mProgressDialog;
					
				case CloudUtils.Dialogs.DIALOG_AUTHENTICATION_ERROR:
					return new AlertDialog.Builder(this)
					.setTitle(R.string.authentication_failed_title)
					.setMessage(R.string.authentication_failed_message)
					.setPositiveButton(R.string.btn_retry, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_ERROR);
							try {
								startActivity(((SoundCloudApplication) getApplication()).getAuthorizationIntent()); 
								finish(); 
							} catch (Exception e) {
								setException(e);
								handleException();
							}
						}
					}).create();
				case CloudUtils.Dialogs.DIALOG_ERROR_MAKING_CONNECTION:
					return new AlertDialog.Builder(this)
					.setTitle(R.string.error_making_connection_title)
					.setMessage(R.string.error_making_connection_message)
					.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_ERROR_MAKING_CONNECTION);
							finish();
						}
					}).create();
					
					
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
	
	
	
	
}
