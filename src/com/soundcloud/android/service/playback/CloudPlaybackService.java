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

public class CloudPlaybackService extends Service implements FocusHelper.MusicFocusable {
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
    private static final int FADE_IN          = 3;
    private static final int FADE_OUT         = 4;
    private static final int DUCK             = 5;
    private static final int CLEAR_LAST_SEEK  = 6;
    private static final int STREAM_EXCEPTION = 7;
    private static final int CHECK_TRACK_EVENT = 8;
    private static final int NOTIFY_META_CHANGED = 9;

    public static final int PLAYBACKSERVICE_STATUS_ID = 1;
    private static final int MINIMUM_SEEKABLE_SDK = Build.VERSION_CODES.ECLAIR_MR1; // 7, 2.1

    private MediaPlayer mMediaPlayer;
    private int mLoadPercent = 0;       // track buffer indicator
    private boolean mAutoPause = true;  // used when svc is first created and playlist is resumed on start
    private boolean mAutoAdvance = true;// automatically skip to next track
    protected NetworkConnectivityListener connectivityListener;
    /* package */ PlayListManager mPlayListManager = new PlayListManager(this);
    private Track mCurrentTrack;
    private RemoteViews mNotificationView;
    private AudioManager mAudioManager;

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

    // audio focus related
    private FocusHelper mFocus;
    private boolean mFocusLost;

    // states the mediaplayer can be in - we need to track these manually
    enum State {
        STOPPED, ERROR, ERROR_RETRYING, PREPARING, PREPARED, PLAYING, PAUSED, PAUSED_FOR_BUFFERING
    }

    private State state = STOPPED;

    private final IBinder mBinder = new ServiceStub(this);

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

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

        mFocus = new FocusHelper(this, this);

