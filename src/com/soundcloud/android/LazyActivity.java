package com.soundcloud.android;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;

public abstract class LazyActivity extends Activity implements OnItemClickListener, OnItemSelectedListener {
	private static final String TAG = "LazyActivity";


	
	protected LinearLayout mHolder;
	protected ArrayList<LazyList> mLists;
	
	
	protected Parcelable mDetailsData;
	private boolean mMultipage = true;
	protected String mCurrentTrackId;
	

	protected String mFilter = "";
	private int mPageSize;
	protected String mTrackOrder;
	protected String mUserOrder;
	
	protected DBAdapter db;
	
	protected SharedPreferences mPreferences;
	
	
	//protected Downloader downloader;
	protected CloudCommunicator mCloudComm;
	protected String oAuthToken = "";
	protected SoundCloudAPI.State mCloudState;
	
	protected ICloudPlaybackService mService = null;
	protected Exception mException = null;
	protected String mError = null;
	protected Comment addComment;
	
	private MenuItem menuCurrentPlayingItem;
	protected Parcelable menuParcelable;
	protected Parcelable dialogParcelable;
	protected String dialogUsername;
	
	protected LoadTask mLoadTask;
	
	
	
	
	protected LinearLayout mMainHolder;
	protected LinearLayout mRefreshBar;
	

	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;

	
	// Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

	
	protected void onCreate(Bundle savedInstanceState, int layoutResId) {
		super.onCreate(savedInstanceState);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		MyPhoneStateListener phoneListener=new MyPhoneStateListener();
		TelephonyManager telephonyManager
		=(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		telephonyManager.listen(phoneListener,
		PhoneStateListener.LISTEN_CALL_STATE);
		
		mCloudComm = CloudCommunicator.getInstance(this);
		mCloudState = mCloudComm.getState();

        // Gesture detection
           gestureDetector = new GestureDetector(new MyGestureDetector());
            gestureListener = new View.OnTouchListener() {
            	
                public boolean onTouch(View v, MotionEvent event) {
                	Log.i("TOUCH","On Touch");
                    if (gestureDetector.onTouchEvent(event)) {
                    	Log.i("TOUCH","return true");
                        return true;
                    }
                    Log.i("TOUCH","return false");
                    return false;
                }
            };
            
           
            
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(layoutResId);
	
		mLists = new ArrayList<LazyList>();
		

		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		setPageSize(Integer.parseInt(mPreferences.getString("defaultPageSize", "20")));
		mTrackOrder = mPreferences.getString("defaultTrackSorting", CloudCommunicator.ORDER_HOTNESS);
		mUserOrder = mPreferences.getString("defaultUserSorting", CloudCommunicator.ORDER_HOTNESS);
		
		build();
		
		
		restoreState();
		initLoadTasks(); 
	}
	
	public CloudCommunicator getCloudComm(){
		return mCloudComm;
	}
	
	public SharedPreferences getPreferences(){
		return mPreferences;
	}
	

	protected int getCurrentSectionIndex() {
		return 0;
	}

	protected void build(){
		
	}
	
	
	protected LazyList buildList(){
		
		

		
		LazyList mList = new LazyList(this);
		mList.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		mList.setOnItemClickListener(this);
		mList.setOnItemSelectedListener(this);
		mList.setFastScrollEnabled(true);
		mList.setTextFilterEnabled(true);
		mList.setVisibility(View.GONE);
		mList.setDivider(getResources().getDrawable(R.drawable.list_separator));
		mList.setDividerHeight(1);
		//mList.setSelector(R.drawable.list_selector_background_states);
		registerForContextMenu(mList);
		
		 
		 
		//AnimUtils.setLayoutAnim_slidedownfromtop(mList, this);
		
		mLists.add(mList);
		
		return mList;
	}

	
	@Override
	protected void onStart() {
    	super.onStart();
    	
    	IntentFilter f = new IntentFilter();
		f.addAction(CloudPlaybackService.META_CHANGED);
		f.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
		this.registerReceiver(mStatusListener, new IntentFilter(f));
		
		if (false == CloudUtils.bindToService(this, osc)) {
			// something went wrong
			//mHandler.sendEmptyMessage(QUIT);
		}
    }
	
	@Override
	protected void onStop() {
    	super.onStop();
    	
    	this.unregisterReceiver(mStatusListener);
    	
		CloudUtils.unbindFromService(this);
		mService = null;
    
    }


	
	
	@Override
    protected void onResume() {
    	super.onResume();
    	
    	if (mService != null)
			try {
				setPlayingTrack(mService.getTrackId());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

    }
	
	@Override
	protected void onDestroy() {
    	super.onDestroy();
    }
	
	protected void initLoadTasks(){
		
	}
	
	protected void setTaskActivity() {
		mLoadTask.setActivity(this);
	}
	
	protected boolean loadFromDB(){
		return false;
	}
	
	protected Object[] saveLoadTasks(){
		Object[] ret = {mLoadTask};
		return ret;
	}
	
	protected void restoreLoadTasks(Object[] taskObjects){
		mLoadTask = (LoadTask) taskObjects[0];
	}
	
	protected Parcelable saveParcelable(){
		return mDetailsData;
	}
	
	protected Object saveListTasks(){
		if (mLists == null || mLists.size() == 0)
			return null;
		
		AppendTask[] appendTasks = new AppendTask[mLists.size()];
		for (int i = 0; i < mLists.size(); i++){
			appendTasks[i] = ((LazyEndlessAdapter) mLists.get(i).getWrapper()).getTask();
		}
		return appendTasks;
	}
	
	
	protected Object saveListConfigs(){
		if (mLists == null || mLists.size() == 0)
			return null;
		
		int[][] configArrays = new int[mLists.size()][2];
		for (int i = 0; i < mLists.size(); i++){
			configArrays[i] = ((LazyEndlessAdapter) mLists.get(i).getWrapper()).savePagingData();
		}
		return configArrays;
	}
	
	protected Object saveListExtras(){
		if (mLists == null || mLists.size() == 0)
			return null;
		
		String[] extraArray = new String[mLists.size()];
		for (int i = 0; i < mLists.size(); i++){
			extraArray[i] = ((LazyEndlessAdapter) mLists.get(i).getWrapper()).saveExtraData();
		}
		return extraArray;
	}
	
	
	
	protected Object saveListAdapters(){
		if (mLists == null || mLists.size() == 0)
			return null;
		
		//store the data for current lists
		ArrayList<ArrayList<Parcelable>> mAdapterDatas = new ArrayList<ArrayList<Parcelable>>();
		Iterator<LazyList>  mListsIterator = mLists.iterator();
		int i = 0;
		while (mListsIterator.hasNext()){
			ListView mList = mListsIterator.next();
			mAdapterDatas.add((ArrayList<Parcelable>) ((LazyBaseAdapter) mList.getAdapter()).getData());
		}
		
		return mAdapterDatas;
	}
	
	protected void activateLists(){
		if (mLists == null || mLists.size() == 0)
			return;
		
		Iterator<LazyList>  mListsIterator = mLists.iterator();
		int i = 0;
		while (mListsIterator.hasNext()){
			Log.i("START","Activating List");
			LazyList mList = mListsIterator.next();
			((LazyEndlessAdapter) mList.getWrapper()).allowLoading();
		}
		
	}
	
	protected void showLists(){
		if (mLists == null || mLists.size() == 0)
			return;
		
		Iterator<LazyList>  mListsIterator = mLists.iterator();
		int i = 0;
		while (mListsIterator.hasNext()){
			Log.i("START","Showing List");
			mListsIterator.next().setVisibility(View.VISIBLE);
		}
		
	}
	
	protected void restoreParcelable(Parcelable p){
		mDetailsData = p;
	}
	
	protected void restoreListTasks(Object taskObject){
		AppendTask[] appendTasks = (AppendTask[]) taskObject;
		
		if (appendTasks == null)
			return;
		
		int i = 0;
		for (AppendTask task : appendTasks){
			((LazyEndlessAdapter) mLists.get(i).getWrapper()).restoreTask(task);
			i++;
		}
	}

	protected void restoreListConfigs(Object configObject){
		int[][] configArrays = (int[][]) configObject;
		
		if (configArrays == null)
			return;
		
		int i = 0;
		for (int[] config : configArrays){
			((LazyEndlessAdapter) mLists.get(i).getWrapper()).restorePagingData(config);
			i++;
		}
	}


	protected void restoreListExtras(Object extraObject){
		String[] extraArrays = (String[]) extraObject;
		
		if (extraArrays == null)
			return;
		
		int i = 0;
		for (String extra : extraArrays){
			((LazyEndlessAdapter) mLists.get(i).getWrapper()).restoreExtraData(extra);
			i++;
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void restoreListAdapters(Object adapterObject){
		if (adapterObject == null)
			return;
		
		ArrayList<ArrayList<Parcelable>> mAdapterDatas = (ArrayList<ArrayList<Parcelable>>) adapterObject; 
		Iterator<ArrayList<Parcelable>>  mAdapterDataIterator = mAdapterDatas.iterator();
		int i = 0;
		while (mAdapterDataIterator.hasNext()){
			ArrayList<Parcelable> mAdapterData = mAdapterDataIterator.next();
			((LazyBaseAdapter) mLists.get(i).getAdapter()).getData().addAll(mAdapterData);
			((LazyBaseAdapter) mLists.get(i).getAdapter()).notifyDataSetChanged();
			i++;
		}
	}
	
	
	
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new Object[] {
				super.onRetainNonConfigurationInstance(),
				saveLoadTasks(),
				saveParcelable(),
				saveListTasks(),
				saveListConfigs(),
				saveListExtras(),
				saveListAdapters()
		};
	}
	
	
	protected void restoreState() {
		
		
		//restore state
		Object[] saved = (Object[]) getLastNonConfigurationInstance();
		
		Log.i("START","Restore State " + saved);
		
		if (saved != null) {
			restoreLoadTasks((Object[]) saved[1]);
			restoreParcelable((Parcelable) saved[2]);
			restoreListTasks(saved[3]);
			restoreListConfigs(saved[4]);
			restoreListExtras(saved[5]);
			restoreListAdapters(saved[6]);
		} else 
			activateLists();
		
		showLists();
	}
	


	protected String format(int resId, Object... args) {
		return String.format(getString(resId, args));
	}

	protected String getLoadDialogText(){
		return getString(R.string.loading);
	}

	
	

	

	final protected void setMultipage(boolean isMultipage) {
		mMultipage = isMultipage;
	}

	final protected boolean isMultipage() {
		return mMultipage;
	}
	


	

	protected void startActivityForPosition(Class<?> targetCls, HashMap<String, String> info) {
		Intent i = new Intent(this, targetCls);
		for (String key : info.keySet()) {
			i.putExtra(key, info.get(key));
		}
		startActivity(i);
	}
	
	
	
	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(CloudPlaybackService.META_CHANGED)) {
				try {
					setPlayingTrack(mService.getTrackId());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
				setPlayingTrack("");
			}
		}
	};
	
	
	
	private void setPlayingTrack(String trackId){
		if (mLists == null || mLists.size() == 0)
			return;
		
		mCurrentTrackId = trackId;
		
		Iterator<LazyList>  mListsIterator = mLists.iterator();
		int i = 0;
		while (mListsIterator.hasNext()){
			ListView mList = mListsIterator.next();
			((TracklistAdapter) mList.getAdapter()).setPlayingId(mCurrentTrackId);
		}
	}
	
	
	public String getCurrentTrackId(){
		return mCurrentTrackId;
	}



	public void onItemClick(AdapterView<?> list, View row, int position, long id) {
		
	}
	
	public void onItemSelected(AdapterView<?> list, View row, int position, long id) {
		//((LazyAdapter) list.getAdapter()).setSelected(position);
	}
	

	public void onNothingSelected(AdapterView<?> list) {
		//((LazyAdapter) list.getAdapter()).setSelected(-1);
	}

	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
	}
	
	
	public void setDialogUsername(String username){
		dialogUsername = username;
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
			/*case CloudUtils.Dialogs.DIALOG_CONFIRM_REMOVE_FOLLOWING:
	            return new AlertDialog.Builder(this)
	                .setTitle(R.string.confirm_remove_following_title)
	                .setMessage(R.string.confirm_remove_following_message)
	                .setPositiveButton(getString(R.string.btn_yes), new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	toggleFollowing(true);
	                    }
	                })
	                .setNegativeButton(getString(R.string.btn_no), new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	removeDialog(CloudUtils.Dialogs.DIALOG_CONFIRM_REMOVE_FOLLOWING);
	                    }
	                })
	                .create();
				*/
		
				
		}  
		return super.onCreateDialog(which);
	}
	
	
	
	
	
	public boolean onContextItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
			
				
			case CloudUtils.ContextMenu.VIEW_UPLOADER:
				if (menuParcelable instanceof Comment)
					CloudUtils.gotoTrackUploader(this, ((Comment) menuParcelable).getData(Comment.key_user_permalink));
				else if (menuParcelable instanceof Track)
					CloudUtils.gotoTrackUploader(this, ((Track) menuParcelable).getData(Track.key_user_permalink));
				break;
				
			case CloudUtils.ContextMenu.VIEW_TRACK:
				CloudUtils.gotoTrackDetails(this, (Track) menuParcelable);
				break;
				
			default:
				return super.onContextItemSelected(item);
		}
		return true;
	}
	
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menuCurrentPlayingItem = menu.add(menu.size(), CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK, menu.size(), R.string.menu_view_current_track).setIcon(R.drawable.ic_menu_info_details);
		menu.add(menu.size(), CloudUtils.OptionsMenu.SETTINGS, menu.size(), R.string.menu_settings).setIcon(R.drawable.ic_menu_preferences);
		return super.onCreateOptionsMenu(menu);
	}
	
    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
    	
    	if (!CloudUtils.stringNullEmptyCheck(getCurrentTrackId()) && !this.getClass().getName().contentEquals("com.soundcloud.android.ScPlayer")){
    		menuCurrentPlayingItem.setVisible(true);
    	} else {
    		menuCurrentPlayingItem.setVisible(false);
    	}
    		
    	return true;	
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case CloudUtils.OptionsMenu.SETTINGS:
				startActivity(new Intent(this, Settings.class));
				return true;
			case CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK:
				startActivity(new Intent(this, ScPlayer.class));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	public String getCurrentUserId() {
		return CloudUtils.getCurrentUserId(this);
	}
	
	protected void showToast(String text) {
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
	
	public void setException(Exception e) {
		Log.i("WARNING", "WE GOT AN EXCEPTION " + e.toString());
		mException = e;
	}
	
	public void setError(String e) {
		Log.i("WARNING", "WE GOT AN ERROR " + e.toString());
		mError = e;
	}


	protected void handleException() {
		if (mException != null)
			Log.i("HANDLE","############handle exception " + mException.toString());
		
		if (mException != null) {
			if (mException instanceof UnknownHostException || mException instanceof SocketException) {
				showDialog(CloudUtils.Dialogs.DIALOG_ERROR_LOADING);
			} 
		}
		mException = null;
	}
	
	protected void handleError() {
		if (mError != null) {
			if (mError.equalsIgnoreCase(CloudCommunicator.ERROR_UNAUTHORIZED)) {
				showDialog(CloudUtils.Dialogs.DIALOG_UNAUTHORIZED);
			} 
		}
		mError = null;
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
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                 }
        }

	}
	
	
	protected void onServiceBound(){
		Log.i("ScPlayer","On Service Bound Lazyyyy");
		
		try {
			setPlayingTrack(mService.getTrackId());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void onServiceUnbound(){
		
	}
	
	
	
	
	private ServiceConnection osc = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName classname, IBinder obj) {
			Log.i("ScPlayer", "ON SERVICE BOUND osc");
			mService = ICloudPlaybackService.Stub.asInterface(obj);
			onServiceBound();
			
			
		}

		@Override
		public void onServiceDisconnected(ComponentName classname) {

			onServiceUnbound();
			mService = null;
		}

	};

	
	

	
	
	public ICloudPlaybackService getPlaybackService(){
		return mService;
	}
	
	public void mapDetails(Parcelable p){
		
	}
	
	public void resolveParcelable(Parcelable p){
		
	}

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
        	Log.i(TAG,"SWIPE");
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
	 
	
	

	
}
