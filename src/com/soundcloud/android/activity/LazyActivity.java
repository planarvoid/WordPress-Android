package com.soundcloud.android.activity;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import oauth.signpost.exception.OAuthCommunicationException;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.soundcloud.android.CloudCommunicator;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.CloudUploaderService;
import com.soundcloud.android.service.ICloudPlaybackService;
import com.soundcloud.android.service.ICloudUploaderService;
import com.soundcloud.android.task.LoadTask;

public abstract class LazyActivity extends Activity implements OnItemClickListener {
	private static final String TAG = "LazyActivity";
	protected LinearLayout mHolder;

	protected Parcelable mDetailsData;
	private boolean mMultipage = true;
	protected String mCurrentTrackId;
	

	protected String mFilter = "";
	private int mPageSize;
	private String mTrackOrder;
	private String mUserOrder;
	
	protected DBAdapter db;
	
	protected SharedPreferences mPreferences;
	
	
	//protected Downloader downloader;
	public CloudCommunicator mCloudComm;
	protected String oAuthToken = "";

	
	protected ICloudPlaybackService mService = null;
	private ICloudUploaderService mUploadService = null;
	
	private Exception mException = null;
	private String mError = null;
	protected Comment addComment;
	
	private MenuItem menuCurrentPlayingItem;
	private MenuItem menuCurrentUploadingItem;
	protected Parcelable menuParcelable;
	protected Parcelable dialogParcelable;
	protected String dialogUsername;
	
	protected LoadTask mLoadTask;
	
	private ProgressDialog mProgressDialog;


	protected LinearLayout mMainHolder;
	protected LinearLayout mRefreshBar;
	
	protected int mSearchListIndex;
	
	private int mLockedOrientation = -1;
	

	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;

	
	// Need handler for callbacks to the UI thread
    public final Handler mHandler = new Handler();
    
    protected GoogleAnalyticsTracker tracker;

