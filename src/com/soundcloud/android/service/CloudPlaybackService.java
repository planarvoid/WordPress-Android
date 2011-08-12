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

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.TrackPlays;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.utils.play.PlayListManager;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

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
    private static final int CLEAR_LAST_SEEK = 8;

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
    private boolean mWaitingForArtwork = false;
    private PlayerAppWidgetProvider mAppWidgetProvider = PlayerAppWidgetProvider.getInstance();

    // interval after which we stop the service when idle
    private static final int IDLE_DELAY = 60000;

    // buffer marks
    private static final int HIGH_WATER_MARK = 5000000; // stop buffering when cache is this size
    private static final int LOW_WATER_MARK = 1000000; // restart buffering when cache is this size
    private static final int PLAYBACK_MARK = 200000; // start playing back after buffer pause here
    private static final int INITIAL_PLAYBACK_MARK = 60000; // start initial playback here
    private static final int PAUSE_FOR_BUFFER_MARK = 40000; // pause and wait for buffer

    private static final int MAX_DOWNLOAD_ATTEMPTS = 3;

    private int mCurrentDownloadAttempts;
    private boolean ignoreBuffer;
    private boolean pausedForBuffering;
    private boolean fillBuffer;
    private boolean initialBuffering = true;
    private long mCurrentBuffer;
    private NetworkInfo mCurrentNetworkInfo;
    private boolean mIsStagefright;
    private int mBufferReportCounter = 0;
    protected int batteryLevel;
    protected int plugState;
    protected boolean mHeadphonePluggedState;

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
        registerReceiver(mIntentReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(mIntentReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));


        // setup call listening
        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock("wifilock");

        mIsStagefright = CloudUtils.isStagefright();

        // track information about used audio engine with GA
        getApp().pageTrack(Consts.TrackingEvents.AUDIO_ENGINE,
                "stagefright", mIsStagefright,
                "model",   Build.MODEL,
                "version", Build.VERSION.SDK_INT,
                "release", Build.VERSION.RELEASE,
                "sc_version", CloudUtils.getAppVersion(this, "unknown"));

        Log.d(TAG,"::Using Stagefright Framework " + mIsStagefright);



        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that
        // case.
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);

        mAutoPause = true;
        mPlayListManager.reloadQueue();
        mPlayingData = mPlayListManager.getCurrentTrack();
    }


    @Override
    public void onDestroy() {
        stopStreaming(null);
        gotoIdleState();

        // release all MediaPlayer resources, including the native player and wakelocks
        mPlayer.release();
        mPlayer = null;

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

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                next();
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
            } else if (ADD_FAVORITE.equals(action)) {
                setFavoriteStatus(intent.getLongExtra("trackId", -1), true);
            } else if (REMOVE_FAVORITE.equals(action)) {
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

    private boolean isConnected() {
        if (connectivityListener == null)
            return false;
        mCurrentNetworkInfo = connectivityListener.getNetworkInfo();
        return mCurrentNetworkInfo != null && mCurrentNetworkInfo.isConnected();
    }

    /*
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
            mPlayListManager.loadCachedPlaylist(getApp().flushCachePlaylist(), playPos);
            stopStreaming(null);
            openCurrent();
            mIsSupposedToBePlaying = true;
            setPlayingStatus();
        }
    }

    private void openCurrent() {
        if (mPlayListManager.getCurrentLength() == 0) {
            return;
        }
        openAsync(mPlayListManager.getCurrentTrack());
    }

    Thread mChangeTracksThread = null;

    public void openAsync(final Track track) {

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

        // if we are already playing this track
        if (mPlayingData != null && mPlayingData.id == track.id) {

            mChangeTracksThread = new ChangeTracksAsync(this, mPlayingData);
            mChangeTracksThread.setPriority(Thread.MAX_PRIORITY);
            mChangeTracksThread.start();
            return;
        }

        //otherwise it will wait for the waveform
        mWaitingForArtwork = getApp().playerWaitForArtwork;

        // stop in a thread so the resetting (or releasing if we are
        // async opening) doesn't holdup the UI
        mChangeTracksThread = new ChangeTracksAsync(this, track);
        mChangeTracksThread.setPriority(Thread.MAX_PRIORITY);
        mChangeTracksThread.start();

        // new play data
        mPlayingData = track;
        ignoreBuffer = true;

        setPlayingStatus();

        // tell the db we played it
        track.user_played = true;

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
            if (mPlayer != null && (mPlayer.isInitialized() || mPlayer.isAsyncOpening())) {
                mPlayer.stop();
            }

            if (CloudUtils.checkThreadAlive(mDownloadThread)
                    && (continueId == null || mDownloadThread.getTrackId() != continueId)) {
                mDownloadThread.interrupt();
                mDownloadThread.stopDownload();
            }
        }
    }

    private void fileLengthUpdated(Track t, boolean changed) {
        if (t.id == mPlayingData.id) {
            if (changed) {
                // stop the track if its playing
                stopStreaming(t.id);

                t.deleteCache();

                mPlayListManager.commitTrackToDb(t);

                // reopen current track with new data
                openCurrent();
            } else {
                // start checking the buffer
                assertBufferCheck();
                mPlayListManager.commitTrackToDb(t);
            }
        } else if (changed) {
            t.deleteCache();
        }
    }

    private void startNextTrack() {
        synchronized (this) {

            mChangeTracksThread = null;
            mCurrentDownloadAttempts = 0;

            if (mPlayingData.isStreamable()) {
                notifyChange(INITIAL_BUFFERING);
                initialBuffering = true;
                if (mIsStagefright) {
                    pausedForBuffering = initialBuffering = fillBuffer = true;
                    ignoreBuffer = false;

                    if (mPlayingData.filelength == 0 && mPlayingData.getCache().length() > 0) {
                        mPlayingData.deleteCache();
                    }

                    if (isConnected()) {
                        prepareDownload(mPlayingData);
                    } else {
                        mPlayListManager.commitTrackToDb(mPlayingData);
                    }

                    // start the buffer check, but not instantly (false)
                    assertBufferCheck(false);
                } else { // !stageFright
                    // commit updated track (user played update only)
                    mPlayListManager.commitTrackToDb(mPlayingData);
                    // need to resolve stream url, because f***ing mediaplayer doesn't handle https
                    setResolvedStreamSourceAsync(mPlayingData.stream_url, mMediaplayerHandler);
                }
            } else {
                gotoIdleState();
            }
        }
    }

    private void setResolvedStreamSourceAsync(final String url, final Handler handler) {
        new Thread() {
            @Override
            public void run() {
                try {
                    HttpResponse resp = getApp().get(Request.to(url));

                    if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                        final Header location = resp.getFirstHeader("Location");
                        if (location != null && location.getValue() != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mPlayer != null) {
                                        mPlayer.setDataSourceAsync(location.getValue());
                                    }
                                }
                            });
                            return;
                        } else {
                            Log.w(TAG, "no location header found");
                        }
                    } else {
                        Log.w(TAG, "unexpected response " + resp);
                    }
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // set with original url, at least we will get proper error handling
                        if (mPlayer != null) {
                            mPlayer.setDataSourceAsync(url);
                        }
                    }
                });

            }
        }.start();
    }


    private void prepareDownload(final Track trackToCache) {
        synchronized (this) {
            if (mDownloadThread != null && mDownloadThread.isAlive()
                    && trackToCache.id == mDownloadThread.getTrackId()) {
                // we are already downloading this
                return;
            }

            trackToCache.createCache();

            // start downloading if there is a valid connection, otherwise it
            // will happen when we regain connectivity
            if (isConnected()) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            if (!TrackCache.trim(trackToCache.getCache(),
                                    Consts.EXTERNAL_TRACK_CACHE_DIRECTORY)) {
                                // TODO move outside of thread
                                CloudUtils.mkdirs(Consts.EXTERNAL_CACHE_DIRECTORY);
                                Log.w(TAG, "error trimming cache");
                            }
                        } catch (IOException ignored) {
                            Log.w(TAG, "error trimming cache", ignored);

                        }
                        trackToCache.touchCache();
                    }
                }.start();
                mMediaplayerHandler.removeMessages(RELEASE_WAKELOCKS);
                mMediaplayerHandler.sendEmptyMessage(ACQUIRE_WAKELOCKS);

                mCurrentDownloadAttempts++;

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

            if (mPlayingData.filelength == 0 || !mPlayingData.getCache().exists()) {
                if (CloudUtils.checkThreadAlive(mDownloadThread) || ignoreBuffer)
                    return true;
                else {
                    Log.i(TAG, "No Thread, No Cache, send exception");
                    sendPlaybackException();
                    return false;
                }
            }

            if (mPlayer != null && mPlayer.isInitialized()) {
                // normal buffer measurement. size of cache file vs playback
                // position
                mCurrentBuffer = mPlayingData.getCache().length() - mPlayingData.filelength
                        * mPlayer.position() / getDuration();

            } else if (mResumeId == mPlayingData.id && mResumeTime > -1) {
                // resume buffer measurement. if stream died due to lack of a
                // buffer, measure the buffer from where we are supposed to
                // resume
                mCurrentBuffer = mPlayingData.getCache().length() - mPlayingData.filelength
                        * mResumeTime / getDuration();
            } else {
                // initial buffer measurement
                mCurrentBuffer = mPlayingData.getCache().length();
            }

            if (mBufferReportCounter == 20) {
                Log.i(TAG, "[buffer size] " + mCurrentBuffer + " [cachefile] "
                        + mPlayingData.getCache().length() + " [waiting]"
                        + pausedForBuffering);
                //Log.i(TAG,"2 " + CloudUtils.checkThreadAlive(mDownloadThread) + " " + (mDownloadThread == null ? "" : mDownloadThread.lastRead));
                if (CloudUtils.checkThreadAlive(mDownloadThread) && mDownloadThread.lastRead > 0
                        && System.currentTimeMillis() - mDownloadThread.lastRead > 10000) {
                    Log.i(TAG, "Download thread stale, killing it ");
                    mDownloadThread.stopDownload();
                    mDownloadThread = null;
                }
                mBufferReportCounter = 0;
            } else
                mBufferReportCounter++;

            if (pausedForBuffering) {

                // make sure we are trying to download
                checkBufferStatus();

                // first round of buffering, special case where we set the
                // playback file
                if ((mCurrentBuffer > INITIAL_PLAYBACK_MARK && initialBuffering) || (mCurrentBuffer > PLAYBACK_MARK && !initialBuffering)
                        || mPlayingData.getCache().length() >= mPlayingData.filelength) {


                    if (mWaitingForArtwork)
                        return true;

                    pausedForBuffering = false;

                    if (!initialBuffering) {
                        // normal buffering done
                        notifyChange(BUFFERING_COMPLETE);

                        if (mIsSupposedToBePlaying && mPlayer != null) {
                            mPlayer.start();
                        }

                    } else {
                        // initial buffering done
                        initialBuffering = false;

                        // set the media player data source
                        if (mPlayer != null && !mPlayer.getPlayingPath()
                                .equalsIgnoreCase(mPlayingData.getCache().getAbsolutePath())) {
                            mPlayer.setDataSourceAsync(mPlayingData.getCache().getAbsolutePath());
                        } else {
                            notifyChange(BUFFERING_COMPLETE);
                        }
                    }

                } else if (!isConnected()) {
                    Log.i(TAG,"Paused for buffering, no network, send error");
                    sendPlaybackException();
                    return false;
                }

            } else {
                // if we are under the buffer, but the track is not fully cached
                if (mPlayingData.getCache().length() < mPlayingData.filelength
                        && mCurrentBuffer < LOW_WATER_MARK) {

                    // make sure we are trying to download
                    fillBuffer = true;
                    checkBufferStatus();

                    // normal time to buffer
                    if (mCurrentBuffer < PAUSE_FOR_BUFFER_MARK) {

                        if (mPlayer != null && mPlayer.isInitialized()) {
                            mPlayer.pause();
                        }

                        notifyChange(BUFFERING);
                        pausedForBuffering = true;

                    }
                }
            }
        }
        return mPlayingData.getCache().length() < mPlayingData.filelength;
    }

    // helper for params
    private void assertBufferCheck() {
        assertBufferCheck(true);
    }

    /*
     * queue a buffer check if we aren't paused and call one right away
     * depending on the param
     */
    private void assertBufferCheck(boolean instant) {
        if (!mAutoPause) {
            if (instant)
                if (!checkBuffer())
                    return;
            queueNextRefresh(500);
        }
    }

    public void sendPlaybackException() {
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
                    && !CloudUtils.checkThreadAlive(mDownloadThread) && isConnected()) {
                // download thread is dead and doesn't have to be
                if (!checkIfTrackCached(mPlayingData) && keepCaching()) {
                    if (mCurrentDownloadAttempts < MAX_DOWNLOAD_ATTEMPTS){
                        prepareDownload(mPlayingData);
                    } else if (pausedForBuffering){
                        sendPlaybackException();
                    }
                }
            }
        }
    }

    private boolean checkIfTrackCached(Track track) {
        return (track != null && track.isCached());
    }

    public boolean keepCaching() {
        // we aren't playing and are not supposed to be caching during pause
        if (!mIsSupposedToBePlaying && !mCacheOnPause) {
            return false;
        } else {
            if (fillBuffer && mCurrentBuffer > HIGH_WATER_MARK) fillBuffer = false;
            return mCurrentNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI || fillBuffer;
        }
    }

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {
        if (mPlayingData == null)
            return;

        boolean wasPlaying = mIsSupposedToBePlaying;
        mIsSupposedToBePlaying = true;


        mCurrentDownloadAttempts = 0; //reset errors, user may be manually trying again after a download error

        if (mPlayer != null && mPlayer.isInitialized() && (!mIsStagefright || mPlayingData.filelength > 0)) {

            if (!mIsStagefright || mPlayingData.getCache().length() > PLAYBACK_MARK) {

                if (mIsStagefright)
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

                if (mIsStagefright) {
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

        if (!wasPlaying) {
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
            mNotificationView = new RemoteViews(getPackageName(), R.layout.playback_service_status_play);
            mNotificationView.setImageViewResource(R.id.icon, R.drawable.statusbar);
        }

        mNotificationView.setTextViewText(R.id.trackname, getTrackName());
        mNotificationView.setTextViewText(R.id.username, getUserName());
        mNotificationView.setTextViewText(R.id.progress, "");

        Intent intent = new Intent(Actions.PLAYER);
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);

        Notification status = new Notification();
        status.contentView = mNotificationView;
        status.flags |= Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.statusbar;
        status.contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        startForeground(PLAYBACKSERVICE_STATUS, status);
    }

    private void stop(boolean remove_status_icon) {
        if (mPlayer != null && mPlayer.isInitialized()) {
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
                gotoIdleState(false);
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

    public void next() {
        synchronized (this) {
            if (mPlayListManager.next())
                openCurrent();
        }
    }

    /**
     * Replay the current reloaded track. Usually occurs after hitting play
     * after an error
     */
    public void restart() {
        synchronized (this) {
            openCurrent();
        }
    }

    private void gotoIdleState() {
        gotoIdleState(true);
    }

    private void gotoIdleState(boolean killBuffer) {
        if (killBuffer) {
            mBufferHandler.removeMessages(BUFFER_CHECK);
            initialBuffering = false;
            pausedForBuffering = false;
        }

        mIsSupposedToBePlaying = false;
        mBufferHandler.removeMessages(BUFFER_FILL_CHECK);
        mBufferHandler.removeMessages(START_NEXT_TRACK);
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
        mWaitingForArtwork = !clearToPlay;
    }

    public void setFavoriteStatus(long trackId, boolean favoriteStatus) {
        synchronized (this) {
            if (mPlayingData != null && mPlayingData.id == trackId) {
                if (favoriteStatus) {
                    addFavorite();
                } else {
                    removeFavorite();
                }
            }
        }
    }

    public void addFavorite() {
        onFavoriteStatusSet(mPlayingData.id, true);
        FavoriteAddTask f = new FavoriteAddTask(getApp());
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
        onFavoriteStatusSet(mPlayingData.id, false);
        FavoriteRemoveTask f = new FavoriteRemoveTask(getApp());
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
        if (mPlayingData.id == trackId && mPlayingData.user_favorite != isFavorite) {
            mPlayingData.user_favorite = isFavorite;
            notifyChange(FAVORITE_SET);
        }
    }

    public String getUserName() {
        synchronized (this) {
            if (mPlayingData != null && mPlayingData.user != null) {
                return mPlayingData.user.username;
            } else return null;
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
            return pausedForBuffering || initialBuffering;
        }
    }

    /*
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

    /*
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (mPlayer != null && mPlayer.isInitialized()) {
            return mPlayer.position();
        } else
            return mResumeTime; // either -1 or a valid resume time
    }

    /*
     * Returns the duration of the file in milliseconds. Currently this method
     * returns -1 for the duration of MIDI files.
     */
    public int loadPercent() {
        synchronized (this) {
            if (mPlayer != null && mPlayer.isInitialized()) {
                if (mIsStagefright) {
                    if (!mPlayingData.getCache().exists() || mPlayingData.filelength <= 0 || mPlayingData.filelength == 0) {
                        return 0;
                    } else {
                        return (int) (100 * mPlayingData.getCache().length() / mPlayingData.filelength);
                    }
                } else
                    return mLoadPercent;
            }
            return 0;
        }
    }

    public boolean isSeekable() {
        synchronized (this) {
            return ((mIsStagefright || Build.VERSION.SDK_INT > 8)
                    && mPlayer != null
                    && mPlayer.isInitialized()
                    && mPlayingData != null
                    && !mPlayer.isAsyncOpening());
        }
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     * @return the new pos, or -1
     */
    public long seek(long pos) {
        synchronized (this) {
            if (mPlayer != null && mPlayer.isInitialized() && mPlayingData != null && !mPlayer.isAsyncOpening()
                    && (mIsStagefright || Build.VERSION.SDK_INT > 8)) {

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
     * @return the new pos, or -1
     */
    public long getSeekResult(long pos) {
        synchronized (this) {
            if (mPlayer != null && mPlayer.isInitialized() && mPlayingData != null && !mPlayer.isAsyncOpening()
                    && (mIsStagefright || Build.VERSION.SDK_INT > 8)) {

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
        private boolean mIsInitialized;
        private boolean mIsAsyncOpening;
        private String mPlayingPath = "";


        public MultiPlayer() {
            refreshMediaplayer();
        }

        private void refreshMediaplayer() {
            if (mMediaPlayer != null) mMediaPlayer.release();
            mMediaPlayer = new MediaPlayer() {
                {
                    setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                    setAudioStreamType(AudioManager.STREAM_MUSIC);
                    setOnPreparedListener(preparedlistener);
                    setOnSeekCompleteListener(seeklistener);
                    setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                    setOnCompletionListener(listener);
                    setOnErrorListener(errorListener);
                    if (!mIsStagefright) {
                        setOnBufferingUpdateListener(bufferinglistener);
                    }
                }
            };
        }

        public void setDataSourceAsync(String path) {
            if (mMediaPlayer == null) refreshMediaplayer();
            mIsAsyncOpening = true;

            try {
                if (mIsStagefright) {
                    mPlayingPath = mPlayingData.getCache().getAbsolutePath();
                } else {
                    mPlayingPath = path;
                }

                mMediaPlayer.setDataSource(mPlayingPath);
                mMediaPlayer.prepareAsync();
            } catch (IllegalStateException e) {
                Log.e(TAG, "error", e);
                mIsInitialized = false;
            } catch (IOException e) {
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
            if (mPlayer == null) return -1;

            if (mIsStagefright) {
                mPlayer.setVolume(0);
                whereto = (int) getSeekResult(whereto, resumeSeek);
            }
            if (whereto != mPlayer.position()) {
                mMediaPlayer.seekTo((int) whereto);
            }
            return whereto;
        }

        public long getSeekResult(long whereto) {
            return getSeekResult(whereto, false);
        }

        public long getSeekResult(long whereto, boolean resumeSeek) {
            if (mPlayer == null) return -1;
            long maxSeek;
            if (!resumeSeek) {
                if (mPlayingData.filelength <= 0) {
                    return mPlayer.position();
                } else {
                    maxSeek = currentMaxSeek();
                }

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

        private long currentMaxSeek(){
            if (mPlayingData.filelength <= mPlayingData.getCache().length()) {
                return getDuration();
            } else {
                return Math.round(getDuration() * (loadPercent() - .1)/100) - PLAYBACK_MARK / (128 / 8);
            }
        }

        public void setVolume(float vol) {
            if (mMediaPlayer != null) {
                try {
                    mMediaPlayer.setVolume(vol, vol);
                } catch (IllegalStateException ignored) {
                    Log.w(TAG, ignored);
                }
            }
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
                if (!mIsStagefright) return;

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

                // keep the last seek time for 3000 ms because getCurrentPosition will be incorrect at first
                Message msg = mMediaplayerHandler.obtainMessage(CLEAR_LAST_SEEK);
                mMediaplayerHandler.removeMessages(CLEAR_LAST_SEEK);
                mMediaplayerHandler.sendMessageDelayed(msg,3000);
                notifyChange(SEEK_COMPLETE);

            }
        };

        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                // check for premature track end
                if (mIsInitialized && mPlayingData != null
                        && getDuration() - mMediaPlayer.getCurrentPosition() > 3000) {

                    mResumeTime = mMediaPlayer.getCurrentPosition();

                    mMediaPlayer.reset();
                    mIsInitialized = false;
                    mPlayingPath = "";

                    if (isConnected() && mCurrentDownloadAttempts < MAX_DOWNLOAD_ATTEMPTS)
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
                mIsAsyncOpening = false;
                mIsInitialized = true;
                initialBuffering = false;
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

                SoundCloudApplication.handleSilentException("mp error",
                        new MediaPlayerException(what, extra, mCurrentNetworkInfo, mIsStagefright));

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

            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                boolean oldPlugged = mHeadphonePluggedState;
                mHeadphonePluggedState = intent.getIntExtra("state", 0) != 0;
                if (mHeadphonePluggedState != oldPlugged && !mHeadphonePluggedState
                        && mIsSupposedToBePlaying)
                    pause();

            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int plugState = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                int rawlevel = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", -1);
                int level = -1;
                if (rawlevel >= 0 && scale > 0) {
                    level = (rawlevel * 100) / scale;
                }

                CloudPlaybackService.this.batteryLevel = level;
                CloudPlaybackService.this.plugState = plugState;

                Log.i(TAG, "Battery Level Remaining: " + level + "%");

            } else {
                if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                    next();
                } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                    prev();
                } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                    if (isPlaying()) {
                        pause();
                    } else if (mPlayingData != null){
                        play();
                    } else {
                        openCurrent();
                        mIsSupposedToBePlaying = true;
                        setPlayingStatus();
                    }
                } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                    pause();
                } else if (CMDSTOP.equals(cmd)) {
                    pause();
                    seek(0);
                } else if (PlayerAppWidgetProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
                    // Someone asked us to refresh a set of specific widgets,
                    // probably because they were just added.
                    int[] appWidgetIds = intent
                            .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                    mAppWidgetProvider.performUpdate(CloudPlaybackService.this, appWidgetIds);
                } else if (ADD_FAVORITE.equals(action)) {
                    setFavoriteStatus(intent.getLongExtra("trackId", -1), true);
                } else if (REMOVE_FAVORITE.equals(action)) {
                    setFavoriteStatus(intent.getLongExtra("trackId", -1), false);
                }
            }
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
                        sendEmptyMessageDelayed(FADEIN, 10);
                    } else {
                        mCurrentVolume += 0.01f;
                        if (mCurrentVolume < 1.0f) {
                            sendEmptyMessageDelayed(FADEIN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        mPlayer.setVolume(mCurrentVolume);
                    }
                    break;
                case SERVER_DIED:
                    if (mIsSupposedToBePlaying) {
                        next();
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
                    next();
                    break;
                case ACQUIRE_WAKELOCKS:
                    if (!mWakeLock.isHeld()) mWakeLock.acquire();
                    if (!mWifiLock.isHeld()) mWifiLock.acquire();
                    break;
                case RELEASE_WAKELOCKS:
                    if (mWakeLock.isHeld()) mWakeLock.release();
                    if (mWifiLock.isHeld()) mWifiLock.release();
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

    public void onDownloadThreadDeath(StoppableDownloadThread thread) {
        if (mMediaplayerHandler != null) {
            mMediaplayerHandler.sendEmptyMessageDelayed(RELEASE_WAKELOCKS, 3000);
        }
        if (thread == mDownloadThread) mDownloadThread = null;

        if (mBufferHandler != null && keepCaching()) {
            Message msg = mBufferHandler.obtainMessage(BUFFER_FILL_CHECK);
            mBufferHandler.removeMessages(BUFFER_FILL_CHECK);
            mBufferHandler.sendMessageDelayed(msg, 100);
        }


        if (thread != null) {
            if (thread.statusLine != null &&
                thread.statusLine.getStatusCode() != HttpStatus.SC_OK) {
                SoundCloudApplication.handleSilentException("invalid status",
                        new StatusException(thread.statusLine, mCurrentNetworkInfo, mIsStagefright));
            } else if (thread.exception != null) {
                SoundCloudApplication.handleSilentException("io exception",
                        new PlaybackError(thread.exception, mCurrentNetworkInfo, mIsStagefright));
            }
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
                    if (mIsStagefright && isConnected()) {
                        checkBufferStatus();
                    }
                    break;
            }
        }
    };

    /**
     * Stop the mediaplayer and stream in a thread since it seems to take a bit
     * of time and sometimes when navigating tracks we don't want the UI to have
     * to wait
     */
    private static class ChangeTracksAsync extends Thread {
        private WeakReference<CloudPlaybackService> serviceRef;
        private Track nextTrack;

        public ChangeTracksAsync(CloudPlaybackService service, Track track) {
            serviceRef = new WeakReference<CloudPlaybackService>(service);
            nextTrack = track;
        }

        @Override
        public void run() {
            CloudPlaybackService svc = serviceRef.get();
            if (svc == null) return;

            final User user = svc.getApp().getLoggedInUser();

            svc.stopStreaming(nextTrack.id);

            Cursor cursor = svc.getContentResolver().query(Content.TRACK_PLAYS, null,
                    TrackPlays.TRACK_ID + " = ?", new String[]{
                    Long.toString(nextTrack.id)
            }, null);

            if (cursor == null || cursor.getCount() == 0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(TrackPlays.TRACK_ID, nextTrack.id);
                contentValues.put(TrackPlays.USER_ID, user.id);
                svc.getContentResolver().insert(Content.TRACK_PLAYS, contentValues);
            }
            if (cursor != null) cursor.close();

            nextTrack.updateFromDb(svc.getContentResolver(), user);

            if (svc.getApp().getTrackFromCache(nextTrack.id) == null) {
                svc.getApp().cacheTrack(nextTrack);
            }

            svc.mChangeTracksThread = null;
            svc.queueNextTrack(0);
        }
    }

    // XXX should be static / outer class
    public class StoppableDownloadThread extends Thread {
        private static final int MODE_NEW = 0;
        private static final int MODE_PARTIAL = 1;
        private static final int MODE_CHECK_COMPLETE = 2;

        private HttpGet mMethod;
        protected volatile boolean mStopped;
        private final Object lock = new Object();

        WeakReference<CloudPlaybackService> serviceRef;

        private Track track;
        public long lastRead = -1;
        private long bytes;
        private int mode;

        public IOException exception;
        public StatusLine statusLine;

        public StoppableDownloadThread(CloudPlaybackService cloudPlaybackService, Track track) {
            serviceRef = new WeakReference<CloudPlaybackService>(cloudPlaybackService);
            this.track = track;
        }

        public long getTrackId() {
            return track.id;
        }

        public void stopDownload() {
            if (mStopped) return;

            mStopped = true;
            interrupt();

            /*
             * A synchronized lock is necessary to avoid catching mMethod in an
             * uncommitted state from the download thread.
             */
            synchronized (lock) {
                /*
                 * This closes the socket handling our blocking I/O, which will
                 * interrupt the request immediately. This is not the same as
                 * closing the InputStream yielded by HttpEntity#getContent, as
                 * the stream is synchronized in such a way that would starve
                 * our main thread.
                 */
                if (mMethod != null) mMethod.abort();
            }
        }

        @Override
        public void run() {
            if (!track.isStreamable()) return;

            CloudPlaybackService svc = serviceRef.get();
            if (svc == null) return;
            SoundCloudApplication app = getApp();

            // 2.1 only
            AndroidHttpClient cli = AndroidHttpClient.newInstance(CloudAPI.USER_AGENT);

            HttpGet method;

            HttpResponse resp;
            FileOutputStream os = null;
            InputStream is = null;

            try {
                resp = app.get(Request.to(track.stream_url));
                if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
                  Log.w(TAG, "invalid status received: " + resp.getStatusLine().getStatusCode());
                  return;
                }

                Header location = resp.getFirstHeader("Location");
                if (location == null) {
                    Log.w(TAG, "no location header found");
                    return;
                }

                final String streamUrl = location.getValue();

                if (track.getCache().length() > 0 && track.filelength > 0) {

                    if (track.getCache().length() >= track.filelength) {
                        mode = MODE_CHECK_COMPLETE;

                        // XXX HttpHead logs extra play
                        HttpUriRequest request = new HttpHead(streamUrl);
                        resp = cli.execute(request);

                        statusLine = resp.getStatusLine();

                        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                            Log.i(TAG, "invalid status received: " + statusLine.toString());
                            return;
                        }

                        // rare case, file length has changed for some reason
                        if (track.filelength != getContentLength(resp)) {
                            serviceRef.get().fileLengthUpdated(track, true); // tell
                        }
                        return;
                    } else {
                        Log.d(TAG, "Resuming partial download of " + track.title +
                                String.format(" (file: %d, track: %d)", track.getCache().length(), track.filelength));

                        mode = MODE_PARTIAL;
                        method = new HttpGet(streamUrl);
                        method.setHeader("Range", "bytes=" + track.getCache().length() + "-");
                    }
                } else {
                    mode = MODE_NEW;
                    method = new HttpGet(streamUrl);
                }

                if (mStopped) return;

                synchronized (lock) {
                    mMethod = method;
                }

                resp = cli.execute(mMethod);

                if (mStopped) return;

                 statusLine = resp.getStatusLine();

                if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                    Log.i(TAG, "invalid status received: " + statusLine.toString());
                    return;
                }
                switch (mode) {
                    case MODE_NEW:
                        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                            Log.d(TAG, "invalid status received: " + statusLine.toString());
                            return;
                        }
                        // is the stored length equal to the cached file length plus
                        track.filelength = getContentLength(resp);
                        Log.d(TAG, "reported track length: " + track.filelength);
                        serviceRef.get().fileLengthUpdated(track, false);
                        break;

                    case MODE_PARTIAL:
                        if (statusLine.getStatusCode() != HttpStatus.SC_PARTIAL_CONTENT) {
                            Log.d(TAG, "invalid status received: " + statusLine.toString());
                            return;
                        }
                        if (track.filelength != getContentLength(resp) + track.getCache().length()) {
                            // rare case
                            serviceRef.get().fileLengthUpdated(track, true);
                            return; // a new download thread will be started
                        } else {
                            serviceRef.get().assertBufferCheck();
                        }
                        break;
                }


                HttpEntity ent = resp.getEntity();
                if (ent != null) {
                    is = ent.getContent();
                    os = new FileOutputStream(track.getCache(), true);

                    // reset download counter. if we got here, we have a successful connection
                    mCurrentDownloadAttempts = 0;

                    byte[] b = new byte[2048];
                    int n;

                    while (serviceRef.get() != null &&
                            serviceRef.get().keepCaching()
                            && (n = is.read(b)) >= 0) {
                        bytes += n;
                        os.write(b, 0, n);
                        lastRead = System.currentTimeMillis();
                    }
                }
            } catch (IOException e) {
                /*
                 * We expect a SocketException on cancellation. Any other type
                 * of exception that occurs during cancellation is ignored
                 * regardless as there would be no need to handle it.
                 */
                if (!mStopped) Log.w(TAG, e);
                exception = e;
            } finally {

                if (bytes > 0) {
                    if (is != null)
                        try {
                            is.close();
                        } catch (IOException ignored) {
                        }
                    if (os != null)
                        try {
                            os.close();
                        } catch (IOException ignored) {
                        }
                }

                synchronized (lock) {
                    mMethod = null;
                }

                /* Close the socket (if it's still open) and cleanup. */
                cli.close();

                if (serviceRef.get() != null)
                    serviceRef.get().onDownloadThreadDeath(this);

            }
        }
    }


    private long getContentLength(HttpResponse resp) {
        return Long.parseLong(resp.getFirstHeader("Content-Length").getValue());
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
                mService.get().next();
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


    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }


    static class PlaybackError extends Exception {
        private final NetworkInfo networkInfo;
        private final boolean isStageFright;

        PlaybackError(IOException ioException, NetworkInfo info, boolean isStageFright) {
            super(ioException);
            this.networkInfo = info;
            this.isStageFright = isStageFright;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage()).append(" ")
              .append("networkInfo: ").append(networkInfo == null ? "" : networkInfo.toString())
              .append(" ")
              .append("isStageFright: ").append(isStageFright);
            return sb.toString();
        }
    }

    static class StatusException extends PlaybackError {
        private final StatusLine status;
        public StatusException(StatusLine status, NetworkInfo info, boolean isStageFright) {
            super(null, info, isStageFright);
            this.status = status;
        }
        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage());
            if (status != null) {
                sb.append(" status: ").append(status.toString());
            }
            return sb.toString();
        }
    }

    static class MediaPlayerException extends PlaybackError {
        final int code, extra;
        MediaPlayerException(int code, int extra, NetworkInfo info, boolean isStageFright) {
            super(null, info, isStageFright);
            this.code = code;
            this.extra = extra;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage())
              .append(" ")
              .append("code: ").append(code).append(", extra: ").append(extra);
            return sb.toString();
        }
    }
}
