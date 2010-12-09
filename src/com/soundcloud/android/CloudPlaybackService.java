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

package com.soundcloud.android;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.soundcloud.android.ICloudPlaybackService;
import com.soundcloud.android.R;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;

/**
 * Provides "background" audio playback capabilities, allowing the
 * user to switch between activities without stopping playback.
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
    
    public static final String PLAYSTATE_CHANGED = "com.dubmoon.overcast.playstatechanged";
    public static final String META_CHANGED = "com.dubmoon.overcast.metachanged";
    public static final String QUEUE_CHANGED = "com.dubmoon.overcast.queuechanged";
    public static final String PLAYBACK_COMPLETE = "com.dubmoon.overcast.playbackcomplete";
    public static final String TRACK_ERROR = "com.dubmoon.overcast.trackerror";
    public static final String STREAM_DIED = "com.dubmoon.overcast.streamdied";
    public static final String COMMENTS_LOADED = "com.dubmoon.overcast.commentsloaded";
    public static final String SEEK_COMPLETE = "com.dubmoon.overcast.seekcomplete";
    
    public static final String ASYNC_OPENING = "com.dubmoon.overcast.asyncopening";
    public static final String ASYNC_OPEN_COMPLETE = "com.dubmoon.overcast.asyncopencomplete";

    public static final String SERVICECMD = "com.dubmoon.overcast.musicservicecommand";
    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";

    public static final String TOGGLEPAUSE_ACTION = "com.dubmoon.overcast.musicservicecommand.togglepause";
    public static final String PAUSE_ACTION = "com.dubmoon.overcast.musicservicecommand.pause";
    public static final String PREVIOUS_ACTION = "com.dubmoon.overcast.musicservicecommand.previous";
    public static final String NEXT_ACTION = "com.dubmoon.overcast.musicservicecommand.next";

    private static final int TRACK_ENDED = 1;
    private static final int RELEASE_WAKELOCK = 2;
    private static final int SERVER_DIED = 3;
    private static final int FADEIN = 4;
    private static final int TRACK_EXCEPTION = 5;
    private static final int MAX_HISTORY_SIZE = 100;
    
    private LoadCommentsTask mLoadCommentsTask;
    private MultiPlayer mPlayer;
    private String mFileToPlay;
    private int mShuffleMode = SHUFFLE_NONE;
    private int mRepeatMode = REPEAT_NONE;
    private int mMediaMountedCount = 0;
    private Track [] mAutoShuffleList = null;
    private boolean mOneShot;
    private int mLoadPercent = 0;
    private int mPlayListLen = 0;
    private Vector<Integer> mHistory = new Vector<Integer>(MAX_HISTORY_SIZE);
    
    private boolean mAutoPause = false;
    
   private HashMap<String,Track> mPlayListStore = new  HashMap<String,Track>();
	private Track [] mPlayList = new Track[0];
    
    private Track mPlayingData;
    private Boolean mMediaplayerError = false;
    
    private RemoteViews mNotificationView;
	private Notification mNotification;
    
	private long mResumeTime = 0;
	private String mResumeId = "";
	private long mSeekTime = 0;
	
    private int mPlayPos = -1;
    private static final String LOGTAG = "CloudPlaybackService";
    private final Shuffler mRand = new Shuffler();
    private int mOpenFailedCounter = 0;
    private int mReconnectCount = 0;
    String[] mCursorCols = new String[] {
            "audio._id AS _id",             // index must match IDCOLIDX below
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
    };
    private final static int IDCOLIDX = 0;
    
    private BroadcastReceiver mUnmountReceiver = null;
    private WakeLock mWakeLock;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mResumeAfterCall = false;
    private boolean mIsSupposedToBePlaying = false;
    private boolean mQuietMode = false;
    
    private AudioManager mAudioManager;
    // used to track what type of audio focus loss caused the playback to pause
    private boolean mPausedByTransientLossOfFocus = false;

    
    private SharedPreferences mPreferences;
    
    // We use this to distinguish between different cards when saving/restoring playlists.
    // This will have to change if we want to support multiple simultaneous cards.
    private int mCardId;
    
    //private MediaAppWidgetProvider mAppWidgetProvider = MediaAppWidgetProvider.getInstance();
    
    // interval after which we stop the service when idle
    private static final int IDLE_DELAY = 60000; 

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (ringvolume > 0) {
                    mResumeAfterCall = (isPlaying() || mResumeAfterCall) && (getAudioId() != null);
                    pause();
                }
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // pause the music while a conversation is in progress
                mResumeAfterCall = (isPlaying() || mResumeAfterCall) && (getAudioId() != null);
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
                	Log.i(TAG,"Server died " +mIsSupposedToBePlaying );
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
                	Log.i(TAG,"Track Exception " +mIsSupposedToBePlaying );
                	notifyChange(TRACK_ERROR);
                    if (mIsSupposedToBePlaying) {
                        next(true);
                    } else {
                    	notifyChange(PLAYBACK_COMPLETE);
                    }
                    break;
                case TRACK_ENDED:
                	Log.d(TAG, "Track ended: repeat mode " + mRepeatMode + "|" + mOneShot);
                    if (mRepeatMode == REPEAT_CURRENT) {
                        seek(0);
                        //play();
                    } else if (!mOneShot) {
                        next(false);
                    } else {
                        notifyChange(PLAYBACK_COMPLETE);
                        mIsSupposedToBePlaying = false;
                    }
                    break;
                case RELEASE_WAKELOCK:
                    mWakeLock.release();
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                next(true);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                mPausedByTransientLossOfFocus = false;
                seek(0);
            }
//            else if (MediaAppWidgetProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
//                // Someone asked us to refresh a set of specific widgets, probably
//                // because they were just added.
//                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
//                mAppWidgetProvider.performUpdate(MediaPlaybackService.this, appWidgetIds);
//            }
        }
    };
    
    
   /* private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            // AudioFocus is a new feature: focus updates are made verbose on purpose
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                    if(isPlaying()) {
                        mPausedByTransientLossOfFocus = false;
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                    if(isPlaying()) {
                        mPausedByTransientLossOfFocus = true;
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                    if(!isPlaying() && mPausedByTransientLossOfFocus) {
                        mPausedByTransientLossOfFocus = false;
                        startAndFadeIn();
                    }
                    break;
                default:
                    Log.e(LOGTAG, "Unknown audio focus change code");
            }
        }
    };*/




    public CloudPlaybackService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
       // mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName()));

        
        mPreferences = getSharedPreferences("Music", MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
        //mCardId = CloudUtils.getCardId(this);
        
        registerExternalStorageListener();

        // Needs to be done in this thread, since otherwise ApplicationContext.getPowerManager() crashes.
        mPlayer = new MultiPlayer();
        mPlayer.setHandler(mMediaplayerHandler);

        reloadQueue();
        
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        registerReceiver(mIntentReceiver, commandFilter);
        
        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }

    @Override
    public void onDestroy() {
    	Log.e(LOGTAG, "Service being destroyed.");
        // Check that we're not being destroyed while something is still playing.
        if (isPlaying()) {
            Log.e(LOGTAG, "Service being destroyed while still playing.");
        }
        // release all MediaPlayer resources, including the native player and wakelocks
        mPlayer.release();
        mPlayer = null;
        
       // mAudioManager.abandonAudioFocus(mAudioFocusListener);

        
        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mMediaplayerHandler.removeCallbacksAndMessages(null);

        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, 0);

        unregisterReceiver(mIntentReceiver);
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
            mUnmountReceiver = null;
        }
        mWakeLock.release();
        super.onDestroy();
    }
    
    private final char hexdigits [] = new char [] {
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'
    };

    private void saveQueue(boolean full) {
        if (mOneShot) {
            return;
        }
        Editor ed = mPreferences.edit();
        //long start = System.currentTimeMillis();
        if (full) {
            StringBuilder q = new StringBuilder();
            
            // The current playlist is saved as a list of "reverse hexadecimal"
            // numbers, which we can generate faster than normal decimal or
            // hexadecimal numbers, which in turn allows us to save the playlist
            // more often without worrying too much about performance.
            // (saving the full state takes about 40 ms under no-load conditions
            // on the phone)
            int len = mPlayListLen;
            for (int i = 0; i < len; i++) {
            	Log.i(TAG,"On Save Queue " + len + " " + mPlayList[i]);
                long n = Long.parseLong(mPlayList[i].getData(Track.key_id));
                Log.i(TAG,"On Save Queue id " + mPlayList[i].getData(Track.key_id));
                if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        int digit = (int)(n & 0xf);
                        n >>= 4;
                        q.append(hexdigits[digit]);
                    }
                    q.append(";");
                }
            }
            //Log.i("@@@@ service", "created queue string in " + (System.currentTimeMillis() - start) + " ms");
            ed.putString("queue", q.toString());
            //ed.putInt("cardid", mCardId);
            if (mShuffleMode != SHUFFLE_NONE) {
                // In shuffle mode we need to save the history too
                len = mHistory.size();
                q.setLength(0);
                for (int i = 0; i < len; i++) {
                    int n = mHistory.get(i);
                    if (n == 0) {
                        q.append("0;");
                    } else {
                        while (n != 0) {
                            int digit = (n & 0xf);
                            n >>= 4;
                            q.append(hexdigits[digit]);
                        }
                        q.append(";");
                    }
                }
                ed.putString("history", q.toString());
            }
        }
        ed.putInt("curpos", mPlayPos);
        if (mPlayer.isInitialized()) {
            ed.putLong("seekpos", mPlayer.position());
        }
        ed.putInt("repeatmode", mRepeatMode);
        ed.putInt("shufflemode", mShuffleMode);
        ed.commit();
  
        //Log.i("@@@@ service", "saved state in " + (System.currentTimeMillis() - start) + " ms");
    }

    private void reloadQueue() {
        String q = null;
        
        boolean newstyle = false;
       /* int id = mCardId;
        if (mPreferences.contains("cardid")) {
            newstyle = true;
            id = mPreferences.getInt("cardid", ~mCardId);
        }
        if (id == mCardId) {
            // Only restore the saved playlist if the card is still
            // the same one as when the playlist was saved
            q = mPreferences.getString("queue", "");
        }*/
        int qlen = q != null ? q.length() : 0;
        if (qlen > 1) {
            //Log.i("@@@@ service", "loaded queue: " + q);
            int plen = 0;
            int n = 0;
            int shift = 0;
            for (int i = 0; i < qlen; i++) {
                char c = q.charAt(i);
                if (c == ';') {
                    ensurePlayListCapacity(plen + 1);
                    mPlayList[plen] = CloudUtils.resolveTrackById(getApplicationContext(), Integer.toString(n), CloudUtils.getCurrentUserId(getApplicationContext()));
                    
                    if (mPlayList[plen] != null)
                    	plen++;
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += ((c - '0') << shift);
                    } else if (c >= 'a' && c <= 'f') {
                        n += ((10 + c - 'a') << shift);
                    } else {
                        // bogus playlist data
                        plen = 0;
                        break;
                    }
                    shift += 4;
                }
            }
            mPlayListLen = plen;

            int pos = mPreferences.getInt("curpos", 0);
            if (pos < 0 || pos >= mPlayListLen) {
                // The saved playlist is bogus, discard it
                mPlayListLen = 0;
                return;
            }
            mPlayPos = pos;
            
            
            
            // When reloadQueue is called in response to a card-insertion,
            // we might not be able to query the media provider right away.
            // To deal with this, try querying for the current file, and if
            // that fails, wait a while and try again. If that too fails,
            // assume there is a problem and don't restore the state.
//            Cursor crsr = CloudUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                        new String [] {"_id"}, "_id=" + mPlayList[mPlayPos] , null, null);
//            if (crsr == null || crsr.getCount() == 0) {
//                // wait a bit and try again
//                SystemClock.sleep(3000);
//                crsr = getContentResolver().query(
//                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                        mCursorCols, "_id=" + mPlayList[mPlayPos] , null, null);
//            }
//            if (crsr != null) {
//                crsr.close();
//            }

            // Make sure we don't auto-skip to the next song, since that
            // also starts playback. What could happen in that case is:
            // - music is paused
            // - go to UMS and delete some files, including the currently playing one
            // - come back from UMS
            // (time passes)
            // - music app is killed for some reason (out of memory)
            // - music service is restarted, service restores state, doesn't find
            //   the "current" file, goes to the next and: playback starts on its
            //   own, potentially at some random inconvenient time.
            
            
            
            
            mOpenFailedCounter = 20;
            mQuietMode = true;
            openCurrent(true);
            mQuietMode = false;
            if (!mPlayer.isInitialized()) {
                // couldn't restore the saved state
                mPlayListLen = 0;
                return;
            }
            
            
            mResumeTime = mPreferences.getLong("seekpos", 0);
    		mResumeId = mPlayingData.getData(Track.key_id);
            //seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);
            Log.d(LOGTAG, "restored queue, currently at position "
                    + position() + " (requested " + mResumeTime + ")");
            
            int repmode = mPreferences.getInt("repeatmode", REPEAT_NONE);
            if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
                repmode = REPEAT_NONE;
            }
            mRepeatMode = repmode;

            int shufmode = mPreferences.getInt("shufflemode", SHUFFLE_NONE);
            if (shufmode != SHUFFLE_AUTO && shufmode != SHUFFLE_NORMAL) {
                shufmode = SHUFFLE_NONE;
            }
            if (shufmode != SHUFFLE_NONE) {
                // in shuffle mode we need to restore the history too
                q = mPreferences.getString("history", "");
                qlen = q != null ? q.length() : 0;
                if (qlen > 1) {
                    plen = 0;
                    n = 0;
                    shift = 0;
                    mHistory.clear();
                    for (int i = 0; i < qlen; i++) {
                        char c = q.charAt(i);
                        if (c == ';') {
                            if (n >= mPlayListLen) {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            mHistory.add(n);
                            n = 0;
                            shift = 0;
                        } else {
                            if (c >= '0' && c <= '9') {
                                n += ((c - '0') << shift);
                            } else if (c >= 'a' && c <= 'f') {
                                n += ((10 + c - 'a') << shift);
                            } else {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            shift += 4;
                        }
                    }
                }
            }
//            if (shufmode == SHUFFLE_AUTO) {
//                if (! makeAutoShuffleList()) {
//                    shufmode = SHUFFLE_NONE;
//                }
//            }
//            mShuffleMode = shufmode;
        }
    }
    
    /* (non-Javadoc)
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
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                if (position() < 2000) {
                    prev();
                } else {
                    seek(0);
                    play();
                }
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
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

        // Take a snapshot of the current playlist
        saveQueue(true);

        if (isPlaying() || mResumeAfterCall) {
            // something is currently playing, or will be playing once 
            // an in-progress call ends, so don't stop the service now.
            return true;
        }
        
        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between tracks.
        if (mPlayListLen > 0  || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
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
            // save the queue again, because it might have changed
            // since the user exited the music app (because of
            // party-shuffle or because the play-position changed)
            saveQueue(true);
            stopSelf(mServiceStartId);
        }
    };
    
    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     *
     * @param storagePath path to mount point for the removed media
     */
    public void closeExternalStorageFiles(String storagePath) {
        // stop playback and clean up if the SD card is going to be unmounted.
        stop(true);
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications.
     * The intent will call closeExternalStorageFiles() if the external media
     * is going to be ejected, so applications can clean up any files they have open.
     */
    public void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        saveQueue(true);
                        mOneShot = true; // This makes us not save the state again later,
                                         // which would be wrong because the song ids and
                                         // card id might not match. 
                        closeExternalStorageFiles(intent.getData().getPath());
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        mMediaMountedCount++;
                        mCardId = CloudUtils.getCardId(CloudPlaybackService.this);
                        reloadQueue();
                        notifyChange(QUEUE_CHANGED);
                        notifyChange(META_CHANGED);
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addDataScheme("file");
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }

    /**
     * Notify the change-receivers that something has changed.
     * The intent that is sent contains the following data
     * for the currently playing track:
     * "id" - Integer: the database row ID
     * "artist" - String: the name of the artist
     * "album" - String: the name of the album
     * "track" - String: the name of the track
     * The intent has an action that is one of
     * "com.dubmoon.overcast.metachanged"
     * "com.dubmoon.overcast.queuechanged",
     * "com.dubmoon.overcast.playbackcomplete"
     * "com.dubmoon.overcast.playstatechanged"
     * respectively indicating that a new track has
     * started playing, that the playback queue has
     * changed, that playback has stopped because
     * the last file in the list has been played,
     * or that the play-state changed (paused/resumed).
     */
    private void notifyChange(String what) {
        
        Intent i = new Intent(what);
        i.putExtra("id", getTrackId());
        i.putExtra("track", getTrackName());
        i.putExtra("user", getUserName());
        sendBroadcast(i);
        
        if (what.equals(QUEUE_CHANGED)) {
            saveQueue(true);
        } else {
            saveQueue(false);
        }
        
        // Share this notification directly with our widgets
        //mAppWidgetProvider.notifyChange(this, what);
    }
    
    
    private void ensurePlayListCapacity(int size) {
        if (mPlayList == null || size > mPlayList.length) {
            // reallocate at 2x requested size so we don't
            // need to grow and copy the array for every
            // insert
            Track [] newlist = new Track[size * 2];
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
    private void addToPlayList(Track [] list, int position) {
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
        for (int i = tailsize ; i > 0 ; i--) {
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
    	Log.i("CloudPlaybackService","add track to playlist " + mPlayListLen + " " + position);
    	
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
        for (int i = tailsize ; i > 0 ; i--) {
            mPlayList[position + i] = mPlayList[position + i - 1]; 
        }
        
        mPlayList[position] = track;
        
        Log.i("CloudPlaybackService","added track to playlist " + position + " " + mPlayList.length);
        
        mPlayListLen += 1;
    }


    
    /**
     * Appends a list of tracks to the current playlist.
     * If nothing is playing currently, playback will be started at
     * the first track.
     * If the action is NOW, playback will switch to the first of
     * the new tracks immediately.
     * @param list.length The list of tracks to append.
     * @param action NOW, NEXT or LAST
     */
    public void enqueueTrack(Track track, int action) {
        synchronized(this) {
        	
        	if (action == NEXT && mPlayPos + 1 < mPlayListLen) {
                addTrackToPlayList(track, mPlayPos + 1);
                notifyChange(QUEUE_CHANGED);
            } else {
                // action == LAST || action == NOW || mPlayPos + 1 == mPlayListLen
                addTrackToPlayList(track, Integer.MAX_VALUE);
                notifyChange(QUEUE_CHANGED);
                if (action == NOW) {
                    mPlayPos = mPlayListLen - 1;
                    openCurrent();
                    notifyChange(META_CHANGED);
                    return;
                }
            }
            
            if (mPlayPos < 0) {
                mPlayPos = 0;
                openCurrent();
                notifyChange(META_CHANGED);
            }
        }
    }
    
    /**
     * Appends a list of tracks to the current playlist.
     * If nothing is playing currently, playback will be started at
     * the first track.
     * If the action is NOW, playback will switch to the first of
     * the new tracks immediately.
     * @param list.length The list of tracks to append.
     * @param action NOW, NEXT or LAST
     */
    public void enqueue(Track[] list, int playPos) {
        synchronized(this) {
        	clearQueue();
        	addToPlayList(list,0);
        	mPlayPos = playPos;
        	openCurrent();
            notifyChange(META_CHANGED);
        }
    }
    
    
    public void clearQueue(){
    	synchronized(this) {
    		this.removeTracks(0, mPlayListLen);
    	}
    }


    /**
     * Replaces the current playlist with a new list,
     * and prepares for starting playback at the specified
     * position in the list, or a random position if the
     * specified position is 0.
     * @param list The new list of tracks.
     */
//    public void open(long [] list, int position) {
//        synchronized (this) {
//            if (mShuffleMode == SHUFFLE_AUTO) {
//                mShuffleMode = SHUFFLE_NORMAL;
//            }
//            long oldId = getAudioId();
//            int listlength = list.length;
//            boolean newlist = true;
//            if (mPlayListLen == listlength) {
//                // possible fast path: list might be the same
//                newlist = false;
//                for (int i = 0; i < listlength; i++) {
//                    if (list[i] != mPlayList[i]) {
//                        newlist = true;
//                        break;
//                    }
//                }
//            }
//            if (newlist) {
//                addToPlayList(list, -1);
//                notifyChange(QUEUE_CHANGED);
//            }
//            int oldpos = mPlayPos;
//            if (position >= 0) {
//                mPlayPos = position;
//            } else {
//                mPlayPos = mRand.nextInt(mPlayListLen);
//            }
//            mHistory.clear();
//
//            
//            openCurrent();
//            if (oldId != getAudioId()) {
//                notifyChange(META_CHANGED);
//            }
//        }
//    }
    
    /**
     * Moves the item at index1 to index2.
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
                    mPlayList[i] = mPlayList[i+1];
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
                    mPlayList[i] = mPlayList[i-1];
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
     * @return An array of integers containing the IDs of the tracks in the play list
     */
    public List<Track> getQueue() {
    	ArrayList<Track> queue = new ArrayList<Track>();
    	
    	for (int i = 0; i < mPlayListLen; i++)
    		queue.add(mPlayList[i]);
    	
        return queue;
    }

    private void openCurrent() {
    	openCurrent(false);
    }
    
    private void openCurrent(Boolean autoPause) {
    	
        synchronized (this) {
        	
            if (mPlayListLen == 0) {
                return;
            }
            
            mAutoPause = autoPause;
            
           stop(false);
           openAsync(mPlayList[mPlayPos]);
        }
    }

    public void openAsync(Track track) {
        synchronized (this) {
        	Log.i("CloudPlaybackService", "Opening current Track " +track);
            if (track == null) {
                return;
            }
            
            Log.i("CloudPlaybackService", "track length " + track.getData(Track.key_duration));
            
            mFileToPlay = track.getData(Track.key_play_url);
            
            
            if (!mFileToPlay.contentEquals(track.getData(Track.key_local_play_url))){
            	mFileToPlay = CloudCommunicator.getInstance(getApplicationContext()).signStreamUrlNaked(mFileToPlay);
            }
            
            mPlayingData = track;
            
            Log.i("CloudPlaybackService", "Opening signed url " + mFileToPlay);
            
            track.putData(Track.key_user_played, "true");
            CloudUtils.resolveTrack(getApplicationContext(), track, true, CloudUtils.getCurrentUserId(this));
            
            
            Log.i("CloudPlaybackService", "opening file " + track.getData(Track.key_duration));
            
           /* if (mFileToPlay.indexOf("/") != 0){
				try {
					URL url = new URL(mFileToPlay);
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			        urlConnection.setRequestMethod("HEAD");
			        urlConnection.setInstanceFollowRedirects(false);
			        urlConnection.connect();
			        mFileToPlay = urlConnection.getHeaderField("Location");
			        Log.i("CloudPlaybackService", "#### location " + urlConnection.getHeaderField("Location"));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }*/
           
            Log.i("CloudPlaybackService","track duration 2 " + mPlayingData.getData(Track.key_duration));

			mPlayer.setDataSourceAsync(mFileToPlay);
            mOneShot = false;
            
            if (track.comments == null){
            	 if (mLoadCommentsTask != null)
                 	mLoadCommentsTask.cancel(true);
            	 
            	mLoadCommentsTask = new PlayerLoadCommentsTask();
                mLoadCommentsTask.track_id = track.getData(Track.key_id);
                mLoadCommentsTask.context = getApplicationContext();
                mLoadCommentsTask.execute();	
            }
            
            if (CloudUtils.isLocalFile(mFileToPlay))
            	mLoadPercent = 100;
            else
            	mLoadPercent = 0;
        }
    }
    
    /**
     * Opens the specified file and readies it for playback.
     *
     * @param path The full path of the file to be opened.
     * @param oneshot when set to true, playback will stop after this file completes, instead
     * of moving on to the next track in the list 
     */
//    public void open(Track track, boolean oneshot) {
//    	
//    	Log.d(LOGTAG, "Opening song " + track.getData(Track.key_title));
//    	
//        synchronized (this) {
//            if (track == null) {
//                return;
//            }
//            
//            if (oneshot) {
//                mRepeatMode = REPEAT_NONE;
//                ensurePlayListCapacity(1);
//                mPlayListLen = 1;
//                mPlayPos = -1;
//            }
//            
//            mPlayListLen = 1;
//            
//            //mPlayListStore.clear();
//            //mPlayListStore.put(track.getData(Song.key_id),track);
//            
//            ensurePlayListCapacity(1);
//            mPlayListLen = 1;
//            mPlayList[0] = track;
//            mPlayPos = 0;
//
//            mPlayingData = track;
//            mFileToPlay = track.getData(Track.key_play_url);
//            
//            mPlayer.setDataSource(mFileToPlay);
//            mOneShot = oneshot;
//            if (CloudUtils.isLocalFile(mFileToPlay))
//            	mLoadPercent = 100;
//            else
//            	mLoadPercent = 0;
//            
//            Log.d(LOGTAG, "Is initialized? " + mPlayer.isInitialized());
//            
//            if (! mPlayer.isInitialized()) {
//                stop(true);
//                if (mOpenFailedCounter++ < 10 &&  mPlayListLen > 1) {
//                    // beware: this ends up being recursive because next() calls open() again.
//                    next(false);
//                }
//                if (! mPlayer.isInitialized() && mOpenFailedCounter != 0) {
//                    // need to make sure we only shows this once
//                    mOpenFailedCounter = 0;
//                    if (!mQuietMode) {
//                        Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
//                    }
//                    Log.d(LOGTAG, "Failed to open file for playback");
//                }
//            } else {
//                mOpenFailedCounter = 0;
//            }
//        }
//    }

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {
    	
    	//mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        //mAudioManager.registerMediaButtonEventReceiver(new ComponentName(this.getPackageName(), MediaButtonIntentReceiver.class.getName()));

    	
    	if (mPlayer.isInitialized()) {
            // if we are at the end of the song, go to the next song first
            long duration = mPlayer.duration();
            if (mRepeatMode != REPEAT_CURRENT && duration > 2000 &&
                mPlayer.position() >= duration - 2000) {
            	Log.i(TAG,"Going to next song, at end of this one");
                next(true);
            }

            mSeekTime = 0;
            
            mPlayer.start();	
            
            
            if (mResumeId != null && mResumeId.contentEquals(mPlayingData.getData(Track.key_id))){
            	mPlayer.seek(mResumeTime);
            	mResumeTime = 0;
            	mResumeId = "";
            }
            
            

            mNotificationView = new RemoteViews(getPackageName(), R.layout.status_play);
            mNotificationView.setImageViewResource(R.id.icon, R.drawable.statusbar);
            mNotificationView.setTextViewText(R.id.trackname, getTrackName());
            mNotificationView.setTextViewText(R.id.username, getUserName());
            mNotificationView.setTextViewText(R.id.progress, "");
            
            Intent i = new Intent(this, ScPlayer.class);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

        } else if (mPlayListLen <= 0) {
            // This is mostly so that if you press 'play' on a bluetooth headset
            // without every having played anything before, it will still play
            // something.
            //setShuffleMode(SHUFFLE_AUTO);
        }
    }
    
    private void stop(boolean remove_status_icon) {
        if (mPlayer.isInitialized()) {
        	
            mPlayer.stop();
        }
        mFileToPlay = null;
        mPlayingData = null;
        
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
        synchronized(this) {
            if (isPlaying()) {
                mPlayer.pause();
                gotoIdleState();
                mIsSupposedToBePlaying = false;
                notifyChange(PLAYSTATE_CHANGED);
                
            }
        }
    }

    /** Returns whether something is currently playing
     *
     * @return true if something is playing (or will be playing shortly, in case
     * we're currently transitioning between tracks), false if not.
     */
    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    /*
      Desired behavior for prev/next/shuffle:

      - NEXT will move to the next track in the list when not shuffling, and to
        a track randomly picked from the not-yet-played tracks when shuffling.
        If all tracks have already been played, pick from the full set, but
        avoid picking the previously played track if possible.
      - when shuffling, PREV will go to the previously played track. Hitting PREV
        again will go to the track played before that, etc. When the start of the
        history has been reached, PREV is a no-op.
        When not shuffling, PREV will go to the sequentially previous track (the
        difference with the shuffle-case is mainly that when not shuffling, the
        user can back up to tracks that are not in the history).

        Example:
        When playing an album with 10 tracks from the start, and enabling shuffle
        while playing track 5, the remaining tracks (6-10) will be shuffled, e.g.
        the final play order might be 1-2-3-4-5-8-10-6-9-7.
        When hitting 'prev' 8 times while playing track 7 in this example, the
        user will go to tracks 9-6-10-8-5-4-3-2. If the user then hits 'next',
        a random track will be picked again. If at any time user disables shuffling
        the next/previous track will be picked in sequential order again.
     */

    public void prev() {
        synchronized (this) {
            if (mOneShot) {
                // we were playing a specific file not part of a playlist, so there is no 'previous'
                seek(0);
                //play();
                return;
            }
            if (mShuffleMode == SHUFFLE_NORMAL) {
                // go to previously-played track and remove it from the history
                int histsize = mHistory.size();
                if (histsize == 0) {
                    // prev is a no-op
                    return;
                }
                Integer pos = mHistory.remove(histsize - 1);
                mPlayPos = pos.intValue();
            } else {
                if (mPlayPos > 0) {
                    mPlayPos--;
                } else {
                    mPlayPos = mPlayListLen - 1;
                }
            }
            
            stop(false);
            openCurrent();
            //play();
            notifyChange(META_CHANGED);
        }
    }

    public void next(boolean force) {
        synchronized (this) {
        	
        	Log.d(LOGTAG, "Next " + mOneShot);
        	
            if (mOneShot) {
                // we were playing a specific file not part of a playlist, so there is no 'next'
                seek(0);
                //play();
                return;
            }

            if (mPlayListLen <= 0) {
                Log.d(LOGTAG, "No play queue");
                return;
            }

            // Store the current file in the history, but keep the history at a
            // reasonable size
            if (mPlayPos >= 0) {
                mHistory.add(Integer.valueOf(mPlayPos));
            }
            if (mHistory.size() > MAX_HISTORY_SIZE) {
                mHistory.removeElementAt(0);
            }

            if (mShuffleMode == SHUFFLE_NORMAL) {
                // Pick random next track from the not-yet-played ones
                // TODO: make it work right after adding/removing items in the queue.

                int numTracks = mPlayListLen;
                int[] tracks = new int[numTracks];
                for (int i=0;i < numTracks; i++) {
                    tracks[i] = i;
                }

                int numHistory = mHistory.size();
                int numUnplayed = numTracks;
                for (int i=0;i < numHistory; i++) {
                    int idx = mHistory.get(i).intValue();
                    if (idx < numTracks && tracks[idx] >= 0) {
                        numUnplayed--;
                        tracks[idx] = -1;
                    }
                }

                // 'numUnplayed' now indicates how many tracks have not yet
                // been played, and 'tracks' contains the indices of those
                // tracks.
                if (numUnplayed <=0) {
                    // everything's already been played
                    if (mRepeatMode == REPEAT_ALL || force) {
                        //pick from full set
                        numUnplayed = numTracks;
                        for (int i=0;i < numTracks; i++) {
                            tracks[i] = i;
                        }
                    } else {
                        // all done
                        gotoIdleState();
                        return;
                    }
                }
                int skip = mRand.nextInt(numUnplayed);
                int cnt = -1;
                while (true) {
                    while (tracks[++cnt] < 0)
                        ;
                    skip--;
                    if (skip < 0) {
                        break;
                    }
                }
                mPlayPos = cnt;
            } else if (mShuffleMode == SHUFFLE_AUTO) {
                doAutoShuffleUpdate();
                mPlayPos++;
            } else {
            	
            	
            	Log.i(TAG,"On last track " + mRepeatMode + " " + force);
                if (mPlayPos >= mPlayListLen - 1) {
                    // we're at the end of the list
                    if (mRepeatMode == REPEAT_NONE && !force) {
                        // all done
                        gotoIdleState();
                        notifyChange(PLAYBACK_COMPLETE);
                        mIsSupposedToBePlaying = false;
                        return;
                    } else if (mRepeatMode == REPEAT_ALL || force) {
                        mPlayPos = 0;
                    }
                } else {
                    mPlayPos++;
                }
            }
            
            stop(false);
            openCurrent();
            //play();
            notifyChange(META_CHANGED);
        }
    }
    
    private void gotoIdleState() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        stopForeground(true);
    }
    

    // Make sure there are at least 5 items after the currently playing item
    // and no more than 10 items before.
    private void doAutoShuffleUpdate() {
        boolean notify = false;
        // remove old entries
        if (mPlayPos > 10) {
            removeTracks(0, mPlayPos - 9);
            notify = true;
        }
        // add new entries if needed
        int to_add = 7 - (mPlayListLen - (mPlayPos < 0 ? -1 : mPlayPos));
        for (int i = 0; i < to_add; i++) {
            // pick something at random from the list
            int idx = mRand.nextInt(mAutoShuffleList.length);
            Track which = mAutoShuffleList[idx];
            ensurePlayListCapacity(mPlayListLen + 1);
            mPlayList[mPlayListLen++] = which;
            notify = true;
        }
        if (notify) {
            notifyChange(QUEUE_CHANGED);
        }
    }

    // A simple variation of Random that makes sure that the
    // value it returns is not equal to the value it returned
    // previously, unless the interval is 1.
    private static class Shuffler {
        private int mPrevious;
        private Random mRandom = new Random();
        public int nextInt(int interval) {
            int ret;
            do {
                ret = mRandom.nextInt(interval);
            } while (ret == mPrevious && interval > 1);
            mPrevious = ret;
            return ret;
        }
    };

   
    
   

    
    /**
     * Removes the range of tracks specified from the play list. If a file within the range is
     * the file currently being played, playback will move to the next file after the
     * range. 
     * @param first The first file to be removed
     * @param last The last file to be removed
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
        	
        	Log.i("CloudPlaybackService","Remove track " + first + " " + last);
            if (last < first) return 0;
            if (first < 0) first = 0;
            if (last >= mPlayListLen) last = mPlayListLen - 1;

            boolean gotonext = false;
            if (first <= mPlayPos && mPlayPos <= last) {
                mPlayPos = first;
                gotonext = true;
            } else if (mPlayPos > last) {
                mPlayPos -= (last - first + 1);
            }
            int num = mPlayListLen - last - 1;
            for (int i = 0; i < num; i++) {
                mPlayList[first + i] = mPlayList[last + 1 + i];
            }
            Log.i("CloudPlaybackService","Old list length " + mPlayListLen);
            mPlayListLen -= last - first + 1;
            Log.i("CloudPlaybackService","New list length " + mPlayListLen);
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
     * Removes all instances of the track with the given id
     * from the playlist.
     * @param id The id to be removed
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
    
   
    
    public void setRepeatMode(int repeatmode) {
        synchronized(this) {
            mRepeatMode = repeatmode;
            saveQueue(false);
        }
    }
    public int getRepeatMode() {
        return mRepeatMode;
    }

    public int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    /**
     * Returns the path of the currently playing file, or null if
     * no file is currently playing.
     */
    public String getPath() {
        return mFileToPlay;
    }
    
    /**
     * Returns the rowid of the currently playing file, or -1 if
     * no file is currently playing.
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
     * @return the position in the queue
     */
    public int getQueuePosition() {
        synchronized(this) {
            return mPlayPos;
        }
    }
    
    /**
     * Starts playing the track at the given position in the queue.
     * @param pos The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int pos) {
        synchronized(this) {
            stop(false);
            mPlayPos = pos;
            openCurrent();
            //play();
            notifyChange(META_CHANGED);
            if (mShuffleMode == SHUFFLE_AUTO) {
                doAutoShuffleUpdate();
            }
        }
    }
    
    
    
    public void setFavoriteStatus(String trackId, String favoriteStatus) {
        synchronized(this) {
        	for (Track track : mPlayList){
        		if (track != null){
        			if (track.getData(Track.key_id).contentEquals(trackId)){
            			track.putData(Track.key_user_favorite, favoriteStatus);
                	}	
        		}        		
        	}
        }
        notifyChange(COMMENTS_LOADED);
    }
    
    public void mapCommentsToTrack(Comment[] comments, String trackId) {
        synchronized(this) {
        	for (Track track : mPlayList){
        		if (track != null){
        			if (track.getData(Track.key_id).contentEquals(trackId)){
            			track.comments = comments;
                	}	
        		}        		
        	}
        }
        notifyChange(COMMENTS_LOADED);
    }
    
    public void addComment(Comment comment) {
        synchronized(this) {
        	
        	for (Track track : mPlayList){
        		if (track != null){
        			Log.i("BLAH","Checking ids | " + track.getData(Track.key_id) + " | " + comment.getData(Comment.key_track_id));
        			if (track.getData(Track.key_id).contentEquals(comment.getData(Comment.key_track_id))){
                	
                		Comment[] oldComments = track.comments;
                		Comment[] newComments = new Comment[oldComments.length + 1];
                		
                		int i = 0;
            			for (Comment oldComment : oldComments){
            				Log.i("BLAH","Transferring comment");
            				newComments[i] = oldComment;
            				i++;
            			}
            			
            			Log.i("BLAH","Putting new comments");
            			newComments[i] = comment;
            			track.comments = newComments;
                	}	
        		}
        	}
        }
    }

    public String getUserName() {
        synchronized(this) {
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
            Log.i(TAG,"Returning play data " + mPlayingData);
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
        	Log.i("CloudPlaybackService","Playing track duration | " + mPlayingData);
            if (mPlayingData == null) {
                return null;
            }
            Log.i("CloudPlaybackService","Playing track duration || " + mPlayingData.getData(Track.key_duration));
            return mPlayingData.getData(Track.key_duration);
        }
    }
    
    /**
     * Returns the duration of the file in milliseconds.
     * Currently this method returns -1 for the duration of MIDI files.
     */
    public long duration() {
    	 synchronized (this) {            
             if (mPlayingData == null) {
                 return -1;
             }
             Log.i("CloudPlaybackService","Playing track duration " + mPlayingData.getData(Track.key_duration));
             return Integer.parseInt(mPlayingData.getData(Track.key_duration));
         }
    }
    
    public String getWaveformUrl() {
        synchronized (this) {            
            if (mPlayingData == null) {
                return null;
            }
            if (mPlayingData.getData(Track.key_local_waveform_url) != "" && mPlayingData.getData(Track.key_download_status).contentEquals(Track.DOWNLOAD_STATUS_DOWNLOADED))
            	return mPlayingData.getData(Track.key_local_waveform_url);
            else
            	return mPlayingData.getData(Track.key_waveform_url);
        }
    }

    
    

    /**
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (mPlayer.isInitialized()) {
        	if (mSeekTime != 0)
        		return mSeekTime;
        	else
        		return mPlayer.position();
        }
        return -1;
    }
    
    /**
     * Returns the duration of the file in milliseconds.
     * Currently this method returns -1 for the duration of MIDI files.
     */
    public int loadPercent() {
        if (mPlayer.isInitialized()) {
            return mLoadPercent;
        }
        return 0;
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     */
    public long seek(long pos) {
        if (mPlayer.isInitialized()) {
        	
            if (pos <= 0) 
            	pos = 0;

            if (pos > mPlayer.duration()) pos = mPlayer.duration();
            return mPlayer.seek(pos);
        }
        return -1;
    }

    /**
     * Provides a unified interface for dealing with midi files and
     * other media files.
     */
    private class MultiPlayer {
        private MediaPlayer mMediaPlayer = new MediaPlayer();
        private Handler mHandler;
        private boolean mIsInitialized = false;

        public MultiPlayer() {
            mMediaPlayer.setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }

        public void setDataSourceAsync(String path) {
            try {
            	
            	if (mMediaPlayer != null){
            		//if (mMediaPlayer.isPlaying())
            			//mMediaPlayer.stop();
            		mMediaPlayer.release();
            		mMediaPlayer = null;
            		System.gc();
            	}
            	
            	Log.i(TAG,"Setting Data Source Async " + path);
            	
            	mMediaPlayer = new MediaPlayer();
            	mMediaPlayer.setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                //mMediaPlayer.reset();
                mMediaPlayer.setDataSource(path);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setOnPreparedListener(preparedlistener);
               	mMediaPlayer.setOnBufferingUpdateListener(bufferinglistener);
               	mMediaPlayer.setOnSeekCompleteListener(seeklistener);
               	
               	Log.i(TAG,"MediaPlayer notifying async change ");
               	notifyChange(ASYNC_OPENING);
               	
               	Log.i(TAG,"MediaPlayer calling async prepare ");
                mMediaPlayer.prepareAsync();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            }
            mMediaPlayer.setOnCompletionListener(listener);
            mMediaPlayer.setOnErrorListener(errorListener);
            
            mIsInitialized = true;
            
            Log.i(TAG,"MediaPlayer done initializing");
        }
        
        public void setDataSource(String path) {
            try {
            	
            	if (mMediaPlayer != null){
            		mMediaPlayer.stop();
            		mMediaPlayer.release();
            	}
            	
            	mMediaPlayer = new MediaPlayer();
            	mMediaPlayer.setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                mMediaPlayer.reset();
                mMediaPlayer.setOnPreparedListener(null);
                mMediaPlayer.setOnBufferingUpdateListener(bufferinglistener);
                if (path.startsWith("content://")) {
                    mMediaPlayer.setDataSource(CloudPlaybackService.this, Uri.parse(path));
                } else {
                    mMediaPlayer.setDataSource(path);
//                    FileDescriptor fd = new FileDescriptor();
//                    
//                    HttpUriRequest req;
//                    req.
//                    
                }
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.prepare();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            }
            mMediaPlayer.setOnCompletionListener(listener);
            mMediaPlayer.setOnErrorListener(errorListener);
            
            mIsInitialized = true;
        }
        
        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void start() {
        	Log.i(TAG,"MEDIA START");
            mMediaPlayer.start();
        }

        public void stop() {
        	Log.i(TAG,"MEDIA STOP");
            mMediaPlayer.reset();
            mIsInitialized = false;
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            mMediaPlayer.release();
        }
        
        public void pause() {
            mMediaPlayer.pause();
        }
        
        public void setHandler(Handler handler) {
            mHandler = handler;
        }
        
    	MediaPlayer.OnBufferingUpdateListener bufferinglistener = new MediaPlayer.OnBufferingUpdateListener() {
			public void onBufferingUpdate(MediaPlayer mp, int percent) {
				Log.i(TAG,"On Buffer " + percent);
				mLoadPercent = percent;
			}
		};
		
		MediaPlayer.OnSeekCompleteListener seeklistener = new MediaPlayer.OnSeekCompleteListener() {
			public void onSeekComplete(MediaPlayer mp) {
				mSeekTime = 0;
				notifyChange(SEEK_COMPLETE);
				Log.i(TAG,"On Seek Complete listener " + mMediaPlayer.isPlaying());
				if (!mMediaPlayer.isPlaying()){
					mMediaPlayer.start();
				}
			}
		};


        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                // Acquire a temporary wakelock, since when we return from
                // this callback the MediaPlayer will release its wakelock
                // and allow the device to go to sleep.
                // This temporary wakelock is released when the RELEASE_WAKELOCK
                // message is processed, but just in case, put a timeout on it.
            	
            	
            	
            	//if (mMediaPlayer.isIniti)
            	//Log.i("MediaPlayer","is it initialized " + mIsInitialized);
            	
            	Log.i(TAG,"Media Player Complete : " + mIsInitialized + "|" + mMediaplayerError + "|" + mReconnectCount);
            	
            	if (mIsInitialized && !mMediaplayerError && mReconnectCount < MAX_RECONNECT_COUNT){
            		
	            		Log.e(TAG, "Media Player complete (no error) " + mMediaPlayer.getCurrentPosition() + " " + mMediaPlayer.getDuration());
		            	if (mMediaPlayer.getDuration() - mMediaPlayer.getCurrentPosition() > 3000 && mPlayingData.getData(Track.key_play_url).indexOf("stream") != -1){
		            		Log.i(TAG,"THIS STREAM JUST DIED ON US, restart it at " + mMediaPlayer.getCurrentPosition() + " | " + mSeekTime);
		            		notifyChange(STREAM_DIED);
		            		mResumeTime = mSeekTime == 0 ? mMediaPlayer.getCurrentPosition() : mSeekTime;
		            		mResumeId = mPlayingData.getData(Track.key_id);
		            		openCurrent(false);
		            		return;
		            	}
            	}
            	
            	Log.i(TAG,"Firing track ended");
            	
                mWakeLock.acquire(30000);
                mHandler.sendEmptyMessage(TRACK_ENDED);
                mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
                
                mPlayingData = null;
            	mMediaplayerError = false;
            }
        };

        MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
            	Log.i(TAG,"MEDIA PLAYER PREPARED " + mAutoPause);
                notifyChange(ASYNC_OPEN_COMPLETE);
                if (!mAutoPause)
                	play();
                else {
                	if (mResumeId.contentEquals(mPlayingData.getData(Track.key_id))){
                    	mPlayer.seek(mResumeTime);
                    	mResumeTime = 0;
                    	mResumeId = "";
                    }
                }
                
                //play();
                /*if (mPlayFromTime != 0){
                    mPlayer.seek(mPlayFromTime);
                    mPlayFromTime = 0;
                }*/
                
            }
        };
 
        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
            	
            	mMediaplayerError = true;
                switch (what) {
                
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                	Log.e(TAG,"MediaplayerError server died");
                	
                    mIsInitialized = false;
                    mMediaPlayer.release();
                    mMediaPlayer = new MediaPlayer(); 
                    mMediaPlayer.setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                    return true;
                default:
                	Log.e(TAG,"MediaplayerError default");
                	
                    mHandler.sendMessage(mHandler.obtainMessage(TRACK_EXCEPTION));
                    break;
                }
                return false;
           }
        };

        public long duration() {
        	Log.i("DURATION","Get duration " + mMediaPlayer.getDuration());
            return mMediaPlayer.getDuration();
        }

        public long position() {
            return mMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
        	Log.i(TAG, "Seek To " + whereto);
        	mSeekTime = whereto;
            mMediaPlayer.seekTo((int) whereto);
            Log.i(TAG, "Returning ");
            return whereto;
        }

        public void setVolume(float vol) {
            mMediaPlayer.setVolume(vol, vol);
        }
    }
    
    
	
    
    
    
    
    
    
    
    
    

    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still
     * has a remote reference to the stub.
     */
    static class ServiceStub extends ICloudPlaybackService.Stub {
        WeakReference<CloudPlaybackService> mService;
        
        ServiceStub(CloudPlaybackService service) {
            mService = new WeakReference<CloudPlaybackService>(service);
        }

       
        public void openFile(Track track, boolean oneShot)
        {
            //mService.get().open(track, oneShot);
        }
        public void open(Track[] list, int position) {
            mService.get().enqueue(list, position);
        }
        public int getQueuePosition() {
            return mService.get().getQueuePosition();
        }
        public void setQueuePosition(int index) {
            mService.get().setQueuePosition(index);
        }
        public boolean isPlaying() {
            return mService.get().isPlaying();
        }
        public void stop() {
            mService.get().stop();
        }
        public void pause() {
            mService.get().pause();
        }
        public void play() {
            mService.get().play();
        }
        public void prev() {
            mService.get().prev();
        }
        public void next() {
            mService.get().next(true);
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
        public void enqueue(Track[] list , int playPos) {
            mService.get().enqueue(list, playPos);
        }
        
//        public ArrayList<HashMap<String,String>> getQueue() {
//            return mService.get().getQueue();
//        }
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
        public void setRepeatMode(int repeatmode) {
            mService.get().setRepeatMode(repeatmode);
        }
        public int getRepeatMode() {
            return mService.get().getRepeatMode();
        }
        public int getMediaMountedCount() {
            return mService.get().getMediaMountedCount();
        }

		public List getQueue() throws RemoteException {
			return mService.get().getQueue();
		}

		public Track getTrack() throws RemoteException {
			return mService.get().getTrack();
		}

		public void setComments(Comment[] commentData, String trackId)
				throws RemoteException {
			mService.get().mapCommentsToTrack(commentData,trackId);
			
		}

		public int removeTrack(String id) throws RemoteException {
			return mService.get().removeTrack(id);
		}

		

		public void enqueueTrack(Track track, int action) throws RemoteException {
			mService.get().enqueueTrack(track, action);
		     
		}
		
		public void clearQueue() throws RemoteException {
			mService.get().clearQueue();
		     
		}
		
		

		public void addComment(Comment commentData) throws RemoteException {
			mService.get().addComment(commentData);
		}
		
		public void setFavoriteStatus(String trackId, String favoriteStatus) throws RemoteException {
			mService.get().setFavoriteStatus(trackId, favoriteStatus);
			
		}

    }
    
    private final IBinder mBinder = new ServiceStub(this);
    
    
    
    class PlayerLoadCommentsTask extends LoadCommentsTask {
    	
    	@Override
    	protected void setComments(Comment[] comments, String trackId){
    		mapCommentsToTrack(comments,trackId);
    	}
    }
    
    
}
