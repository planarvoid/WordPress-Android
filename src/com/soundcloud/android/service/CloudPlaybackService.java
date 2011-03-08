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

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.net.NetworkConnectivityListener;
import com.soundcloud.utils.play.MediaFrameworkChecker;
import com.soundcloud.utils.play.PlayListManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Provides "background" audio playback capabilities, allowing the user to
 * switch between activities without stopping playback. Derived from the Android
 * 2.1 default media player.
 */
public class CloudPlaybackService extends Service {

    private static final String TAG = "CloudPlaybackService";

    public static final int PLAYBACKSERVICE_STATUS = 1;

    public static final int BUFFER_CHECK = 0;

    public static final int BUFFER_FILL_CHECK = 1;

    public static final int START_NEXT_TRACK = 2;

    public static final int RESTART_TRACK = 3;

    public static final String PLAYSTATE_CHANGED = "com.soundcloud.android.playstatechanged";

    public static final String META_CHANGED = "com.soundcloud.android.metachanged";

    public static final String QUEUE_CHANGED = "com.soundcloud.android.queuechanged";

    public static final String PLAYBACK_COMPLETE = "com.soundcloud.android.playbackcomplete";

    public static final String TRACK_ERROR = "com.soundcloud.android.trackerror";

    public static final String STREAM_DIED = "com.soundcloud.android.streamdied";

    public static final String COMMENTS_LOADED = "com.soundcloud.android.commentsloaded";

    public static final String FAVORITE_SET = "com.soundcloud.android.favoriteset";

    public static final String ADD_FAVORITE = "com.soundcloud.android.addfavorite";

    public static final String REMOVE_FAVORITE = "com.soundcloud.android.removefavorite";

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

    private static final int SERVER_DIED = 3;

    private static final int FADEIN = 4;

    private static final int TRACK_EXCEPTION = 5;

    private static final int ACQUIRE_WAKELOCKS = 6;

    private static final int RELEASE_WAKELOCKS = 7;

    private MultiPlayer mPlayer;

    private int mLoadPercent = 0;

    private boolean mAutoPause = false;

    protected NetworkConnectivityListener connectivityListener;

    protected static final int CONNECTIVITY_MSG = 9;

    private PlayListManager mPlayListManager = new PlayListManager(this);

    private Track mPlayingData;

    private StoppableDownloadThread mDownloadThread;

    private boolean mMediaplayerError;

    private RemoteViews mNotificationView;

    private long mResumeTime = -1;

    private long mResumeId = -1;

    private WifiLock mWifiLock;

    private WakeLock mWakeLock;

    private int mServiceStartId = -1;

    private boolean mServiceInUse = false;

    private boolean mResumeAfterCall = false;

    private boolean mIsSupposedToBePlaying = false;

    private boolean mClearToPlay = false;

    private PlayerAppWidgetProvider mAppWidgetProvider = PlayerAppWidgetProvider.getInstance();

    // interval after which we stop the service when idle
    private static final int IDLE_DELAY = 60000;

    private static final int HIGH_WATER_MARK = 8000000;

    private static final int PLAYBACK_MARK = 60000;

    private static final int LOW_WATER_MARK = 20000;

    private boolean pausedForBuffering;

    private boolean initialBuffering = true;

    private long mCurrentBuffer;

    private NetworkInfo mCurrentNetworkInfo;

    private boolean isStagefright = false;

    private int mBufferReportCounter = 0;

    protected int batteryLevel;

    protected int plugState;

    public CloudPlaybackService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Needs to be done in this thread, since otherwise
        // ApplicationContext.getPowerManager() crashes.
        mPlayer = new MultiPlayer();
        mPlayer.setHandler(mMediaplayerHandler);

