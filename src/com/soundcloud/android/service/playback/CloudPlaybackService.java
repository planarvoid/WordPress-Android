package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.CloudPlaybackService.State.*;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.streaming.StreamProxy;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.utils.play.PlayListManager;

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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;

public class CloudPlaybackService extends Service {
    public static final String TAG = "CloudPlaybackService";

    public static final String PLAYSTATE_CHANGED  = "com.soundcloud.android.playstatechanged";
    public static final String META_CHANGED       = "com.soundcloud.android.metachanged";
    public static final String QUEUE_CHANGED      = "com.soundcloud.android.queuechanged";
    public static final String PLAYBACK_COMPLETE  = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_ERROR     = "com.soundcloud.android.trackerror";
    public static final String STREAM_DIED        = "com.soundcloud.android.streamdied";
    public static final String COMMENTS_LOADED    = "com.soundcloud.android.commentsloaded";
    public static final String FAVORITE_SET       = "com.soundcloud.android.favoriteset";
    public static final String SEEK_COMPLETE      = "com.soundcloud.android.seekcomplete";
    public static final String BUFFERING          = "com.soundcloud.android.buffering";
    public static final String SERVICECMD         = "com.soundcloud.android.musicservicecommand";
    public static final String BUFFERING_COMPLETE = "com.soundcloud.android.bufferingcomplete";

    public static final String TOGGLEPAUSE_ACTION = "com.soundcloud.android.musicservicecommand.togglepause";
    public static final String PAUSE_ACTION       = "com.soundcloud.android.musicservicecommand.pause";
    public static final String PREVIOUS_ACTION    = "com.soundcloud.android.musicservicecommand.previous";
    public static final String NEXT_ACTION        = "com.soundcloud.android.musicservicecommand.next";
    public static final String ONE_SHOT_PLAY      = "com.soundcloud.android.musicservicecommand.oneshotplay";
    public static final String RESET_ALL          = "com.soundcloud.android.musicservicecommand.resetall";

    public static final String ADD_FAVORITE       = "com.soundcloud.android.favorite.add";
    public static final String REMOVE_FAVORITE    = "com.soundcloud.android.favorite.remove";

    public static final String CMDNAME        = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP        = "stop";
    public static final String CMDPAUSE       = "pause";
    public static final String CMDPREVIOUS    = "previous";
    public static final String CMDNEXT        = "next";

    private static final int TRACK_ENDED      = 1;
    private static final int SERVER_DIED      = 2;
    private static final int FADEIN           = 3;
    private static final int CLEAR_LAST_SEEK  = 4;
    private static final int STREAM_EXCEPTION = 5;
    private static final int CHECK_TRACK_EVENT = 6;
    private static final int NOTIFY_META       = 7;

    public static final int PLAYBACKSERVICE_STATUS_ID = 1;
    private static final int MINIMUM_SEEKABLE_SDK = Build.VERSION_CODES.ECLAIR_MR1; // 7, 2.1

    private MediaPlayer mMediaPlayer;
    private int mLoadPercent = 0;       // track buffer indicator
    private boolean mAutoPause = false; // used when svc is first created and playlist is resumed
    private boolean mAutoAdvance = true;// automatically skip to next track
    protected NetworkConnectivityListener connectivityListener;
    /* package */ PlayListManager mPlayListManager = new PlayListManager(this);
    private Track mCurrentTrack;
    private RemoteViews mNotificationView;

    private long mResumeTime = -1;      // time of played track
    private long mResumeTrackId = -1;   // id of last played track
    private long mSeekPos = -1;         // desired seek position
    private int mConnectRetries = 0;

    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mResumeAfterCall = false;
    private PlayerAppWidgetProvider mAppWidgetProvider = PlayerAppWidgetProvider.getInstance();

    private static final int IDLE_DELAY = 60*1000;            // interval after which we stop the service when idle
    private static final long TRACK_EVENT_CHECK_DELAY = 1000; // check for track timestamp events at this frequency

    private boolean resumeSeeking;
    private boolean mHeadphonePluggedState;

