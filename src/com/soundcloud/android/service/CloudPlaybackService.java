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
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.TrackPlays;
import com.soundcloud.android.streaming.StreamProxy;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.utils.play.PlayListManager;
import org.apache.http.StatusLine;

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
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Provides "background" audio playback capabilities, allowing the user to
 * switch between activities without stopping playback. Derived from the Android
 * 2.1 default media player.
 */
public class CloudPlaybackService extends Service {

    public static final String TAG = "CloudPlaybackService";

    public static final int PLAYBACKSERVICE_STATUS = 1;

    public static final String PLAYSTATE_CHANGED = "com.soundcloud.android.playstatechanged";
    public static final String META_CHANGED = "com.soundcloud.android.metachanged";
    public static final String QUEUE_CHANGED = "com.soundcloud.android.queuechanged";
    public static final String PLAYBACK_COMPLETE = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_ERROR = "com.soundcloud.android.trackerror";
    public static final String STREAM_DIED = "com.soundcloud.android.streamdied";
    public static final String COMMENTS_LOADED = "com.soundcloud.android.commentsloaded";
    public static final String FAVORITE_SET = "com.soundcloud.android.favoriteset";
    public static final String ADD_FAVORITE = "com.soundcloud.android.addfavorite";
    public static final String REMOVE_FAVORITE = "com.soundcloud.android.removefavorite";
    public static final String SEEK_COMPLETE = "com.soundcloud.android.seekcomplete";
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
    public static final String ONE_SHOT_PLAY = "com.soundcloud.android.musicservicecommand.oneshotplay";

    private static final int TRACK_ENDED = 1;
    private static final int SERVER_DIED = 3;
    private static final int FADEIN = 4;
    private static final int TRACK_EXCEPTION = 5;
    private static final int CLEAR_LAST_SEEK = 8;
    private static final int STREAM_EXCEPTION = 9;
    private static final int CHECK_TRACK_EVENT = 10;
    public static final int START_NEXT_TRACK = 11;

    private MultiPlayer mPlayer;
    private int mLoadPercent = 0;
    private boolean mAutoPause = false;
    protected NetworkConnectivityListener connectivityListener;
    private PlayListManager mPlayListManager = new PlayListManager(this);
    private Track mPlayingData;
    private boolean mMediaplayerError;
    private RemoteViews mNotificationView;
    private long mResumeTime = -1;
    private long mResumeId = -1;

    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mResumeAfterCall = false;
    private boolean mIsSupposedToBePlaying = false;
    private boolean mWaitingForArtwork = false;
    private PlayerAppWidgetProvider mAppWidgetProvider = PlayerAppWidgetProvider.getInstance();
    private long mSeekPos = -1;

    // interval after which we stop the service when idle
    private static final int IDLE_DELAY = 60000;

    private static final long TRACK_EVENT_CHECK_DELAY = 1000; // check for track timestamp events at this frequency

    private boolean pausedForBuffering;
    private NetworkInfo mCurrentNetworkInfo;

    protected int batteryLevel;
    protected int plugState;
    protected boolean mHeadphonePluggedState;

    public boolean mAutoAdvance = true;

    private static final int MINIMUM_SEEKABLE_SDK = Build.VERSION_CODES.ECLAIR_MR1; // 7, 2.1

    private long m10percentStamp;
    private long m95percentStamp;

    private boolean m10percentStampReached;
    private boolean m95percentStampReached;

    private StreamProxy mProxy;

