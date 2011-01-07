package com.soundcloud.android.activity;

import java.util.List;

import org.urbanstew.soundcloudapi.SoundCloudAPI;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.imageloader.ImageLoader;
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
import com.soundcloud.utils.net.NetworkConnectivityListener;

public abstract class LazyActivity extends ScActivity implements OnItemClickListener {
	private static final String TAG = "LazyActivity";
	protected LinearLayout mHolder;

	protected Parcelable mDetailsData;
	protected String mCurrentTrackId;
	
	private int[] mAuthorizePostDelays = {1000,3000,10000};
	private int mAuthorizeRetries = 0;
	protected SoundCloudAPI.State mLastCloudState;
	private Exception mAuthException;
	

	protected String mFilter = "";
	private int mPageSize;
	private String mTrackOrder;
	private String mUserOrder;
	
	protected DBAdapter db;
	
	protected SharedPreferences mPreferences;
	
	
	//protected Downloader downloader;
	protected String oAuthToken = "";

	
	protected ICloudPlaybackService mService = null;
	private ICloudUploaderService mUploadService = null;
	
	protected Comment addComment;
	
	private MenuItem menuCurrentPlayingItem;
	private MenuItem menuCurrentUploadingItem;
	protected Parcelable menuParcelable;
	protected Parcelable dialogParcelable;
	protected String dialogUsername;
	
	protected LoadTask mLoadTask;
	
	private ProgressDialog mProgressDialog;


	protected LinearLayout mMainHolder;
	
	protected int mSearchListIndex;
	
