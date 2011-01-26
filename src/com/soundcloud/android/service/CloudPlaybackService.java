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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.MediaFrameworkChecker;
import com.soundcloud.utils.StreamProxy;
import com.soundcloud.utils.net.NetworkConnectivityListener;

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
	private int mPlayListLen = 0;
	private boolean mAutoPause = false;
	
	protected NetworkConnectivityListener connectivityListener;
	protected static final int CONNECTIVITY_MSG = 9;
	
	

	private Track[] mPlayList = new Track[0];
	
	private Track mPlayingData;
	
	private DownloadThread mDownloadThread;

	private Boolean mDownloadError = false;
	private Boolean mMediaplayerError = false;

	private RemoteViews mNotificationView;

	private long mResumeTime = 0;
	private int mResumeId = -1;
	
	private long mSeekTime = -1;
	private long mLastSeekTime = -1;
	private boolean mPlayAfterSeek = false;

	private int mPlayPos = -1;
	private int mReconnectCount = 0;
	private boolean mPlayerResetting = false;
	
	private StreamProxy mStreamProxy;

	private WakeLock mWakeLock;
	private int mServiceStartId = -1;
	private boolean mServiceInUse = false;
	private boolean mResumeAfterCall = false;
	private boolean mIsSupposedToBePlaying = false;
	// interval after which we stop the service when idle
	private static final int IDLE_DELAY = 60000;
	
	protected static final int MAX_CACHE_SIZE = 200000000;
	
	protected static final int WIFI_HIGH_WATER_MARK = 100000000;
	protected static final int HIGH_WATER_MARK = 20000000;
	protected static final int PLAYBACK_MARK = 60000;
	protected static final int LOW_WATER_MARK = 20000;
	
	private boolean pausedForBuffering;
	private boolean initialBuffering = true;
	
	private long mCurrentBuffer;
	
	private NetworkInfo mCurrentNetworkInfo;
	private int mCurrentPlugState;
	
	private boolean stageFrightDetermined = false;
	private boolean isStagefright = false;

	

	




	public CloudPlaybackService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();


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
        		    stageFrightDetermined = true;
            }
        };
        t.start();	
	
		
		    
		    
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
	        
	        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
		    
		// If the service was idle, but got killed before it stopped itself, the
		// system will relaunch it. Make sure it gets stopped again in that
		// case.
		Message msg = mDelayedStopHandler.obtainMessage();
		mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
		
		
	}

	@Override
	public void onDestroy() {
		stopStreaming(mPlayingData,-1);
		gotoIdleState();
		
		// release all MediaPlayer resources, including the native player and
		// wakelocks
		mPlayer.release();
		mPlayer = null;

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
		
		Log.i(TAG,"ON DESTROY " + mWakeLock);
		
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
					mPlayAfterSeek = true;
					//play();
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


		if (isPlaying() || mResumeAfterCall) {
			// something is currently playing, or will be playing once
			// an in-progress call ends, so don't stop the service now.
			return true;
		}

		// If there is a playlist but playback is paused, then wait a while
		// before stopping the service, so that pause/resume isn't slow.
		// Also delay stopping the service if we're transitioning between
		// tracks.
		if (mPlayListLen > 0 || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
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
		sendBroadcast(i);
	}

	private void ensurePlayListCapacity(int size) {
		if (mPlayList == null || size > mPlayList.length) {
			// reallocate at 2x requested size so we don't
			// need to grow and copy the array for every
			// insert
			Track[] newlist = new Track[size * 2];
			int len = mPlayList != null ? mPlayList.length : mPlayListLen;
			for (int i = 0; i < len; i++) {
				newlist[i] = mPlayList[i];
			}
			mPlayList = newlist;
		}
		// FIXME: shrink the array when the needed size is much smaller
		// than the allocated size
	}

	// insert the list of songs at the specified position in the playlist
	private void addToPlayList(Track[] list, int position) {
		int addlen = list.length;
		if (position < 0) { // overwrite
			mPlayListLen = 0;
			position = 0;
		}
		ensurePlayListCapacity(mPlayListLen + addlen);
		if (position > mPlayListLen) {
			position = mPlayListLen;
		}

		// move part of list after insertion point
		int tailsize = mPlayListLen - position;
		for (int i = tailsize; i > 0; i--) {
			mPlayList[position + i] = mPlayList[position + i - addlen];
		}

		// copy list into playlist
		for (int i = 0; i < addlen; i++) {
			mPlayList[position + i] = list[i];
		}
		mPlayListLen += addlen;
	}

	// insert the track at the specified position in the playlist
	private void addTrackToPlayList(Track track, int position) {

		if (position < 0) { // overwrite
			mPlayListLen = 0;
			position = 0;
		}
		ensurePlayListCapacity(mPlayListLen + 1);
		if (position > mPlayListLen) {
			position = mPlayListLen;
		}

		// move part of list after insertion point
		int tailsize = mPlayListLen - position;
		for (int i = tailsize; i > 0; i--) {
			mPlayList[position + i] = mPlayList[position + i - 1];
		}

		mPlayList[position] = track;
		mPlayListLen += 1;
	}

	/**
	 * Appends a list of tracks to the current playlist. If nothing is playing
	 * currently, playback will be started at the first track. If the action is
	 * NOW, playback will switch to the first of the new tracks immediately.
	 * 
	 * @param list
	 *            .length The list of tracks to append.
	 * @param action
	 *            NOW, NEXT or LAST
	 */
	public void enqueueTrack(Track track, int action) {
		synchronized (this) {

			if (action == NEXT && mPlayPos + 1 < mPlayListLen) {
				addTrackToPlayList(track, mPlayPos + 1);
				notifyChange(QUEUE_CHANGED);
			} else {
				// action == LAST || action == NOW || mPlayPos + 1 ==
				// mPlayListLen
				addTrackToPlayList(track, Integer.MAX_VALUE);
				notifyChange(QUEUE_CHANGED);
				if (action == NOW) {
					mPlayPos = mPlayListLen - 1;
					openCurrent();
					return;
				}
			}

			if (mPlayPos < 0) {
				mPlayPos = 0;
				openCurrent();
			}
		}
	}

	/**
	 * Appends a list of tracks to the current playlist. If nothing is playing
	 * currently, playback will be started at the first track. If the action is
	 * NOW, playback will switch to the first of the new tracks immediately.
	 * 
	 * @param list
	 *            .length The list of tracks to append.
	 * @param action
	 *            NOW, NEXT or LAST
	 */
	public void enqueue(Track[] list, int playPos) {
		synchronized (this) {
			clearQueue();
			addToPlayList(list, 0);
			mPlayPos = playPos;
			openCurrent();
		}
	}

	public void clearQueue() {
		synchronized (this) {
			stopStreaming(mPlayingData,-1);
			removeTracks(0, mPlayListLen);
			clearCacheDir();
		}
	}
	

	public void playFromAppCache(int playPos) {
		enqueue(((SoundCloudApplication) this.getApplication()).flushCachePlaylist(),playPos);
		play();
	}


	/**
	 * Moves the item at index1 to index2.
	 * 
	 * @param index1
	 * @param index2
	 */
	public void moveQueueItem(int index1, int index2) {
		synchronized (this) {
			if (index1 >= mPlayListLen) {
				index1 = mPlayListLen - 1;
			}
			if (index2 >= mPlayListLen) {
				index2 = mPlayListLen - 1;
			}
			if (index1 < index2) {
				Track tmp = mPlayList[index1];
				for (int i = index1; i < index2; i++) {
					mPlayList[i] = mPlayList[i + 1];
				}
				mPlayList[index2] = tmp;
				if (mPlayPos == index1) {
					mPlayPos = index2;
				} else if (mPlayPos >= index1 && mPlayPos <= index2) {
					mPlayPos--;
				}
			} else if (index2 < index1) {
				Track tmp = mPlayList[index1];
				for (int i = index1; i > index2; i--) {
					mPlayList[i] = mPlayList[i - 1];
				}
				mPlayList[index2] = tmp;
				if (mPlayPos == index1) {
					mPlayPos = index2;
				} else if (mPlayPos >= index2 && mPlayPos <= index1) {
					mPlayPos++;
				}
			}
			notifyChange(QUEUE_CHANGED);
		}
	}

	/**
	 * Returns the current play list
	 * 
	 * @return An array of integers containing the IDs of the tracks in the play
	 *         list
	 */
	public List<Track> getQueue() {
		ArrayList<Track> queue = new ArrayList<Track>();

		for (int i = 0; i < mPlayListLen; i++) {
			queue.add(mPlayList[i]);
		}

		return queue;
	}


	private void openCurrent() {
		openCurrent(false);
	}


	private void openCurrent(Boolean autoPause) {

			if (mPlayListLen == 0) {
				return;
			}

			mAutoPause = autoPause;
			openAsync(mPlayList[mPlayPos]);
	}

	
	Thread mStopThread = null;
	public void openAsync(Track track) {
			if (track == null) {
				return;
			}
			
			while (!stageFrightDetermined){}

			mLoadPercent = 0;
			mCurrentBuffer = 0;
			
			//if we are already playing this track
			if (mPlayingData != null && mPlayingData.getId() == track.getId()){
				
				mStopThread = new StreamStopper(this, null, mPlayingData.getId());
				mStopThread.setPriority(Thread.MAX_PRIORITY);
				mStopThread.start();
				return;
				
			} 
			
			//stop in a thread so the resetting (or releasing if we are asyncopening) doesn't holdup the UI
				mStopThread = new StreamStopper(this, null, track.getId());
				mStopThread.setPriority(Thread.MAX_PRIORITY);
				mStopThread.start();
			
			//new play data
			mPlayingData = track;
			
			setPlayingStatus();
			
			//tell the db we played it
			track.setUserPlayed("true");
			CloudUtils.resolveTrack(getApplicationContext(), track, true,CloudUtils.getCurrentUserId(this));
			
			//meta has changed
			notifyChange(META_CHANGED);
	}
	
	
	/**
	 * Stops the stream and media player
	 */
	private void stopStreaming(Track stopTrack, int continueStreamingId){
		synchronized (this){
			//stop checking buffer
			pausedForBuffering = false;
			
			mBufferHandler.removeCallbacksAndMessages(BUFFER_CHECK);
			mBufferHandler.removeCallbacksAndMessages(BUFFER_FILL_CHECK);
			mBufferHandler.removeCallbacksAndMessages(START_NEXT_TRACK);
			
			//stop playing
			if (mPlayer.isInitialized() || mPlayer.isAsyncOpening() )
				mPlayer.stop();
		
			if (stopTrack != null && stopTrack.getCacheFile() != null){
				if (stopTrack.getCacheFile().exists())
					stopTrack.getCacheFile().delete();
				
				stopTrack.setCacheFile(null);
				stopTrack.setFileLength(null);
			}	
			
			if (CloudUtils.checkThreadAlive(mDownloadThread) && continueStreamingId != -1 && mDownloadThread.getTrackId() != continueStreamingId){
				mDownloadThread.interrupt();
				mDownloadThread.kill();
				
			}
		}
		
	}
	
	private void startNextTrack(){
			mStopThread = null;

			Log.i(TAG,"Start next track " + CloudUtils.isTrackPlayable(mPlayingData));
			if (CloudUtils.isTrackPlayable(mPlayingData)){
				//mPlaybackHandler.post(mDownloadCurrentTrack);
				pausedForBuffering = true;
				initialBuffering = true;
				
				notifyChange(INITIAL_BUFFERING);
				
				if (isStagefright){
					Log.i(TAG,"Prepare download and asser buffer check");
					prepareDownload(mPlayingData);
					assertBufferCheck();
				} else {
					mPlayer.setDataSourceAsync(mPlayingData.getSignedUri());
				}
				return;
			}
			
			mIsSupposedToBePlaying = false;
			gotoIdleState();
	}
	
	private void prepareDownload(Track trackToCache){
		
		if (trackToCache.getSignedUri() == null)
			trackToCache.setSignedUri(((SoundCloudApplication) getApplication()).signStreamUrlNaked(trackToCache.getStreamUrl()));
		
		
		//2 cases where we do nothing. This track is already fully downloaded, or is being downloaded
		if (trackToCache.getCacheFile() != null && trackToCache.getCacheFile().exists() && trackToCache.getFileLength() != null){
			Log.i(TAG,"Cache File already started");
			if (trackToCache.getCacheFile().length() >= trackToCache.getFileLength()){
				Log.i(TAG,"Alread downloading, tell buffer");
				//nothing to do
				return;
			}
			
			if (mDownloadThread != null && mDownloadThread.isAlive() && trackToCache.getId() == mDownloadThread.getTrackId()){
				// we are already downloading this
				Log.i(TAG,"Alread downloading, tell buffer");
				return;
			}
		}
		
		//start downloading if there is a valid connection, otherwise it will happen when we regain connectivity
		if (checkNetworkStatus()){ 
			try {
				Log.i(TAG,"Setting cache file and doing it");
				trackToCache.setCacheFile(prepareCacheFile(trackToCache));
				mDownloadThread = new DownloadThread(this, trackToCache);
			    mDownloadThread.start();
			} catch (IOException e) {
				e.printStackTrace();
				Log.i(TAG,"IO Exception");
				this.sendTrackException();
			}
			
		}
	}
	
	private void clearCacheDir(){
		File cacheFile = null;
		File cacheDir = new File(CloudCache.EXTERNAL_TRACK_CACHE_DIRECTORY);
		if (cacheDir.exists()){
		 File[] fileList = cacheDir.listFiles();
	        if (fileList != null){
		        for(int i = 0; i < fileList.length; i++) {
		            if(!fileList[i].isDirectory()) {
		            	fileList[i].delete(); 
		            } 
		        }
	        }
		}
	}
	
	
	private File prepareCacheFile(Track trackToCache) throws IOException{
		long size = 0;
		long maxSize = 200000000;
		
		StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
		
		Log.i(TAG,"Calculating Space left " + fs.getBlockSize() + " " + fs.getAvailableBlocks() + " " + fs.getBlockCount());
		
        Long spaceLeft = Long.parseLong(Integer.toString(fs.getBlockSize()))*(fs.getAvailableBlocks() - fs.getBlockCount()/10); 
        
        Log.i(TAG,"Space left " + spaceLeft);
        
		File cacheFile = null;
		File cacheDir = new File(CloudCache.EXTERNAL_TRACK_CACHE_DIRECTORY);
		if (cacheDir.exists()){
		 File[] fileList = cacheDir.listFiles();
	        if (fileList != null){
	        	ArrayList<File> orderedFiles = new ArrayList<File>();
	        	Log.i(TAG,"File List length " + fileList.length);
		        for(int i = 0; i < fileList.length; i++) {
		            if(!fileList[i].isDirectory()) {
		            	if (fileList[i].getName().indexOf("_") != -1 && fileList[i].getName().substring(0,fileList[i].getName().indexOf("_")).contentEquals(Integer.toString(trackToCache.getId()))){
		            		cacheFile = fileList[i];
		            		cacheFile.setLastModified(System.currentTimeMillis());
		            	}else{
		            		Log.i(TAG,"New Size is " + size);
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
		            } 
		        }
		        Log.i(TAG,"Cache Size " + size + " " + maxSize + " " + spaceLeft);
		        
		        if (size > maxSize || spaceLeft < 0){
		        	Long  toTrim =  size - maxSize > 0-spaceLeft ?  size - maxSize : 0-spaceLeft;
		        	int j = 0;
		        	long trimmed = 0;
		        	while (j < orderedFiles.size() && trimmed < toTrim){
		        		if (orderedFiles.get(j) != cacheFile)
		        			((File) orderedFiles.get(j)).delete();
		        	}
		        }
		        
	        }
		} else {
			cacheDir.mkdirs();
		}
		
		if (cacheFile == null)
			cacheFile = File.createTempFile(trackToCache.getId()+"_","", cacheDir);
		
		cacheFile.deleteOnExit(); // file will delete itself on app exit
		
		return cacheFile;
	}
	
	public void sendTrackException(){
		Log.i(TAG,"Send track exception");
		mMediaplayerHandler.sendMessage(mMediaplayerHandler.obtainMessage(TRACK_EXCEPTION));
		mDownloadError = true;
	}
	

	/**
	 * Manage the buffering status. 
	 * @return
	 */
	private long checkBuffer(){
				synchronized (this){
					if (mPlayer != null && mPlayingData.getCacheFile() != null  && mPlayingData.getFileLength() != null){
						
						if (mPlayer.isInitialized()){
							// normal buffer measurement. size of cache file vs playback position
							mCurrentBuffer = mPlayingData.getCacheFile().length() - mPlayingData.getFileLength()*mPlayer.position()/getDuration();
						} else if (mResumeId != -1 && mResumeId== mPlayingData.getId()){
							// resume buffer measurement. if stream died due to lack of a buffer, measure the buffer from where we are supposed to resume
							mCurrentBuffer = mPlayingData.getCacheFile().length() - mPlayingData.getFileLength()*mResumeTime/getDuration();
						} else
							mCurrentBuffer = mPlayingData.getCacheFile().length(); //initial buffer measurement
							
						//Log.i(TAG,"Buffer: " + mCurrentBuffer + " | " + mPlayingData.getCacheFile().length() + " | " + pausedForBuffering + " | " + initialBuffering);
						if (pausedForBuffering){
							
							// first round of buffering, special case where we set the playback file
							if (initialBuffering && mCurrentBuffer > PLAYBACK_MARK){
								initialBuffering = false;
								pausedForBuffering = false;
								if (!mPlayer.getPlayingPath().equalsIgnoreCase(mPlayingData.getCacheFile().getAbsolutePath()))
									mPlayer.setDataSourceAsync(mPlayingData.getCacheFile().getAbsolutePath());
								
							} else if (mCurrentBuffer > PLAYBACK_MARK ){
								
								//normal buffering done
								notifyChange(BUFFERING_COMPLETE);
								pausedForBuffering = false;
								if (mIsSupposedToBePlaying){
									mPlayer.setVolume(0);
									play();
									startAndFadeIn();
								}
							}
						} else {
							
							// normal time to buffer (makes sure we aren't at the end of the track)
							if (mPlayer.isInitialized() && mCurrentBuffer < LOW_WATER_MARK && mPlayingData.getCacheFile().length() < mPlayingData.getFileLength()){
								notifyChange(BUFFERING);
								mPlayer.pause();
								pausedForBuffering = true;
								checkBufferStatus();
							} 
						}
					}
				}
			return 500;
	}
	
	private void assertBufferCheck(){
		long next = checkBuffer();
		queueNextRefresh(next);
		
	}
	

	
	
	
	
	public void queueBufferCheck(){
		if (keepCaching()){
			Log.i(TAG,"QUEUE Buffer Check");
			Message msg = mBufferHandler.obtainMessage(BUFFER_FILL_CHECK);
			mBufferHandler.removeMessages(BUFFER_FILL_CHECK);
			mBufferHandler.sendMessageDelayed(msg, 100);	
		}
		
	}
	
	/*
	 * Make sure the buffer is filling if it needs to. If it doesn't see if the next track can be buffered
	 */
	private void checkBufferStatus(){
		synchronized(this){
			// are we able to cache something
			if (mPlayingData != null && !CloudUtils.checkThreadAlive(mDownloadThread) && checkNetworkStatus() ){
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
			&&  track.getCacheFile() != null && track.getFileLength() != null
			&& track.getCacheFile().length() >= track.getFileLength());
	}
	
	/**
	 * Check to see if we have any errors, have hit a high water mark, or are paused
	 * and not supposed to be caching
	 * @return
	 */
	public Boolean keepCaching(){
		// we aren't playing and are not supposed to be caching during pause
		if ((!mIsSupposedToBePlaying && !mCacheOnPause) ||  mDownloadError)
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
		
			if (mPlayingData == null)
				return;
			
			if (mPlayer.isInitialized()){
				mSeekTime = -1;
	
				mPlayer.start();
	
				if (mResumeId != -1 && mPlayingData != null && mResumeId == mPlayingData.getId()) {
					mPlayer.seek(mResumeTime, true);
					mResumeTime = 0;
					mResumeId = -1;
				}
				
				checkBufferStatus();
				
			} else if (mMediaplayerError || mDownloadError){
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
		
		mNotificationView = new RemoteViews(getPackageName(),
				R.layout.status_play);
		mNotificationView.setImageViewResource(R.id.icon,
				R.drawable.statusbar);
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
			stopStreaming(mPlayingData,-1);
		}

		if (remove_status_icon) {
			gotoIdleState();
		} else {
			stopForeground(false);
		}
		if (remove_status_icon) {
			mIsSupposedToBePlaying = false;
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
				mIsSupposedToBePlaying = false;
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
			
			if (mPlayPos == 0) 
					return;
			
			mPlayPos--;
			openCurrent();
		}
	}

	public void next(boolean force) {
		synchronized (this) {
			
			if (mPlayPos >= mPlayListLen - 1) {
				//do nothing, just keep playing whatever we are playing
				return;

			} else {
				mPlayPos++;
			}

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
		mBufferHandler.removeCallbacksAndMessages(BUFFER_CHECK);
		mBufferHandler.removeCallbacksAndMessages(BUFFER_FILL_CHECK);
		mBufferHandler.removeCallbacksAndMessages(START_NEXT_TRACK);
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		Message msg = mDelayedStopHandler.obtainMessage();
		mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
		stopForeground(true);
	}



	/**
	 * Removes the range of tracks specified from the play list. If a file
	 * within the range is the file currently being played, playback will move
	 * to the next file after the range.
	 * 
	 * @param first
	 *            The first file to be removed
	 * @param last
	 *            The last file to be removed
	 * @return the number of tracks deleted
	 */
	public int removeTracks(int first, int last) {
		int numremoved = removeTracksInternal(first, last);
		if (numremoved > 0) {
			notifyChange(QUEUE_CHANGED);
		}
		return numremoved;
	}

	private int removeTracksInternal(int first, int last) {
		synchronized (this) {

			if (last < first) {
				return 0;
			}
			if (first < 0) {
				first = 0;
			}
			if (last >= mPlayListLen) {
				last = mPlayListLen - 1;
			}

			boolean gotonext = false;
			if (first <= mPlayPos && mPlayPos <= last) {
				mPlayPos = first;
				gotonext = true;
			} else if (mPlayPos > last) {
				mPlayPos -= last - first + 1;
			}
			int num = mPlayListLen - last - 1;
			for (int i = 0; i < num; i++) {
				mPlayList[first + i] = mPlayList[last + 1 + i];
			}
			mPlayListLen -= last - first + 1;
			if (gotonext) {
				if (mPlayListLen == 0) {
					stop(true);
					gotoIdleState();
					notifyChange(PLAYBACK_COMPLETE);
					mPlayingData = null;
					mIsSupposedToBePlaying = false;
					mPlayPos = -1;
				} else {
					if (mPlayPos >= mPlayListLen) {
						mPlayPos = 0;
					}
					boolean wasPlaying = isPlaying();
					stop(false);
					openCurrent();
					if (wasPlaying) {
						play();
					}
				}
			}
			return last - first + 1;
		}
	}

	/**
	 * Removes all instances of the track with the given id from the playlist.
	 * 
	 * @param id
	 *            The id to be removed
	 * @return how many instances of the track were removed
	 */
	public int removeTrack(int id) {
		int numremoved = 0;
		synchronized (this) {
			for (int i = 0; i < mPlayListLen; i++) {
				if (mPlayList[i].getId() == id) {
					numremoved += removeTracksInternal(i, i);
					i--;
				}
			}
		}
		if (numremoved > 0) {
			notifyChange(QUEUE_CHANGED);
		}
		return numremoved;
	}

	/**
	 * Returns the rowid of the currently playing file, or -1 if no file is
	 * currently playing.
	 */
	public int getAudioId() {
		synchronized (this) {
			if (mPlayPos >= 0 && mPlayer.isInitialized()) {
				return mPlayList[mPlayPos].getId();
			}
		}
		return -1;
	}

	/**
	 * Returns the position in the queue
	 * 
	 * @return the position in the queue
	 */
	public int getQueuePosition() {
		synchronized (this) {
			return mPlayPos;
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
			stop(false);
			mPlayPos = pos;
			openCurrent();
		}
	}

	public void setFavoriteStatus(int trackId, String favoriteStatus) {
		synchronized (this) {
			for (Track track : mPlayList) {
				if (track != null) {
					if (track.getId() == trackId) {
						track.setUserFavorite(favoriteStatus);
					}
				}
			}
		}
		notifyChange(FAVORITES_SET);
	}

	public void mapCommentsToTrack(Comment[] comments, int trackId) {
		synchronized (this) {
			for (Track track : mPlayList) {
				if (track != null) {
					if (track.getId() == trackId) {
						track.comments = comments;
					}
				}
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

	public int getTrackId() {
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
		}
		return -1;
	}

	
	
	

	/**
	 * Returns the duration of the file in milliseconds. Currently this method
	 * returns -1 for the duration of MIDI files.
	 */
	public int loadPercent() {
		synchronized(this){
			if (mPlayer.isInitialized()) {
				if (isStagefright){
					if (mPlayingData.getCacheFile() == null || mPlayingData.getFileLength() == null)
						return 0;
					
	 				return (int) (100*mPlayingData.getCacheFile().length()/mPlayingData.getFileLength());
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
			
			if (mMediaPlayer == null)
				refreshMediaplayer();
			
			mIsAsyncOpening = true;
			mPlayingPath = path;
	 
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

		public long duration() {
			if (mIsInitialized)
				return mMediaPlayer.getDuration();
			else
				return 0;
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
			long maxSeek = mPlayer.position();
			
			if (!resumeSeek){
				if (mPlayingData.getFileLength() == null)
					return mPlayer.position();
				else
					maxSeek = getDuration()*mPlayingData.getCacheFile().length()/mPlayingData.getFileLength() - PLAYBACK_MARK/(128/8);
				
				if (whereto > mPlayer.position() && maxSeek < mPlayer.position())
					return mPlayer.position();
				
				
				if (whereto > maxSeek) {
					whereto = maxSeek;
				}
			}

			
			
			mSeekTime = whereto;
			//mStreamProxy.stop();
			//mStreamProxy.start();
			mPlayer.setVolume(0);
			
			Log.i(TAG,"Seeking to " + whereto);
			
			mMediaPlayer.seekTo((int) whereto);

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
				mSeekTime = -1;
				
				if (!mMediaPlayer.isPlaying()) {
					mPlayer.setVolume(0);
					play();
					startAndFadeIn();
				} else {
					mPlayer.setVolume(0);
					startAndFadeIn();
				}
				notifyChange(SEEK_COMPLETE);
				
			}
		};

		
		MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				
				Log.i(TAG,"On Complete!! " + mIsInitialized + " " +  mPlayingData  + " " + mMediaPlayer.getCurrentPosition()  + " of " +mMediaPlayer.getDuration() );
				//check for premature track end
				if (mIsInitialized 
						&& mPlayingData != null //valid track playing
						&& mMediaPlayer.getDuration()- mMediaPlayer.getCurrentPosition() > 3000 // not at the end of the track 
						) { //seeking before the end, otherwise not enough romm to buffer anyways
					
					notifyChange(STREAM_DIED);
					mResumeTime = mMediaPlayer.getCurrentPosition();
					mResumeId = mPlayingData.getId();
					openCurrent(false);
					
					mMediaPlayer.reset();
					mIsInitialized = false;
					mPlayingPath = "";
					
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
					if (mResumeId == mPlayingData.getId()) {
						mPlayer.seek(mResumeTime);
						mResumeTime = 0;
						mResumeId = -1;
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

				case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
					mHandler.sendMessageDelayed(mHandler
							.obtainMessage(SERVER_DIED), 2000);
					mIsInitialized = false;
					mPlayingPath = "";
					mMediaPlayer.reset();
					break;
					
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
	
	
	
	
	private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int ringvolume = audioManager
				.getStreamVolume(AudioManager.STREAM_RING);
				if (ringvolume > 0) {
					mResumeAfterCall = (isPlaying() || mResumeAfterCall)
					&& getAudioId() != -1;
					pause();
				}
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				// pause the music while a conversation is in progress
				mResumeAfterCall = (isPlaying() || mResumeAfterCall)
				&& getAudioId() !=-1;
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
				mIsSupposedToBePlaying = false;
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
				long next = checkBuffer();
				queueNextRefresh(next);
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
		if (!mDownloadError){
			Message msg = mBufferHandler.obtainMessage(BUFFER_CHECK);
			mBufferHandler.removeMessages(BUFFER_CHECK);
			mBufferHandler.sendMessageDelayed(msg, delay);
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
		Track stopTrack;
		int continueStreamingId;
		
		public StreamStopper(CloudPlaybackService service, Track stopTrack, int continueStreamingId){
			serviceRef = new WeakReference<CloudPlaybackService>(service);
			this.stopTrack = stopTrack;
			this.continueStreamingId = continueStreamingId;
		}
		
		@Override
		public void run() {
			serviceRef.get().stopStreaming(stopTrack, continueStreamingId);
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

		public int getTrackId(){
			  return track.getId();
		  }
		  
		  public void kill(){
			  killed = true;
		  }
		 
		 
		 public void run(){
			 Log.i("Cacher","Running download thread " + getTrackId());
			 serviceRef.get().mDownloadError = false;
			 InputStream is = null;
			 FileOutputStream os = null;
			 try {

				 Log.i("Cacher","Download " + track.getSignedUri());
				//get the remote stream
					HttpGet request = new HttpGet(track.getSignedUri());
					if (track.getCacheFile().length() > 0){
						request.setHeader("Range","bytes="+track.getCacheFile().length()+"-");	
					}
					
					HttpClient client = new DefaultHttpClient();
			        HttpResponse httpResponse;
			        httpResponse = client.execute(request);
			        if ( httpResponse.getStatusLine().getStatusCode() >= 400){
			        	if (serviceRef.get() != null) serviceRef.get().sendTrackException();
			        	return;
			        }
			        
			        Long filesize = Long.parseLong(httpResponse.getHeaders("content-length")[0].getValue()); 
			        
			        //get the total file size from the headers
			        if (track.getCacheFile().length() == 0){
			        	track.setFileLength(filesize);
			        	Log.i("Cacher","Download length " + filesize);
			        }
			        
			        if (track.getCacheFile().length() > track.getFileLength())
			        	return;
			        
			        // get the streams
			        is = httpResponse.getEntity().getContent();
			        os = new FileOutputStream(track.getCacheFile(),true);
				 
			        //skip ahead  in the content if we already have a partially cached file
					//long skipped = 0;
					//while (skipped < track.getCacheFile().length()){
						//skipped += is.skip(track.getCacheFile().length()-skipped);
					//}
				 
			     
			      // Start streaming track body.
			      byte[] buff = new byte[1024 * 50];
			      int readBytes = 0;
					while (serviceRef.get().keepCaching() &&  (readBytes = is.read(buff, 0, buff.length)) != -1 && !killed) {
							os.write(buff, 0, readBytes);
					  }
						
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

		public void openFile(Track track, boolean oneShot) {
			// mService.get().open(track, oneShot);
		}

		public void open(Track[] list, int position) {
			if (mService.get() != null) mService.get().enqueue(list, position);
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

		public int getTrackId() {
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

		public void enqueue(Track[] list, int playPos) {
			mService.get().enqueue(list, playPos);
		}

		public void moveQueueItem(int from, int to) {
			mService.get().moveQueueItem(from, to);
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

		public int removeTracks(int first, int last) {
			return mService.get().removeTracks(first, last);
		}

		public List getQueue() throws RemoteException {
			return mService.get().getQueue();
		}

		public Track getTrack() throws RemoteException {
			return mService.get().getTrack();
		}

		public void setComments(Comment[] commentData, int trackId)
		throws RemoteException {
			if (mService.get() != null) mService.get().mapCommentsToTrack(commentData, trackId);
		}

		public int removeTrack(int id) throws RemoteException {
			return mService.get().removeTrack(id);
		}

		public void enqueueTrack(Track track, int action)
		throws RemoteException {
			mService.get().enqueueTrack(track, action);
		}
		
		@Override
		public void playFromAppCache(int playPos) throws RemoteException {
			if (mService.get() != null) mService.get().playFromAppCache(playPos);
		}

		public void clearQueue() throws RemoteException {
			mService.get().clearQueue();
		}

		public void addComment(Comment commentData) throws RemoteException {
			if (mService.get() != null) mService.get().addComment(commentData);
		}

		public void setFavoriteStatus(int trackId, String favoriteStatus)
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
