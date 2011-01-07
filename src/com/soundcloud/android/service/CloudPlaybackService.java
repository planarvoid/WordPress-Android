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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
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

	public static final String ASYNC_OPENING = "com.soundcloud.android.asyncopening";
	public static final String ASYNC_OPEN_COMPLETE = "com.soundcloud.android.asyncopencomplete";

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
	private String mFileToPlay;
	private int mLoadPercent = 0;
	private int mPlayListLen = 0;
	private boolean mAutoPause = false;
	

	private Track[] mPlayList = new Track[0];
	private Track mPlayingData;

	private Boolean mMediaplayerError = false;

	private RemoteViews mNotificationView;

	private long mResumeTime = 0;
	private String mResumeId = "";
	
	private long mSeekTime = -1;
	private long mLastSeekTime = -1;
	private boolean mPlayAfterSeek = false;

	private int mPlayPos = -1;
	private int mReconnectCount = 0;
	private boolean mPlayerResetting = false;
	

	private WakeLock mWakeLock;
	private int mServiceStartId = -1;
	private boolean mServiceInUse = false;
	private boolean mResumeAfterCall = false;
	private boolean mIsSupposedToBePlaying = false;
	// interval after which we stop the service when idle
	private static final int IDLE_DELAY = 60000;

	private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int ringvolume = audioManager
				.getStreamVolume(AudioManager.STREAM_RING);
				if (ringvolume > 0) {
					mResumeAfterCall = (isPlaying() || mResumeAfterCall)
					&& getAudioId() != null;
					pause();
				}
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				// pause the music while a conversation is in progress
				mResumeAfterCall = (isPlaying() || mResumeAfterCall)
				&& getAudioId() != null;
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

	private Handler mAsyncPlaybackHandler = new Handler();
	
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






	public CloudPlaybackService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();


		// Needs to be done in this thread, since otherwise
		// ApplicationContext.getPowerManager() crashes.
		mPlayer = new MultiPlayer();
		mPlayer.setHandler(mMediaplayerHandler);


		//setup call listening
		TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this
				.getClass().getName());
		mWakeLock.setReferenceCounted(false);

		// If the service was idle, but got killed before it stopped itself, the
		// system will relaunch it. Make sure it gets stopped again in that
		// case.
		Message msg = mDelayedStopHandler.obtainMessage();
		mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
	}

	@Override
	public void onDestroy() {
		// release all MediaPlayer resources, including the native player and
		// wakelocks
		mPlayer.release();
		mPlayer = null;

		// make sure there aren't any other messages coming
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		mMediaplayerHandler.removeCallbacksAndMessages(null);

		TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tmgr.listen(mPhoneStateListener, 0);

		mWakeLock.release();
		super.onDestroy();
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		mDelayedStopHandler.removeCallbacksAndMessages(null);
		mServiceInUse = true;
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
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
			removeTracks(0, mPlayListLen);
		}
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

			if (!CloudUtils.isTrackPlayable(track))
				return;
			
			mFileToPlay = track.getData(Track.key_play_url);

			if (!mFileToPlay.contentEquals(track
					.getData(Track.key_local_play_url))) {
				mFileToPlay = ((SoundCloudApplication) CloudPlaybackService.this.getApplication()).signStreamUrlNaked(mFileToPlay);
			}

			
			
			boolean metaChanged = true;
			if (mPlayingData != null && mPlayingData.getData(Track.key_id).contentEquals(track.getData(Track.key_id)))
				metaChanged = false;
			
			mPlayingData = track;

			track.putData(Track.key_user_played, "true");
			CloudUtils.resolveTrack(getApplicationContext(), track, true,
					CloudUtils.getCurrentUserId(this));

			
			mLoadPercent = 0;
			
			if (metaChanged)
				notifyChange(META_CHANGED);

			//stop in a thread so the resetting (or releasing if we are asyncopening) doesn't holdup the UI
			if (mStopThread == null){
				if (mPlayer.isInitialized() || mPlayer.isAsyncOpening() ){
					mStopThread = new Thread() {
						@Override
						public void run() {
							mPlayer.stop();
							mAsyncPlaybackHandler.post(mSetCurrentDataSource);
						}
					};
					mStopThread.setPriority(Thread.MAX_PRIORITY);
					mStopThread.start();
				} else {
					mAsyncPlaybackHandler.post(mSetCurrentDataSource);
				}
			}
	}
	
	final Runnable mSetCurrentDataSource = new Runnable() {
		public void run() {
			mStopThread = null;
			mPlayer.setDataSourceAsync(mFileToPlay);
		}
	};
	
	


	/**
	 * Starts playback of a previously opened file.
	 */
	public void play() {
		Log.i(TAG,"MP PLAY " +mPlayer.isInitialized() );

		if (mPlayer.isInitialized()){
			Log.i(TAG,"MP Is Initialized");

			if (mPlayingData == null)
				return;
			
			mSeekTime = -1;

			mPlayer.start();

			if (mResumeId != null && mPlayingData != null && mResumeId.contentEquals(mPlayingData .getData(Track.key_id))) {
				mPlayer.seek(mResumeTime);
				mResumeTime = 0;
				mResumeId = "";
			}

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
			if (!mIsSupposedToBePlaying) {
				mIsSupposedToBePlaying = true;
				notifyChange(PLAYSTATE_CHANGED);
			}

		} else {
			this.restart();
		}
	}

	private void stop(boolean remove_status_icon) {
		if (mPlayer.isInitialized()) {
			mPlayer.stop();
		}
		mFileToPlay = null;

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

	/**
	 * Pauses playback (call play() to resume)
	 */
	public void pause() {
		synchronized (this) {
			if (isPlaying()) {
				mPlayer.pause();
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

	/*
	 * Desired behavior for prev/next/shuffle:
	 * 
	 * - NEXT will move to the next track in the list when not shuffling, and to
	 * a track randomly picked from the not-yet-played tracks when shuffling. If
	 * all tracks have already been played, pick from the full set, but avoid
	 * picking the previously played track if possible. - when shuffling, PREV
	 * will go to the previously played track. Hitting PREV again will go to the
	 * track played before that, etc. When the start of the history has been
	 * reached, PREV is a no-op. When not shuffling, PREV will go to the
	 * sequentially previous track (the difference with the shuffle-case is
	 * mainly that when not shuffling, the user can back up to tracks that are
	 * not in the history).
	 * 
	 * Example: When playing an album with 10 tracks from the start, and
	 * enabling shuffle while playing track 5, the remaining tracks (6-10) will
	 * be shuffled, e.g. the final play order might be 1-2-3-4-5-8-10-6-9-7.
	 * When hitting 'prev' 8 times while playing track 7 in this example, the
	 * user will go to tracks 9-6-10-8-5-4-3-2. If the user then hits 'next', a
	 * random track will be picked again. If at any time user disables shuffling
	 * the next/previous track will be picked in sequential order again.
	 */

	public void prev() {
		synchronized (this) {
			
			if (mPlayPos == 0) 
					return;
			
			mPlayPos--;

			if (mPlayListLen > 1 && !CloudUtils.isTrackPlayable(mPlayList[mPlayPos])){
				prev();
				return;
			}
			openCurrent();
		}
	}

	public void next(boolean force) {
		synchronized (this) {

			if (mPlayPos >= mPlayListLen - 1) {
				// we're at the end of the list
				//gotoIdleState();
				//notifyChange(PLAYBACK_COMPLETE);
				//mIsSupposedToBePlaying = false;

				//do nothing
				return;

			} else {
				mPlayPos++;
				if (!CloudUtils.isTrackPlayable(mPlayList[mPlayPos])){
					next(true);
					return;
				}
			}

			openCurrent();
		}
	}
	
	public void restart() {
		synchronized (this) {
			openCurrent();
		}
	}

	private void gotoIdleState() {
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
	public int removeTrack(String id) {
		int numremoved = 0;
		synchronized (this) {
			for (int i = 0; i < mPlayListLen; i++) {
				if (mPlayList[i].getData(Track.key_id).contentEquals(id)) {
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
	 * Returns the path of the currently playing file, or null if no file is
	 * currently playing.
	 */
	public String getPath() {
		return mFileToPlay;
	}

	/**
	 * Returns the rowid of the currently playing file, or -1 if no file is
	 * currently playing.
	 */
	public String getAudioId() {
		synchronized (this) {

			if (mPlayPos >= 0 && mPlayer.isInitialized()) {
				return mPlayList[mPlayPos].getData(Track.key_id);
			}
		}
		return null;
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

	public void setFavoriteStatus(String trackId, String favoriteStatus) {
		synchronized (this) {
			for (Track track : mPlayList) {
				if (track != null) {
					if (track.getData(Track.key_id).contentEquals(trackId)) {
						track.putData(Track.key_user_favorite, favoriteStatus);
					}
				}
			}
		}
		notifyChange(FAVORITES_SET);
	}

	public void mapCommentsToTrack(Comment[] comments, String trackId) {
		synchronized (this) {
			for (Track track : mPlayList) {
				if (track != null) {
					if (track.getData(Track.key_id).contentEquals(trackId)) {
						track.comments = comments;
					}
				}
			}
		}
		notifyChange(COMMENTS_LOADED);
	}

	public void addComment(Comment comment) {
		synchronized (this) {

			for (Track track : mPlayList) {
				if (track != null) {
					if (track.getData(Track.key_id).contentEquals(
							comment.getData(Comment.key_track_id))) {

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
		}
	}

	public String getUserName() {
		synchronized (this) {
			if (mPlayingData == null) {
				return null;
			}
			return mPlayingData.getData(Track.key_username);
		}
	}

	public String getUserPermalink() {
		synchronized (this) {
			if (mPlayingData == null) {
				return null;
			}
			return mPlayingData.getData(Track.key_user_permalink);
		}
	}

	public String getTrackId() {
		synchronized (this) {
			if (mPlayingData == null) {
				return null;
			}
			return mPlayingData.getData(Track.key_id);
		}
	}

	public String getDownloadable() {
		synchronized (this) {

			if (mPlayingData == null) {
				return null;
			}
			return mPlayingData.getData(Track.key_downloadable);
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
			return mPlayingData.getData(Track.key_title);
		}
	}

	public String getDuration() {
		synchronized (this) {
			if (mPlayingData == null) {
				return null;
			}
			return mPlayingData.getData(Track.key_duration);
		}
	}
	
	public Boolean isAsyncOpening() {
		synchronized (this) {
			return mPlayer.isAsyncOpening();
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
			return Integer.parseInt(mPlayingData.getData(Track.key_duration));
		}
	}

	public String getWaveformUrl() {
		synchronized (this) {
			if (mPlayingData == null) {
				return null;
			}
			if (mPlayingData.getData(Track.key_local_waveform_url) != ""
				&& mPlayingData.getData(Track.key_download_status)
				.contentEquals(Track.DOWNLOAD_STATUS_DOWNLOADED)) {
				return mPlayingData.getData(Track.key_local_waveform_url);
			} else {
				return mPlayingData.getData(Track.key_waveform_url);
			}
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
	 * Returns the current playback position in milliseconds
	 */
	public long lastSuccessfulSeek() {
		return mLastSeekTime;
	}

	/**
	 * Returns the duration of the file in milliseconds. Currently this method
	 * returns -1 for the duration of MIDI files.
	 */
	public int loadPercent() {
		if (mPlayer.isInitialized()) {
			return mLoadPercent;
		}
		return 0;
	}
	
	public boolean isSeekable(){
		return (mPlayer.isInitialized() && mPlayingData != null && !mPlayer.isAsyncOpening());
	}

	/**
	 * Seeks to the position specified.
	 * 
	 * @param pos
	 *            The position to seek to, in milliseconds
	 */
	public long seek(long pos) {
		//Log.i(TAG,"Seek: " + mPlayer.isInitialized() + " " + mPlayingData + " " + mPlayer.isAsyncOpening());
		
		if (mPlayer.isInitialized() && mPlayingData != null && !mPlayer.isAsyncOpening()) {
			
			if (pos <= 0) {
				pos = 0;
			}

			if (pos > mPlayer.duration()) {
				pos = mPlayer.duration();
			}
			//Log.i(TAG,"SEeking to " + pos);
			return mPlayer.seek(pos);
		}
		return -1;
	}

	/**
	 * Provides a unified interface for audio control
	 */
	private class MultiPlayer {
		private MediaPlayer mMediaPlayer;
		private Handler mHandler;
		private boolean mIsInitialized = false;
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
			mMediaPlayer.setOnBufferingUpdateListener(bufferinglistener);
			mMediaPlayer.setOnSeekCompleteListener(seeklistener);
			mMediaPlayer.setWakeMode(CloudPlaybackService.this,PowerManager.PARTIAL_WAKE_LOCK);
			mMediaPlayer.setOnCompletionListener(listener);
			mMediaPlayer.setOnErrorListener(errorListener);
		}

		public void setDataSourceAsync(String path) {
			
			if (mMediaPlayer == null)
				refreshMediaplayer();
			
			mIsAsyncOpening = true;
			notifyChange(ASYNC_OPENING);
			
			try {
				mMediaPlayer.setDataSource(path);
				mMediaPlayer.prepareAsync();
			} catch (IOException ex) {
				mIsInitialized = false;
				return;
			} catch (IllegalArgumentException ex) {
				mIsInitialized = false;
				return;
			}
			
			
			mIsInitialized = true;
		}

		public boolean isInitialized() {
			return mIsInitialized;
		}
		
		public boolean isAsyncOpening() {
			return mIsAsyncOpening;
		}
		
		public void start() {
			mMediaPlayer.start();
		}

		public void stop() {
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
			
			mIsInitialized = false;
		}

		public long duration() {
			return mMediaPlayer.getDuration();
		}

		public long position() {
			try{
				return mMediaPlayer.getCurrentPosition();	
			} catch (Exception e){
				return 0;
			}
			
		}

		public long seek(long whereto) {
			Log.i(TAG,"Seeking to " + whereto);
			mSeekTime = whereto;
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
				Log.i(TAG,"SEEK COMPLETE  " + mSeekTime + " " + mLastSeekTime + " " + mp.getCurrentPosition());
				mLastSeekTime = mSeekTime; //preserve seek time in case connection dies
				mSeekTime = -1;
				
				if (!mMediaPlayer.isPlaying()) {
					play();
				}
				notifyChange(SEEK_COMPLETE);
				
				//Log.i(TAG,"SEEK COMPLETE new  " + mLastSeekTime);
				
				// clear last seek time after delay to see if we can properly
				// connect to stream after seek
				mHandler.removeCallbacks(clearLastSeek);
				mHandler.postDelayed(clearLastSeek, 10000);
			}
		};

		
		private Runnable clearLastSeek = new Runnable() {
			   public void run() {
				   //Log.i(TAG,"SEEK COMPLETE clear  " + mLastSeekTime);
				   mLastSeekTime = -1;
			   }
			};
			
		MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {

				//Log.i(TAG,"ON COMPLETE " + mLastSeekTime);
				
				if (mIsInitialized && !mMediaplayerError
						&& mReconnectCount < MAX_RECONNECT_COUNT && mPlayingData != null) {
					//Log.i(TAG,"On complete Listener calling getduration " + mMediaplayerError + " " + mLastSeekTime + " " + mMediaPlayer.getDuration() + " " + mMediaPlayer.getCurrentPosition());
					
					if ((mMediaPlayer.getDuration()- mMediaPlayer.getCurrentPosition() > 3000 
							|| (mLastSeekTime != -1 && mMediaplayerError != null))
							&& mPlayingData.getData(Track.key_play_url).indexOf("stream") != -1) {

						notifyChange(STREAM_DIED);
						mResumeTime = mLastSeekTime == -1 ? mMediaPlayer.getCurrentPosition() : mLastSeekTime;
						Log.i(TAG,"Setting resume time to " + mResumeTime);
						mResumeId = mPlayingData.getData(Track.key_id);
						openCurrent(false);
						return;
					}
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
				
				notifyChange(ASYNC_OPEN_COMPLETE);
				
				if (!mAutoPause) {
					play();
				} else {
					if (mResumeId.contentEquals(mPlayingData
							.getData(Track.key_id))) {
						mPlayer.seek(mResumeTime);
						mResumeTime = 0;
						mResumeId = "";
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
					mMediaPlayer.reset();
					break;
					
				default:
					mIsInitialized = false;
					mMediaPlayer.reset();
					mHandler.sendMessage(mHandler
							.obtainMessage(TRACK_EXCEPTION));
					break;
				}
				return true;
			}
		};

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

		public String getTrackId() {
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

		public String getPath() {
			return mService.get().getPath();
		}

		public String getDuration() {
			return mService.get().getDuration();
		}

		public String getDownloadable() {
			return mService.get().getDownloadable();
		}
		

		public boolean isAsyncOpening() {
			return mService.get().isAsyncOpening();
		}

		public long position() {
			return mService.get().position();
		}
		
		public long lastSuccessfulSeek() {
			return mService.get().lastSuccessfulSeek();
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

		public void setComments(Comment[] commentData, String trackId)
		throws RemoteException {
			if (mService.get() != null) mService.get().mapCommentsToTrack(commentData, trackId);
		}

		public int removeTrack(String id) throws RemoteException {
			return mService.get().removeTrack(id);
		}

		public void enqueueTrack(Track track, int action)
		throws RemoteException {
			mService.get().enqueueTrack(track, action);
		}

		public void clearQueue() throws RemoteException {
			mService.get().clearQueue();
		}

		public void addComment(Comment commentData) throws RemoteException {
			if (mService.get() != null) mService.get().addComment(commentData);
		}

		public void setFavoriteStatus(String trackId, String favoriteStatus)
		throws RemoteException {
			if (mService.get() != null) mService.get().setFavoriteStatus(trackId, favoriteStatus);
		}

	}

	private final IBinder mBinder = new ServiceStub(this);

	class PlayerLoadCommentsTask extends LoadCommentsTask {

		@Override
		protected void setComments(Comment[] comments, String trackId) {
			mapCommentsToTrack(comments, trackId);
		}
	}

}
