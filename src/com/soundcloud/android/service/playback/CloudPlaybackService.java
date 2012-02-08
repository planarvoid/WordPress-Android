package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.State.*;

import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.*;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.streaming.StreamProxy;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.task.LoadTrackInfoTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;

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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import com.soundcloud.android.view.PlaybackRemoteViews;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class CloudPlaybackService extends Service implements FocusHelper.MusicFocusable {
    public static final String TAG = "CloudPlaybackService";
    public static List<Playable> playlistXfer;

    public static final String PLAYSTATE_CHANGED  = "com.soundcloud.android.playstatechanged";
    public static final String META_CHANGED       = "com.soundcloud.android.metachanged";
    public static final String PLAYLIST_CHANGED   = "com.soundcloud.android.playlistchanged";
    public static final String PLAYBACK_COMPLETE  = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_ERROR     = "com.soundcloud.android.trackerror";
    public static final String STREAM_DIED        = "com.soundcloud.android.streamdied";
    public static final String COMMENTS_LOADED    = "com.soundcloud.android.commentsloaded";
    public static final String FAVORITE_SET       = "com.soundcloud.android.favoriteset";
    public static final String SEEK_COMPLETE      = "com.soundcloud.android.seekcomplete";
    public static final String BUFFERING          = "com.soundcloud.android.buffering";
    public static final String BUFFERING_COMPLETE = "com.soundcloud.android.bufferingcomplete";
    public static final String SERVICECMD         = "com.soundcloud.android.musicservicecommand";

    public static final String TOGGLEPAUSE_ACTION = "com.soundcloud.android.musicservicecommand.togglepause";
    public static final String PAUSE_ACTION       = "com.soundcloud.android.musicservicecommand.pause";
    public static final String PREVIOUS_ACTION    = "com.soundcloud.android.musicservicecommand.previous";
    public static final String NEXT_ACTION        = "com.soundcloud.android.musicservicecommand.next";
    public static final String PLAY               = "com.soundcloud.android.musicservicecommand.play";
    public static final String RESET_ALL          = "com.soundcloud.android.musicservicecommand.resetall";
    public static final String STOP_ACTION        = "com.soundcloud.android.musicservicecommand.stop";

    public static final String EXTRA_FROM_NOTIFICATION  = "com.soundcloud.android.musicserviceextra.fromNotification";

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

    private static final float FADE_CHANGE = 0.02f; // change to fade faster/slower

    private MediaPlayer mMediaPlayer;
    private int mLoadPercent = 0;       // track buffer indicator
    private boolean mAutoPause = true;  // used when svc is first created and playlist is resumed on start
    private boolean mAutoAdvance = true;// automatically skip to next track
    protected NetworkConnectivityListener connectivityListener;
    /* package */ PlaylistManager mPlaylistManager;
    private Track mCurrentTrack;
    private RemoteViews mNotificationView;
    private AudioManager mAudioManager;

    private long mResumeTime = -1;      // time of played track
    private long mResumeTrackId = -1;   // id of last played track
    private long mSeekPos = -1;         // desired seek position
    private int mConnectRetries = 0;

    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private PlayerAppWidgetProvider mAppWidgetProvider = PlayerAppWidgetProvider.getInstance();

    private static final int IDLE_DELAY = 60*1000;            // interval after which we stop the service when idle
    private static final long TRACK_EVENT_CHECK_DELAY = 1000; // check for track timestamp events at this frequency

    private boolean resumeSeeking;

    private long m10percentStamp;
    private long m95percentStamp;

    private boolean m10percentStampReached;
    private boolean m95percentStampReached;

    private StreamProxy mProxy;
    private TrackCache mCache;

    // audio focus related
    private FocusHelper mFocus;
    private boolean mTransientFocusLoss;

    private State state = STOPPED;

    private final IBinder mBinder = new ServiceStub(this);
    private WeakReference<Bitmap> mDefaultArtwork;
    public static final ImageLoader.Options ICON_OPTIONS = new ImageLoader.Options(false);
    private Notification status;

    public interface PlayExtras{
        String trackId = "track_id";
        String playPosition = "play_position";
        String playFromXferCache = "play_from_xfer_cache";
    }

    public interface BroadcastExtras{
        String id = "id";
        String title = "title";
        String user_id = "user_id";
        String username = "username";
        String isPlaying = "isPlaying";
        String isSupposedToBePlaying = "isSupposedToBePlaying";
        String isBuffering = "isBuffering";
        String position = "position";
        String queuePosition = "queuePosition";
        String isFavorite = "isFavorite";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCache = SoundCloudApplication.TRACK_CACHE;
        mPlaylistManager = new PlaylistManager(this, mCache, getApp().getCurrentUserId());

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
        commandFilter.addAction(STOP_ACTION);
        registerReceiver(mIntentReceiver, commandFilter);
        registerReceiver(mIntentReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(mIntentReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        mFocus = new FocusHelper(this, this);

        if (!mFocus.isSupported()) {
            // setup call listening if not handled by audiofocus
            TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        scheduleServiceShutdownCheck();

        mResumeTime = mPlaylistManager.reloadQueue();
        mCurrentTrack = mPlaylistManager.getCurrentTrack();
        if (mCurrentTrack != null && mResumeTime > 0) {
            mResumeTrackId = mCurrentTrack.id;
        }

        try {
            mProxy = new StreamProxy(getApp()).init().start();
        } catch (IOException e) {
            Log.i(TAG, "Unable to start service " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mPlayerHandler.removeCallbacksAndMessages(null);
        mPlaylistManager.onDestroy();

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

        mPlaylistManager.saveQueue(mCurrentTrack == null ? 0 : getPosition());

        if (state.isSupposedToBePlaying() || state == PAUSED_FOCUS_LOST) {
            // something is currently playing, or will be playing once
            // an in-progress call ends, so don't stop the service now.
            return true;

        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between
        // tracks.
        } else if (!mPlaylistManager.isEmpty() || mPlayerHandler.hasMessages(TRACK_ENDED)) {
            mDelayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
            return true;
        } else {
            // No active playlist, OK to stop the service right now
            stopSelf(mServiceStartId);
            return true;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onStartCommand(action="+action+", cmd="+cmd+", intent="+intent+")");
            }
            mIntentReceiver.onReceive(this, intent);
        }
        scheduleServiceShutdownCheck();
        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        return START_STICKY;
    }

    @Override
    public void focusGained() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "focusGained");
        if ((state.isSupposedToBePlaying() || state == State.PAUSED_FOCUS_LOST) && mTransientFocusLoss) {
            mPlayerHandler.sendEmptyMessage(FADE_IN);
            mTransientFocusLoss = false;
        } else {
            setVolume(1.0f);
        }
    }

    @Override
    public void focusLost(boolean isTransient, boolean canDuck) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "focusLost("+isTransient+", canDuck="+canDuck+")");
        }
        if (state.isSupposedToBePlaying()) {
            mPlayerHandler.sendEmptyMessage(canDuck ? DUCK : FADE_OUT);
        }
        mTransientFocusLoss = isTransient;
    }

    private void scheduleServiceShutdownCheck() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "scheduleServiceShutdownCheck()");
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
    }

    private void notifyChange(String what) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "notifyChange(" + what + ")");
        }
        Intent i = new Intent(what)
            .putExtra(BroadcastExtras.id, getTrackId())
            .putExtra(BroadcastExtras.title, getTrackName())
            .putExtra(BroadcastExtras.user_id, getUserId())
            .putExtra(BroadcastExtras.username, getUserName())
            .putExtra(BroadcastExtras.isPlaying, isPlaying())
            .putExtra(BroadcastExtras.isSupposedToBePlaying, state.isSupposedToBePlaying())
            .putExtra(BroadcastExtras.isBuffering, isBuffering())
            .putExtra(BroadcastExtras.position, getPosition())
            .putExtra(BroadcastExtras.queuePosition, mPlaylistManager.getPosition())
            .putExtra(BroadcastExtras.isFavorite, getIsFavorite());

        sendBroadcast(i);

        if (what.equals(META_CHANGED) || what.equals(PLAYBACK_ERROR) || what.equals(PLAYBACK_COMPLETE)) {
            mPlaylistManager.saveQueue(mCurrentTrack == null ? 0 : getPosition());
        }


        // Share this notification directly with our widgets
        mAppWidgetProvider.notifyChange(this, i);
    }

    /* package */ void openCurrent() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "openCurrent(state="+state+")");
        }

        final Track track = mPlaylistManager.getCurrentTrack();
        if (track != null) {
            if (mAutoPause) {
                mAutoPause = false;
            }

            mLoadPercent = 0;
            if (track.equals(mCurrentTrack) && track.isStreamable()) {
                notifyChange(META_CHANGED);
                startTrack(track);
            } else {
                mCurrentTrack = track;
                notifyChange(META_CHANGED);
                mConnectRetries = 0; // new track, reset connection attempts

                if (track.isStreamable()) {
                    onStreamableTrack(track);
                } else if (!CloudUtils.isTaskFinished(track.load_info_task)) {
                    track.load_info_task.addListener(mInfoListener);
                } else {
                    onUnstreamableTrack(track.id);
                }
            }

        } else {
            Log.d(TAG, "playlist is empty");
        }
    }

    private LoadTrackInfoTask.LoadTrackInfoListener mInfoListener = new LoadTrackInfoTask.LoadTrackInfoListener() {
        @Override
        public void onTrackInfoLoaded(Track track, String action) {
            if (track.isStreamable()) {
                onStreamableTrack(track);
            } else {
                onUnstreamableTrack(track.id);
            }
        }

        @Override
        public void onTrackInfoError(long trackId) {
            onUnstreamableTrack(trackId);
        }
    };

    private void onUnstreamableTrack(long trackId){
        if (mCurrentTrack.id != trackId) return;

        mPlayerHandler.sendEmptyMessage(STREAM_EXCEPTION);
        gotoIdleState(STOPPED);
    }

    private void onStreamableTrack(Track track){
        if (mCurrentTrack.id != track.id) return;

        m10percentStamp = (long) (mCurrentTrack.duration * .1);
        m10percentStampReached = false;
        m95percentStamp = (long) (mCurrentTrack.duration * .95);
        m95percentStampReached = false;

        new Thread() {
            @Override
            public void run() {
                if (SoundCloudDB.markTrackAsPlayed(getContentResolver(), mCurrentTrack)) {
                    mPlayerHandler.sendEmptyMessage(NOTIFY_META_CHANGED);
                }
            }
        }.start();

        getApp().trackEvent(
                Consts.Tracking.Categories.TRACKS,
                Consts.Tracking.Actions.TRACK_PLAY,
                mCurrentTrack.getTrackEventLabel());
        startTrack(track);
    }

    private void startTrack(Track track) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "startTrack("+track.title+")");
        }
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

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
            Track next = mPlaylistManager.getNext();
            mMediaPlayer.setDataSource(mProxy.createUri(mCurrentTrack.stream_url, next == null ? null : next.stream_url).toString());
            mMediaPlayer.prepareAsync();
            notifyChange(BUFFERING);

        } catch (IllegalStateException e) {
            Log.e(TAG, "error", e);
            gotoIdleState(ERROR);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            errorListener.onError(mMediaPlayer, 0, 0);
        }
    }


    /* package */ void play() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "play(state=" + state + ")");
        if (mCurrentTrack != null && mFocus.requestMusicFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (mMediaPlayer != null && state.isStartable()) {
                // resume
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.start");

                mMediaPlayer.start();
                state = PLAYING;
                setPlayingNotification(mCurrentTrack);

                mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                mPlayerHandler.sendEmptyMessageDelayed(CHECK_TRACK_EVENT, TRACK_EVENT_CHECK_DELAY);

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
            if (state.isPausable() && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mFocus.abandonMusicFocus(true);
                gotoIdleState(PAUSED);
            } else {
                // get into a determined state
                stop();
            }
        }
        notifyChange(PLAYSTATE_CHANGED);
    }

    /* package */ void stop() {
        // this is not usually called due to errors, not user interaction
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "stop(state="+state+")");
        }
        if (mMediaPlayer != null) {
            if (state.isStoppable()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mFocus.abandonMusicFocus(false);
        gotoIdleState(STOPPED);
    }


    private void gotoIdleState(State newState) {
        state = newState;
        mPlayerHandler.removeMessages(FADE_OUT);
        mPlayerHandler.removeMessages(FADE_IN);
        mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
        scheduleServiceShutdownCheck();
        if (useRichNotifications()){
            stopForeground(false);
            if (status != null){
                ((PlaybackRemoteViews) status.contentView).setPlaybackStatus(isPlaying());
            NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mManager.notify(PLAYBACKSERVICE_STATUS_ID, status);
            }
        } else {
            stopForeground(true);
        }
    }

    /* package */ State getState() {
        return state;
    }

    /* package */ void prev() {
        if (mPlaylistManager.prev()) openCurrent();
    }

    /* package */ void next() {
        if (mPlaylistManager.next()) openCurrent();
    }


    private void setPlayingNotification(final Track track) {
        if (track == null) return;

        status = new Notification();
        status.flags |= Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.statusbar;

        Intent intent = new Intent(Actions.PLAYER);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

        if (!useRichNotifications()) {
            status.setLatestEventInfo(this, track.getUserName(),track.title, pi);
        } else {

            if (mNotificationView == null){
                mNotificationView = new PlaybackRemoteViews(getPackageName(), R.layout.playback_status_v11);
            }
            ((PlaybackRemoteViews) mNotificationView).setCurrentTrack(track.title,track.user.username);
            ((PlaybackRemoteViews) mNotificationView).linkButtons(this,track.id,track.user_id,track.user_favorite, EXTRA_FROM_NOTIFICATION);
            ((PlaybackRemoteViews) mNotificationView).setPlaybackStatus(state.isSupposedToBePlaying());

            final String artworkUri = track.getListArtworkUrl(getApplicationContext());
            final Bitmap bmp = ImageLoader.get(getApplicationContext()).getBitmap(artworkUri,null, ICON_OPTIONS);
            if (bmp != null){
                ((PlaybackRemoteViews) mNotificationView).setIcon(bmp);
            } else {
                ((PlaybackRemoteViews) mNotificationView).setIcon(getDefaultArtwork());
                ImageLoader.get(getApplicationContext()).getBitmap(artworkUri,new ImageLoader.BitmapCallback(){
                    public void onImageLoaded(Bitmap mBitmap, String uri) {if (track == mCurrentTrack) setPlayingNotification(mCurrentTrack);}
                    public void onImageError(String uri, Throwable error) {}
                });

            }
            status.contentView = mNotificationView;
            status.contentIntent = pi;
        }

        startForeground(PLAYBACKSERVICE_STATUS_ID, status);
    }

    private boolean useRichNotifications() {
        return false;
        //return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    private Bitmap getDefaultArtwork() {
        if (mDefaultArtwork == null || mDefaultArtwork.get() == null){
            Bitmap defaultArtwork = null;
            try {
                 defaultArtwork = BitmapFactory.decodeResource(getResources(),R.drawable.artwork_badge);
            } catch (OutOfMemoryError ignored){}
            if (defaultArtwork != null) mDefaultArtwork = new WeakReference<Bitmap>(defaultArtwork);
        }
        return mDefaultArtwork.get();
    }

    /* package */ void setQueuePosition(int pos) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setQueuePosition("+pos+")");

        if (mPlaylistManager.getPosition() != pos &&
            mPlaylistManager.setPosition(pos)) {
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

    /* package */ Track getCurrentTrack() {
        return mCurrentTrack == null ? mPlaylistManager.getCurrentTrack() : mCurrentTrack;
    }

    /* package */ long getCurrentTrackId() {
        return mCurrentTrack == null ? mPlaylistManager.getCurrentTrackId() : mCurrentTrack.id;
    }

    /* package */ int getDuration() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.duration;
    }

    /* package */ boolean isBuffering() {
        return state == PAUSED_FOR_BUFFERING || state == PREPARING || resumeSeeking;
    }

    /*
     * Returns the current playback position in milliseconds
     */
    /* package */ long getPosition() {
        if (mMediaPlayer != null && !state.isError()) {
            return mMediaPlayer.getCurrentPosition();
        } else if (mCurrentTrack != null && mResumeTrackId == mCurrentTrack.id) {
            return mResumeTime; // either -1 or a valid resume time
        } else {
            return 0;
        }
    }

    /* package */ int loadPercent() {
        return mMediaPlayer != null && !state.isError() ? mLoadPercent : 0;
    }

    /* package */ boolean isSeekable() {
        return (Build.VERSION.SDK_INT >= MINIMUM_SEEKABLE_SDK
                && mMediaPlayer != null
                && state.isSeekable()
                && mCurrentTrack != null);
    }

    /* package */ boolean isNotSeekablePastBuffer() {
        // Some phones on 2.2 ship with broken opencore
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && StreamProxy.isOpenCore();
    }

    /* package */ long seek(long pos, boolean performSeek) {
        if (isSeekable()) {
            if (pos <= 0) {
                pos = 0;
            }
            final long currentPos = getPosition();
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
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "seeking to "+newPos);
                    }
                    mMediaPlayer.seekTo((int) newPos);
                } else {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "NOT seeking to "+newPos);
                    }
                }
                return newPos;
            }
        } else {
            return -1;
        }
    }

    /* package */ boolean isPlaying() {
        try {
            return mMediaPlayer != null && mMediaPlayer.isPlaying() && state.isSupposedToBePlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /* package */ void setAutoAdvance(boolean autoAdvance) {
        mAutoAdvance = autoAdvance;
    }

    private String getUserName() {
        return mCurrentTrack != null && mCurrentTrack.user != null ? mCurrentTrack.user.username : null;
    }

    private long getUserId() {
        return mCurrentTrack != null ? mCurrentTrack.user_id : -1;
    }

    private long getTrackId() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.id;
    }

    private String getTrackName() {
        return mCurrentTrack == null ? null : mCurrentTrack.title;
    }

    private boolean getIsFavorite() {
        return mCurrentTrack != null && mCurrentTrack.user_favorite;
    }

    private boolean isPastBuffer(long pos) {
        return (pos / (double) mCurrentTrack.duration) * 100 > mLoadPercent;
    }

    private void setVolume(float vol) {
        if (mMediaPlayer != null && !state.isError()) {
            try {
                mMediaPlayer.setVolume(vol, vol);
            } catch (IllegalStateException ignored) {
                Log.w(TAG, ignored);
            }
        }
    }

    private boolean isConnected() {
        return connectivityListener != null && connectivityListener.isConnected();
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
        Track track = SoundCloudDB.getTrackById(getContentResolver(), trackId);
        if (track != null) {
            track.user_favorite = isFavorite;
            SoundCloudDB.upsertTrack(getContentResolver(), track);

        }

        if (mCache.containsKey(trackId)) {
            mCache.get(trackId).user_favorite = isFavorite;
        }

        if (mCurrentTrack.id == trackId && mCurrentTrack.user_favorite != isFavorite) {
            mCurrentTrack.user_favorite = isFavorite;
            notifyChange(FAVORITE_SET);
        } else {
            final Intent intent = new Intent(FAVORITE_SET);
            sendBroadcast(intent
                .putExtra("id", trackId)
                .putExtra("isFavorite", isFavorite));
            // Share this notification directly with our widgets
            mAppWidgetProvider.notifyChange(this, intent);
        }
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    private final Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (!state.isSupposedToBePlaying()
                    && state != PAUSED_FOCUS_LOST
                    && !mServiceInUse
                    && !mPlayerHandler.hasMessages(TRACK_ENDED)) {

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "DelayedStopHandler: stopping service");
                }

                mPlaylistManager.saveQueue(mCurrentTrack == null ? 0 : getPosition());
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

            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                next();
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (state.isSupposedToBePlaying()) {
                    pause();
                } else if (mCurrentTrack != null) {
                    play();
                } else {
                    openCurrent();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
            } else if (PlayerAppWidgetProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to executeRefreshTask a set of specific widgets,
                // probably because they were just added.
                int[] appWidgetIds = intent
                        .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

                mAppWidgetProvider.performUpdate(CloudPlaybackService.this, appWidgetIds,
                        new Intent(PLAYSTATE_CHANGED));

            } else if (ADD_FAVORITE.equals(action)) {
                setFavoriteStatus(intent.getLongExtra("trackId", -1), true);
            } else if (REMOVE_FAVORITE.equals(action)) {
                setFavoriteStatus(intent.getLongExtra("trackId", -1), false);
            } else if (PLAY.equals(action)) {
                handlePlayAction(intent);
            } else if (RESET_ALL.equals(action)) {
                stop();
                mPlaylistManager.clear();
            } else if (CMDSTOP.equals(cmd) || STOP_ACTION.equals(action)) {
                if (state.isSupposedToBePlaying()) pause();
                stop();
                if (intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
                    stopForeground(true);
                }
            }
        }
    };

    private void handlePlayAction(Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "handlePlayAction("+intent+")");
        }
        Track track = intent.getParcelableExtra("track");
        if (track != null) {
            mPlaylistManager.setTrack(track);
            openCurrent();
        } else if (intent.getData() != null) {
            mPlaylistManager.setUri(intent.getData(),
                    intent.getIntExtra(PlayExtras.playPosition, 0),
                    intent.getLongExtra(PlayExtras.trackId, -1)
            );
            openCurrent();
        } else if (intent.getBooleanExtra(PlayExtras.playFromXferCache, false)) {
            mPlaylistManager.setPlaylist(playlistXfer,intent.getIntExtra(PlayExtras.playPosition, 0));
            playlistXfer = null;
            openCurrent();
        } else {
            Log.w(TAG, "invalid play action");
        }
    }

    private final Handler mPlayerHandler = new Handler() {
        private static final float DUCK_VOLUME = 0.1f;
        private float mCurrentVolume = 1.0f;

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

                    if (!state.isSupposedToBePlaying()) {
                        mCurrentVolume = 0f;
                        setVolume(0f);
                        play();
                        sendEmptyMessageDelayed(FADE_IN, 10);
                    } else {
                        mCurrentVolume += FADE_CHANGE;
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
                        mCurrentVolume -= FADE_CHANGE;
                        if (mCurrentVolume > 0f) {
                            sendEmptyMessageDelayed(FADE_OUT, 10);
                        } else {
                            mCurrentVolume = 0f;
                            mMediaPlayer.pause();
                            state = PAUSED_FOCUS_LOST;
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
                        gotoIdleState(COMPLETED);
                    }
                    break;
                case CLEAR_LAST_SEEK:
                    mSeekPos = -1;
                    break;
                case CHECK_TRACK_EVENT:
                    if (mCurrentTrack != null) {
                        final long pos = getPosition();
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
                        mPlayerHandler.sendEmptyMessageDelayed(CHECK_TRACK_EVENT, TRACK_EVENT_CHECK_DELAY);
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
                    break;
                default:
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
                Log.d(TAG, "onSeekComplete(state="+state+")");
            }

            if (mMediaPlayer == mp) {
                // keep the last seek time for 3000 ms because getCurrentPosition will be incorrect at first
                mPlayerHandler.removeMessages(CLEAR_LAST_SEEK);
                mPlayerHandler.sendEmptyMessageDelayed(CLEAR_LAST_SEEK, 3000);
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
            final long targetPosition = (mSeekPos == -1) ? mMediaPlayer.getCurrentPosition() : mSeekPos;
            // premature track end ?
            if (isSeekable() && getDuration() - targetPosition > 3000) {
                Log.w(TAG, "premature end of track (targetpos="+targetPosition+")");
                // track ended prematurely (probably end of buffer, unreported IO error),
                // so try to resume at last time
                mResumeTrackId = mCurrentTrack.id;
                mResumeTime = targetPosition;
                errorListener.onError(mp, MediaPlayer.MEDIA_ERROR_UNKNOWN, Errors.STAGEFRIGHT_ERROR_BUFFER_EMPTY);
            } else if (!state.isError()) {
                getApp().trackEvent(
                        Consts.Tracking.Categories.TRACKS,
                        Consts.Tracking.Actions.TRACK_COMPLETE,
                        mCurrentTrack.getTrackEventLabel());

                mPlayerHandler.sendEmptyMessage(TRACK_ENDED);
            } else {
                // onComplete must have been called in error state
                stop();
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

                    // do we need to resume a track position ?
                    if (mCurrentTrack.id == mResumeTrackId && mResumeTime > 0) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "resuming to "+mResumeTime);
                        }
                        seek(mResumeTime, true);
                        mResumeTime = mResumeTrackId -1;
                        resumeSeeking = true;
                        play();

                    // normal play, unless first start (autopause=true)
                    } else {
                        setVolume(0);
                        //  FADE_IN will call play()
                        notifyChange(BUFFERING_COMPLETE);
                        if (!mAutoPause && mFocus.requestMusicFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                            mPlayerHandler.sendEmptyMessage(FADE_IN);
                        }
                    }
                } else {
                    stop();
                }
            }
        }
    };

    MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(TAG, "onError("+what+ ", "+extra+", state="+state+")");

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
                mMediaPlayer.release();
                mMediaPlayer = null;
                mFocus.abandonMusicFocus(false);

                gotoIdleState(ERROR);
                notifyChange(isConnected() ? PLAYBACK_ERROR : STREAM_DIED);
                notifyChange(PLAYBACK_COMPLETE);
            }
            return true;
        }
    };

    // this is only used in pre 2.2 phones where there is no audio focus support
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int callState, String incomingNumber) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCallStateChanged(state="+callState+", playerState="+state+")");
            }
            if (callState == TelephonyManager.CALL_STATE_OFFHOOK ||
               (callState == TelephonyManager.CALL_STATE_RINGING &&
                 mAudioManager.getStreamVolume(AudioManager.STREAM_RING) > 0)) {
                focusLost(true, false);
            } else if (callState == TelephonyManager.CALL_STATE_IDLE) {
                focusGained();
            }
        }
    };
}
