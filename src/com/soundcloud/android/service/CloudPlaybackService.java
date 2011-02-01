/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.android.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.BaseObj.WriteState;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.net.NetworkConnectivityListener;
import com.soundcloud.utils.play.MediaFrameworkChecker;
import com.soundcloud.utils.play.PlayListManager;

/**
 * 
 * Provides "background" audio playback capabilities, allowing the user to
 * switch between activities without stopping playback.
 * 
 * ** Derived from the androi 2.1 default media player
 * 
 */
public class CloudPlaybackService extends Service {

	private static final String TAG = "CloudPlaybackService";
	
	public static final int NOW = 1;
	public static final int NEXT = 2;
	public static final int LAST = 3;
	public static final int PLAYBACKSERVICE_STATUS = 1;

	public static final int SHUFFLE_NONE = 0;
	public static final int SHUFFLE_NORMAL = 1;
	public static final int SHUFFLE_AUTO = 2;

	public static final int REPEAT_NONE = 0;
	public static final int REPEAT_CURRENT = 1;
	public static final int REPEAT_ALL = 2;
	
	public static final int BUFFER_CHECK = 0;
	public static final int BUFFER_FILL_CHECK = 1;
	public static final int START_NEXT_TRACK = 2;
	public static final int RESTART_TRACK = 3;
	public static final int UPDATE_TRACK = 4;

	public static final int MAX_RECONNECT_COUNT = 1;

	public static final String PLAYSTATE_CHANGED = "com.soundcloud.android.playstatechanged";
	public static final String META_CHANGED = "com.soundcloud.android.metachanged";
	public static final String QUEUE_CHANGED = "com.soundcloud.android.queuechanged";
	public static final String PLAYBACK_COMPLETE = "com.soundcloud.android.playbackcomplete";
	public static final String TRACK_ERROR = "com.soundcloud.android.trackerror";
	public static final String STREAM_DIED = "com.soundcloud.android.streamdied";
	public static final String COMMENTS_LOADED = "com.soundcloud.android.commentsloaded";
	public static final String FAVORITES_SET = "com.soundcloud.android.favoriteset";
	public static final String SEEK_START = "com.soundcloud.android.seekstart";
	public static final String SEEK_COMPLETE = "com.soundcloud.android.seekcomplete";

	public static final String INITIAL_BUFFERING = "com.soundcloud.android.initialbuffering";
	public static final String BUFFERING = "com.soundcloud.android.buffering";
	public static final String BUFFERING_COMPLETE = "com.soundcloud.android.bufferingcomplete";

	public static final String SERVICECMD = "com.soundcloud.android.musicservicecommand";
	public static final String CMDNAME = "command";
	public static final String CMDTOGGLEPAUSE = "togglepause";
	public static final String CMDSTOP = "stop";
	public static final String CMDPAUSE = "pause";
	public static final String CMDPREVIOUS = "previous";
	public static final String CMDNEXT = "next";

	public static final String TOGGLEPAUSE_ACTION = "com.soundcloud.android.musicservicecommand.togglepause";
	public static final String PAUSE_ACTION = "com.soundcloud.android.musicservicecommand.pause";
	public static final String PREVIOUS_ACTION = "com.soundcloud.android.musicservicecommand.previous";
	public static final String NEXT_ACTION = "com.soundcloud.android.musicservicecommand.next";

	private static final int TRACK_ENDED = 1;
	private static final int RELEASE_WAKELOCK = 2;
	private static final int SERVER_DIED = 3;
	private static final int FADEIN = 4;
	private static final int TRACK_EXCEPTION = 5;
	private LoadCommentsTask mLoadCommentsTask;
	private MultiPlayer mPlayer;
	
	private int mLoadPercent = 0;
	private boolean mAutoPause = false;
	
	protected NetworkConnectivityListener connectivityListener;
	protected static final int CONNECTIVITY_MSG = 9;

	private PlayListManager mPlayListManager = new PlayListManager(this);
	private Track mPlayingData;
	
	private DownloadThread mDownloadThread;
	private Boolean mMediaplayerError = false;

	private RemoteViews mNotificationView;

	private long mResumeTime = -1;
	private Long mResumeId = null;
	
	private WakeLock mWakeLock;
	private int mServiceStartId = -1;
	private boolean mServiceInUse = false;
	private boolean mResumeAfterCall = false;
	private boolean mIsSupposedToBePlaying = false;
	// interval after which we stop the service when idle
	private static final int IDLE_DELAY = 60000;
	
	protected static final int MAX_CACHE_SIZE = 200000000;
	
	protected static final int WIFI_HIGH_WATER_MARK = 100000000;
	protected static final int HIGH_WATER_MARK = 8000000;
	protected static final int PLAYBACK_MARK = 60000;
	protected static final int LOW_WATER_MARK = 20000;
	
	private boolean pausedForBuffering;
	private boolean initialBuffering = true;
	
	private long mCurrentBuffer;
	
	private NetworkInfo mCurrentNetworkInfo;
	private int mCurrentPlugState;
	private boolean isStagefright = false;

	
	public CloudPlaybackService() {
		
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.i(TAG,"Service on create");

		// Needs to be done in this thread, since otherwise
		// ApplicationContext.getPowerManager() crashes.
		mPlayer = new MultiPlayer();
		mPlayer.setHandler(mMediaplayerHandler);

		//setup connectivity listening
		connectivityListener = new NetworkConnectivityListener();
		connectivityListener.registerHandler(connHandler,CONNECTIVITY_MSG);
		connectivityListener.startListening(this);
		

		//setup call listening
		TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this
				.getClass().getName());
		mWakeLock.setReferenceCounted(false);
	
        if (Build.VERSION.SDK_INT < 8) //2.1 or earlier, opencore only, no stream seeking
            isStagefright = false;
        else if (Build.VERSION.SDK_INT == 8){ // 2.2, check to see if stagefright enabled
        	determineSdk8Framework();
        } else { //greater than 2.2, assume stagefright from here on out
            isStagefright = true;
        }
	        
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
		    
		// If the service was idle, but got killed before it stopped itself, the
		// system will relaunch it. Make sure it gets stopped again in that
		// case.
		Message msg = mDelayedStopHandler.obtainMessage();
		mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
		
		mAutoPause = true;
		mPlayListManager.reloadQueue();
		mPlayingData = mPlayListManager.getCurrentTrack();
		//openCurrent(true);
		