	protected NetworkConnectivityListener connectivityListener;
	protected static final int CONNECTIVITY_MSG = 0;
	
	
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;
	
    
	/**
	 * 
	 * @param savedInstanceState
	 * @param layoutResId
	 */
	protected void onCreate(Bundle savedInstanceState, int layoutResId) {
		super.onCreate(savedInstanceState);
		
		setContentView(layoutResId);
		
		// Setup listener for the telephone
		MyPhoneStateListener phoneListener=new MyPhoneStateListener();
		TelephonyManager telephonyManager
		=(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		telephonyManager.listen(phoneListener,
		PhoneStateListener.LISTEN_CALL_STATE);
		
		connectivityListener = new NetworkConnectivityListener();
		connectivityListener.registerHandler(connHandler,CONNECTIVITY_MSG);
		
		build();
		restoreState();
		initLoadTasks();	
		 
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
	 * any mapping necessary on that object, for example: {@link com.soundcloud.android.activity.Dashboard}
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
	
	
	
	public String getTrackOrder() {
		return mTrackOrder;
	}

	
	public String getUserOrder() {
		return mUserOrder;
	}
	
	
	public void forcePause(){
		try {
			if (mService != null){
				if (mService.isPlaying()){
						mService.pause();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	public void playTrack(final List<Parcelable> list, final int playPos){
		
		Track t = null;
		
		// is this a track of a list
		 if (list.get(playPos) instanceof Track)
			t = ((Track) list.get(playPos));
		 else if (list.get(playPos) instanceof Event)
			t = (Track) ((Event) list.get(playPos)).getDataParcelable(Event.key_track);
		 
		try {
			if (t != null && mService != null && mService.getTrackId() != null && mService.getTrackId().contentEquals(t.getData(Track.key_id))){
				//skip the enquing, its already playing
				 Intent intent = new Intent(this, ScPlayer.class);
				 startActivityForResult(intent, CloudUtils.RequestCodes.REUATHORIZE);
				 return;
			}
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		Track[] tl = new Track[list.size()];
			
			for (int i = 0; i < list.size(); i++){
				if (list.get(i) instanceof Track)
					tl[i] = (Track) list.get(i);
				else if (list.get(i) instanceof Event){
					tl[i] = (Track) ((Event) list.get(i)).getDataParcelable(Event.key_track);
				}
			}
			
			 Intent intent = new Intent(this, ScPlayer.class);
			 intent.putExtra("enqueuePosition", playPos);
			 intent.putExtra("enqueueList", tl);
			 startActivityForResult(intent, CloudUtils.RequestCodes.REUATHORIZE);
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
	
	
	
	
	public class MyPhoneStateListener extends PhoneStateListener {
        Context context;
        @Override
        public void onCallStateChanged(int state,String incomingNumber){
                 Log.e("PhoneCallStateNotified", "Incoming number "+incomingNumber);
                 if (state != TelephonyManager.CALL_STATE_IDLE){
                	 if (mService != null)
						try {
							mService.pause();
						} catch (Exception e) {
							e.printStackTrace();
						}
                 }
        }

	}
	
	
	protected void onServiceBound(){
		if (getSoundCloudApplication().getState() !=SoundCloudAPI.State.AUTHORIZED){
			forcePause();
		}
	}
	
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
		} catch (Exception e) {
			menuCurrentUploadingItem.setVisible(false);
		}
    		
    	return true;	
    }
	
    @Override
    protected void cancelCurrentUpload(){
		try {
			mUploadService.cancelUpload();
		} catch (RemoteException e) {
			setException(e);
			handleException();
		}
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

    	connectivityListener.startListening(this);
    	
		//start it so it is persistent outside this activity, then bind it
		startService(new Intent(this, CloudUploaderService.class));
		bindService(new Intent(this, CloudUploaderService.class), uploadOsc,0);

		if (false == CloudUtils.bindToService(this, osc)) {
			Log.i(TAG,"BIND TO SERVICE FAILED");
		}	
    }
	
	
	/**
	 * Unbind our services
	 */
	@Override
	protected void onStop() {
    	super.onStop();
    	
    	connectivityListener.stopListening();
    	
    	if (mUploadService != null){
    		this.unbindService(uploadOsc);
    		mUploadService = null;
    	}
    	
		CloudUtils.unbindFromService(this);
    
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
		
		if (getSoundCloudApplication().getState() == SoundCloudAPI.State.AUTHORIZED) {
			onAuthenticated();
		} else{
			if (mHolder != null) mHolder.setVisibility(View.GONE);
			forcePause();
			showDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_CONTACTING);
			mHandler.postDelayed(mLaunchIntent, 1000);
		}
		
		
    }
	
	@Override
	protected void onDestroy() {
    	super.onDestroy();
    }
	
	
	
	private void launchAuthorizationIntent(){
		mAuthException = null;
		Thread t = new Thread() {
			@Override
			public void run() {
				try {startActivity(getSoundCloudApplication().getAuthorizationIntent());finish(); } 
				catch (Exception e) {mAuthException = e;}
				mHandler.post(mHandleAuthIntent);
			}
		};
		t.start();
	}
	
	// Create runnable for posting so we can handle auth tries from a thread
	final Runnable mHandleAuthIntent = new Runnable() {
		public void run() {
			if (mAuthException != null){
				if (mAuthorizeRetries < mAuthorizePostDelays.length) {
                    mHandler.postDelayed(mLaunchIntent, mAuthorizePostDelays[mAuthorizeRetries]);
                    mAuthorizeRetries++;		                    
                } else {
                	removeDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_CONTACTING);
                	try{
                		showDialog(CloudUtils.Dialogs.DIALOG_ERROR_MAKING_CONNECTION);
                	} catch (BadTokenException e){ e.printStackTrace(); }
                }
			} else {
				removeDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_CONTACTING);
			}
			
		}
	};
	
	// Create runnable for posting since we want to launch the authorization at intervals
	final Runnable mLaunchIntent = new Runnable() {
		public void run() {
			launchAuthorizationIntent();
		}
	};
	

	
	private void onAuthenticated(){
		oAuthToken = "";
	
		if (mHolder != null) mHolder.setVisibility(View.VISIBLE);
		initLoadTasks();
		mLastCloudState = getSoundCloudApplication().getState();
	}
	
	

	private Handler connHandler = new Handler() {
	    public void handleMessage(Message msg) {
	            switch(msg.what) {
	                case CONNECTIVITY_MSG:
	                	  
	                	if (connectivityListener == null) return;
	                      NetworkInfo networkInfo = connectivityListener.getNetworkInfo();
	                     
	                      if (networkInfo == null) return;
	                      if (networkInfo.isConnected()){
	                    	  //clear image loading errors
	                    	  ImageLoader.get(LazyActivity.this).clearErrors();
	                      }
	                      break;
	                      }
	                }
	};
	
}