    private long m10percentStamp;
    private long m95percentStamp;

    private boolean m10percentStampReached;
    private boolean m95percentStampReached;

    private StreamProxy mProxy;

    // states the mediaplayer can be in - we need to track these manually
    enum State {
        STOPPED, ERROR, ERROR_RETRYING, PREPARING, PREPARED, PLAYING, PAUSED, PAUSED_FOR_BUFFERING
    }

    private State state = STOPPED;

    private final IBinder mBinder = new ServiceStub(this);

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        commandFilter.addAction(ADD_FAVORITE);
        commandFilter.addAction(REMOVE_FAVORITE);
        commandFilter.addAction(RESET_ALL);
        registerReceiver(mIntentReceiver, commandFilter);
        registerReceiver(mIntentReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(mIntentReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        // setup call listening
        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that
        // case.
        scheduleServiceShutdownCheck();

        mAutoPause = true;
        mResumeTime = mPlayListManager.reloadQueue();
        mCurrentTrack = mPlayListManager.getCurrentTrack();
        if (mCurrentTrack != null && mResumeTime > 0) {
            mResumeTrackId = mCurrentTrack.id;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stop();
        gotoIdleState();

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        mMediaPlayer = null;

        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mMediaPlayerHandler.removeCallbacksAndMessages(null);

        unregisterReceiver(mIntentReceiver);
        if (mProxy != null && mProxy.isRunning()) mProxy.stop();
    }

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
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;

        mPlayListManager.saveQueue(true, mCurrentTrack == null ? 0 : position());

        if (isSupposedToBePlaying() || mResumeAfterCall) {
            // something is currently playing, or will be playing once
            // an in-progress call ends, so don't stop the service now.
            return true;
        }

        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between
        // tracks.
        if (mPlayListManager.getCurrentLength() > 0 || mMediaPlayerHandler.hasMessages(TRACK_ENDED)) {
            Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }

        // No active playlist, OK to stop the service right now
        stopSelf(mServiceStartId);
        return true;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onStartCommand("+cmd+")");
            }

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
            } else if (RESET_ALL.equals(action)) {
                stop();
                mPlayListManager.clear();
            }

        }

        scheduleServiceShutdownCheck();
        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        return START_STICKY;
    }

    private void scheduleServiceShutdownCheck() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "scheduleServiceShutdownCheck()");
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendMessageDelayed(mDelayedStopHandler.obtainMessage(), IDLE_DELAY);
    }

    private void notifyChange(String what) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "notifyChange("+what+")");
        }
        Intent i = new Intent(what)
            .putExtra("id", getTrackId())
            .putExtra("track", getTrackName())
            .putExtra("user", getUserName())
            .putExtra("isPlaying", isPlaying())
            .putExtra("isSupposedToBePlaying", isSupposedToBePlaying())
            .putExtra("isBuffering", isBuffering())
            .putExtra("position", position())
            .putExtra("queuePosition", mPlayListManager.getCurrentPosition());
        if (FAVORITE_SET.equals(what)) {
            i.putExtra("isFavorite", mCurrentTrack.user_favorite);
        }

        sendBroadcast(i);
        mPlayListManager.saveQueue(what.equals(QUEUE_CHANGED), mCurrentTrack == null ? 0 : position());

        // Share this notification directly with our widgets
        mAppWidgetProvider.notifyChange(this, what);
    }

    private void oneShotPlay(Track track) {
        if (track != null) {
            mPlayListManager.oneShotTrack(track);
            openCurrent();
        }
    }

    public void playFromAppCache(int playPos) {
        mPlayListManager.loadPlaylist(getApp().flushCachePlaylist(), playPos);
        openCurrent();
    }

    public void openCurrent() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "openCurrent(state="+state+")");
        }
        final Track track = mPlayListManager.getCurrentTrack();
        if (track != null) {
            if (mAutoPause) {
                mAutoPause = false;
                notifyChange(META_CHANGED);
            }
            mLoadPercent = 0;
            if (!track.equals(mCurrentTrack)) {
                mCurrentTrack = track;
                mConnectRetries = 0; // new track, reset connection attempts

                m10percentStamp = (long) (track.duration * .1);
                m10percentStampReached = false;
                m95percentStamp = (long) (track.duration * .95);
                m95percentStampReached = false;

                new Thread() {
                    @Override public void run() {
                        track.markAsPlayed(getContentResolver());
                        mCurrentTrack.updateFromDb(getContentResolver(), getApp().getLoggedInUser());
                        if (getApp().getTrackFromCache(mCurrentTrack.id) == null) {
                            getApp().cacheTrack(mCurrentTrack);
                            mMediaPlayerHandler.sendMessage(mMediaPlayerHandler.obtainMessage(NOTIFY_META));
                        }
                    }
                }.start();

                getApp().trackEvent(
                        Consts.Tracking.Categories.TRACKS,
                        Consts.Tracking.Actions.TRACK_PLAY,
                        track.getTrackEventLabel());
            }
            startTrack(track);
            setPlayingNotification(track);
        } else {
            Log.d(TAG, "playlist is empty");
        }
    }


    private void startTrack(Track track) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "startTrack("+track.title+")");
        }
        if (track.isStreamable()) {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            }
            notifyChange(BUFFERING);

            // commit updated track (user played update only)
            mPlayListManager.commitTrackToDb(track);

            switch (state) {
                case PREPARING:
                    Log.w(TAG, "stuck in preparing state!");
                    final MediaPlayer old = mMediaPlayer;
                    new Thread() {
                        @Override
                        public void run() {
                            old.reset();
                            old.release();
                        }
                    }.start();
                    mMediaPlayer = new MediaPlayer();
                    break;
                case PLAYING:
                    mMediaPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                    try {
                        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.stop");
                        mMediaPlayer.stop();
                        state = STOPPED;
                    } catch (IllegalStateException e) {
                        Log.w(TAG, e);
                    }
                    break;
            }
            state = PREPARING;
            try {
                if (mProxy == null) {
                    mProxy = new StreamProxy(getApp()).init().start();
                }
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.reset");
                mMediaPlayer.reset();
                mMediaPlayer.setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setOnPreparedListener(preparedlistener);
                mMediaPlayer.setOnSeekCompleteListener(seekListener);
                mMediaPlayer.setOnCompletionListener(completionListener);
                mMediaPlayer.setOnErrorListener(errorListener);
                mMediaPlayer.setOnBufferingUpdateListener(bufferingListener);
                mMediaPlayer.setOnInfoListener(infolistener);
                Track next = mPlayListManager.getNextTrack();
                mMediaPlayer.setDataSource(mProxy.createUri(mCurrentTrack.stream_url, next == null ? null : next.stream_url).toString());
                mMediaPlayer.prepareAsync();
            } catch (IllegalStateException e) {
                Log.e(TAG, "error", e);
                state = ERROR;
            } catch (IOException e) {
                Log.e(TAG, "error", e);
                errorListener.onError(mMediaPlayer, 0, 0);
            }
        } else {
            // track not streamable
            mMediaPlayerHandler.sendMessage(mMediaPlayerHandler.obtainMessage(STREAM_EXCEPTION));
            gotoIdleState();
        }
    }


    public void play() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "play(state="+state+")");
        if (mCurrentTrack != null) {
            if (mMediaPlayer != null && (state == PAUSED || state == PREPARED)) {
                // resume
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.start");
                mMediaPlayer.start();
                state = PLAYING;

                setPlayingNotification(mCurrentTrack);

                Message msg = mMediaPlayerHandler.obtainMessage(CHECK_TRACK_EVENT);
                mMediaPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                mMediaPlayerHandler.sendMessageDelayed(msg, TRACK_EVENT_CHECK_DELAY);

                notifyChange(PLAYSTATE_CHANGED);
            } else if (state != PLAYING) {
                // must have been a playback error
                openCurrent();
            }
        }
    }


    // Pauses playback (call play() to resume)
    public void pause() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "pause(state="+state+")");
        }

        if (mMediaPlayer != null && state != PAUSED) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();

                state = PAUSED;
            } /* else {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
                state = STOPPED;
            }
            */
        }
        gotoIdleState();
        notifyChange(PLAYSTATE_CHANGED);
    }

    public void stop() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "stop(state="+state+")");
        }

        if (mMediaPlayer != null) {
            if (state == PLAYING || state == State.PREPARING || state == PREPARED||
                    state == PAUSED_FOR_BUFFERING) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
            stopForeground(true);
        }
        state = State.STOPPED;

        mMediaPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
        scheduleServiceShutdownCheck();
    }


    private void gotoIdleState() {
        state = STOPPED;
        mMediaPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
        scheduleServiceShutdownCheck();
        stopForeground(true);
    }

    public boolean isSupposedToBePlaying() {
        return state == PLAYING || state == PREPARING;
    }

    public void prev() {
        if (mPlayListManager.prev()) openCurrent();
    }

    public void next() {
        if (mPlayListManager.next()) openCurrent();
    }

    private void setPlayingNotification(Track track) {
        if (track == null) return;

        if (mNotificationView == null) {
            mNotificationView = new RemoteViews(getPackageName(), R.layout.playback_service_status_play);
            mNotificationView.setImageViewResource(R.id.icon, R.drawable.statusbar);
        }

        mNotificationView.setTextViewText(R.id.trackname, track.title);
        mNotificationView.setTextViewText(R.id.username, track.getUserName());
        mNotificationView.setTextViewText(R.id.progress, "");

        Intent intent = new Intent(Actions.PLAYER);
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);

        Notification status = new Notification();
        status.contentView = mNotificationView;
        status.flags |= Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.statusbar;
        status.contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        startForeground(PLAYBACKSERVICE_STATUS_ID, status);
    }

    public void setQueuePosition(int pos) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setQueuePosition("+pos+")");

        if (mPlayListManager.getCurrentPosition() != pos
                && mPlayListManager.setCurrentPosition(pos)) {
            openCurrent();
        }
    }

    public void setClearToPlay(boolean clearToPlay) {
    }

    public void setFavoriteStatus(long trackId, boolean favoriteStatus) {
        if (mCurrentTrack != null && mCurrentTrack.id == trackId) {
            if (favoriteStatus) {
                addFavorite();
            } else {
                removeFavorite();
            }
        }
    }

    public void addFavorite() {
        onFavoriteStatusSet(mCurrentTrack.id, true);
        FavoriteAddTask f = new FavoriteAddTask(getApp());
        f.setOnFavoriteListener(new FavoriteTask.FavoriteListener() {
            @Override
            public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
                onFavoriteStatusSet(trackId, isFavorite);
            }

            @Override
            public void onException(long trackId, Exception e) {
                onFavoriteStatusSet(trackId, false);
                // failed, so it shouldn't be a favorite
            }
        });
        f.execute(mCurrentTrack);
    }

    public void removeFavorite() {
        onFavoriteStatusSet(mCurrentTrack.id, false);
        FavoriteRemoveTask f = new FavoriteRemoveTask(getApp());
        f.setOnFavoriteListener(new FavoriteTask.FavoriteListener() {
            @Override
            public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
                onFavoriteStatusSet(trackId, isFavorite);
            }

            @Override
            public void onException(long trackId, Exception e) {
                onFavoriteStatusSet(trackId, true); // failed, so it should still be a favorite
            }

        });
        f.execute(mCurrentTrack);
    }

    private void onFavoriteStatusSet(long trackId, boolean isFavorite) {
        SoundCloudDB.setTrackIsFavorite(getApp().getContentResolver(), trackId, isFavorite, getApp().getCurrentUserId());
        if (getApp().getTrackFromCache(trackId) != null) {
            getApp().getTrackFromCache(trackId).user_favorite = isFavorite;
        }

        if (mCurrentTrack.id == trackId && mCurrentTrack.user_favorite != isFavorite) {
            mCurrentTrack.user_favorite = isFavorite;
            notifyChange(FAVORITE_SET);
        } else {
            sendBroadcast(new Intent(FAVORITE_SET)
                .putExtra("id", trackId)
                .putExtra("isFavorite", isFavorite));
            // Share this notification directly with our widgets
            mAppWidgetProvider.notifyChange(this, FAVORITE_SET);
        }
    }

    public String getUserName() {
        return mCurrentTrack != null && mCurrentTrack.user != null ? mCurrentTrack.user.username : null;
    }

    public String getUserPermalink() {
        return mCurrentTrack == null ? null : mCurrentTrack.user.permalink;
    }

    public long getTrackId() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.id;
    }

    public boolean getDownloadable() {
        return mCurrentTrack != null && mCurrentTrack.downloadable;
    }

    public Track getTrack() {
        return mCurrentTrack == null ? mPlayListManager.getCurrentTrack() : mCurrentTrack;
    }

    public String getTrackName() {
        return mCurrentTrack == null ? null : mCurrentTrack.title;
    }

    public int getDuration() {
        return mCurrentTrack == null ? 0 : mCurrentTrack.duration;
    }

    public boolean isBuffering() {
        return state == PAUSED_FOR_BUFFERING || resumeSeeking;
    }

    /*
     * Returns the duration of the file in milliseconds. Currently this method
     * returns -1 for the duration of MIDI files.
     */
    public long duration() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.duration;
    }

    public String getWaveformUrl() {
        return mCurrentTrack == null ? "" : mCurrentTrack.waveform_url;
    }

    /*
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (mMediaPlayer != null && (state == PLAYING || state == PAUSED)) {
            return mMediaPlayer.getCurrentPosition();
        } else if (mCurrentTrack != null && mResumeTrackId == mCurrentTrack.id) {
            return mResumeTime; // either -1 or a valid resume time
        } else {
            return 0;
        }
    }

    public int loadPercent() {
        return mMediaPlayer != null && state != ERROR ? mLoadPercent : 0;
    }

    public boolean isSeekable() {
        return (Build.VERSION.SDK_INT >= MINIMUM_SEEKABLE_SDK
                && mMediaPlayer != null
                && state != ERROR
                && mCurrentTrack != null);
    }

    public boolean isNotSeekablePastBuffer() {
        // Some phones on 2.2 ship with broken opencore
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && StreamProxy.isOpenCore();
    }


    private boolean isPastBuffer(long pos) {
        return (pos / (double) mCurrentTrack.duration) * 100 > mLoadPercent;
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     * @return the new pos, or -1
     */
    public long seek(long pos) {
        if (isSeekable()) {
            if (pos <= 0) {
                pos = 0;
            }

            if (isNotSeekablePastBuffer() && isPastBuffer(pos)) {
                Log.d(TAG, "MediaPlayer bug: cannot seek past buffer");
                return -1;
            } else if (pos != position()) {
                mMediaPlayer.seekTo((int) pos);
                return pos;
            } else {
                return pos;
            }
        } else {
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
            long currentPos = position();
            // workaround for devices which can't do content-range requests
            if (isNotSeekablePastBuffer() && isPastBuffer(pos)) {
                return currentPos;
            } else {
                long duration = getDuration();

                // don't go before the playhead if they are trying to seek
                // beyond, just maintain their current position
                if (pos > currentPos && currentPos > duration) {
                    return currentPos;
                } else if (pos > duration) {
                    return duration;
                } else {
                    return pos;
                }
            }
        } else {
            return -1;
        }
    }

    public void setVolume(float vol) {
        if (mMediaPlayer != null && state != ERROR) {
            try {
                mMediaPlayer.setVolume(vol, vol);
            } catch (IllegalStateException ignored) {
                Log.w(TAG, ignored);
            }
        }
    }

    public boolean isPlaying() {
        try {
            return mMediaPlayer != null && mMediaPlayer.isPlaying() && isSupposedToBePlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public boolean isConnected() {
        return connectivityListener != null && connectivityListener.isConnected();
    }

    public void setAutoAdvance(boolean autoAdvance) {
        mAutoAdvance = autoAdvance;
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    private final Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (!isSupposedToBePlaying() && !mResumeAfterCall && !mServiceInUse
                    && !mMediaPlayerHandler.hasMessages(TRACK_ENDED)) {

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "DelayedStopHandler: stopping service");
                }

                mPlayListManager.saveQueue(true, mCurrentTrack == null ? 0 : position());
                stopSelf(mServiceStartId);
            }
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "BroadcastReceiver#onReceive("+action+", "+cmd+")");
            }
            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                boolean oldPlugged = mHeadphonePluggedState;
                mHeadphonePluggedState = intent.getIntExtra("state", 0) != 0;
                if (mHeadphonePluggedState != oldPlugged && !mHeadphonePluggedState
                        && state == PLAYING) {
                    pause();
                }
            } else {
                if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                    next();
                } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                    prev();
                } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                    if (isSupposedToBePlaying()) {
                        pause();
                    } else if (mCurrentTrack != null) {
                        play();
                    } else {
                        openCurrent();
                        setPlayingNotification(mCurrentTrack);
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
                } else if (RESET_ALL.equals(action)) {
                    stop();
                    mPlayListManager.clear();
                }
            }
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCallStateChanged("+incomingNumber+")");
            }

            if (state == TelephonyManager.CALL_STATE_RINGING) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (ringvolume > 0) {
                    mResumeAfterCall = (isSupposedToBePlaying() || mResumeAfterCall) && mCurrentTrack != null;
                    pause();
                }
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // pause the music while a conversation is in progress
                mResumeAfterCall = (isSupposedToBePlaying() || mResumeAfterCall) && mCurrentTrack != null;
                pause();
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                // start playing again
                if (mResumeAfterCall) {
                    // resume playback only if music was playing
                    // when the call was answered
                    mMediaPlayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
                    mResumeAfterCall = false;
                }
            }
        }
    };

    private final Handler mMediaPlayerHandler = new Handler() {
        float mCurrentVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage("+msg.what+", state="+state+")");
            }
            switch (msg.what) {
                case NOTIFY_META:
                    notifyChange(META_CHANGED);
                    break;
                case FADEIN:
                    if (isSupposedToBePlaying()) {
                        mCurrentVolume += 0.01f;
                        if (mCurrentVolume < 1.0f) {
                            sendEmptyMessageDelayed(FADEIN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        setVolume(mCurrentVolume);
                    } else {
                        mCurrentVolume = 0f;
                        setVolume(mCurrentVolume);
                        play();
                        sendEmptyMessageDelayed(FADEIN, 10);
                    }
                    break;
                case SERVER_DIED:
                    if (state == PLAYING && mAutoAdvance) next();
                    break;
                case TRACK_ENDED:
                    if (mAutoAdvance) {
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
                    if (mCurrentTrack != null) {
                        final long pos = position();
                        final long window = (long) (TRACK_EVENT_CHECK_DELAY * 1.5); // account for lack of accuracy in actual delay between checks
                        if (!m10percentStampReached && pos > m10percentStamp && pos - m10percentStamp < window) {
                            m10percentStampReached = true;
                            getApp().trackEvent(Consts.Tracking.Categories.TRACKS, Consts.Tracking.Actions.TEN_PERCENT,
                                    mCurrentTrack.getTrackEventLabel());
                        }

                        if (!m95percentStampReached && pos > m95percentStamp && pos - m95percentStamp < window) {
                            m95percentStampReached = true;
                            getApp().trackEvent(Consts.Tracking.Categories.TRACKS, Consts.Tracking.Actions.NINTY_FIVE_PERCENT,
                                    mCurrentTrack.getTrackEventLabel());
                        }
                    }
                    if (!m10percentStampReached || !m95percentStampReached) {
                        Message newMsg = mMediaPlayerHandler.obtainMessage(CHECK_TRACK_EVENT);
                        mMediaPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                        mMediaPlayerHandler.sendMessageDelayed(newMsg, TRACK_EVENT_CHECK_DELAY);
                    }
                    break;

                default:
                    break;
            }
        }
    };


    final MediaPlayer.OnInfoListener infolistener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onInfo("+what+","+extra+", state="+state+")");
            }

            switch (what) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    state = PAUSED_FOR_BUFFERING;
                    notifyChange(BUFFERING);
                    break;

                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    state = PLAYING;
                    notifyChange(BUFFERING_COMPLETE);
                default:
                    break;
            }
            return true;
        }
    };

    final MediaPlayer.OnBufferingUpdateListener bufferingListener = new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (mMediaPlayer == mp) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onBufferingUpdate("+percent+")");
                }

                mLoadPercent = percent;
            }
        }
    };

    final MediaPlayer.OnSeekCompleteListener seekListener = new MediaPlayer.OnSeekCompleteListener() {
        public void onSeekComplete(MediaPlayer mp) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSeekComplete()");
            }

            if (mMediaPlayer == mp) {
                // keep the last seek time for 3000 ms because getCurrentPosition will be incorrect at first
                Message msg = mMediaPlayerHandler.obtainMessage(CLEAR_LAST_SEEK);
                mMediaPlayerHandler.removeMessages(CLEAR_LAST_SEEK);
                mMediaPlayerHandler.sendMessageDelayed(msg, 3000);
                resumeSeeking = false;
                notifyChange(SEEK_COMPLETE);
            }
        }
    };

    final MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCompletion(state="+state+")");
            }

            // check for premature track end
            final long targetPosition = (mSeekPos == -1) ? mMediaPlayer.getCurrentPosition() : mSeekPos;

            if (isSeekable() && getDuration() - targetPosition > 3000) {

                // track ended prematurely (probably end of buffer, unreported IO error), so try to resume at last time
                mResumeTrackId = mCurrentTrack.id;
                mResumeTime = targetPosition;
                errorListener.onError(mp, MediaPlayer.MEDIA_ERROR_UNKNOWN, Errors.STAGEFRIGHT_ERROR_BUFFER_EMPTY);
            } else {
                // Acquire a temporary wakelock, since when we return from
                // this callback the MediaPlayer will release its wakelock
                // and allow the device to go to sleep.
                // This temporary wakelock is released when the RELEASE_WAKELOCK
                // message is processed, but just in case, put a timeout on it.
                if (state != ERROR) {
                    mMediaPlayerHandler.sendEmptyMessage(TRACK_ENDED);
                    getApp().trackEvent(Consts.Tracking.Categories.TRACKS, Consts.Tracking.Actions.TRACK_COMPLETE,
                            mCurrentTrack.getTrackEventLabel());
                } else {
                    // XXX
                    stop();
                }
            }
        }
    };

    MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            if (mp == mMediaPlayer) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onPrepared(state="+state+")");
                }

                if (state == PREPARING) {
                    state = PREPARED;
                    if (!mAutoPause) {
                        if (isSupposedToBePlaying()) {
                            setVolume(0);
                            play();
                            mMediaPlayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
                        }
                    }

                    if (mResumeTrackId == mCurrentTrack.id) {
                        seek(mResumeTime);
                        mResumeTime = -1;
                        mResumeTrackId = -1;
                        resumeSeeking = true;
                    } else {
                        notifyChange(BUFFERING_COMPLETE);
                    }
                } else {
                    mp.stop();
                }
            }
        }
    };

    MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(TAG, "MP ERROR " + what + " | " + extra);
            if (mp == mMediaPlayer && state != STOPPED) {
                // when the proxy times out it will just close the connection - different implementations
                // return different error codes. try to reconnect at least twice before giving up.
                if (mConnectRetries++ < 4) {
                    Log.d(TAG, "stream disconnected, retrying (try=" + mConnectRetries + ")");
                    state = ERROR_RETRYING;
                    openCurrent();
                    return true;
                } else {
                    Log.d(TAG, "stream disconnected, giving up");
                    mConnectRetries = 0;
                }

                state = ERROR;
                mMediaPlayer.release();
                mMediaPlayer = null;

                gotoIdleState();
                notifyChange(isConnected() ? PLAYBACK_ERROR : STREAM_DIED);
                notifyChange(PLAYBACK_COMPLETE);
            }
            return true;
        }
    };
}