    public CloudPlaybackService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Needs to be done in this thread, since otherwise
        // ApplicationContext.getPowerManager() crashes.
        mPlayer = new MultiPlayer();
        mPlayer.setHandler(mMediaplayerHandler);


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

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that
        // case.
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);

        mAutoPause = true;
        mResumeTime = mPlayListManager.reloadQueue();
        mPlayingData = mPlayListManager.getCurrentTrack();
        if (mPlayingData != null && mResumeTime > 0) {
            mResumeId = mPlayingData.id;
        }

    }


    @Override
    public void onDestroy() {
        stopStreaming();
        gotoIdleState();

        // release all MediaPlayer resources, including the native player and wakelocks
        mPlayer.release();
        mPlayer = null;

        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mMediaplayerHandler.removeCallbacksAndMessages(null);

        mMediaplayerHandler = null;
        mDelayedStopHandler = null;
        mPhoneStateListener = null;

        unregisterReceiver(mIntentReceiver);
        if (mProxy != null && mProxy.isRunning()) mProxy.stop();
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
                if (isSupposedToBePlaying()) {
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
            } else if (ONE_SHOT_PLAY.equals(action)) {
                oneShotPlay(intent.<Track>getParcelableExtra("track"));
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

        mPlayListManager.saveQueue(true, mPlayingData == null ? 0 : position());

        if (isSupposedToBePlaying() || mResumeAfterCall) {
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
            if (isSupposedToBePlaying() || mResumeAfterCall || mServiceInUse
                    || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
                return;
            }
            mPlayListManager.saveQueue(true, mPlayingData == null ? 0 : position());
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

        Log.i(TAG, "Notify Change " + what + " " + getTrackId() + " " + isSupposedToBePlaying());
        Intent i = new Intent(what);
        i.putExtra("id", getTrackId());
        i.putExtra("track", getTrackName());
        i.putExtra("user", getUserName());
        i.putExtra("isPlaying", isPlaying());
        i.putExtra("isSupposedToBePlaying", isSupposedToBePlaying());
        i.putExtra("isBuffering", isBuffering());
        i.putExtra("position",position());
        i.putExtra("queuePosition",getQueuePosition());
        if (FAVORITE_SET.equals(what)) {
            i.putExtra("isFavorite", mPlayingData.user_favorite);
        }

        sendBroadcast(i);

        if (what.equals(QUEUE_CHANGED)) {
            mPlayListManager.saveQueue(true, mPlayingData == null ? 0 : position());
        } else {
            mPlayListManager.saveQueue(false, mPlayingData == null ? 0 : position());
        }

        // Share this notification directly with our widgets
        mAppWidgetProvider.notifyChange(this, what);
    }

    private void oneShotPlay(Track track){
        if (track == null) return;
         synchronized (this) {
            mPlayListManager.oneShotTrack(track);
            stopStreaming();
            openCurrent();
            mIsSupposedToBePlaying = true;
            setPlayingStatus();
        }
    }

    public void playFromAppCache(int playPos) {
        synchronized (this) {
            mPlayListManager.loadPlaylist(getApp().flushCachePlaylist(), playPos);
            stopStreaming();
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

    public void openAsync(final Track track) {

        if (track == null) {
            return;
        }

        if (mAutoPause) {
            mAutoPause = false;
            notifyChange(META_CHANGED);
            setPlayingStatus();
        }

        if (mPlayer != null && (mPlayer.isInitialized() || mPlayer.isAsyncOpening())) {
            new Thread(new Runnable() {
                public void run() {
                    mPlayer.stop();
                }
            }).start();
        }


        mLoadPercent = 0;

        // if we are already playing this track
        if (mPlayingData != null && mPlayingData.id == track.id) {
            startNextTrack();
            return;
        }

        //otherwise it will wait for the waveform
        mWaitingForArtwork = getApp().playerWaitForArtwork;

        // new play data
        mPlayingData = track;

        final User user = getApp().getLoggedInUser();

        Cursor cursor = getContentResolver().query(Content.TRACK_PLAYS, null,
                TrackPlays.TRACK_ID + " = ?", new String[]{
                Long.toString(mPlayingData.id)
        }, null);

        if (cursor == null || cursor.getCount() == 0) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(TrackPlays.TRACK_ID, mPlayingData.id);
            contentValues.put(TrackPlays.USER_ID, user.id);
            getContentResolver().insert(Content.TRACK_PLAYS, contentValues);
        }
        if (cursor != null) cursor.close();

        mPlayingData.updateFromDb(getContentResolver(), user);
        if (getApp().getTrackFromCache(mPlayingData.id) == null) {
            getApp().cacheTrack(mPlayingData);
        }

        m10percentStamp = (long) (mPlayingData.duration * .1);
        m10percentStampReached = false;
        m95percentStamp = (long) (mPlayingData.duration * .95);
        m95percentStampReached = false;

        getApp().trackEvent(Consts.Tracking.Categories.TRACKS, Consts.Tracking.Actions.TRACK_PLAY,
                                    mPlayingData.getTrackEventLabel());

        setPlayingStatus();

        // tell the db we played it
        track.user_played = true;

        // meta has changed
        notifyChange(META_CHANGED);

        startNextTrack();
    }

    private void stopStreaming() {
        // stop checking buffer
        pausedForBuffering = false;
        // stop playing
        if (mPlayer != null && (mPlayer.isInitialized() || mPlayer.isAsyncOpening())) {
            mPlayer.stop();
        }
    }

    private void startNextTrack() {
        synchronized (this) {
            if (mPlayingData.isStreamable()) {
                pausedForBuffering = true;
                notifyChange(BUFFERING);

                // commit updated track (user played update only)
                mPlayListManager.commitTrackToDb(mPlayingData);
                mPlayer.setDataSourceAsync(mPlayingData.stream_url);
            } else {
                sendStreamException(0);
                gotoIdleState();
            }
        }
    }

    public void sendStreamException(long delay) {
        gotoIdleState();
        mMediaplayerHandler.sendMessageDelayed(mMediaplayerHandler.obtainMessage(STREAM_EXCEPTION), delay);
    }

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {
        if (mPlayingData == null)
            return;

        boolean wasPlaying = mIsSupposedToBePlaying;
        mIsSupposedToBePlaying = true;

        if (mPlayer != null && mPlayer.isInitialized()) {
             mPlayer.start();

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
            stopStreaming();
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
            if (isSupposedToBePlaying()) {
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

    public boolean isSupposedToBePlaying() {
        return mIsSupposedToBePlaying;
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
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
        if (killBuffer) pausedForBuffering = false;

        mIsSupposedToBePlaying = false;
        mMediaplayerHandler.removeMessages(CHECK_TRACK_EVENT);
        mMediaplayerHandler.removeMessages(START_NEXT_TRACK);
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
            return mPlayListManager.getCurrentPosition();
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
            if (mPlayingData != null && mPlayingData.id == trackId) {
                if (favoriteStatus) {
                    addFavorite();
                } else {
                    removeFavorite();
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
        SoundCloudDB.setTrackIsFavorite(getApp().getContentResolver(),trackId,isFavorite,getApp().getCurrentUserId());
        if (getApp().getTrackFromCache(trackId) != null){
            getApp().getTrackFromCache(trackId).user_favorite = isFavorite;
        }

        if (mPlayingData.id == trackId && mPlayingData.user_favorite != isFavorite) {
            mPlayingData.user_favorite = isFavorite;
            notifyChange(FAVORITE_SET);
        } else {
            Intent i = new Intent(FAVORITE_SET);
            i.putExtra("id", trackId);
            i.putExtra("isFavorite", isFavorite);
            sendBroadcast(i);
            // Share this notification directly with our widgets
            mAppWidgetProvider.notifyChange(this, FAVORITE_SET);
        }
    }

    public String getUserName() {
        if (mPlayingData != null && mPlayingData.user != null) {
            return mPlayingData.user.username;
        } else return null;
    }

    public String getUserPermalink() {
        if (mPlayingData == null) {
            return null;
        }
        return mPlayingData.user.permalink;
    }

    public long getTrackId() {
        if (mPlayingData == null) {
            return -1;
        }
        return mPlayingData.id;
    }

    public boolean getDownloadable() {
        return mPlayingData != null && mPlayingData.downloadable;
    }

    public Track getTrack() {
        return mPlayingData == null ? mPlayListManager.getCurrentTrack() : mPlayingData;
    }

    public String getTrackName() {
        if (mPlayingData == null) {
            return null;
        }
        return mPlayingData.title;
    }

    public int getDuration() {
        if (mPlayingData == null) {
            return 0;
        }
        return mPlayingData.duration;
    }

    public boolean isBuffering() {
        return pausedForBuffering;
    }

    /*
     * Returns the duration of the file in milliseconds. Currently this method
     * returns -1 for the duration of MIDI files.
     */
    public long duration() {
        if (mPlayingData == null) {
            return -1;
        }
        return mPlayingData.duration;
    }

    public String getWaveformUrl() {
        if (mPlayingData == null) {
            return "";
        }
        return mPlayingData.waveform_url;
    }

    /*
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (mPlayer != null && mPlayer.isInitialized()) {
            return mPlayer.position();
        } else if (mPlayingData != null && mResumeId == mPlayingData.id) {
            return mResumeTime; // either -1 or a valid resume time
        } else return 0;
    }

    /*
     * Returns the duration of the file in milliseconds. Currently this method
     * returns -1 for the duration of MIDI files.
     */
    public int loadPercent() {
        if (mPlayer != null && mPlayer.isInitialized()) {
            return mLoadPercent;
        }
        return 0;
    }

    public boolean isSeekable() {
        return (Build.VERSION.SDK_INT >= MINIMUM_SEEKABLE_SDK
                && mPlayer != null
                && mPlayer.isInitialized()
                && !mPlayer.isAsyncOpening()
                && mPlayingData != null);
    }

    public boolean isNotSeekablePastBuffer() {
        // Some phones on 2.2 ship with broken opencore
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && StreamProxy.opencoreClient;
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     * @return the new pos, or -1
     */
    public long seek(long pos) {
        synchronized (this) {
            if (isSeekable()) {

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
        if (isSeekable()) {
            if (pos <= 0) {
                pos = 0;
            }
            return mPlayer.getSeekResult(pos);
        }
        return -1;
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
        private int mRetries = 0;

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
                    setOnCompletionListener(listener);
                    setOnErrorListener(errorListener);
                    setOnBufferingUpdateListener(bufferinglistener);
                    setOnInfoListener(infolistener);
                }
            };
        }

        public void setDataSourceAsync(String path) {

            if (mMediaPlayer == null) refreshMediaplayer();
            mIsAsyncOpening = true;

            try {
                if (mProxy == null) {
                    mProxy = new StreamProxy(getApp()).init().start();
                }
                mPlayingPath = String.format("http://127.0.0.1:%d/%s", mProxy.getPort(), path);

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

        public boolean isAsyncOpening() {
            return mIsAsyncOpening;
        }

        public void start() {
            Message msg = mMediaplayerHandler.obtainMessage(CHECK_TRACK_EVENT);
            mMediaplayerHandler.removeMessages(CHECK_TRACK_EVENT);
            mMediaplayerHandler.sendMessageDelayed(msg, TRACK_EVENT_CHECK_DELAY);

            if (mMediaPlayer != null) mMediaPlayer.start();
        }

        public void stop() {
            mIsInitialized = false;
            mPlayingPath = "";

            if (mMediaPlayer == null)
                return;

            final MediaPlayer releasing = mMediaPlayer;
            mMediaPlayer = null;

            try {
                if (releasing.isPlaying())
                releasing.pause();
            } catch (IllegalStateException ignore) {}

            long start = System.currentTimeMillis();
            releasing.release();
            Log.d(TAG, "released in "+ (System.currentTimeMillis()-start)+ " ms");
        }

        public long position() {
            try {
                return mMediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                Log.w(TAG, e);
                return 0;
            }

        }

        public long seek(long whereto) {
            if (mPlayer == null) return -1;
            else if (isNotSeekablePastBuffer() && isPastBuffer(whereto)) {
                Log.d(TAG, "MediaPlayer bug: cannot seek past buffer");
                return -1;
            }
            else if (whereto != mPlayer.position()) {
                mSeekPos = whereto;
                mMediaPlayer.seekTo((int) whereto);
            }
            return whereto;
        }

        public long getSeekResult(long whereto) {
            return getSeekResult(whereto, false);
        }

        private boolean isPastBuffer(long pos) {
             return (pos / (double) mPlayingData.duration) * 100 > mLoadPercent;
        }

        public long getSeekResult(long whereto, boolean resumeSeek) {
            if (mPlayer == null) return -1;
            else if (isNotSeekablePastBuffer() && isPastBuffer(whereto)) {
                return mPlayer.position();
            }
            long maxSeek;
            if (!resumeSeek) {
                maxSeek = getDuration();

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

        MediaPlayer.OnInfoListener infolistener = new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
                Log.d(TAG, "onInfo("+what+", "+extra+")");

                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        pausedForBuffering = true;
                        notifyChange(BUFFERING);

                        break;

                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        pausedForBuffering = false;
                        notifyChange(BUFFERING_COMPLETE);

                    default:
                        break;
                }
                return true;
            }
        };

        MediaPlayer.OnBufferingUpdateListener bufferinglistener = new MediaPlayer.OnBufferingUpdateListener() {
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                mRetries = 0;
                mLoadPercent = percent;
            }
        };

        MediaPlayer.OnSeekCompleteListener seeklistener = new MediaPlayer.OnSeekCompleteListener() {
            public void onSeekComplete(MediaPlayer mp) {
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
                final long targetPosition = (mSeekPos == -1) ? mMediaPlayer.getCurrentPosition() : mSeekPos;
                if (mIsInitialized && mPlayingData != null && isSeekable()
                        && getDuration() - targetPosition > 3000) {
                    mResumeId = mPlayingData.id;
                    mResumeTime = targetPosition;

                    mMediaPlayer.reset();
                    mIsInitialized = false;
                    mRetries = 0;
                    mPlayingPath = "";
                    return;
                }

                // Acquire a temporary wakelock, since when we return from
                // this callback the MediaPlayer will release its wakelock
                // and allow the device to go to sleep.
                // This temporary wakelock is released when the RELEASE_WAKELOCK
                // message is processed, but just in case, put a timeout on it.
                if (!mMediaplayerError){
                    mHandler.sendEmptyMessage(TRACK_ENDED);

                    getApp().trackEvent(Consts.Tracking.Categories.TRACKS, Consts.Tracking.Actions.TRACK_COMPLETE,
                                    mPlayingData.getTrackEventLabel());
                }

                mMediaplayerError = false;
            }
        };

        MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                mIsAsyncOpening = false;
                mIsInitialized = true;
                notifyChange(BUFFERING_COMPLETE);

                if (!mAutoPause) {
                    if (mIsSupposedToBePlaying) {
                        mPlayer.setVolume(0);
                        play();
                        startAndFadeIn();
                    }
                }

                if (mResumeId == mPlayingData.id) {
                    mPlayer.seek(mResumeTime);
                    mResumeTime = -1;
                    mResumeId = -1;
                }
                pausedForBuffering = false;

            }
        };

        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "MP ERROR " + what + " | " + extra);
                // when the proxy times out it will just close the connection - this gets reported
                // as error -1005 (in some implementations). try to reconnect at least twice before giving up
                if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN &&
                        extra == -1004 /* ERROR_IO */ ||
                        extra == -1005 /* ERROR_CONNECTION_LOST */) {
                    if (mRetries < 3) {
                        Log.d(TAG, "stream disconnected, retrying (try="+(mRetries+1)+")");
                        mRetries++;
                        mMediaPlayer.reset();
                        play();
                        return true;
                    } else {
                        Log.d(TAG, "stream disconnected, giving up");
                    }
                }

                mIsAsyncOpening = false;
                mMediaplayerError = true;

                if (isConnected() && SoundCloudApplication.REPORT_PLAYBACK_ERRORS) {
                    if (SoundCloudApplication.REPORT_PLAYBACK_ERRORS_BUGSENSE) {
                        SoundCloudApplication.handleSilentException("mp error",
                                new MediaPlayerException(what, extra, mCurrentNetworkInfo));
                    }
                    getApp().trackEvent(Consts.Tracking.Categories.PLAYBACK_ERROR, "mediaPlayer", "code", what);
                }

                mIsInitialized = false;
                mPlayingPath = "";
                mMediaPlayer.reset();
                mMediaplayerHandler.sendMessage(mMediaplayerHandler.obtainMessage(isConnected() ?
                        TRACK_EXCEPTION : STREAM_EXCEPTION));
                return true;
            }
        };

        public boolean isPlaying() {
            try {
                return mMediaPlayer != null && mMediaPlayer.isPlaying() && isSupposedToBePlaying();
            } catch (IllegalStateException e) {
                return false;
            }
        }
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
                    if (isSupposedToBePlaying()) {
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
                    mResumeAfterCall = (isSupposedToBePlaying() || mResumeAfterCall) && mPlayingData != null;
                    pause();
                }
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // pause the music while a conversation is in progress
                mResumeAfterCall = (isSupposedToBePlaying() || mResumeAfterCall) && mPlayingData != null;
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
                case START_NEXT_TRACK:
                    startNextTrack();
                    break;
                case FADEIN:
                    if (!isSupposedToBePlaying()) {
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
                    if (mIsSupposedToBePlaying && mAutoAdvance){
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
                    notifyChange(PLAYBACK_ERROR);
                    notifyChange(PLAYBACK_COMPLETE);
                    break;

                case STREAM_EXCEPTION:
                    gotoIdleState();
                    notifyChange(STREAM_DIED);
                    notifyChange(PLAYBACK_COMPLETE);
                    break;
                case TRACK_ENDED:
                    if (mAutoAdvance){
                        next();
                    } else {
                        notifyChange(PLAYBACK_COMPLETE);
                        gotoIdleState();
                    }
                    break;
                case CLEAR_LAST_SEEK:
                    mSeekPos = -1;
                    break;
                case CHECK_TRACK_EVENT:
                    if (mPlayingData != null) {
                        final long pos = position();
                        final long window = (long) (TRACK_EVENT_CHECK_DELAY * 1.5); // account for lack of accuracy in actual delay between checks
                        if (!m10percentStampReached && pos > m10percentStamp && pos - m10percentStamp < window) {
                            m10percentStampReached = true;
                            getApp().trackEvent(Consts.Tracking.Categories.TRACKS, Consts.Tracking.Actions.TEN_PERCENT,
                                    mPlayingData.getTrackEventLabel());
                        }

                        if (!m95percentStampReached && pos > m95percentStamp && pos - m95percentStamp < window) {
                            m95percentStampReached = true;
                            getApp().trackEvent(Consts.Tracking.Categories.TRACKS, Consts.Tracking.Actions.NINTY_FIVE_PERCENT,
                                    mPlayingData.getTrackEventLabel());
                        }
                    }
                    if (!m10percentStampReached || !m95percentStampReached) {
                        Message newMsg = mMediaplayerHandler.obtainMessage(CHECK_TRACK_EVENT);
                        mMediaplayerHandler.removeMessages(CHECK_TRACK_EVENT);
                        mMediaplayerHandler.sendMessageDelayed(newMsg, TRACK_EVENT_CHECK_DELAY);
                    }
                    break;

                default:
                    break;
            }
        }
    };

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

        public boolean isSupposedToBePlaying() {
            return mService.get().isSupposedToBePlaying();
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

        public Track getTrackAt(int pos) throws RemoteException {
            return mService.get().mPlayListManager.getTrackAt(pos);
        }

        public long getTrackIdAt(int pos) throws RemoteException {
            return mService.get().mPlayListManager.getTrackIdAt(pos);
        }

        public int getQueueLength() throws RemoteException {
            return mService.get().mPlayListManager.getCurrentLength();
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

        public void setAutoAdvance(boolean autoAdvance) throws RemoteException {
            if (mService.get() != null)
                mService.get().setAutoAdvance(autoAdvance);
        }
    }

    private void setAutoAdvance(boolean autoAdvance) {
        mAutoAdvance = autoAdvance;
    }

    private final IBinder mBinder = new ServiceStub(this);


    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    static class PlaybackError extends Exception {
        private final NetworkInfo networkInfo;

        PlaybackError(IOException ioException, NetworkInfo info) {
            super(ioException);
            this.networkInfo = info;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage()).append(" ")
                    .append("networkType: ").append(networkInfo == null ? null : networkInfo.getTypeName())
                    .append(" ");
            return sb.toString();
        }
    }

    static class StatusException extends PlaybackError {
        private final StatusLine status;
        public StatusException(StatusLine status, NetworkInfo info) {
            super(null, info);
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
        MediaPlayerException(int code, int extra, NetworkInfo info) {
            super(null, info);
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