		Log.i(TAG,"Service on create done");
		
	}

	/**
	 * SDK 8 can be either open core or stagefright. This determines it as best we can
	 */
	private void determineSdk8Framework(){
		
		isStagefright = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("isStagefright", false);
		
		//check the build file, works in most cases and will catch cases for instant playback
		try {
	            InputStream instream = openFileInput("/system/build.prop");
	            if (instream != null) {
	              BufferedReader buffreader = new BufferedReader(new InputStreamReader(instream));
	              String line;
	              while (( line = buffreader.readLine()) != null) {
	            	  Log.i(TAG,"Checking for stagefright in " + line);
	                if (line.contains("media.stagefright.enable-player")){
	                	if (line.contains("true"))
	                		isStagefright = true;
	                	break;
	                }
	              }
	            }
	            instream.close();
	     
	    }catch (Exception e) {} 
	    
	    //check throgh a socket, only way to be sure, but takes a little time
		final MediaFrameworkChecker mfc = new MediaFrameworkChecker();
		mfc.start();
		 // Fire off a thread to do some work that we shouldn't do directly in the UI thread
	    Thread t = new Thread() {
	        @Override
			public void run() {
	        	try {
	    			MediaPlayer mp = new MediaPlayer();
	    		    mp.setDataSource(String.format("http://127.0.0.1:%d/", mfc.getSocketPort()));
	    		    mp.prepare();
	    		    mp.start();
	    		    while (mfc.isAlive()){
	    				Thread.sleep(100);
	    		    }
	    		} catch (IOException e) {
	    		} catch (InterruptedException e) {
	    		}
	    		 Log.i(TAG,"mfc done, is stagefright? " + mfc.isStagefright());
	    		    isStagefright = mfc.isStagefright();
	        }
	    };
	    t.start();	
	}
	


	@Override
	public void onDestroy() {
		stopStreaming(null);
		gotoIdleState();
		
		// release all MediaPlayer resources, including the native player and
		// wakelocks
		mPlayer.release();
		mPlayer = null;
		
		unregisterReceiver(batteryLevelReceiver);

		//TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		//tmgr.listen(mPhoneStateListener, 0);

		// make sure there aren't any other messages coming
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		mMediaplayerHandler.removeCallbacksAndMessages(null);
		mBufferHandler.removeCallbacksAndMessages(null);
		
		mMediaplayerHandler = null;
		mDelayedStopHandler = null;
		mPhoneStateListener = null;
		mDownloadThread = null;
		connHandler = null;
		
		
		connectivityListener.stopListening();
		connectivityListener.unregisterHandler(connHandler);
		connectivityListener = null;
		
		
		if (mWakeLock.isHeld())
			mWakeLock.release();
		mWakeLock = null;
		
		super.onDestroy();
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.i("BINDER","On Bind " + mBinder);
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		mServiceInUse = true;
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		Log.i("BINDER","On ReBind ");
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		mServiceInUse = true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mServiceStartId = startId;
		mDelayedStopHandler.removeCallbacksAndMessages(null);

		if (intent != null) {
			String action = intent.getAction();
			String cmd = intent.getStringExtra("command");

			if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
				next(true);
			} else if (CMDPREVIOUS.equals(cmd)
					|| PREVIOUS_ACTION.equals(action)) {
				if (position() < 2000) {
					prev();
				} else {
					seek(0);
					play();
				}
			} else if (CMDTOGGLEPAUSE.equals(cmd)
					|| TOGGLEPAUSE_ACTION.equals(action)) {
				if (isPlaying()) {
					pause();
				} else {
					play();
				}
			} else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
				pause();
			} else if (CMDSTOP.equals(cmd)) {
				pause();
				seek(0);
			}
		}

		// make sure the service will shut down on its own if it was
		// just started but not bound to and nothing is playing
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		Message msg = mDelayedStopHandler.obtainMessage();
		mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
		return START_STICKY;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mServiceInUse = false;

		mPlayListManager.saveQueue(true);

		if (isPlaying() || mResumeAfterCall) {
			// something is currently playing, or will be playing once
			// an in-progress call ends, so don't stop the service now.
			return true;
		}

		// If there is a playlist but playback is paused, then wait a while
		// before stopping the service, so that pause/resume isn't slow.
		// Also delay stopping the service if we're transitioning between
		// tracks.
		if (mPlayListManager.getCurrentLength() > 0 || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
			Message msg = mDelayedStopHandler.obtainMessage();
			mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
			return true;
		}
		
		// No active playlist, OK to stop the service right now
		stopSelf(mServiceStartId);
		return true;
	}

	private Handler mDelayedStopHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// Check again to make sure nothing is playing right now
			if (isPlaying() || mResumeAfterCall || mServiceInUse
					|| mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
				return;
			}
			mPlayListManager.saveQueue(true);
			stopSelf(mServiceStartId);
		}
	};

	/**
	 * Called when we receive a ACTION_MEDIA_EJECT notification.
	 * 
	 * @param storagePath
	 *            path to mount point for the removed media
	 */
	public void closeExternalStorageFiles(String storagePath) {
		// stop playback and clean up if the SD card is going to be unmounted.
		stop(true);
		notifyChange(QUEUE_CHANGED);
	}
	
	private boolean checkNetworkStatus(){
		if (connectivityListener == null)
			return false;
		
		mCurrentNetworkInfo = connectivityListener.getNetworkInfo();
		Log.i(TAG,"NETWORK INFO " + mCurrentNetworkInfo);
		
        if (mCurrentNetworkInfo != null) 
        	return mCurrentNetworkInfo.isConnected();
        else
        	return false;
	}


	/**
	 * Notify the change-receivers that something has changed. The intent that
	 * is sent contains the following data for the currently playing track: "id"
	 * - Integer: the database row ID "artist" - String: the name of the artist
	 * "album" - String: the name of the album "track" - String: the name of the
	 * track The intent has an action that is one of
	 * "com.soundcloud.android.metachanged" "com.soundcloud.android.queuechanged",
	 * "com.soundcloud.android.playbackcomplete"
	 * "com.soundcloud.android.playstatechanged" respectively indicating that a
	 * new track has started playing, that the playback queue has changed, that
	 * playback has stopped because the last file in the list has been played,
	 * or that the play-state changed (paused/resumed).
	 */
	private void notifyChange(String what) {

		Intent i = new Intent(what);
		i.putExtra("id", getTrackId());
		i.putExtra("track", getTrackName());
		i.putExtra("user", getUserName());
		i.putExtra("isPlaying", isPlaying());
		sendBroadcast(i);
		
		 if (what.equals(QUEUE_CHANGED)) {
	            mPlayListManager.saveQueue(true);
	        } else {
	        	mPlayListManager.saveQueue(false);
	        }
	}

	

	public void playFromAppCache(int playPos) {
		synchronized(this){
			Log.i(TAG,"Play from cache");
			mPlayListManager.loadCachedPlaylist(((SoundCloudApplication) this.getApplication()).flushCachePlaylist(),playPos);
			stopStreaming(null);
			play();
			Log.i(TAG,"Play from cache");
			openCurrent();
		}
	}


	



	private void openCurrent() {
		Log.i(TAG,"open current " + mPlayListManager.getCurrentLength());
		if (mPlayListManager.getCurrentLength() == 0) {
				return;
			}
			openAsync(mPlayListManager.getCurrentTrack());
	}

	
	Thread mStopThread = null;
	public void openAsync(Track track) {
		Log.i(TAG,"Open Async " + track);
			if (track == null) {
				return;
			}
			
			if (mAutoPause){
				mAutoPause = false;	
				Log.i(TAG,"Calling auto meta change");
				notifyChange(META_CHANGED);
				setPlayingStatus();
			}
			
			mLoadPercent = 0;
			mCurrentBuffer = 0;
			
			//if we are already playing this track
			if (mPlayingData != null && mPlayingData.getId().compareTo(track.getId())==0){
				
				mStopThread = new StreamStopper(this, mPlayingData.getId());
				mStopThread.setPriority(Thread.MAX_PRIORITY);
				mStopThread.start();
				return;
				
			} 
			
			//stop in a thread so the resetting (or releasing if we are asyncopening) doesn't holdup the UI
				mStopThread = new StreamStopper(this, track.getId());
				mStopThread.setPriority(Thread.MAX_PRIORITY);
				mStopThread.start();
			
			//new play data
			mPlayingData = track;
			
			setPlayingStatus();
			
			//tell the db we played it
			track.setUserPlayed(true);
			CloudUtils.resolveTrack((SoundCloudApplication) this.getApplication(), track, WriteState.all,CloudUtils.getCurrentUserId(this));
			
			//meta has changed
			notifyChange(META_CHANGED);
	}
	
	/**
	 * Stops the stream and media player
	 */
	private void stopStreaming(Long continueId){
		synchronized (this){
			//stop checking buffer
			pausedForBuffering = false;
			
			mBufferHandler.removeCallbacksAndMessages(BUFFER_CHECK);
			mBufferHandler.removeCallbacksAndMessages(BUFFER_FILL_CHECK);
			mBufferHandler.removeCallbacksAndMessages(START_NEXT_TRACK);
			
			//stop playing
			if (mPlayer.isInitialized() || mPlayer.isAsyncOpening() )
				mPlayer.stop();
		
			
			if (CloudUtils.checkThreadAlive(mDownloadThread) && (continueId == null || mDownloadThread.getTrackId() != continueId)){
				mDownloadThread.interrupt();
				mDownloadThread.kill();
				
			}
		}
		
	}
	
	public void fileLengthUpdated(Track t, Boolean changed){
		Log.i(TAG,"File Length updated " + t.getId() + " to " + t.getFilelength());
		
		if (t.getId().compareTo(mPlayingData.getId()) == 0){
			if (changed){
				Log.i(TAG,"FILE LENGTH CHANGED");
				
				//stop the track if its playing
				stopStreaming(t.getId());
				
				// get rid of the existing cache file
				if (t.getCacheFile() != null && t.getCacheFile().exists())
					t.getCacheFile().delete();
				
				commitTrackToDb(t); //save info
				
				//reopen current track with new data
				openCurrent();
			} else {
				//start checking the buffer
				assertBufferCheck();
				commitTrackToDb(t); // save info
			}
		}
	}
	
	public void commitTrackToDb(Track t){
		Log.i(TAG,"Committing " + t.getId() + " to db with file length " + t.getFilelength());
		CloudUtils.resolveTrack((SoundCloudApplication) this.getApplication(), t, WriteState.all,CloudUtils.getCurrentUserId(this));
	}
	
	private void startNextTrack(){
		synchronized (this){
			mStopThread = null;

			Log.i(TAG,"Start next track " + CloudUtils.isTrackPlayable(mPlayingData));
			if (CloudUtils.isTrackPlayable(mPlayingData)){
				
				configureTrackData(mPlayingData);
				
				pausedForBuffering = true;
				initialBuffering = true;
				notifyChange(INITIAL_BUFFERING);

				if (isStagefright){
					if (checkNetworkStatus())
						prepareDownload(mPlayingData);
					else
						commitTrackToDb(mPlayingData);
					
					//start the buffer check, but not instantly (false)
					assertBufferCheck(false);
					
				} else {
					//commit updated track (user played update only)
					commitTrackToDb(mPlayingData);
					mPlayer.setDataSourceAsync(mPlayingData.getSignedUri());
				}
				return;
			}
			
			gotoIdleState();
		}
	}
	
	private void configureTrackData(Track t){
		if (t.getSignedUri() == null)
			t.setSignedUri(((SoundCloudApplication) getApplication()).signStreamUrlNaked(t.getStreamUrl()));
		
		if (t.getCacheFile() == null){
			t.setCacheFile(new File(CloudCache.EXTERNAL_TRACK_CACHE_DIRECTORY + CloudUtils.md5(Long.toString(t.getId()))));
		}
		
	}
	
	private void prepareDownload(Track trackToCache){
		synchronized (this){
			configureTrackData(trackToCache);
		
			if (mDownloadThread != null && mDownloadThread.isAlive() && trackToCache.getId().compareTo(mDownloadThread.getTrackId()) == 0){
				// we are already downloading this
				Log.i(TAG,"Alread downloading this track. Just wait for the buffer to play it.");
				return;
			}
			
			//start downloading if there is a valid connection, otherwise it will happen when we regain connectivity
			if (checkNetworkStatus()){ 
				try {
					
					Log.i(TAG,"Trimming cache and downloading new file");
					trimCache(trackToCache.getCacheFile());
					trackToCache.getCacheFile().setLastModified(System.currentTimeMillis());
					(mDownloadThread = new DownloadThread(this, trackToCache)).start();
					
				} catch (IOException e) {
					e.printStackTrace();
					Log.i(TAG,"Unable to start download, no connection");
					this.sendDownloadException();
				}
				
			}
		}
	}
	
	
	/**
	 * Manage the buffering status. 
	 * @return
	 */
	private boolean checkBuffer(){
			synchronized (this){
				
				if (mPlayingData == null || getDuration() == 0) return false;
				if (mPlayingData.getFilelength() == null || mPlayingData.getCacheFile() == null){
					if (CloudUtils.checkThreadAlive(mDownloadThread))
						return true;
					else{
						Log.i(TAG,"No Thread, No Cache, send exception");
						return false;
					}
				}
					
				if (mPlayer != null && mPlayer.isInitialized()){
					// normal buffer measurement. size of cache file vs playback position
					mCurrentBuffer = mPlayingData.getCacheFile().length() - mPlayingData.getFilelength()*mPlayer.position()/getDuration();
					
				} else if (mResumeId != null && mResumeId== mPlayingData.getId() && mResumeTime > -1){
					// resume buffer measurement. if stream died due to lack of a buffer, measure the buffer from where we are supposed to resume
					mCurrentBuffer = mPlayingData.getCacheFile().length() - mPlayingData.getFilelength()*mResumeTime/getDuration();
				} else // initial buffer measurement
					mCurrentBuffer = mPlayingData.getCacheFile().length(); 
						
				Log.i(TAG,"Buffer: " + mCurrentBuffer + " | " + mPlayingData.getCacheFile().length() + " | " + pausedForBuffering + " | " + initialBuffering);
				
				if (pausedForBuffering){
					
					// first round of buffering, special case where we set the playback file
					if (mCurrentBuffer > PLAYBACK_MARK){
						
							pausedForBuffering = false;
							
							if (!initialBuffering){
								//normal buffering done
								notifyChange(BUFFERING_COMPLETE);
								mPlayer.start();
								
							} else {
								//initial buffering done
								initialBuffering = false;
								
								//set the media player data source
								if (!mPlayer.getPlayingPath().equalsIgnoreCase(mPlayingData.getCacheFile().getAbsolutePath()))
									mPlayer.setDataSourceAsync(mPlayingData.getCacheFile().getAbsolutePath());
							} 
							
					} else if (!checkNetworkStatus()){
						sendDownloadException();
						return false;
					}
					
				} else {
					// if we are under the buffer, but the track is not fully cached
					if (mPlayingData.getCacheFile().length() < mPlayingData.getFilelength() && mCurrentBuffer < PLAYBACK_MARK){
						
						// make sure we are trying to download
						checkBufferStatus();

						// normal time to buffer 
						if (mCurrentBuffer < LOW_WATER_MARK){
							
							if (mPlayer.isInitialized()){
								mPlayer.pause();
							}
							
							Log.i(TAG,"PAUSED FOR BUFFERING");
						
							notifyChange(BUFFERING);
							pausedForBuffering = true;	
							
						}
					}
				}
			}
			
		return true;
	}
	
	//helper for params
	private void assertBufferCheck(){
		assertBufferCheck(true);
	}
	
	/**
	 * queue a buffer check if we aren't paused and call one right away depending on the param
	 * @param instant
	 */
	private void assertBufferCheck(boolean instant){
		if (!mAutoPause){
			if (instant) 
				if (!checkBuffer()) 
					return;
			queueNextRefresh(500);		
		}
		
	}
	
	private void trimCache(File keepFile) throws IOException{
		long size = 0;
		long maxSize = 200000000;
		
		StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        Long spaceLeft = Long.parseLong(Integer.toString(fs.getBlockSize()))*(fs.getAvailableBlocks() - fs.getBlockCount()/10);
        
		File cacheDir = new File(CloudCache.EXTERNAL_TRACK_CACHE_DIRECTORY);
		if (cacheDir.exists()){
		File[] fileList = cacheDir.listFiles();
	        if (fileList != null){
	        	ArrayList<File> orderedFiles = new ArrayList<File>();
		        for(int i = 0; i < fileList.length; i++) {
		            if(!fileList[i].isDirectory() && (keepFile == null || fileList[i]!= keepFile))
		            		size += fileList[i].length();
		            		if (orderedFiles.size() == 0)
		            			orderedFiles.add(fileList[i]);
		            		else {
		            			int j = 0;
			            		while (j < orderedFiles.size() && ((File)orderedFiles.get(j)).lastModified() < fileList[i].lastModified()){
			            			j++;
			            		}
			            		orderedFiles.add(j,fileList[i]);
		            		}
		            } 
		        
		        Log.i(TAG,"Cache Size " + size + " " + maxSize + " " + spaceLeft);
		        
		        if (size > maxSize || spaceLeft < 0){
		        	Long  toTrim =  size - maxSize > 0-spaceLeft ?  size - maxSize : 0-spaceLeft;
		        	int j = 0;
		        	long trimmed = 0;
		        	while (j < orderedFiles.size() && trimmed < toTrim){
		        		if (orderedFiles.get(j) != keepFile)
		        			((File) orderedFiles.get(j)).delete();
		        	}
		        }
	        }
		} else {
			cacheDir.mkdirs();
		}
	}
	
	public void sendDownloadException(){
		Log.i(TAG,"TRACK_EXCEPTION: Go idle, set error and broadcast exception");
		gotoIdleState();
		mMediaplayerHandler.sendMessage(mMediaplayerHandler.obtainMessage(TRACK_EXCEPTION));
		
	}
	

	
	/*
	 * Make sure the buffer is filling if it needs to. If it doesn't see if the next track can be buffered
	 */
	private void checkBufferStatus(){
		synchronized(this){
			Log.i(TAG,"CHECK BUFFER STATUS " + CloudUtils.checkThreadAlive(mDownloadThread));
			
				// are we able to cache something
			if (!mAutoPause && mPlayingData != null && !CloudUtils.checkThreadAlive(mDownloadThread) && checkNetworkStatus() ){
				Log.i(TAG,"NO THREAD AVAIL " + checkIfTrackCached(mPlayingData) + " " + keepCaching());
				
				//we are able to buffer something, see if we need to download the current track
				if (!checkIfTrackCached(mPlayingData) && keepCaching()){
					
					Log.i(TAG,"Premature download death, open another stream and continue");
					//need to cache the current track
					prepareDownload(mPlayingData);
					
				} 
			}
		}
	}
	
	/**
	 * See if a track has been fully cached
	 * @param track
	 * @return
	 */
	private Boolean checkIfTrackCached(Track track){
		return (track != null 
			&&  track.getCacheFile() != null && track.getFilelength() != null
			&& track.getCacheFile().length() >= track.getFilelength());
	}
	
	/**
	 * Check to see if we have any errors, have hit a high water mark, or are paused
	 * and not supposed to be caching
	 * @return
	 */
	public Boolean keepCaching(){
		// we aren't playing and are not supposed to be caching during pause
		if (!mIsSupposedToBePlaying && !mCacheOnPause)
			return false;
		
		if (mCurrentNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI ){
			return true;
			
			//we are on wifi, figure out our connection state
			/*
			Double powerMult = 1.0;
			switch (mCurrentPlugState) {
				case 0 : // none
					powerMult = 0.3;
					break;
				case 1 : // a.c. adapter
					powerMult = 1.0;
					break;
				case 2 : // usb power
					powerMult = 0.6;
					break;
			}
			
			return mCurrentBuffer < WIFI_HIGH_WATER_MARK*powerMult;
			*/
			
		} 
		return mCurrentBuffer < HIGH_WATER_MARK;
	}
	
	/**
	 * Starts playback of a previously opened file.
	 */
	public void play() {
		Log.i(TAG,"PLAY " + mPlayingData);
			if (mPlayingData == null)
				return;
			
			Log.i(TAG,"PLAY " + mPlayer.isInitialized() + " " + mMediaplayerError);
			if (mPlayer.isInitialized() && (!isStagefright || mPlayingData.getFilelength() != null)){
				
				if (!isStagefright || mPlayingData.getCacheFile().length() > PLAYBACK_MARK){
					
					if (isStagefright)
					if (mCurrentBuffer < PLAYBACK_MARK){
						// we are not allowed to play from wherever we are
						mPlayer.seek(0);
					} else if (mResumeId != null && mPlayingData != null && mPlayingData.getId().compareTo(mResumeId) == 0 && mResumeTime > -1) {
						// we are supposed to resume somehwere in the middle
						mPlayer.seek(mResumeTime, true);
						mResumeTime = -1;
						mResumeId = null;
					}
					
					mPlayer.start();
					
					if (isStagefright){
						assertBufferCheck();
						//make sure we are downloading if we should be
						checkBufferStatus();	
					}
					
				} else {
					//we should be buffering
					pausedForBuffering = true;
					notifyChange(BUFFERING);
					assertBufferCheck();
				}
				
				
				
			//} else if (mMediaplayerError || mDownloadError || mAutoPause){
			} else {
				//must have been a playback error
				this.restart();
			}
			
			if (!mIsSupposedToBePlaying) {
				mIsSupposedToBePlaying = true;
				setPlayingStatus();
				notifyChange(PLAYSTATE_CHANGED);
			}
			
	}
	
	/**
	 * Configure the status notification
	 */
	private void setPlayingStatus(){
		if (mPlayingData == null)
			return;
		if (mNotificationView == null){
			mNotificationView = new RemoteViews(getPackageName(),
					R.layout.status_play);
			mNotificationView.setImageViewResource(R.id.icon,
					R.drawable.statusbar);	
		}
		
		mNotificationView.setTextViewText(R.id.trackname, getTrackName());
		mNotificationView.setTextViewText(R.id.username, getUserName());
		mNotificationView.setTextViewText(R.id.progress, "");

		Intent i = new Intent(this, ScPlayer.class);
		i.addCategory(Intent.CATEGORY_LAUNCHER);
		i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
		i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		i.setAction(Intent.ACTION_MAIN);

		Notification status = new Notification();
		status.contentView = mNotificationView;
		status.flags |= Notification.FLAG_ONGOING_EVENT;
		status.icon = R.drawable.statusbar;
		status.contentIntent = PendingIntent.getActivity(this, 0, i, 0);

		startForeground(PLAYBACKSERVICE_STATUS, status);
	}

	private void stop(boolean remove_status_icon) {
		if (mPlayer.isInitialized()) {
			stopStreaming(null);
		}

		if (remove_status_icon) {
			gotoIdleState();
		} else {
			stopForeground(false);
		}
	}

	/**
	 * Stops playback.
	 */
	public void stop() {
		stop(true);
	}
	
	private Boolean mCacheOnPause = true;

	public void pause(){
		pause(false);
	}
	
	/**
	 * Pauses playback (call play() to resume)
	 */
	public void pause(Boolean force) {
		synchronized (this) {
			if (isPlaying()) {
				mCacheOnPause = force ? true : PreferenceManager.getDefaultSharedPreferences(this).getBoolean("bufferOnPause", true);
				if (mPlayer != null && mPlayer.isInitialized()) mPlayer.pause();
				gotoIdleState();
				notifyChange(PLAYSTATE_CHANGED);
			}
		}
	}

	/**
	 * Returns whether something is currently playing
	 * 
	 * @return true if something is playing (or will be playing shortly, in case
	 *         we're currently transitioning between tracks), false if not.
	 */
	public boolean isPlaying() {
		return mIsSupposedToBePlaying;
	}

	public void prev() {
		synchronized (this) {
			if (mPlayListManager.prev())
			openCurrent();
		}
	}

	public void next(boolean force) {
		synchronized (this) {
			if (mPlayListManager.next())
				openCurrent();
		}
	}
	
	public void next() {
		synchronized (this) {
			if (mPlayListManager.next())
				openCurrent();
		}
	}
	
	/**
	 * Replay the current reloaded track. Usually occurs after hitting play after an error
	 */
	public void restart() {
		synchronized (this) {
			openCurrent();
		}
	}

	private void gotoIdleState() {
		mIsSupposedToBePlaying = false;
		initialBuffering = false;
		pausedForBuffering = false;
		mBufferHandler.removeCallbacksAndMessages(BUFFER_CHECK);
		mBufferHandler.removeCallbacksAndMessages(BUFFER_FILL_CHECK);
		mBufferHandler.removeCallbacksAndMessages(START_NEXT_TRACK);
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		Message msg = mDelayedStopHandler.obtainMessage();
		mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
		stopForeground(true);
	}



	

	/**
	 * Returns the position in the queue
	 * 
	 * @return the position in the queue
	 */
	public int getQueuePosition() {
		synchronized (this) {
			return mPlayListManager.getCurrentPosition();
		}
	}

	/**
	 * Starts playing the track at the given position in the queue.
	 * 
	 * @param pos
	 *            The position in the queue of the track that will be played.
	 */
	public void setQueuePosition(int pos) {
		synchronized (this) {
			if (mPlayListManager.setCurrentPosition(pos)){
				openCurrent();
			}
		}
	}

	public void setFavoriteStatus(long trackId, Boolean favoriteStatus) {
		synchronized (this) {
			if (mPlayingData.getId().compareTo(trackId) == 0){
				mPlayingData.setUserFavorite(favoriteStatus);
			}
		}
		notifyChange(FAVORITES_SET);
	}

	public void mapCommentsToTrack(Comment[] comments, long trackId) {
		synchronized (this) {
			if (mPlayingData.getId().compareTo(trackId) == 0){
				mPlayingData.comments = comments;
			}
		}
		notifyChange(COMMENTS_LOADED);
	}

	public void addComment(Comment comment) {
		synchronized (this) {
/*
			for (Track track : mPlayList) {
				if (track != null) {
					if (track.getId() == comment.getId()) {

						Comment[] oldComments = track.comments;
						Comment[] newComments = new Comment[oldComments.length + 1];

						int i = 0;
						for (Comment oldComment : oldComments) {
							newComments[i] = oldComment;
							i++;
						}

						newComments[i] = comment;
						track.comments = newComments;
					}
				}
			}
			*/
		}
	}

	public String getUserName() {
		synchronized (this) {
			if (mPlayingData == null) {
				return null;
			}
			return mPlayingData.getUser().getUsername();
		}
	}

	public String getUserPermalink() {
		synchronized (this) {
			if (mPlayingData == null) {
				return null;
			}
			return mPlayingData.getUser().getPermalink();
		}
	}

	public long getTrackId() {
		synchronized (this) {
			if (mPlayingData == null) {
				return -1;
			}
			return mPlayingData.getId();
		}
	}

	public Boolean getDownloadable() {
		synchronized (this) {

			if (mPlayingData == null) {
				return false;
			}
			return mPlayingData.getDownloadable();
		}
	}

	public Track getTrack() {
		synchronized (this) {
			if (mPlayingData == null) {
				return null;
			}
			return mPlayingData;
		}
	}

	public String getTrackName() {
		synchronized (this) {
			if (mPlayingData == null) {
				return null;
			}
			return mPlayingData.getTitle();
		}
	}

	public int getDuration() {
		synchronized (this) {
			if (mPlayingData == null) {
				return 0;
			}
			return mPlayingData.getDuration();
		}
	}
	
	public Boolean isBuffering() {
		synchronized (this) {
			return pausedForBuffering;
		}
	}

	/**
	 * Returns the duration of the file in milliseconds. Currently this method
	 * returns -1 for the duration of MIDI files.
	 */
	public long duration() {
		synchronized (this) {
			if (mPlayingData == null) {
				return -1;
			}
			return mPlayingData.getDuration();
		}
	}

	public String getWaveformUrl() {
		synchronized (this) {
			if (mPlayingData == null) {
				return "";
			}
			return mPlayingData.getWaveformUrl();
		}
	}

	/**
	 * Returns the current playback position in milliseconds
	 */
	public long position() {
		if (mPlayer.isInitialized()) {
				return mPlayer.position();
		} else return mResumeTime; //either -1 or a valid resume time
	}

	
	
	

	/**
	 * Returns the duration of the file in milliseconds. Currently this method
	 * returns -1 for the duration of MIDI files.
	 */
	public int loadPercent() {
		synchronized(this){
			if (mPlayer.isInitialized()) {
				if (isStagefright){
					if (mPlayingData.getCacheFile() == null || mPlayingData.getFilelength() == null || mPlayingData.getFilelength() == 0)
						return 0;
					
	 				return (int) (100*mPlayingData.getCacheFile().length()/mPlayingData.getFilelength());
				} else
					return mLoadPercent;
			}
			return 0;
		}
	}
	
	public boolean isSeekable(){
		synchronized(this){
			return (isStagefright && mPlayer.isInitialized() && mPlayingData != null && !mPlayer.isAsyncOpening());
		}
	}
	

	/**
	 * Seeks to the position specified.
	 * 
	 * @param pos
	 *            The position to seek to, in milliseconds
	 */
	public long seek(long pos) {
		//Log.i(TAG,"Seek: " + mPlayer.isInitialized() + " " + mPlayingData + " " + mPlayer.isAsyncOpening());
		synchronized(this){
			if (mPlayer.isInitialized() && mPlayingData != null && !mPlayer.isAsyncOpening() && isStagefright) {
			
				if (pos <= 0) {
					pos = 0;
				}
				
				return mPlayer.seek(pos);
			}
			return -1;
		}
	}
	
	/**
	 * Gets the actual seek value based on a desired value. Simulates the result of an actual seek
	 * 
	 * @param pos
	 *            The position to seek to, in milliseconds
	 */
	public long getSeekResult(long pos) {
		//Log.i(TAG,"Seek: " + mPlayer.isInitialized() + " " + mPlayingData + " " + mPlayer.isAsyncOpening());
		synchronized(this){
			if (mPlayer.isInitialized() && mPlayingData != null && !mPlayer.isAsyncOpening() && isStagefright) {
			
				if (pos <= 0) {
					pos = 0;
				}
				
				return mPlayer.getSeekResult(pos);
			}
			return -1;
		}
	}
	
	

	/**
	 * Provides a unified interface for audio control
	 */
	private class MultiPlayer {
		private MediaPlayer mMediaPlayer;
		private Handler mHandler;
		private boolean mIsInitialized = false;
		private String mPlayingPath = "";
		private boolean mIsAsyncOpening = false;

		public MultiPlayer() {
			refreshMediaplayer();
		}
		
		private void refreshMediaplayer(){
			Log.i(TAG,"Refreshingi media player");
			if (mMediaPlayer != null)
				mMediaPlayer.release();
			
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setWakeMode(CloudPlaybackService.this,
					PowerManager.PARTIAL_WAKE_LOCK);
			// mMediaPlayer.reset();
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setOnPreparedListener(preparedlistener);
			mMediaPlayer.setOnSeekCompleteListener(seeklistener);
			mMediaPlayer.setWakeMode(CloudPlaybackService.this,PowerManager.PARTIAL_WAKE_LOCK);
			mMediaPlayer.setOnCompletionListener(listener);
			mMediaPlayer.setOnErrorListener(errorListener);
			
			if (!isStagefright){
				mMediaPlayer.setOnBufferingUpdateListener(bufferinglistener);
			}
			
		}

		public void setDataSourceAsync(String path) {
			Log.i(TAG, "Set data source async " + path);
			mPlayingPath = path;
			if (mMediaPlayer == null)
				refreshMediaplayer();
			
			mIsAsyncOpening = true;
		
	 
	        try {
	        	if (isStagefright)
	        		mMediaPlayer.setDataSource(mPlayingData.getCacheFile().getAbsolutePath());
	        	else
	        		mMediaPlayer.setDataSource(((SoundCloudApplication) CloudPlaybackService.this.getApplication()).signStreamUrlNaked(mPlayingData.getStreamUrl()));
		         
		         /*try {
		                 Thread.sleep(1000);
		         } catch (InterruptedException e) {
		                 // TODO Auto-generated catch block
		                 e.printStackTrace();
		         }*/
		         
		         mMediaPlayer.prepareAsync();
	            
	        } catch (Exception e){
	        	e.printStackTrace();
	        	   mIsInitialized = false;
	        }
			/*
			
			 try {
	                 if(mStreamProxy==null)
	                 {
	                	 mStreamProxy = new StreamProxy();
	                	 mStreamProxy.init(getApplicationContext());
	                	 mStreamProxy.start();
	                 }
				         String proxyUrl = String.format("http://127.0.0.1:%d/%s", mStreamProxy
			                         .getPort(), mFileToPlay);
			         
			         mMediaPlayer.setDataSource(proxyUrl);
			         //mMediaPlayer.setDataSource(mFileToPlay);
			         
			         /*try {
			                 Thread.sleep(1000);
			         } catch (InterruptedException e) {
			                 // TODO Auto-generated catch block
			                 e.printStackTrace();
			         }
			         
			         mMediaPlayer.prepareAsync();
			         
			 } catch (Exception e) {
			         // TODO Auto-generated catch block
			         e.printStackTrace();
			         mIsInitialized = false;
						return;
			 }*/
		}

		public boolean isInitialized() {
			return mIsInitialized;
		}
		
		public String getPlayingPath() {
			return mPlayingPath;
		}
		
		public boolean isAsyncOpening() {
			return mIsAsyncOpening;
		}
		
		public void start() {
			mMediaPlayer.start();
		}

		public void stop() {
			mIsInitialized = false;
			mPlayingPath = "";
			
			if (mMediaPlayer == null)
				return;
			
			if (mMediaPlayer.isPlaying())
				mMediaPlayer.pause();
			
			if (mIsAsyncOpening){
				mMediaPlayer.release();
				mMediaPlayer = null;
			} else {
				try{
					mMediaPlayer.reset();
				} catch (IllegalStateException e){
					mMediaPlayer.release();
					mMediaPlayer = null;
				}
			}
			
			
		}

		public long position() {
			try{
				return mMediaPlayer.getCurrentPosition();	
			} catch (Exception e){
				return 0;
			}
			
		}
		
		public long seek(long whereto) {
			return seek(whereto,false);
		}
		
		public long seek(long whereto, Boolean resumeSeek) {
			mPlayer.setVolume(0);
			whereto = (int) getSeekResult(whereto,resumeSeek);
			mMediaPlayer.seekTo((int) whereto);
			return whereto;
		}
		
		public long getSeekResult(long whereto){
			return getSeekResult(whereto,false);
		}
		
		public long getSeekResult(long whereto, Boolean resumeSeek){
			long maxSeek = mPlayer.position();
			
			if (!resumeSeek){
				if (mPlayingData.getFilelength() == null)
					return mPlayer.position();
				else
					maxSeek = getDuration()*mPlayingData.getCacheFile().length()/mPlayingData.getFilelength() - PLAYBACK_MARK/(128/8) - 3000;
				
				if (whereto > mPlayer.position() && maxSeek < mPlayer.position())
					return mPlayer.position();
				
				
				if (whereto > maxSeek) {
					whereto = maxSeek;
				}
			}
			
			Log.i(TAG,"seek validated to " + whereto);
			
			return whereto;

		}

		public void setVolume(float vol) {
			mMediaPlayer.setVolume(vol, vol);
		}

		/**
		 * You CANNOT use this player anymore after calling release()
		 */
		public void release() {
			stop();
			if (mMediaPlayer != null) {
				mMediaPlayer.release();
			}
		}

		public void pause() {
				mMediaPlayer.pause();	
		}

		public void setHandler(Handler handler) {
			mHandler = handler;
		}

		MediaPlayer.OnBufferingUpdateListener bufferinglistener = new MediaPlayer.OnBufferingUpdateListener() {
			public void onBufferingUpdate(MediaPlayer mp, int percent) {
				mLoadPercent = percent;
			}
		};

		MediaPlayer.OnSeekCompleteListener seeklistener = new MediaPlayer.OnSeekCompleteListener() {
			public void onSeekComplete(MediaPlayer mp) {
				if (!mMediaPlayer.isPlaying()) {
					mPlayer.setVolume(0);
					play();
					startAndFadeIn();
				} else {
					//checkBufferStatus();
					mPlayer.setVolume(0);
					startAndFadeIn();
					assertBufferCheck();
				}
				notifyChange(SEEK_COMPLETE);
				
			}
		};

		
		MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				
				Log.i(TAG,"On Complete!! " + mIsInitialized + " " +  mPlayingData  + " " + mMediaPlayer.getCurrentPosition()  + " of " +getDuration() );
				//check for premature track end
				if (mIsInitialized 
						&& mPlayingData != null //valid track playing
						&& getDuration()- mMediaPlayer.getCurrentPosition() > 3000 // not at the end of the track 
						) { //seeking before the end, otherwise not enough romm to buffer anyways
					
					//mResumeTime = mMediaPlayer.getCurrentPosition();
					//mResumeId = mPlayingData.getId();
					
					mMediaPlayer.reset();
					mIsInitialized = false;
					mPlayingPath = "";
					
					if (checkNetworkStatus())
						openCurrent();
					else {
						notifyChange(STREAM_DIED);
						gotoIdleState();
					}
					
					return;
				}

				// Acquire a temporary wakelock, since when we return from
				// this callback the MediaPlayer will release its wakelock
				// and allow the device to go to sleep.
				// This temporary wakelock is released when the RELEASE_WAKELOCK
				// message is processed, but just in case, put a timeout on it.

				mWakeLock.acquire(30000);
				mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
				
				if (!mMediaplayerError) mHandler.sendEmptyMessage(TRACK_ENDED);
				

				mMediaplayerError = false;
			}
		};

		MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				mIsAsyncOpening = false;
				mIsInitialized = true;
				
				notifyChange(BUFFERING_COMPLETE);
				
				if (!mAutoPause) {
					if (mIsSupposedToBePlaying){
						mPlayer.setVolume(0);
						play();
						startAndFadeIn();
					}
				} else {
					if (mResumeId.compareTo(mPlayingData.getId()) == 0) {
						mPlayer.seek(mResumeTime);
						mResumeTime = -1;
						mResumeId = null;
					}
				}
			}
		};

		MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
			public boolean onError(MediaPlayer mp, int what, int extra) {
				mIsAsyncOpening = false;
				mMediaplayerError = true;
				
				Log.e(TAG,"MP ERROR " + what + " | " + extra);
				
				switch (what) {

				default:
					mIsInitialized = false;
					mPlayingPath = "";
					mMediaPlayer.reset();
					mHandler.sendMessage(mHandler
							.obtainMessage(TRACK_EXCEPTION));
					break;
				}
				return true;
			}
		};

	}
	
	 BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
         public void onReceive(Context context, Intent intent) {
         	mCurrentPlugState =  intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
             //context.unregisterReceiver(this);
             int rawlevel = intent.getIntExtra("level", -1);
             int scale = intent.getIntExtra("scale", -1);
             int level = -1;
             if (rawlevel >= 0 && scale > 0) {
                 level = (rawlevel * 100) / scale;
             }
             Log.i(TAG,"Plugged state: " +mCurrentPlugState);
            Log.i(TAG,"Battery Level Remaining: " + level + "%");
         }
     };
	
	
	private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int ringvolume = audioManager
				.getStreamVolume(AudioManager.STREAM_RING);
				if (ringvolume > 0) {
					mResumeAfterCall = (isPlaying() || mResumeAfterCall)
					&& mPlayingData != null;
					pause();
				}
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				// pause the music while a conversation is in progress
				mResumeAfterCall = (isPlaying() || mResumeAfterCall)
				&& mPlayingData != null;
				pause();
			} else if (state == TelephonyManager.CALL_STATE_IDLE) {
				// start playing again
				if (mResumeAfterCall) {
					// resume playback only if music was playing
					// when the call was answered
					startAndFadeIn();
					mResumeAfterCall = false;
				}
			}
		}
	};
	
	private void startAndFadeIn() {
		mMediaplayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
	}

	
	/**
	 * Handle changes to the mediaplayer
	 */
	private Handler mMediaplayerHandler = new Handler() {
		float mCurrentVolume = 1.0f;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FADEIN:
				if (!isPlaying()) {
					mCurrentVolume = 0f;
					mPlayer.setVolume(mCurrentVolume);
					play();
					mMediaplayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
				} else {
					mCurrentVolume += 0.01f;
					if (mCurrentVolume < 1.0f) {
						mMediaplayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
					} else {
						mCurrentVolume = 1.0f;
					}
					mPlayer.setVolume(mCurrentVolume);
				}
				break;
			case SERVER_DIED:
				if (mIsSupposedToBePlaying) {
					next(true);
				} else {
					// the server died when we were idle, so just
					// reopen the same song (it will start again
					// from the beginning though when the user
					// restarts)
					openCurrent();
				}
				break;
			case TRACK_EXCEPTION:
				gotoIdleState();
				notifyChange(TRACK_ERROR);
				notifyChange(PLAYBACK_COMPLETE);
				break;
			case TRACK_ENDED:
				next(false);
				break;
			case RELEASE_WAKELOCK:
				mWakeLock.release();
				break;
			default:
				break;
			}
		}
	};

	/**
	 * Handles changes to the buffer
	 */
	private final Handler mBufferHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BUFFER_CHECK:
				if (checkBuffer())
					queueNextRefresh(500);
				break;
			case BUFFER_FILL_CHECK:
				checkBufferStatus();
				break;
			case START_NEXT_TRACK:
				startNextTrack();
				break;	
				
				
			default:
				break;
			}
		}
	};
	
	
	private void queueNextTrack(long delay) {
			Message msg = mBufferHandler.obtainMessage(START_NEXT_TRACK);
			mBufferHandler.removeMessages(START_NEXT_TRACK);
			mBufferHandler.sendMessageDelayed(msg, delay);
	}
	
	private void queueNextRefresh(long delay) {
			Message msg = mBufferHandler.obtainMessage(BUFFER_CHECK);
			mBufferHandler.removeMessages(BUFFER_CHECK);
			mBufferHandler.sendMessageDelayed(msg, delay);
	}
	
	public void queueBufferCheck(){
		if (keepCaching()){
			Log.i(TAG,"QUEUE Buffer Check");
			Message msg = mBufferHandler.obtainMessage(BUFFER_FILL_CHECK);
			mBufferHandler.removeMessages(BUFFER_FILL_CHECK);
			mBufferHandler.sendMessageDelayed(msg, 100);	
		}
	}
	
	public void queueRestart(){
		if (keepCaching()){
			Log.i(TAG,"QUEUE Buffer Check");
			Message msg = mBufferHandler.obtainMessage(RESTART_TRACK);
			mBufferHandler.removeMessages(RESTART_TRACK);
			mBufferHandler.sendMessageDelayed(msg, 100);	
		}
	}
	
	
	/**
	 * Connection handler to check 
	 */
	private Handler connHandler = new Handler() {
	    public void handleMessage(Message msg) {
	            switch(msg.what) {
	                case CONNECTIVITY_MSG:
	                	if (connectivityListener == null) return;
	                		if (checkNetworkStatus())
	                			checkBufferStatus();
	                      break;
	                      
	                }
	       }
	};
	
	
	/**
	 * Stop the mediaplayer and stream in a thread since it seems to take a bit of time and
	 *  sometimes when navigating tracks we don't want the UI to have to wait
	 */
	private static class StreamStopper extends Thread {
		
		WeakReference<CloudPlaybackService> serviceRef;
		Long continueStreamingId;
		
		public StreamStopper(CloudPlaybackService service, Long long1){
			serviceRef = new WeakReference<CloudPlaybackService>(service);
			this.continueStreamingId = long1;
		}
		
		@Override
		public void run() {
			serviceRef.get().stopStreaming(continueStreamingId);
			if (serviceRef.get() != null) {
				serviceRef.get() .mStopThread = null;
				serviceRef.get().queueNextTrack(0);
			}
		}
	}
	
	
	/**
	 * This thread pipes the remote stream to the cache file
	 *
	 */
	private static class DownloadThread extends Thread {
		
		WeakReference<CloudPlaybackService> serviceRef;
		  private Track track;
		  private boolean killed = false;
		
		  public DownloadThread(CloudPlaybackService service, InputStream is, OutputStream os, int trackId, long toSkip){
			 
		 }
		  
		  public DownloadThread(CloudPlaybackService cloudPlaybackService,Track track) {
			  serviceRef = new WeakReference<CloudPlaybackService>(cloudPlaybackService);
				 this.track = track;
		}

		public Long getTrackId(){
			  return track.getId();
		  }
		  
		  public void kill(){
			  killed = true;
		  }
		 
		 
		 public void run(){
			 Log.i("Cacher","Running download thread " + getTrackId());
			 
			 InputStream is = null;
			 FileOutputStream os = null;
			 try {

				 Log.i("Cacher","Download " + track.getSignedUri());
				//get the remote stream
				 
				 HttpClient client = new DefaultHttpClient();
				 HttpResponse httpResponse;

				 // is this already cached at all with a valid size
				 Log.i(TAG,"Checking cache file " + track.getCacheFile().length() + " " + track.getFilelength());
				 if (track.getCacheFile().length() > 0 && track.getFilelength() != null){
					
					 //already totally cached, supposedly, check length and return if its valid
					 
					 if (track.getCacheFile().length() >= track.getFilelength()){ 
						 Log.i(TAG,"++++++checking head");
						 HttpHead request = new HttpHead(track.getSignedUri());
						 httpResponse = client.execute(request);
						 
						   if ( httpResponse.getStatusLine().getStatusCode() != 200){ //invalid status
							   Log.i(TAG,"invalid status received: " + httpResponse.getStatusLine().getStatusCode());
					        	if (serviceRef.get() != null) serviceRef.get().sendDownloadException();
					        	return;
					        }
						   
						 //rare case, file length has changed for some reason
						   if (track.getFilelength() != Long.parseLong(httpResponse.getHeaders("content-length")[0].getValue())){ 
				        		serviceRef.get().fileLengthUpdated(track,true); //tell the player to updat the changed length 
				        	} 
						   
						   return;
						   
					 } else {
						//already partially cached, check length and continue if its valid
						 Log.i(TAG,"++++++checking partial " + track.getCacheFile().length());
						 HttpGet request = new HttpGet(track.getSignedUri());
						 request.setHeader("Range","bytes="+track.getCacheFile().length()+"-");	 // get only the part of the response we need
						 httpResponse = client.execute(request);
						 
						 if ( httpResponse.getStatusLine().getStatusCode() != 206){ //invalid status
							 Log.i(TAG,"invalid status received: " + httpResponse.getStatusLine().getStatusCode());
					        	if (serviceRef.get() != null) serviceRef.get().sendDownloadException();
					        	return;
					     }
						 
						 Log.i(TAG,"Comparing " + track.getFilelength() + " to " + Long.parseLong(httpResponse.getHeaders("content-length")[0].getValue()) + " plus " + track.getCacheFile().length());
						 //is the stored length equal to the cached file length plus
						 if (track.getFilelength() != Long.parseLong(httpResponse.getHeaders("content-length")[0].getValue()) + track.getCacheFile().length()){ //rare case
				        		serviceRef.get().fileLengthUpdated(track,true); // changed file length
				        		return; //a new download thread will be started
						 } else {
							 serviceRef.get().assertBufferCheck(); //tell the buffer to start
						 }
					 }
					 
				  } else {
					  Log.i(TAG,"++++++normal reading");
					  //file not cached at all
					  
					  HttpGet request = new HttpGet(track.getSignedUri());
					  httpResponse = client.execute(request);
						 
					 if ( httpResponse.getStatusLine().getStatusCode() != 200){ //invalid status
						 Log.i(TAG,"invalid status received: " + httpResponse.getStatusLine().getStatusCode());
				        	if (serviceRef.get() != null) serviceRef.get().sendDownloadException();
				        	return;
				     }
						 
						 //is the stored length equal to the cached file length plus
						track.setFilelength(Long.parseLong(httpResponse.getHeaders("content-length")[0].getValue()));
						serviceRef.get().fileLengthUpdated(track,false); // changed file length
				  }
				 Log.i("Cacher","Getting streams");
			        // get the streams
			        is = httpResponse.getEntity().getContent();
			        os = new FileOutputStream(track.getCacheFile(),true);
					 
			     
			      // Start streaming track body.
			      byte[] buff = new byte[1024 * 50];
			      int readBytes = 0;
					while (!killed && serviceRef.get().keepCaching() &&  (readBytes = is.read(buff, 0, buff.length)) != -1) {
							os.write(buff, 0, readBytes);
					  }
					Log.i("Cacher","Finished Normally");
						
			 	} catch (IOException e) {
			 		Log.i("Cacher","IOException");
					e.printStackTrace();
				} catch (Exception e) {
					Log.i("Cacher","Exception " + e.toString());
					e.printStackTrace();
				} finally {
					if (os != null) try {os.close();} catch (IOException e) {e.printStackTrace();}
					if (is != null) try {is.close();} catch (IOException e) {e.printStackTrace();}
					Log.i(TAG,"FINALLY buffer check");
					if (serviceRef.get() != null)
						serviceRef.get().queueBufferCheck();
				}
			 }
	  }
	
	

	/*
	 * By making this a static class with a WeakReference to the Service, we
	 * ensure that the Service can be GCd even when the system process still has
	 * a remote reference to the stub.
	 */
	static class ServiceStub extends ICloudPlaybackService.Stub {
		WeakReference<CloudPlaybackService> mService;

		ServiceStub(CloudPlaybackService service) {
			mService = new WeakReference<CloudPlaybackService>(service);
		}

		public int getQueuePosition() {
			return mService.get().getQueuePosition();
		}

		public void setQueuePosition(int index) {
			if (mService.get() != null) mService.get().setQueuePosition(index);
		}

		public boolean isPlaying() {
			return mService.get().isPlaying();
		}
		
		public boolean isSeekable() {
			return mService.get().isSeekable();
		}
		
		public void stop() {
			if (mService.get() != null) mService.get().stop();
		}

		public void pause() {
			if (mService.get() != null) mService.get().pause();
		}
		
		public void forcePause() {
			if (mService.get() != null) mService.get().pause(true);
		}

		public void play() {
			if (mService.get() != null) mService.get().play();
		}

		public void prev() {
			if (mService.get() != null) mService.get().prev();
		}

		public void next() {
			if (mService.get() != null) mService.get().next(true);
		}
		
		public void restart() {
			if (mService.get() != null) mService.get().restart();
		}

		public String getTrackName() {
			return mService.get().getTrackName();
		}

		public long getTrackId() {
			return mService.get().getTrackId();
		}

		public String getUserName() {
			return mService.get().getUserName();
		}

		public String getUserPermalink() {
			return mService.get().getUserPermalink();
		}

		public String getWaveformUrl() {
			return mService.get().getWaveformUrl();
		}

		public long getDuration() {
			return mService.get().getDuration();
		}

		public boolean getDownloadable() {
			return mService.get().getDownloadable();
		}
		
		public boolean isBuffering() {
			return mService.get().isBuffering();
		}

		public long position() {
			return mService.get().position();
		}
		
		public long duration() {
			return mService.get().duration();
		}

		public int loadPercent() {
			return mService.get().loadPercent();
		}

		public long seek(long pos) {
			return mService.get().seek(pos);
		}
		
		public long getSeekResult(long pos) {
			return mService.get().getSeekResult(pos);
		}
	

		public Track getTrack() throws RemoteException {
			return mService.get().getTrack();
		}

		public void setComments(Comment[] commentData, int trackId)
		throws RemoteException {
			if (mService.get() != null) mService.get().mapCommentsToTrack(commentData, trackId);
		}
		
		@Override
		public void playFromAppCache(int playPos) throws RemoteException {
			if (mService.get() != null) mService.get().playFromAppCache(playPos);
		}

		public void addComment(Comment commentData) throws RemoteException {
			if (mService.get() != null) mService.get().addComment(commentData);
		}

		public void setFavoriteStatus(long trackId, boolean favoriteStatus)
		throws RemoteException {
			if (mService.get() != null) mService.get().setFavoriteStatus(trackId, favoriteStatus);
		}


	}

	private final IBinder mBinder = new ServiceStub(this);

	class PlayerLoadCommentsTask extends LoadCommentsTask {

		@Override
		protected void setComments(Comment[] comments, int trackId) {
			mapCommentsToTrack(comments, trackId);
		}
	}


}