        if (!mFocus.isSupported()) {
            // setup call listening if not handled by the audiofocus
            TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        scheduleServiceShutdownCheck();

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
            mMediaPlayer = null;
        }

        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mPlayerHandler.removeCallbacksAndMessages(null);

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
        if (mPlayListManager.getCurrentLength() > 0 || mPlayerHandler.hasMessages(TRACK_ENDED)) {
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
                    seek(0, true);
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
                seek(0, true);
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

    @Override
    public void focusGained() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "focusGained");
        if (state == State.PAUSED && mFocusLost) {
            mPlayerHandler.sendEmptyMessage(FADE_IN);
            mFocusLost = false;
        } else {
            setVolume(1.0f);
        }
    }

    @Override
    public void focusLost(boolean isTransient, boolean canDuck) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "focusLost("+isTransient+", canDuck="+canDuck+")");
        }
        if (state == PLAYING) {
            mPlayerHandler.sendEmptyMessage(canDuck ? DUCK : FADE_OUT);
        }
        mFocusLost = isTransient;
    }


    /* package */ void scheduleServiceShutdownCheck() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "scheduleServiceShutdownCheck()");
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendMessageDelayed(mDelayedStopHandler.obtainMessage(), IDLE_DELAY);
    }

    /* package */ void notifyChange(String what) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "notifyChange("+what+")");
        }
        Intent i = new Intent(what)
            .putExtra("id", getTrackId())      // TODO: parcel track?
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

    /* package */ void oneShotPlay(Track track) {
        if (track != null) {
            mPlayListManager.oneShotTrack(track);
            openCurrent();
        }
    }

    /* package */ void playFromAppCache(int playPos) {
        mPlayListManager.loadPlaylist(getApp().flushCachePlaylist(), playPos);
        openCurrent();
    }

    /* package */ void openCurrent() {
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
                        }
                        mPlayerHandler.sendMessage(mPlayerHandler.obtainMessage(NOTIFY_META_CHANGED));
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
                    mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
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
            mPlayerHandler.sendMessage(mPlayerHandler.obtainMessage(STREAM_EXCEPTION));
            gotoIdleState();
        }
    }


    /* package */ void play() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "play(state="+state+")");
        if (mCurrentTrack != null) {
            if (mMediaPlayer != null && (state == PAUSED || state == PREPARED)) {
                // resume
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.start");

                mFocus.requestMusicFocus();
                mMediaPlayer.start();
                state = PLAYING;

                setPlayingNotification(mCurrentTrack);

                Message msg = mPlayerHandler.obtainMessage(CHECK_TRACK_EVENT);
                mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                mPlayerHandler.sendMessageDelayed(msg, TRACK_EVENT_CHECK_DELAY);

                notifyChange(PLAYSTATE_CHANGED);
            } else if (state != PLAYING) {
                // must have been a playback error
                openCurrent();
            }
        }
    }


    // Pauses playback (call play() to resume)
    /* package */ void pause() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "pause(state="+state+")");
        }

        if (mMediaPlayer != null && state != PAUSED) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                // don't abandon the focus here - otherwise we won't get it back
                state = PAUSED;
            } else {
                // get into a determined state
                stop();
            }
        }
        gotoIdleState();
        notifyChange(PLAYSTATE_CHANGED);
    }

    /* package */ void stop() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "stop(state="+state+")");
        }

        if (mMediaPlayer != null) {
            if (state == PLAYING ||
                state == PREPARED ||
                state == PAUSED_FOR_BUFFERING) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
            mFocus.abandonMusicFocus();
            stopForeground(true);
        }
        state = State.STOPPED;
        mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
        scheduleServiceShutdownCheck();
    }


    private void gotoIdleState() {
        if (state != PAUSED) state = STOPPED;
        mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
        scheduleServiceShutdownCheck();
        stopForeground(true);
    }

    /* package */ boolean isSupposedToBePlaying() {
        return state == PREPARING || state == PLAYING;
    }

    /* package */ void prev() {
        if (mPlayListManager.prev()) openCurrent();
    }

    /* package */ void next() {
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

    /* package */ void setQueuePosition(int pos) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setQueuePosition("+pos+")");

        if (mPlayListManager.getCurrentPosition() != pos &&
            mPlayListManager.setCurrentPosition(pos)) {
            openCurrent();
        }
    }

    /* package */ void setClearToPlay(boolean clearToPlay) {
    }

    /* package */ void setFavoriteStatus(long trackId, boolean favoriteStatus) {
        if (mCurrentTrack != null && mCurrentTrack.id == trackId) {
            if (favoriteStatus) {
                addFavorite();
            } else {
                removeFavorite();
            }
        }
    }


    /* package */String getUserName() {
        return mCurrentTrack != null && mCurrentTrack.user != null ? mCurrentTrack.user.username : null;
    }

    /* package */ String getUserPermalink() {
        return mCurrentTrack == null ? null : mCurrentTrack.user.permalink;
    }

    /* package */ long getTrackId() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.id;
    }

    /* package */ boolean getDownloadable() {
        return mCurrentTrack != null && mCurrentTrack.downloadable;
    }

    /* package */ Track getTrack() {
        return mCurrentTrack == null ? mPlayListManager.getCurrentTrack() : mCurrentTrack;
    }

    /* package */ String getTrackName() {
        return mCurrentTrack == null ? null : mCurrentTrack.title;
    }

    /* package */ int getDuration() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.duration;
    }

    /* package */ boolean isBuffering() {
        return state == PAUSED_FOR_BUFFERING || resumeSeeking;
    }

    /* package */ String getWaveformUrl() {
        return mCurrentTrack == null ? "" : mCurrentTrack.waveform_url;
    }

    /*
     * Returns the current playback position in milliseconds
     */
    /* package */ long position() {
        if (mMediaPlayer != null && (state == PLAYING || state == PAUSED)) {
            return mMediaPlayer.getCurrentPosition();
        } else if (mCurrentTrack != null && mResumeTrackId == mCurrentTrack.id) {
            return mResumeTime; // either -1 or a valid resume time
        } else {
            return 0;
        }
    }

    /* package */ int loadPercent() {
        return mMediaPlayer != null && state != ERROR ? mLoadPercent : 0;
    }

    /* package */ boolean isSeekable() {
        return (Build.VERSION.SDK_INT >= MINIMUM_SEEKABLE_SDK
                && mMediaPlayer != null
                && state != ERROR
                && mCurrentTrack != null);
    }

    /* package */ boolean isNotSeekablePastBuffer() {
        // Some phones on 2.2 ship with broken opencore
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && StreamProxy.isOpenCore();
    }


    private boolean isPastBuffer(long pos) {
        return (pos / (double) mCurrentTrack.duration) * 100 > mLoadPercent;
    }

    /* package */ long seek(long pos, boolean performSeek) {
        if (isSeekable()) {
            if (pos <= 0) {
                pos = 0;
            }
            final long currentPos = position();
            // workaround for devices which can't do content-range requests
            if (isNotSeekablePastBuffer() && isPastBuffer(pos)) {
                Log.d(TAG, "MediaPlayer bug: cannot seek past buffer");
                return currentPos;
            } else {
                long duration = getDuration();

                final long newPos;
                // don't go before the playhead if they are trying to seek
                // beyond, just maintain their current position
                if (pos > currentPos && currentPos > duration) {
                    newPos = currentPos;
                } else if (pos > duration) {
                    newPos = duration;
                } else {
                    newPos = pos;
                }

                if (performSeek && newPos != currentPos) {
                    mMediaPlayer.seekTo((int) newPos);
                }
                return newPos;
            }
        } else {
            return -1;
        }
    }

    private void setVolume(float vol) {
        if (mMediaPlayer != null && state != ERROR) {
            try {
                mMediaPlayer.setVolume(vol, vol);
            } catch (IllegalStateException ignored) {
                Log.w(TAG, ignored);
            }
        }
    }

    /* package */ boolean isPlaying() {
        try {
            return mMediaPlayer != null && mMediaPlayer.isPlaying() && isSupposedToBePlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private boolean isConnected() {
        return connectivityListener != null && connectivityListener.isConnected();
    }

    /* package */ void setAutoAdvance(boolean autoAdvance) {
        mAutoAdvance = autoAdvance;
    }

    private void addFavorite() {
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

    private void removeFavorite() {
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


    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    private final Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (!isSupposedToBePlaying() && !mResumeAfterCall && !mServiceInUse
                    && !mPlayerHandler.hasMessages(TRACK_ENDED)) {

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
                    seek(0, true);
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


    private final Handler mPlayerHandler = new Handler() {
        private static final float DUCK_VOLUME = 0.1f;

        float mCurrentVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage("+msg.what+", state="+state+")");
            }
            switch (msg.what) {
                case NOTIFY_META_CHANGED:
                    notifyChange(META_CHANGED);
                    break;
                case FADE_IN:
                    removeMessages(FADE_OUT);
                    if (!isSupposedToBePlaying()) {
                        mCurrentVolume = 0f;
                        setVolume(0f);
                        play();
                        sendEmptyMessageDelayed(FADE_IN, 10);
                    } else {
                        mCurrentVolume += 0.01f;
                        if (mCurrentVolume < 1.0f) {
                            sendEmptyMessageDelayed(FADE_IN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        setVolume(mCurrentVolume);
                    }
                    break;
                case FADE_OUT:
                    removeMessages(FADE_IN);
                    if (isPlaying())  {
                        mCurrentVolume -= 0.01f;
                        if (mCurrentVolume > 0f) {
                            sendEmptyMessageDelayed(FADE_OUT, 10);
                        } else {
                            pause();
                            mCurrentVolume = 0f;
                        }
                        setVolume(mCurrentVolume);
                    } else {
                        setVolume(0f);
                    }
                    break;
                case DUCK:
                    removeMessages(FADE_IN);
                    removeMessages(FADE_OUT);
                    setVolume(DUCK_VOLUME);
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
                        // account for lack of accuracy in actual delay between checks
                        final long window = (long) (TRACK_EVENT_CHECK_DELAY * 1.5);

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
                    mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                    if (!m10percentStampReached || !m95percentStampReached) {
                        mPlayerHandler.sendMessageDelayed(mPlayerHandler.obtainMessage(CHECK_TRACK_EVENT),
                                TRACK_EVENT_CHECK_DELAY);
                    }
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
                if (Log.isLoggable(TAG, Log.DEBUG) && mLoadPercent != percent) {
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
                Message msg = mPlayerHandler.obtainMessage(CLEAR_LAST_SEEK);
                mPlayerHandler.removeMessages(CLEAR_LAST_SEEK);
                mPlayerHandler.sendMessageDelayed(msg, 3000);
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
                if (state != ERROR) {
                    mPlayerHandler.sendEmptyMessage(TRACK_ENDED);
                    getApp().trackEvent(Consts.Tracking.Categories.TRACKS, Consts.Tracking.Actions.TRACK_COMPLETE,
                            mCurrentTrack.getTrackEventLabel());
                } else {
                    stop(); // XXX
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
                        setVolume(0);
                        // this will also play
                        mPlayerHandler.sendEmptyMessage(FADE_IN);
                    }

                    if (mResumeTrackId == mCurrentTrack.id) {
                        seek(mResumeTime, true);
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
            Log.e(TAG, "onError("+what+ ", "+extra+")");

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

                mFocus.abandonMusicFocus();

                gotoIdleState();
                notifyChange(isConnected() ? PLAYBACK_ERROR : STREAM_DIED);
                notifyChange(PLAYBACK_COMPLETE);
            }
            return true;
        }
    };

    // this is only used in pre 2.2 phones where there is no audiofocus support
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCallStateChanged("+incomingNumber+")");
            }
            if (state == TelephonyManager.CALL_STATE_OFFHOOK ||
               (state == TelephonyManager.CALL_STATE_RINGING &&
                 mAudioManager.getStreamVolume(AudioManager.STREAM_RING) > 0)) {
                    mResumeAfterCall = (isSupposedToBePlaying() || mResumeAfterCall) && mCurrentTrack != null;
                    mPlayerHandler.sendEmptyMessage(FADE_OUT);
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                // start playing again
                if (mResumeAfterCall) {
                    // resume playback only if music was playing when the call was answered
                    mPlayerHandler.sendEmptyMessageDelayed(FADE_IN, 10);
                    mResumeAfterCall = false;
                }
            }
        }
    };

}