	/**
	 * 
	 * @param savedInstanceState
	 * @param layoutResId
	 */
	protected void onCreate(Bundle savedInstanceState, int layoutResId) {
		super.onCreate(savedInstanceState);
		
		setContentView(layoutResId);
		
		// Get goole tracker instance
		tracker = GoogleAnalyticsTracker.getInstance();

	    // Start the tracker in manual dispatch mode...
	    tracker.start("UA-2519404-11", this);
		
	    // Volume mode should always be music in this app
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// Setup listener for the telephone
		MyPhoneStateListener phoneListener=new MyPhoneStateListener();
		TelephonyManager telephonyManager
		=(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		telephonyManager.listen(phoneListener,
		PhoneStateListener.LISTEN_CALL_STATE);
		
		// Get the instance of our communicator
		mCloudComm = CloudCommunicator.getInstance(this);
		
		build();
		
		restoreState();
		initLoadTasks(); 
	}
	
	/**
	 * Get an instance of our communicator
	 * @return the Cloud Communicator singleton
	 */
	public CloudCommunicator getCloudComm(){
		return mCloudComm;
	}
	
	/**
	 * Get an instance of our playback service
	 * @return the playback service, or null if it doesn't exist for some reason
	 */
	public ICloudPlaybackService getPlaybackService(){
		return mService;
	}
	
	/**
	 * Get an instance of our upload service
	 * @return the upload service, or null if it doesn't exist for some reason
	 */
	public ICloudUploaderService getUploadService(){
		return mUploadService;
	}
	
	/**
	 * A parcelable object has just been retrieved by an async task somewhere, so perform 
	 * any mapping necessary on that object, for example: {@link com.soundcloud.android.activity.Main}
	 * @param p
	 */
	public void mapDetails(Parcelable p){
		
	}

	/**
	 * Get a progress dialog that has been created. Used primarily to update dialog as necessary
	 * @return the current progress dialog, or null if one doesnt exist
	 */
	public ProgressDialog getProgressDialog() {
		return mProgressDialog;
	}
	
	/**
	 * Set the current progress dialog
	 * @param progressDialog : the progress dialog that shoudld be set as current
	 */
	public void setProgressDialog(ProgressDialog progressDialog) {
		mProgressDialog = progressDialog;
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
	
	
	public String getTrackOrder() {
		return mTrackOrder;
	}

	
	public String getUserOrder() {
		return mUserOrder;
	}
	
	/**
	 * Initialize any new loading, this will take place after any running loading tasks have been restored
	 */
	protected void initLoadTasks(){
		
	}
	
	
	/**
	 * Get the id of the track that is currently playing
	 * @return the track id that is being played
	 */
	public String getCurrentTrackId(){
		return mCurrentTrackId;
	}

	/**
	 * A parcelable has just been loaded, so perform any data operations necessary
	 * @param p : the parcelable that has just been loaded
	 */
	public void resolveParcelable(Parcelable p){
		if (p instanceof Track){
			CloudUtils.resolveTrack(this, (Track) p, false, CloudUtils.getCurrentUserId(this));
		} else if (p instanceof Event){
			if (((Event) p).getDataParcelable(Event.key_track) != null)
				CloudUtils.resolveTrack(this, (Track) ((Event) p).getDataParcelable(Event.key_track), false, CloudUtils.getCurrentUserId(this));
		}
	}
	
	/**
	 * Track has been clicked in a list, enqueu the list of tracks if necessary and send the user to the player
	 * @param list
	 * @param playPos
	 */
	public void playTrack(List<Parcelable> list, int playPos){
		
		Track t = null;
		
		// is this a track of a list
		 if (list.get(playPos) instanceof Track)
			t = ((Track) list.get(playPos));
		 else if (list.get(playPos) instanceof Event)
			t = (Track) ((Event) list.get(playPos)).getDataParcelable(Event.key_track);
		 
		try {
			if (t != null && mService != null && mService.getTrackId() != null && mService.getTrackId().contentEquals(t.getData(Track.key_id))){
				//skip the enquing, its already playing
				 Intent i = new Intent(this, ScPlayer.class);
				 startActivity(i);
				 return;
			}
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		 Intent intent = new Intent(this, ScPlayer.class);
		 intent.putExtra("track", t);
		 startActivity(intent);
		
		Track[] tl = new Track[list.size()];
		
		for (int i = 0; i < list.size(); i++){
			if (list.get(i) instanceof Track)
				tl[i] = (Track) list.get(i);
			else if (list.get(i) instanceof Event){
				tl[i] = (Track) ((Event) list.get(i)).getDataParcelable(Event.key_track);
			}
		}

		try {
			mService.enqueue(tl, playPos);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	
	/**
	 * A list item has been clicked
	 */
	public void onItemClick(AdapterView<?> list, View row, int position, long id) {
		
		if (((LazyBaseAdapter) list.getAdapter()).getData().size() <= 0 || position >= ((LazyBaseAdapter) list.getAdapter()).getData().size())
			return; // bad list item clicked (possibly loading item)
			
		if (((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof Track || ((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof Event){
			
			//track clicked
			this.playTrack(((LazyBaseAdapter) list.getAdapter()).getData(),position);
			
		} else if (((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof User){

			//user clicked
			Intent i = new Intent(this, ScProfile.class);
			i.putExtra("user", ((LazyBaseAdapter) list.getAdapter()).getData().get(position));
			startActivity(i);
			
		}
	}
	
	
	
	/**
	 * Handle common options menu building
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menuCurrentPlayingItem = menu.add(menu.size(), CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK, menu.size(), R.string.menu_view_current_track).setIcon(R.drawable.ic_menu_info_details);
		menuCurrentUploadingItem = menu.add(menu.size(), CloudUtils.OptionsMenu.CANCEL_CURRENT_UPLOAD, menu.size(), R.string.menu_cancel_current_upload).setIcon(R.drawable.ic_menu_delete);
		
		menu.add(menu.size(), CloudUtils.OptionsMenu.SETTINGS, menu.size(), R.string.menu_settings).setIcon(R.drawable.ic_menu_preferences);
		return super.onCreateOptionsMenu(menu);
	}
	
	
	
	
	public String getCurrentUserId() {
		return CloudUtils.getCurrentUserId(this);
	}
	
	public void showToast(int stringId) {
		showToast(getResources().getString(stringId));
	}
	
	protected void showToast(String text) {
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
	
	public void setException(Exception e) {
		if (e != null) Log.i("WARNING", "WE GOT AN EXCEPTION " + e.toString());
		mException = e;
	}
	
	public void setError(String e) {
		if (e != null) Log.i("WARNING", "WE GOT AN ERROR " + e.toString());
		mError = e;
	}


	public void handleException() {
		
		if (getException() != null) {
			if (getException() instanceof UnknownHostException 
					|| getException() instanceof SocketException
					|| getException() instanceof JSONException
					|| getException() instanceof OAuthCommunicationException) {
				//showDialog(CloudUtils.Dialogs.DIALOG_ERROR_LOADING);
			}  else {
				//showDialog(CloudUtils.Dialogs.DIALOG_GENERAL_ERROR);
			}
		}
		setException(null);
	}
	
	protected void handleError() {
		if (getError() != null) {
			if (getError().equalsIgnoreCase(CloudCommunicator.ERROR_UNAUTHORIZED)) {
				showDialog(CloudUtils.Dialogs.DIALOG_UNAUTHORIZED);
			} 
		}
		setError(null);
	}
	
	public class MyPhoneStateListener extends PhoneStateListener {
        Context context;
        @Override
        public void onCallStateChanged(int state,String incomingNumber){
                 Log.e("PhoneCallStateNotified", "Incoming number "+incomingNumber);
                 if (state != TelephonyManager.CALL_STATE_IDLE){
                	 if (mService != null)
						try {
							mService.pause();
						} catch (RemoteException e) {
							e.printStackTrace();
						}
                 }
        }

	}
	
	
	protected void onServiceBound(){}
	
	protected void onServiceUnbound(){}
	
	
	
	
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
	
	private ServiceConnection uploadOsc = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			mUploadService = (ICloudUploaderService) binder;
		}

		public void onServiceDisconnected(ComponentName className) {
		}

	};
    

	
	

	
	
	


	protected void showRefreshBar(String text){
		showRefreshBar(text,true);
	}
	
	protected void showRefreshBar(String text, Boolean showLoader){
		if (mRefreshBar == null){
			mRefreshBar = new LinearLayout(this);
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    inflater.inflate(R.layout.refresh_bar, mRefreshBar);
		    mMainHolder.addView(mRefreshBar,0);
		}
		
		if (showLoader)
			((ProgressBar) mRefreshBar.findViewById(R.id.refresh_progress_bar)).setVisibility(View.VISIBLE);
		else
			((ProgressBar) mRefreshBar.findViewById(R.id.refresh_progress_bar)).setVisibility(View.GONE);
		
		((TextView) mRefreshBar.findViewById(R.id.refresh_progress_bar_text)).setText(text);
	}
	
	protected void hideRefreshBar(){
		if (mRefreshBar == null)
			return;
		
		if (mRefreshBar.getParent() == mMainHolder)
			mMainHolder.removeView(mRefreshBar);
		
		mRefreshBar = null;
		
		System.gc();
	}
	
	public void leftSwipe(){
		Toast.makeText(this, "Left Swipe", Toast.LENGTH_SHORT).show();
	}
	
	public void rightSwipe(){
		Toast.makeText(this, "Right Swipe", Toast.LENGTH_SHORT).show();
	}
	
	public void setPageSize(int mPageSize) {
		this.mPageSize = mPageSize;
	}

	public int getPageSize() {
		return mPageSize;
	}



	protected float oldTouchValue;
	protected final static int SWIPE_TOLERANCE = 30;
	protected OnTouchListener swipeListener = new OnTouchListener(){ 
        @Override 
        public boolean onTouch(View v, MotionEvent touchevent) {
        	switch (touchevent.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                {
                    oldTouchValue = touchevent.getX();
                    break;
                }
                case MotionEvent.ACTION_UP:
                {
                    float currentX = touchevent.getX();
                    if (oldTouchValue > currentX && Math.abs(oldTouchValue - currentX) > SWIPE_TOLERANCE)
                    {
                    	leftSwipe();
                    }
                    if (oldTouchValue < currentX && Math.abs(oldTouchValue - currentX) > SWIPE_TOLERANCE)
                    {
                    	rightSwipe();
                    }
                break;
                }
            }
        	v.onTouchEvent(touchevent);
            return true;
        } 
	}; 

	
	
	
	
	 class MyGestureDetector extends SimpleOnGestureListener {
		    @Override
		    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		        try {
		            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
		                return false;
		            // right to left swipe
		            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
		                LazyActivity.this.leftSwipe();
		            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
		            	LazyActivity.this.rightSwipe();
		            }
		        } catch (Exception e) {
		            // nothing
		        }
		        return false;
		    }
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
				case CloudUtils.Dialogs.DIALOG_UNAUTHORIZED:
		            return new AlertDialog.Builder(this)
		                .setTitle(R.string.error_unauthorized_title)
		                .setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
								try {
									mCloudComm.clearSoundCloudAccount();
									mCloudComm.launchAuthorization();
								} catch (Exception e) {
									setException(e);
									handleException();
								}
								
								removeDialog(CloudUtils.Dialogs.DIALOG_UNAUTHORIZED);
		                       
		                    }
		                })
		                .setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
		                    	System.out.println("cancel clicked.");
		                    	removeDialog(CloudUtils.Dialogs.DIALOG_UNAUTHORIZED);
		                    }
		                })
		                .create(); 
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
								try {
									mUploadService.cancelUpload();
								} catch (Exception e) {
									setException(e);
									handleException();
								}
								
								removeDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
		                       
		                    }
		                })
		                .setNegativeButton(getString(R.string.btn_no), new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
		                    	removeDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
		                    }
		                })
		                .create(); 
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
							try {startActivity(mCloudComm.getAuthorizationIntent()); finish();} 
							catch (Exception e) {setException(e);handleException();}
						}
					}).create();
					
					
			}  
			return super.onCreateDialog(which);
		}

	
	 /**
		 * Prepare the options menu based on the current class and current play state
		 */
	    @Override
		public boolean onPrepareOptionsMenu(Menu menu) {
	    	if (!CloudUtils.stringNullEmptyCheck(getCurrentTrackId()) && !this.getClass().getName().contentEquals("com.soundcloud.android.ScPlayer")){
	    		menuCurrentPlayingItem.setVisible(true);
	    	} else {
	    		menuCurrentPlayingItem.setVisible(false);
	    	}
	    	
	    	try {
				if (mUploadService.isUploading()){
					menuCurrentUploadingItem.setVisible(true);
				} else {
					menuCurrentUploadingItem.setVisible(false);
				}
			} catch (RemoteException e) {
				menuCurrentUploadingItem.setVisible(false);
			}
	    		
	    	return true;	
	    }

	    /**
	     * Handle options menu selections
	     */
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				case CloudUtils.OptionsMenu.SETTINGS:
					startActivity(new Intent(this, Settings.class));
					return true;
				case CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK:
					startActivity(new Intent(this, ScPlayer.class));
					return true;
				case CloudUtils.OptionsMenu.CANCEL_CURRENT_UPLOAD:
					showDialog(CloudUtils.Dialogs.DIALOG_CANCEL_UPLOAD);
					return true;
			}
			return super.onOptionsItemSelected(item);
		}
	
	
	/***** State saving/restoring *****/
	
	
	/**
	 * Restore the state of this activity from a previous state, if one exists
	 */
	protected void restoreState() {
		
	}

	
	/***** Build and Lifecycle *****/
	
	/**
	 * Build any components we need to for this activity
	 */
	protected void build(){
		
	}
	
	
	/**
	 * Bind our services
	 */
	@Override
	protected void onStart() {
    	super.onStart();
    	
    	//start it so it is persistent outside this activity, then bind it
		startService(new Intent(this, CloudUploaderService.class));
		bindService(new Intent(this, CloudUploaderService.class), uploadOsc,0);
    	
		if (false == CloudUtils.bindToService(this, osc)) {
			// something went wrong
			//mHandler.sendEmptyMessage(QUIT);
		}
    }
	
	/**
	 * Unbind our services
	 */
	@Override
	protected void onStop() {
    	super.onStop();
    	
    	this.unbindService(uploadOsc);
    	
		CloudUtils.unbindFromService(this);
		mService = null;
    
    }


	
	/**
	 * 
	 */
	@Override
    protected void onResume() {
    	super.onResume();
    	// set our default preferences here, in case they were just changed 
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		setPageSize(Integer.parseInt(mPreferences.getString("defaultPageSize", "20")));
		mTrackOrder = mPreferences.getString("defaultTrackSorting", "");
		mUserOrder = mPreferences.getString("defaultUserSorting", "");
		
    }
	
	@Override
	protected void onDestroy() {
    	super.onDestroy();
    }
	
}