        // setup connectivity listening
        connectivityListener = new NetworkConnectivityListener();
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);
        connectivityListener.startListening(this);

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        commandFilter.addAction(ADD_FAVORITE);
        commandFilter.addAction(REMOVE_FAVORITE);
        registerReceiver(mIntentReceiver, commandFilter);

        // setup call listening
        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock("wifilock");

        if (Build.VERSION.SDK_INT < 8)
            // 2.1 or earlier, opencore only, no stream seeking
            isStagefright = false;
        else if (Build.VERSION.SDK_INT == 8) {
            // 2.2, check to see if stagefright enabled
            determineSdk8Framework();
        } else { // greater than 2.2, assume stagefright from here on out
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
    }

    /**
     * SDK 8 can be either open core or stagefright. This determines it as best
     * we can
     */
    private void determineSdk8Framework() {
        isStagefright = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                "isStagefright", false);

        // check the build file, works in most cases and will catch cases for
        // instant playback
        try {
            // XXX throws IllegalArgumentException since pathname contains
            // separators
            InputStream instream = openFileInput("/system/build.prop");
            if (instream != null) {
                String line;
                BufferedReader buffreader = new BufferedReader(new InputStreamReader(instream));
                while ((line = buffreader.readLine()) != null) {
                    if (line.contains("media.stagefright.enable-player")) {
                        if (line.contains("true")) {
                            isStagefright = true;
                        }
                        break;
                    }
                }
            }
            instream.close();
        } catch (Exception e) {
            // really need to catch exception here
            Log.e(TAG, "error", e);
        }

        // check through a socket, only way to be sure, but takes a little time
        final MediaFrameworkChecker mfc = new MediaFrameworkChecker();
        mfc.start();
        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    MediaPlayer mp = new MediaPlayer();
                    mp.setDataSource(String.format("http://127.0.0.1:%d/", mfc.getSocketPort()));
                    mp.prepare();
                    mp.start();
                    while (mfc.isAlive()) {
                        Thread.sleep(100);
                    }
                } catch (IOException ignored) {
                } catch (InterruptedException ignored) {
                }
                isStagefright = mfc.isStagefright();
                PreferenceManager.getDefaultSharedPreferences(CloudPlaybackService.this).edit()
                        .putBoolean("isStagefright", isStagefright).commit();
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

        unregisterReceiver(mIntentReceiver);

        if (mWakeLock.isHeld())
            mWakeLock.release();
        mWakeLock = null;

        if (mWifiLock.isHeld())
            mWifiLock.release();
        mWifiLock = null;

        super.onDestroy();
    }

    /*
     * (non-Javadoc)
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

        Log.i(TAG,"ON START COMMAND " + intent.getLongExtra("trackId", -1));

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                next(true);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                if (position() < 2000) {
                    prev(true);
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
            } else if (ADD_FAVORITE.equals(action)) {
                Log.i(TAG,"Got an add fave call " + intent.getLongExtra("trackId", -1));
                setFavoriteStatus(intent.getLongExtra("trackId", -1), true);
            } else if (REMOVE_FAVORITE.equals(action)) {
                Log.i(TAG,"Got a remove fave call " + intent.getLongExtra("trackId", -1));
                setFavoriteStatus(intent.getLongExtra("trackId", -1), false);
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
     * @param storagePath path to mount point for the removed media
     */
    public void closeExternalStorageFiles(String storagePath) {
        // stop playback and clean up if the SD card is going to be unmounted.
        stop(true);
        notifyChange(QUEUE_CHANGED);
    }

    private boolean checkNetworkStatus() {
        if (connectivityListener == null)
            return false;

        mCurrentNetworkInfo = connectivityListener.getNetworkInfo();
        return mCurrentNetworkInfo != null && mCurrentNetworkInfo.isConnected();
    }

    /**
     * Notify the change-receivers that something has changed. The intent that
     * is sent contains the following data for the currently playing track: "id"
     * - Integer: the database row ID "artist" - String: the name of the artist
     * "album" - String: the name of the album "track" - String: the name of the
     * track The intent has an action that is one of
     * "com.soundcloud.android.metachanged"
     * "com.soundcloud.android.queuechanged",
     * "com.soundcloud.android.playbackcomplete"
     * "com.soundcloud.android.playstatechanged" respectively indicating that a
     * new track has started playing, that the playback queue has changed, that
     * playback has stopped because the last file in the list has been played,
     * or that the play-state changed (paused/resumed).
     */
    private void notifyChange(String what) {

        Log.i(TAG, "Notify Change " + what + " " + getTrackId() + " " + isPlaying());
        Intent i = new Intent(what);
        i.putExtra("id", getTrackId());
        i.putExtra("track", getTrackName());
        i.putExtra("user", getUserName());
        i.putExtra("isPlaying", isPlaying());
        if (FAVORITE_SET.equals(what))
            i.putExtra("isFavorite", mPlayingData.user_favorite);
        sendBroadcast(i);

        if (what.equals(QUEUE_CHANGED)) {
            mPlayListManager.saveQueue(true);
        } else {
            mPlayListManager.saveQueue(false);
        }

        // Share this notification directly with our widgets
        mAppWidgetProvider.notifyChange(this, what);
    }

    public void playFromAppCache(int playPos) {
        synchronized (this) {
            mPlayListManager.loadCachedPlaylist(
                    ((SoundCloudApplication) this.getApplication()).flushCachePlaylist(), playPos);
            stopStreaming(null);
            openCurrent();
            mIsSupposedToBePlaying = true;
            setPlayingStatus();
        }
    }

    private void openCurrent() {
        openCurrent(false);
    }

    private void openCurrent(boolean force) {
        Log.i(TAG, "Open Current " + mPlayListManager.getCurrentLength());
        if (mPlayListManager.getCurrentLength() == 0) {
            return;
        }
        openAsync(mPlayListManager.getCurrentTrack(), force);
    }

    Thread mStopThread = null;

    public void openAsync(Track track, boolean force) {
        Log.i(TAG, "TRACK " + track);
        if (track == null) {
            return;
        }

        if (mAutoPause) {
            mAutoPause = false;
            notifyChange(META_CHANGED);
            setPlayingStatus();
        }

        mLoadPercent = 0;
        mCurrentBuffer = 0;

        mClearToPlay = force; //otherwise it will wait for the waveform

        Log.i(TAG, "Playing Data " + mPlayingData);

        // if we are already playing this track
        if (mPlayingData != null && mPlayingData.id == track.id) {

            mStopThread = new StreamStopper(this, mPlayingData.id);
            mStopThread.setPriority(Thread.MAX_PRIORITY);
            mStopThread.start();
            return;

        }

        // stop in a thread so the resetting (or releasing if we are
        // async opening) doesn't holdup the UI
        mStopThread = new StreamStopper(this, track.id);
        mStopThread.setPriority(Thread.MAX_PRIORITY);
        mStopThread.start();

        // new play data
        mPlayingData = track;

        setPlayingStatus();

        // tell the db we played it
        track.user_played = true;

        commitTrackToDb(track);

        // meta has changed
        notifyChange(META_CHANGED);
    }

    private void stopStreaming(Long continueId) {
        synchronized (this) {
            // stop checking buffer
            pausedForBuffering = false;

            mBufferHandler.removeCallbacksAndMessages(BUFFER_CHECK);
            mBufferHandler.removeCallbacksAndMessages(BUFFER_FILL_CHECK);
            mBufferHandler.removeCallbacksAndMessages(START_NEXT_TRACK);

            // stop playing
            if (mPlayer.isInitialized() || mPlayer.isAsyncOpening())
                mPlayer.stop();

            if (CloudUtils.checkThreadAlive(mDownloadThread)
                    && (continueId == null || mDownloadThread.getTrackId() != continueId)) {
                mDownloadThread.interrupt();
                mDownloadThread.stopDownload();

            }
        }

    }

    public void fileLengthUpdated(Track t, boolean changed) {
        if (t.id == mPlayingData.id) {
            if (changed) {

                // stop the track if its playing
                stopStreaming(t.id);

                // get rid of the existing cache file
                if (t.mCacheFile != null && t.mCacheFile.exists()) {
                    if (!t.mCacheFile.delete())
                        Log.w(TAG, "error deleting " + t.mCacheFile);
                }

                commitTrackToDb(t); // save info

                // reopen current track with new data
                openCurrent();
            } else {
                // start checking the buffer
                assertBufferCheck();
                commitTrackToDb(t); // save info
            }
        }
    }

    public void commitTrackToDb(final Track t) {
        new Thread() {
            @Override
            public void run() {
                SoundCloudDB.getInstance().resolveTrack(getContentResolver(), t, WriteState.all,
                        CloudUtils.getCurrentUserId(CloudPlaybackService.this));
            }
        }.start();
    }

    private void startNextTrack() {
        synchronized (this) {
            mStopThread = null;

            if (CloudUtils.isTrackPlayable(mPlayingData)) {

                configureTrackData(mPlayingData);

                notifyChange(INITIAL_BUFFERING);

                if (isStagefright) {
                    pausedForBuffering = true;
                    initialBuffering = true;

                    if (checkNetworkStatus())
                        prepareDownload(mPlayingData);
                    else
                        commitTrackToDb(mPlayingData);

                    // start the buffer check, but not instantly (false)
                    assertBufferCheck(false);

                } else {
                    // commit updated track (user played update only)
                    commitTrackToDb(mPlayingData);
                    mPlayer.setDataSourceAsync(((SoundCloudApplication) getApplication())
                            .signStreamUrlNaked(mPlayingData.stream_url));
                }
                return;
            }

            gotoIdleState();
        }
    }

    private void configureTrackData(Track t) {

        if (t.mCacheFile == null) {
            t.mCacheFile = new File(CloudCache.EXTERNAL_TRACK_CACHE_DIRECTORY
                    + CloudUtils.md5(Long.toString(t.id)));
        }

    }

    private void prepareDownload(final Track trackToCache) {
        synchronized (this) {
            configureTrackData(trackToCache);

            Log.i(TAG, "Prepare Download " + mDownloadThread + " "
                    + (mDownloadThread != null ? mDownloadThread.isAlive() : ""));
            if (mDownloadThread != null && mDownloadThread.isAlive()
                    && trackToCache.id == mDownloadThread.getTrackId()) {
                // we are already downloading this
                return;
            }

            // start downloading if there is a valid connection, otherwise it
            // will happen when we regain connectivity
            if (checkNetworkStatus()) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            trimCache(trackToCache.mCacheFile);
                        } catch (IOException ignored) {
                            Log.w(TAG, "error", ignored);

                        }
                        trackToCache.mCacheFile.setLastModified(System.currentTimeMillis());
                    }
                }.start();
                mMediaplayerHandler.removeMessages(RELEASE_WAKELOCKS);
                mMediaplayerHandler.sendEmptyMessage(ACQUIRE_WAKELOCKS);

                mDownloadThread = new StoppableDownloadThread(this, trackToCache);
                mDownloadThread.setPriority(Thread.MAX_PRIORITY);
                mDownloadThread.start();
            }
        }
    }

    private boolean checkBuffer() {
        synchronized (this) {
            if (mPlayingData == null || getDuration() == 0)
                return false;

            if (mPlayingData.filelength == 0 || mPlayingData.mCacheFile == null) {
                if (CloudUtils.checkThreadAlive(mDownloadThread))
                    return true;
                else {
                    Log.i(TAG, "No Thread, No Cache, send exception");
                    return false;
                }
            }

            if (mPlayer != null && mPlayer.isInitialized()) {
                // normal buffer measurement. size of cache file vs playback
                // position
                mCurrentBuffer = mPlayingData.mCacheFile.length() - mPlayingData.filelength
                        * mPlayer.position() / getDuration();

            } else if (mResumeId == mPlayingData.id && mResumeTime > -1) {
                // resume buffer measurement. if stream died due to lack of a
                // buffer, measure the buffer from where we are supposed to
                // resume
                mCurrentBuffer = mPlayingData.mCacheFile.length() - mPlayingData.filelength
                        * mResumeTime / getDuration();
            } else {
                // initial buffer measurement
                mCurrentBuffer = mPlayingData.mCacheFile.length();
            }

            if (mBufferReportCounter == 10) {
                Log.i(TAG, "[buffer size] " + mCurrentBuffer + " | [cachefile size] "
                        + mPlayingData.mCacheFile.length() + " | [buffer:initialBuffer:wifilock:wakelock]"
                        + pausedForBuffering + ":" + initialBuffering + ":" + mWifiLock.isHeld() + ":" + mWakeLock.isHeld());
                if (CloudUtils.checkThreadAlive(mDownloadThread) && mDownloadThread.lastRead > 0
                        && System.currentTimeMillis() - mDownloadThread.lastRead > 10000) {
                    Log.i(TAG, "Download thread stale, rebooting it ");
                    mDownloadThread.stopDownload();
                    mDownloadThread = null;
                }
                checkBufferStatus();
                mBufferReportCounter = 0;
            } else
                mBufferReportCounter++;

            if (pausedForBuffering) {

                // make sure we are trying to download
                checkBufferStatus();

                // first round of buffering, special case where we set the
                // playback file
                if (mCurrentBuffer > PLAYBACK_MARK
                        || mPlayingData.mCacheFile.length() >= mPlayingData.filelength) {

                    if (!mClearToPlay)
                        return true;

                    pausedForBuffering = false;

                    if (!initialBuffering) {
                        // normal buffering done
                        notifyChange(BUFFERING_COMPLETE);

                        if (mIsSupposedToBePlaying)
                            mPlayer.start();

                    } else {
                        // initial buffering done
                        initialBuffering = false;

                        // set the media player data source
                        if (!mPlayer.getPlayingPath().equalsIgnoreCase(
                                mPlayingData.mCacheFile.getAbsolutePath()))
                            mPlayer.setDataSourceAsync(mPlayingData.mCacheFile.getAbsolutePath());
                        else
                            Log.i(TAG, "Player already set??");
                    }

                } else if (!checkNetworkStatus()) {
                    sendDownloadException();
                    return false;
                }

            } else {
                // if we are under the buffer, but the track is not fully cached
                if (mPlayingData.mCacheFile.length() < mPlayingData.filelength
                        && mCurrentBuffer < PLAYBACK_MARK) {

                    // make sure we are trying to download
                    checkBufferStatus();

                    // normal time to buffer
                    if (mCurrentBuffer < LOW_WATER_MARK) {

                        if (mPlayer.isInitialized()) {
                            mPlayer.pause();
                        }

                        notifyChange(BUFFERING);
                        pausedForBuffering = true;

                    }
                }
            }
        }

        if (mPlayingData.mCacheFile.length() >= mPlayingData.filelength) return false; //no need to keep buffering

        return true;
    }

    // helper for params
    private void assertBufferCheck() {
        assertBufferCheck(true);
    }

    /**
     * queue a buffer check if we aren't paused and call one right away
     * depending on the param
     *
     * @param instant
     */
    private void assertBufferCheck(boolean instant) {
        if (!mAutoPause) {
            if (instant)
                if (!checkBuffer())
                    return;
            queueNextRefresh(500);
        }

    }

    private void trimCache(File keepFile) throws IOException {
        long size = 0;
        final long maxSize = 200000000;

        StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        final long spaceLeft = ((long) fs.getBlockSize())
                * (fs.getAvailableBlocks() - fs.getBlockCount() / 10);

        File cacheDir = new File(CloudCache.EXTERNAL_TRACK_CACHE_DIRECTORY);
        if (cacheDir.exists()) {
            File[] fileList = cacheDir.listFiles();
            if (fileList != null) {
                ArrayList<File> orderedFiles = new ArrayList<File>();
                for (File file : fileList) {
                    if (!file.isDirectory() && (keepFile == null || !file.equals(keepFile))) {
                        size += file.length();
                    }

                    if (orderedFiles.size() == 0) {
                        orderedFiles.add(file);
                    } else {
                        int j = 0;
                        while (j < orderedFiles.size()
                                && (orderedFiles.get(j)).lastModified() < file.lastModified()) {
                            j++;
                        }
                        orderedFiles.add(j, file);
                    }
                }

                Log.i(TAG, "Current Cache Size " + size + " (space left " + spaceLeft + ")");

                if (size > maxSize || spaceLeft < 0) {
                    final long toTrim = Math.max(size - maxSize, Math.abs(spaceLeft));
                    int j = 0;
                    long trimmed = 0;
                    // XXX PUT THIS CODE UNDER TEST (INFINITE LOOPS)
                    while (j < orderedFiles.size() && trimmed < toTrim) {
                        final File moribund = orderedFiles.get(j);
                        if (!moribund.equals(keepFile)) {
                            Log.v(TAG, "Trimming " + moribund);
                            trimmed += moribund.length();
                            if (!moribund.delete()) {
                                Log.w(TAG, "error deleting " + moribund);
                            }
                        }
                        j++;
                    }
                }
            }
        } else {
            if (!cacheDir.mkdirs())
                Log.w(TAG, "error creating " + cacheDir);
        }
    }

    public void sendDownloadException() {
        gotoIdleState();
        mMediaplayerHandler.sendMessage(mMediaplayerHandler.obtainMessage(TRACK_EXCEPTION));

    }

    /*
     * Make sure the buffer is filling if it needs to. If it doesn't see if the
     * next track can be buffered
     */
    private void checkBufferStatus() {
        synchronized (this) {
            // are we able to cache something
            if (!mAutoPause && mPlayingData != null
                    && !CloudUtils.checkThreadAlive(mDownloadThread) && checkNetworkStatus()) {
                // we are able to buffer something, see if we need to download
                // the current track
                if (!checkIfTrackCached(mPlayingData) && keepCaching()) {
                    // need to cache the current track
                    prepareDownload(mPlayingData);
                }
            }
        }
    }

    private boolean checkIfTrackCached(Track track) {
        return (track != null && track.mCacheFile != null && track.filelength > 0 && track.mCacheFile
                .length() >= track.filelength);
    }

    public boolean keepCaching() {
        // we aren't playing and are not supposed to be caching during pause
        if (!mIsSupposedToBePlaying && !mCacheOnPause)
            return false;

        return mCurrentNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI
                || mCurrentBuffer < HIGH_WATER_MARK;
    }

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {
        if (mPlayingData == null)
            return;

        if (mPlayer.isInitialized() && (!isStagefright || mPlayingData.filelength > 0)) {

            if (!isStagefright || mPlayingData.mCacheFile.length() > PLAYBACK_MARK) {

                if (isStagefright)
                    if (mCurrentBuffer < PLAYBACK_MARK) {
                        // we are not allowed to play from wherever we are
                        // mPlayer.seek(0);
                    } else if (mPlayingData != null && mPlayingData.id == mResumeId
                            && mResumeTime > -1) {
                        // we are supposed to resume somewhere in the middle
                        mPlayer.seek(mResumeTime, true);
                        mResumeTime = -1;
                        mResumeId = -1;
                    }

                mPlayer.start();

                if (isStagefright) {
                    assertBufferCheck();
                    // make sure we are downloading if we should be
                    checkBufferStatus();
                }

            } else {
                // we should be buffering
                pausedForBuffering = true;
                notifyChange(BUFFERING);
                assertBufferCheck();
            }
        } else {
            // must have been a playback error
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
    private void setPlayingStatus() {
        if (mPlayingData == null)
            return;
        if (mNotificationView == null) {
            mNotificationView = new RemoteViews(getPackageName(), R.layout.status_play);
            mNotificationView.setImageViewResource(R.id.icon, R.drawable.statusbar);
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

    private boolean mCacheOnPause = true;

    public void pause() {
        pause(false);
    }

    // Pauses playback (call play() to resume)
    public void pause(boolean force) {
        synchronized (this) {
            if (isPlaying()) {
                mCacheOnPause = force
                        || PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                                "bufferOnPause", true);
                if (mPlayer != null && mPlayer.isInitialized()) {
                    mPlayer.pause();
                }
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

    public void prev(boolean force) {
        synchronized (this) {
            if (mPlayListManager.prev())
                openCurrent(force);
        }
    }

    public void prev() {
        prev(false);
    }

    public void next(boolean force) {
        synchronized (this) {
            if (mPlayListManager.next())
                openCurrent(force);
        }
    }

    public void next() {
        next(false);
    }

    /**
     * Replay the current reloaded track. Usually occurs after hitting play
     * after an error
     */
    public void restart() {
        synchronized (this) {
            openCurrent(true);
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
     * @param pos The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int pos) {
        synchronized (this) {
            if (mPlayListManager.setCurrentPosition(pos)) {
                openCurrent();
            }
        }
    }

    public void setClearToPlay(boolean clearToPlay) {
        mClearToPlay = clearToPlay;
    }

    public void setFavoriteStatus(long trackId, boolean favoriteStatus) {
        synchronized (this) {
            Log.i(TAG,"Set Favorite Status " + trackId + " " + mPlayingData.id);
            if (mPlayingData.id == trackId) {
                if (favoriteStatus)
                    addFavorite();
                else
                    removeFavorite();
            }
        }

    }

    public void addFavorite() {
        Log.i(TAG,"Adding favorite");
        FavoriteAddTask f = new FavoriteAddTask((SoundCloudApplication) this.getApplication());
        f.setOnFavoriteListener(new FavoriteTask.FavoriteListener() {
            @Override
            public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
                onFavoriteStatusSet(trackId, isFavorite);
            }

            @Override
            public void onException(long trackId, Exception e) {
                onFavoriteStatusSet(trackId, false); // failed, so it shouldn't
                                                     // be a favorite
            }

        });
        f.execute(mPlayingData);
    }

    public void removeFavorite() {
        Log.i(TAG,"Removing favorite");
        FavoriteRemoveTask f = new FavoriteRemoveTask((SoundCloudApplication) this.getApplication());
        f.setOnFavoriteListener(new FavoriteTask.FavoriteListener() {
            @Override
            public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
                onFavoriteStatusSet(trackId, isFavorite);
            }

            @Override
            public void onException(long trackId, Exception e) {
                onFavoriteStatusSet(trackId, true); // failed, so it should
                                                    // still be a favorite
            }

        });
        f.execute(mPlayingData);
    }

    private void onFavoriteStatusSet(long trackId, boolean isFavorite) {
        Log.i(TAG,"On Favorite Status Set " + trackId + " " + mPlayingData.id);
        if (mPlayingData.id == trackId) {
            mPlayingData.user_favorite = isFavorite;
            notifyChange(FAVORITE_SET);
        }
    }

    public String getUserName() {
        synchronized (this) {
            if (mPlayingData == null) {
                return null;
            }
            return mPlayingData.user.username;
        }
    }

    public String getUserPermalink() {
        synchronized (this) {
            if (mPlayingData == null) {
                return null;
            }
            return mPlayingData.user.permalink;
        }
    }

    public long getTrackId() {
        synchronized (this) {
            if (mPlayingData == null) {
                return -1;
            }
            return mPlayingData.id;
        }
    }

    public boolean getDownloadable() {
        synchronized (this) {
            return mPlayingData != null && mPlayingData.downloadable;
        }
    }

    public Track getTrack() {
        synchronized (this) {
            return mPlayingData == null ? mPlayListManager.getCurrentTrack() : mPlayingData;
        }
    }

    public String getTrackName() {
        synchronized (this) {
            if (mPlayingData == null) {
                return null;
            }
            return mPlayingData.title;
        }
    }

    public int getDuration() {
        synchronized (this) {
            if (mPlayingData == null) {
                return 0;
            }
            return mPlayingData.duration;
        }
    }

    public boolean isBuffering() {
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
            return mPlayingData.duration;
        }
    }

    public String getWaveformUrl() {
        synchronized (this) {
            if (mPlayingData == null) {
                return "";
            }
            return mPlayingData.waveform_url;
        }
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (mPlayer.isInitialized()) {
            return mPlayer.position();
        } else
            return mResumeTime; // either -1 or a valid resume time
    }

    /**
     * Returns the duration of the file in milliseconds. Currently this method
     * returns -1 for the duration of MIDI files.
     */
    public int loadPercent() {
        synchronized (this) {
            if (mPlayer.isInitialized()) {
                if (isStagefright) {
                    if (mPlayingData.mCacheFile == null || mPlayingData.filelength <= 0
                            || mPlayingData.filelength == 0)
                        return 0;

                    return (int) (100 * mPlayingData.mCacheFile.length() / mPlayingData.filelength);
                } else
                    return mLoadPercent;
            }
            return 0;
        }
    }

    public boolean isSeekable() {
        synchronized (this) {
            return (isStagefright && mPlayer.isInitialized() && mPlayingData != null && !mPlayer
                    .isAsyncOpening());
        }
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     */
    public long seek(long pos) {
        synchronized (this) {
            if (mPlayer.isInitialized() && mPlayingData != null && !mPlayer.isAsyncOpening()
                    && isStagefright) {

                if (pos <= 0) {
                    pos = 0;
                }

                return mPlayer.seek(pos);
            }
            return -1;
        }
    }

    /**
     * Gets the actual seek value based on a desired value. Simulates the result
     * of an actual seek
     *
     * @param pos The position to seek to, in milliseconds
     */
    public long getSeekResult(long pos) {
        // Log.i(TAG,"Seek: " + mPlayer.isInitialized() + " " + mPlayingData +
        // " " + mPlayer.isAsyncOpening());
        synchronized (this) {
            if (mPlayer.isInitialized() && mPlayingData != null && !mPlayer.isAsyncOpening()
                    && isStagefright) {

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

        private void refreshMediaplayer() {
            if (mMediaPlayer != null)
                mMediaPlayer.release();

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
            // mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(preparedlistener);
            mMediaPlayer.setOnSeekCompleteListener(seeklistener);
            mMediaPlayer.setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setOnCompletionListener(listener);
            mMediaPlayer.setOnErrorListener(errorListener);

            if (!isStagefright) {
                mMediaPlayer.setOnBufferingUpdateListener(bufferinglistener);
            }

        }

        public void setDataSourceAsync(String path) {
            mPlayingPath = path;
            if (mMediaPlayer == null)
                refreshMediaplayer();

            mIsAsyncOpening = true;

            try {
                if (isStagefright)
                    mMediaPlayer.setDataSource(mPlayingData.mCacheFile.getAbsolutePath());
                else
                    mMediaPlayer.setDataSource(((SoundCloudApplication) CloudPlaybackService.this
                            .getApplication()).signStreamUrlNaked(mPlayingData.stream_url));

                mMediaPlayer.prepareAsync();

            } catch (Exception e) {
                Log.e(TAG, "error", e);
                mIsInitialized = false;
            }
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

            if (mIsAsyncOpening) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            } else {
                try {
                    mMediaPlayer.reset();
                } catch (IllegalStateException e) {
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
            }

        }

        public long position() {
            try {
                return mMediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                return 0;
            }

        }

        public long seek(long whereto) {
            return seek(whereto, false);
        }

        public long seek(long whereto, boolean resumeSeek) {
            mPlayer.setVolume(0);
            whereto = (int) getSeekResult(whereto, resumeSeek);
            mMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public long getSeekResult(long whereto) {
            return getSeekResult(whereto, false);
        }

        public long getSeekResult(long whereto, boolean resumeSeek) {
            long maxSeek;

            if (!resumeSeek) {
                if (mPlayingData.filelength <= 0)
                    return mPlayer.position();
                else
                    maxSeek = getDuration() * mPlayingData.mCacheFile.length()
                            / mPlayingData.filelength - PLAYBACK_MARK / (128 / 8) - 3000;

                // don't go before the playhead if they are trying to seek
                // beyond, just maintain their current position
                if (whereto > mPlayer.position() && maxSeek < mPlayer.position())
                    return mPlayer.position();

                if (whereto > maxSeek) {
                    whereto = maxSeek;
                }
            }
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
                    // checkBufferStatus();
                    mPlayer.setVolume(0);
                    startAndFadeIn();
                    assertBufferCheck();
                }
                notifyChange(SEEK_COMPLETE);

            }
        };

        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {

                Log.i(TAG, "ON COMPLETE ");

                // check for premature track end
                if (mIsInitialized && mPlayingData != null // valid track
                                                           // playing
                        && mMediaPlayer.getDuration() - mMediaPlayer.getCurrentPosition() > 3000) {

                    Log.i(TAG, "ON COMPLETE resume time is " + mMediaPlayer.getCurrentPosition());

                    notifyChange(STREAM_DIED);
                    mResumeTime = mMediaPlayer.getCurrentPosition();
                    mResumeId = mPlayingData.id;
                    openCurrent();
                    return;
                }

                // check for premature track end
                if (mIsInitialized && mPlayingData != null
                        && getDuration() - mMediaPlayer.getCurrentPosition() > 3000) {

                    Log.i(TAG, "ON COMPLETE resetting");

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

                Log.i(TAG, "ON COMPLETE done ");

                // Acquire a temporary wakelock, since when we return from
                // this callback the MediaPlayer will release its wakelock
                // and allow the device to go to sleep.
                // This temporary wakelock is released when the RELEASE_WAKELOCK
                // message is processed, but just in case, put a timeout on it.

                if (!CloudUtils.checkThreadAlive(mDownloadThread)) {
                    mWakeLock.acquire(30000);
                    mHandler.sendEmptyMessage(RELEASE_WAKELOCKS);
                }

                if (!mMediaplayerError)
                    mHandler.sendEmptyMessage(TRACK_ENDED);

                mMediaplayerError = false;
            }
        };

        MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "Media player prepared");
                mIsAsyncOpening = false;
                mIsInitialized = true;

                notifyChange(BUFFERING_COMPLETE);

                if (!mAutoPause) {
                    if (mIsSupposedToBePlaying) {
                        mPlayer.setVolume(0);
                        play();
                        startAndFadeIn();
                    }
                } else {
                    if (mResumeId == mPlayingData.id) {
                        mPlayer.seek(mResumeTime);
                        mResumeTime = -1;
                        mResumeId = -1;
                    }
                }
            }
        };

        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mIsAsyncOpening = false;
                mMediaplayerError = true;

                Log.e(TAG, "MP ERROR " + what + " | " + extra);

                switch (what) {

                    default:
                        mIsInitialized = false;
                        mPlayingPath = "";
                        mMediaPlayer.reset();
                        mHandler.sendMessage(mHandler.obtainMessage(TRACK_EXCEPTION));
                        break;
                }
                return true;
            }
        };

    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                next(true);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                prev(true);
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
            } else if (PlayerAppWidgetProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets,
                // probably
                // because they were just added.
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider.performUpdate(CloudPlaybackService.this, appWidgetIds);
            } else if (ADD_FAVORITE.equals(action)) {
                Log.i(TAG,"Got an add fave call " + intent.getLongExtra("trackId", -1));
                setFavoriteStatus(intent.getLongExtra("trackId", -1), true);
            } else if (REMOVE_FAVORITE.equals(action)) {
                Log.i(TAG,"Got a remove fave call " + intent.getLongExtra("trackId", -1));
                setFavoriteStatus(intent.getLongExtra("trackId", -1), false);
            }
        }
    };

    BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int plugState = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            int rawlevel = intent.getIntExtra("level", -1);
            int scale = intent.getIntExtra("scale", -1);
            int level = -1;
            if (rawlevel >= 0 && scale > 0) {
                level = (rawlevel * 100) / scale;
            }

            CloudPlaybackService.this.batteryLevel = level;
            CloudPlaybackService.this.plugState = plugState;

            Log.i(TAG, "Plugged state: " + plugState);
            Log.i(TAG, "Battery Level Remaining: " + level + "%");
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (ringvolume > 0) {
                    mResumeAfterCall = (isPlaying() || mResumeAfterCall) && mPlayingData != null;
                    pause();
                }
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // pause the music while a conversation is in progress
                mResumeAfterCall = (isPlaying() || mResumeAfterCall) && mPlayingData != null;
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
                case ACQUIRE_WAKELOCKS:
                    if (!mWakeLock.isHeld())
                        mWakeLock.acquire();
                    if (!mWifiLock.isHeld())
                        mWifiLock.acquire();
                    break;
                case RELEASE_WAKELOCKS:
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    if (mWifiLock.isHeld())
                        mWifiLock.release();
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

    public void onDownloadThreadDeath() {
        mMediaplayerHandler.sendEmptyMessageDelayed(RELEASE_WAKELOCKS, 3000);

        if (keepCaching()) {
            Message msg = mBufferHandler.obtainMessage(BUFFER_FILL_CHECK);
            mBufferHandler.removeMessages(BUFFER_FILL_CHECK);
            mBufferHandler.sendMessageDelayed(msg, 100);
        }
    }

    public void queueRestart() {
        if (keepCaching()) {
            Message msg = mBufferHandler.obtainMessage(RESTART_TRACK);
            mBufferHandler.removeMessages(RESTART_TRACK);
            mBufferHandler.sendMessageDelayed(msg, 100);
        }
    }

    /**
     * Connection handler to check
     */
    private Handler connHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (connectivityListener == null)
                        return;
                    if (checkNetworkStatus())
                        checkBufferStatus();
                    break;

            }
        }
    };

    /**
     * Stop the mediaplayer and stream in a thread since it seems to take a bit
     * of time and sometimes when navigating tracks we don't want the UI to have
     * to wait
     */
    private static class StreamStopper extends Thread {

        WeakReference<CloudPlaybackService> serviceRef;

        Long continueStreamingId;

        public StreamStopper(CloudPlaybackService service, Long long1) {
            serviceRef = new WeakReference<CloudPlaybackService>(service);
            this.continueStreamingId = long1;
        }

        @Override
        public void run() {
            serviceRef.get().stopStreaming(continueStreamingId);
            if (serviceRef.get() != null) {
                serviceRef.get().mStopThread = null;
                serviceRef.get().queueNextTrack(0);
            }
        }
    }

    public class StoppableDownloadThread extends Thread {
        private String mURL;

        private HttpGet mMethod = null;

        /*
         * Volatile stop flag used to coordinate state between the two threads
         * involved in this example.
         */
        protected volatile boolean mStopped = false;

        /*
         * Synchronizes access to mMethod to prevent an unlikely race condition
         * when stopDownload() is called before mMethod has been committed.
         */
        private Object lock = new Object();

        WeakReference<CloudPlaybackService> serviceRef;

        private Track track;

        public long lastRead = -1;

        private static final int MODE_NEW = 0;

        private static final int MODE_PARTIAL = 1;

        private static final int MODE_CHECK_COMPLETE = 2;

        private int mode;

        public StoppableDownloadThread(CloudPlaybackService cloudPlaybackService, Track track) {
            serviceRef = new WeakReference<CloudPlaybackService>(cloudPlaybackService);

            try {
                Log.i(TAG,
                        "GETTING WIFI SETTINGS "
                                + Settings.System.getInt(serviceRef.get().getContentResolver(),
                                        Settings.System.WIFI_SLEEP_POLICY));
            } catch (SettingNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Settings.System.putInt(serviceRef.get().getContentResolver(),
                    Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);

            this.track = track;
        }

        public long getTrackId() {
            return track.id;
        }

        public void stopDownload() {
            if (mStopped == true)
                return;

            /*
             * Flag to instruct the downloading thread to halt at the next
             * opportunity.
             */
            mStopped = true;

            /*
             * Interrupt the blocking thread. This won't break out of a blocking
             * I/O request, but will break out of a wait or sleep call. While in
             * this case we know that no such condition is possible, it is
             * always a good idea to include an interrupt to avoid assumptions
             * about the thread in question.
             */
            interrupt();

            /*
             * A synchronized lock is necessary to avoid catching mMethod in an
             * uncommitted state from the download thread.
             */
            synchronized (lock) {
                /*
                 * This closes the socket handling our blocking I/O, which will
                 * interrupt the request immediately. This is not the same as
                 * closing the InputStream yieled by HttpEntity#getContent, as
                 * the stream is synchronized in such a way that would starve
                 * our main thread.
                 */
                if (mMethod != null)
                    mMethod.abort();
            }
        }

        @Override
        public void run() {
            HttpClient cli = new DefaultHttpClient();
            HttpGet method;

            HttpResponse resp = null;
            HttpEntity ent = null;
            InputStream is = null;
            FileOutputStream os = null;

            try {

                if (track.mCacheFile.length() > 0 && track.filelength > 0) {
                    if (track.mCacheFile.length() >= track.filelength) {
                        mode = MODE_CHECK_COMPLETE;

                        HttpHead request = new HttpHead(((SoundCloudApplication) serviceRef.get()
                                .getApplication()).signStreamUrlNaked(track.stream_url));
                        resp = cli.execute(request);

                        if (resp.getStatusLine().getStatusCode() != 200) { // invalid
                            Log.i(TAG, "invalid status received: "
                                    + resp.getStatusLine().getStatusCode());
                            return;
                        }
                        // rare case, file length has changed for some reason
                        if (track.filelength != Long.parseLong(resp.getHeaders("content-length")[0]
                                .getValue())) {
                            serviceRef.get().fileLengthUpdated(track, true); // tell
                        }
                        return;
                    } else {
                        mode = MODE_PARTIAL;
                        method = new HttpGet(((SoundCloudApplication) serviceRef.get()
                                .getApplication()).signStreamUrlNaked(track.stream_url));
                        method.setHeader("Range", "bytes=" + track.mCacheFile.length() + "-"); // get
                    }

                } else {
                    mode = MODE_NEW;
                    method = new HttpGet(
                            ((SoundCloudApplication) serviceRef.get().getApplication())
                                    .signStreamUrlNaked(track.stream_url));
                }

                if (mStopped == true)
                    return;

                synchronized (lock) {
                    mMethod = method;
                }

                resp = cli.execute(mMethod);

                if (mStopped == true)
                    return;

                StatusLine status = resp.getStatusLine();

                switch (mode) {
                    case MODE_NEW:
                        if (status.getStatusCode() != 200) { // invalid
                            // status
                            Log.i(TAG, "invalid status received: " + status.getStatusCode());
                            if (serviceRef.get() != null)
                                serviceRef.get().sendDownloadException();
                            return;
                        }
                        // is the stored length equal to the cached file length
                        // plus
                        track.filelength = Long.parseLong(resp.getHeaders("content-length")[0]
                                .getValue());
                        serviceRef.get().fileLengthUpdated(track, false);

                        break;

                    case MODE_PARTIAL:
                        if (status.getStatusCode() != 206) { // invalid
                            // status
                            Log.i(TAG, "invalid status received: " + status.getStatusCode());
                            if (serviceRef.get() != null)
                                serviceRef.get().sendDownloadException();
                            return;
                        }
                        if (track.filelength != Long.parseLong(resp.getHeaders("content-length")[0]
                                .getValue()) + track.mCacheFile.length()) { // rare
                                                                            // case
                            serviceRef.get().fileLengthUpdated(track, true);
                            return; // a new download thread will be started
                        } else {
                            serviceRef.get().assertBufferCheck();
                        }
                        break;
                }

                if ((ent = resp.getEntity()) != null) {
                    is = ent.getContent();

                    os = new FileOutputStream(track.mCacheFile, true);

                    byte[] b = new byte[2048];
                    int n;
                    long bytes = 0;

                    /*
                     * Note that for most applications, sending a handler
                     * message after each read() would be unnecessary. Instead,
                     * a timed approach should be utilized to send a message at
                     * most every x seconds.
                     */
                    while (serviceRef.get() != null && serviceRef.get().keepCaching()
                            && (n = is.read(b)) >= 0) {
                        bytes += n;
                        os.write(b, 0, n);
                        lastRead = System.currentTimeMillis();
                    }
                }
            } catch (Exception e) {
                /*
                 * We expect a SocketException on cancellation. Any other type
                 * of exception that occurs during cancellation is ignored
                 * regardless as there would be no need to handle it.
                 */
                if (mStopped == false)
                    e.printStackTrace();
            } finally {

                if (is != null)
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                if (os != null)
                    try {
                        os.close();
                    } catch (IOException e) {
                    }

                synchronized (lock) {
                    mMethod = null;
                }

                /* Close the socket (if it's still open) and cleanup. */
                cli.getConnectionManager().shutdown();

                if (serviceRef.get() != null)
                    serviceRef.get().onDownloadThreadDeath();
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
            if (mService.get() != null)
                mService.get().setQueuePosition(index);
        }

        public boolean isPlaying() {
            return mService.get().isPlaying();
        }

        public boolean isSeekable() {
            return mService.get().isSeekable();
        }

        public void stop() {
            if (mService.get() != null)
                mService.get().stop();
        }

        public void pause() {
            if (mService.get() != null)
                mService.get().pause();
        }

        public void forcePause() {
            if (mService.get() != null)
                mService.get().pause(true);
        }

        public void play() {
            if (mService.get() != null)
                mService.get().play();
        }

        public void prev() {
            if (mService.get() != null)
                mService.get().prev();
        }

        public void next() {
            if (mService.get() != null)
                mService.get().next(true);
        }

        public void restart() {
            if (mService.get() != null)
                mService.get().restart();
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

        @Override
        public void playFromAppCache(int playPos) throws RemoteException {
            if (mService.get() != null)
                mService.get().playFromAppCache(playPos);
        }

        public void setFavoriteStatus(long trackId, boolean favoriteStatus) throws RemoteException {
            if (mService.get() != null)
                mService.get().setFavoriteStatus(trackId, favoriteStatus);
        }

        public void setClearToPlay(boolean clearToPlay) throws RemoteException {
            if (mService.get() != null)
                mService.get().setClearToPlay(clearToPlay);
        }

    }

    private final IBinder mBinder = new ServiceStub(this);

}
