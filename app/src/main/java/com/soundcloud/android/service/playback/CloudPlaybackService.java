package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.State.*;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.streaming.StreamItem;
import com.soundcloud.android.streaming.StreamProxy;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.Media;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DebugUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.play.PlaybackRemoteViews;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
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

import java.io.IOException;
import java.util.List;

public class CloudPlaybackService extends Service implements IAudioManager.MusicFocusable, Tracker {
    public static final String TAG = "CloudPlaybackService";
    public static List<Playable> playlistXfer;

    private static Track currentTrack;
    public static Track getCurrentTrack()  { return currentTrack; }
    public static long getCurrentTrackId() { return currentTrack == null ? -1 : currentTrack.id; }
    public static Boolean isTrackPlaying(long id) { return getCurrentTrackId() == id && state.isSupposedToBePlaying(); }

    private static State state = STOPPED;
    public static State getState() { return state; }


    // public service actions
    public static final String PLAY_ACTION          = "com.soundcloud.android.playback.play";
    public static final String TOGGLEPAUSE_ACTION   = "com.soundcloud.android.playback.togglepause";
    public static final String PAUSE_ACTION         = "com.soundcloud.android.playback.pause";
    public static final String NEXT_ACTION          = "com.soundcloud.android.playback.next";
    public static final String PREVIOUS_ACTION      = "com.soundcloud.android.playback.previous";

    public static final String RESET_ALL            = "com.soundcloud.android.playback.reset"; // used on logout
    public static final String STOP_ACTION          = "com.soundcloud.android.playback.stop"; // from the notification
    public static final String UPDATE_WIDGET_ACTION = "com.soundcloud.android.playback.updatewidgetaction";

    public static final String ADD_FAVORITE_ACTION    = "com.soundcloud.android.favorite.add";
    public static final String REMOVE_FAVORITE_ACTION = "com.soundcloud.android.favorite.remove";

    // broadcast notifications
    public static final String PLAYSTATE_CHANGED  = "com.soundcloud.android.playstatechanged";
    public static final String META_CHANGED       = "com.soundcloud.android.metachanged";
    public static final String PLAYLIST_CHANGED   = "com.soundcloud.android.playlistchanged";
    public static final String PLAYBACK_COMPLETE  = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_ERROR     = "com.soundcloud.android.trackerror";
    public static final String STREAM_DIED        = "com.soundcloud.android.streamdied";
    public static final String TRACK_UNAVAILABLE  = "com.soundcloud.android.trackunavailable";
    public static final String COMMENTS_LOADED    = "com.soundcloud.android.commentsloaded";
    public static final String FAVORITE_SET       = "com.soundcloud.android.favoriteset";
    public static final String SEEKING            = "com.soundcloud.android.seeking";
    public static final String SEEK_COMPLETE      = "com.soundcloud.android.seekcomplete";
    public static final String BUFFERING          = "com.soundcloud.android.buffering";
    public static final String BUFFERING_COMPLETE = "com.soundcloud.android.bufferingcomplete";

    // extras
    public static final String EXTRA_UNMUTE       = "com.soundcloud.android.playback.extra.unmute"; // used by alarm clock
    public static final String EXTRA_TRACK_ID     = "com.soundcloud.android.playback.extra.trackId";

    // private stuff
    private static final int TRACK_ENDED      = 1;
    private static final int SERVER_DIED      = 2;
    private static final int FADE_IN          = 3;
    private static final int FADE_OUT         = 4;
    private static final int DUCK             = 5;
    private static final int CLEAR_LAST_SEEK  = 6;
    private static final int STREAM_EXCEPTION = 7;
    private static final int CHECK_TRACK_EVENT = 8;
    private static final int NOTIFY_META_CHANGED = 9;

    private static final int PLAYBACKSERVICE_STATUS_ID = 1;
    private static final int MINIMUM_SEEKABLE_SDK = Build.VERSION_CODES.ECLAIR_MR1; // 7, 2.1

    private static final float FADE_CHANGE = 0.02f; // change to fade faster/slower
    private static final String AUDIO_BECOMING_NOISY = "android.media.AUDIO_BECOMING_NOISY";

    private MediaPlayer mMediaPlayer;
    private int mLoadPercent = 0;       // track buffer indicator
    private boolean mAutoPause = true;  // used when svc is first created and playlist is resumed on start
    private boolean mAutoAdvance = true;// automatically skip to next track
    /* package */ PlaylistManager mPlaylistManager;
    private AudioManager mAudioManager;

    private long mResumeTime = -1;      // time of played track
    private long mResumeTrackId = -1;   // id of last played track
    private long mSeekPos = -1;         // desired seek position
    private int mConnectRetries = 0;
    private long mLastRefresh;          // time last refresh hit was sent

    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private PlayerAppWidgetProvider mAppWidgetProvider = PlayerAppWidgetProvider.getInstance();

    private static final int IDLE_DELAY = 60*1000;  // interval after which we stop the service when idle
    private static final long CHECK_TRACK_EVENT_DELAY = Media.REFRESH_MIN; // check for track timestamp events at this frequency

    private boolean mWaitingForSeek;

    private StreamProxy mProxy;
    private TrackCache mCache;

    // audio focus related
    private IAudioManager mFocus;
    private boolean mTransientFocusLoss;

    private final IBinder mBinder = new LocalBinder<CloudPlaybackService>() {
        @Override public CloudPlaybackService getService() {
            return CloudPlaybackService.this;
        }
    };

    private static final ImageLoader.Options ICON_OPTIONS = new ImageLoader.Options(false);
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
        mPlaylistManager = new PlaylistManager(this, mCache, SoundCloudApplication.getUserId());
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        commandFilter.addAction(ADD_FAVORITE_ACTION);
        commandFilter.addAction(REMOVE_FAVORITE_ACTION);
        commandFilter.addAction(RESET_ALL);
        commandFilter.addAction(STOP_ACTION);
        commandFilter.addAction(PLAYLIST_CHANGED);

        registerReceiver(mIntentReceiver, commandFilter);
        registerReceiver(mNoisyReceiver, new IntentFilter(AUDIO_BECOMING_NOISY));

        mFocus = AudioManagerFactory.createAudioManager(this);
        if (!mFocus.isFocusSupported()) {
            // setup call listening if not handled by audiofocus
            TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        scheduleServiceShutdownCheck();

        mResumeTime = mPlaylistManager.reloadQueue();
        currentTrack = mPlaylistManager.getCurrentTrack();
        if (currentTrack != null && mResumeTime > 0) {
            mResumeTrackId = currentTrack.id;
        }

        try {
            mProxy = new StreamProxy(getApp()).init().start();
        } catch (IOException e) {
            Log.e(TAG, "Unable to start service ", e);
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

        mFocus.abandonMusicFocus(false);
        unregisterReceiver(mIntentReceiver);
        unregisterReceiver(mNoisyReceiver);
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
            if (isTransient){
                mPlayerHandler.sendEmptyMessage(canDuck ? DUCK : FADE_OUT);
            } else {
                pause();
            }
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
            .putExtra(BroadcastExtras.position, getProgress())
            .putExtra(BroadcastExtras.queuePosition, mPlaylistManager.getPosition())
            .putExtra(BroadcastExtras.isFavorite, getIsFavorite());

        sendBroadcast(i);

        if (Consts.SdkSwitches.useRichNotifications) {
            if (what.equals(PLAYSTATE_CHANGED)) {
                mFocus.setPlaybackState(state);
                setPlayingNotification(currentTrack);
            } else if (what.equals(META_CHANGED)) {
                onTrackChanged(currentTrack);
            }
        }

        if (what.equals(META_CHANGED) || what.equals(PLAYBACK_ERROR) || what.equals(PLAYBACK_COMPLETE)) {
            saveQueue();
        }
        // Share this notification directly with our widgets
        mAppWidgetProvider.notifyChange(this, i);
    }

    private void saveQueue(){
        mPlaylistManager.saveQueue(currentTrack == null ? 0 : getProgress());
    }

    private void onTrackChanged(final Track track) {
        if (mFocus.isTrackChangeSupported()) {
            final String artworkUri = track.getPlayerArtworkUri(this);
            if (ImageUtils.checkIconShouldLoad(artworkUri)) {
                final Bitmap cached = ImageLoader.get(this).getBitmap(artworkUri, null, new ImageLoader.Options(false));
                if (cached != null) {
                    // use a copy of the bitmap because it is going to get recycled afterwards
                    try {
                        mFocus.onTrackChanged(track, cached.copy(Bitmap.Config.ARGB_8888, false));
                    } catch (OutOfMemoryError e) {
                        mFocus.onTrackChanged(track, null);
                        System.gc();
                        // retry?
                    }
                } else {
                    mFocus.onTrackChanged(track, null);
                    ImageLoader.get(this).getBitmap(artworkUri, new ImageLoader.BitmapCallback() {
                        public void onImageLoaded(Bitmap loadedBmp, String uri) {
                            if (track.equals(currentTrack)) onTrackChanged(track);
                        }
                        public void onImageError(String uri, Throwable error) {}
                    });
                }
            }
        }
    }

    /* package */ void openCurrent() {
        openCurrent(Media.Action.Stop);
    }

    /* package */ void openCurrent(Media.Action action) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "openCurrent(state="+state+")");
        }

        final Track track = mPlaylistManager.getCurrentTrack();
        if (track != null) {
            if (mAutoPause) {
                mAutoPause = false;
            }
            mLoadPercent = 0;
            if (track.equals(currentTrack) && track.isStreamable()) {
                notifyChange(META_CHANGED);
                startTrack(track);
            } else { // new track
                track(Media.fromTrack(currentTrack), action);
                currentTrack = track;
                notifyChange(META_CHANGED);
                mConnectRetries = 0; // new track, reset connection attempts

                if (track.isStreamable()) {
                    onStreamableTrack(track);
                } else if (!AndroidUtils.isTaskFinished(track.load_info_task)) {
                    track.load_info_task.addListener(mInfoListener);
                } else {
                    onUnstreamableTrack(track.id);
                }
            }
        } else {
            Log.d(TAG, "playlist is empty");
            state = EMPTY_PLAYLIST;
        }
    }

    private FetchTrackTask.FetchTrackListener mInfoListener = new FetchTrackTask.FetchTrackListener() {
        @Override
        public void onSuccess(Track track, String action) {
            if (track.isStreamable()) {
                onStreamableTrack(track);
            } else {
                onUnstreamableTrack(track.id);
            }
        }

        @Override
        public void onError(long trackId) {
            onUnstreamableTrack(trackId);
        }
    };

    private void onUnstreamableTrack(long trackId){
        if (currentTrack.id != trackId) return;

        mPlayerHandler.sendEmptyMessage(STREAM_EXCEPTION);
        gotoIdleState(STOPPED);
    }

    private void onStreamableTrack(Track track){
        if (currentTrack.id != track.id) return;

        new Thread() {
            @Override
            public void run() {
                SoundCloudDB.markTrackAsPlayed(getContentResolver(), currentTrack);
            }
        }.start();
        startTrack(track);


    }

    private void startTrack(Track track) {
        track(Page.Sounds_main, track);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "startTrack("+track.title+")");
        }
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        if (mWaitingForSeek) {
            mWaitingForSeek = false;
            releaseMediaPlayer(true);
        } else {
            switch (state) {
                case PREPARING:
                case PAUSED_FOR_BUFFERING:
                    releaseMediaPlayer(true);
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
        }
        state = PREPARING;
        setPlayingNotification(track);
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
            mMediaPlayer.setDataSource(mProxy.createUri(currentTrack.stream_url, next == null ? null : next.stream_url).toString());
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

    private void releaseMediaPlayer(boolean refresh) {
        Log.w(TAG, "stuck in preparing state!");
        final MediaPlayer old = mMediaPlayer;
        new Thread() {
            @Override
            public void run() {
                old.reset();
                old.release();
            }
        }.start();
        mMediaPlayer = refresh ? new MediaPlayer() : null;
    }


    /* package */ void play() {
        if (state.isSupposedToBePlaying()) return;

        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "play(state=" + state + ")");
        track(Media.fromTrack(currentTrack), Media.Action.Play);
        mLastRefresh = System.currentTimeMillis();

        if (currentTrack != null && mFocus.requestMusicFocus(this, IAudioManager.FOCUS_GAIN)) {
            if (mMediaPlayer != null && state.isStartable()) {
                // resume
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.start");
                mMediaPlayer.start();
                state = PLAYING;
                mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                mPlayerHandler.sendEmptyMessageDelayed(CHECK_TRACK_EVENT, CHECK_TRACK_EVENT_DELAY);
                notifyChange(PLAYSTATE_CHANGED);
                if (!Consts.SdkSwitches.useRichNotifications) setPlayingNotification(currentTrack);

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
        if (!state.isSupposedToBePlaying()) return;
        track(Media.fromTrack(currentTrack), Media.Action.Pause);

        if (mMediaPlayer != null) {
            if (state.isPausable() && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
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
        if (state != STOPPED ) {
            saveQueue();

            if (mMediaPlayer != null) {
                if (state.isStoppable()) {
                    mMediaPlayer.stop();
                }
                releaseMediaPlayer(false);
            }
            gotoIdleState(STOPPED);
        }
    }

    public void togglePlayback() {
        if (state.isSupposedToBePlaying()) {
            pause();
        } else if (currentTrack != null) {
            play();
        } else {
            openCurrent();
        }
    }


    private void gotoIdleState(State newState) {
        if (!newState.isInIdleState()) throw new IllegalArgumentException(newState + " is not a valid idle state");

        state = newState;
        mPlayerHandler.removeMessages(FADE_OUT);
        mPlayerHandler.removeMessages(FADE_IN);
        mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
        scheduleServiceShutdownCheck();

        if (Consts.SdkSwitches.useRichNotifications && currentTrack != null && state != STOPPED){
            stopForeground(false);
            setPlayingNotification(currentTrack);
        } else {
            stopForeground(true);
            status = null;
        }
    }

    /* package */ void prev() {
        if (mPlaylistManager.prev()) openCurrent(Media.Action.Backward);
    }

    /* package */ void next() {
        if (mPlaylistManager.next()) openCurrent(Media.Action.Forward);
    }

    private void setPlayingNotification(final Track track) {

        if (track == null || state == STOPPED ||
                (Consts.SdkSwitches.useRichNotifications && status != null && status.contentView != null &&
                    ((PlaybackRemoteViews) status.contentView).isAlreadyNotifying(track, state.isSupposedToBePlaying()))){
            return;
        }

        final Notification notification = new Notification();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.icon = R.drawable.ic_notification_cloud;

        Intent intent = new Intent(Actions.PLAYER);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

        if (!Consts.SdkSwitches.useRichNotifications) {
            notification.setLatestEventInfo(this, track.getUserName(), track.title, pi);
        } else {
            final PlaybackRemoteViews view = new PlaybackRemoteViews(getPackageName(), R.layout.playback_status_v11,
                    R.drawable.ic_notification_play_states,R.drawable.ic_notification_pause_states);
            view.setNotification(track, state.isSupposedToBePlaying());
            view.linkButtonsNotification(this);
            view.setPlaybackStatus(state.isSupposedToBePlaying());

            final String artworkUri = track.getListArtworkUrl(this);
            if (ImageUtils.checkIconShouldLoad(artworkUri)) {
                final Bitmap cachedBmp = ImageLoader.get(this).getBitmap(artworkUri, null, ICON_OPTIONS);
                if (cachedBmp != null) {
                    view.setIcon(cachedBmp);
                } else {
                    view.clearIcon();
                    ImageLoader.get(this).getBitmap(artworkUri, new ImageLoader.BitmapCallback() {
                        public void onImageLoaded(Bitmap bitmap, String uri) {
                            //noinspection ObjectEquality
                            if (currentTrack == track) {
                                view.setIcon(bitmap);
                                startForeground(PLAYBACKSERVICE_STATUS_ID, notification);
                            }
                        }
                        public void onImageError(String uri, Throwable error) {
                        }
                    });
                }
            } else {
                view.clearIcon();
            }
            notification.contentView = view;
            notification.contentIntent = pi;
        }
        startForeground(PLAYBACKSERVICE_STATUS_ID, notification);
        status = notification;
    }

    /* package */
    public void setQueuePosition(int pos) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setQueuePosition("+pos+")");

        if (mPlaylistManager.getPosition() != pos &&
            mPlaylistManager.setPosition(pos)) {
            openCurrent();
        }
    }

    /* package */
    public void setFavoriteStatus(long trackId, boolean favoriteStatus) {
        if (currentTrack != null && currentTrack.id == trackId) {
            if (favoriteStatus) {
                addFavorite();
            } else {
                removeFavorite();
            }
        }
    }

    /* package */ int getDuration() {
        return currentTrack == null ? -1 : currentTrack.duration;
    }

    /* package */
    public boolean isBuffering() {
        return state == PAUSED_FOR_BUFFERING || state == PREPARING || mWaitingForSeek;
    }

    /*
     * Returns the current playback position in milliseconds
     */
    /* package */
    public long getProgress() {
        if (currentTrack != null && mResumeTrackId == currentTrack.id) {
            return mResumeTime; // either -1 or a valid resume time
        } else if (mWaitingForSeek && mSeekPos > 0) {
            return mSeekPos;
        } else if (mMediaPlayer != null && !state.isError() && state != PREPARING) {
            return mMediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    /* package */
    public int loadPercent() {
        return mMediaPlayer != null && !state.isError() ? mLoadPercent : 0;
    }

    /* package */
    public boolean isSeekable() {
        return (Build.VERSION.SDK_INT >= MINIMUM_SEEKABLE_SDK
                && mMediaPlayer != null
                && state.isSeekable()
                && currentTrack != null);
    }

    /* package */ boolean isNotSeekablePastBuffer() {
        // Some phones on 2.2 ship with broken opencore
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && StreamProxy.isOpenCore();
    }

    /* package */
    public long seek(long pos, boolean performSeek) {
        if (isSeekable()) {
            if (pos <= 0) {
                pos = 0;
            }

            final long currentPos = (mMediaPlayer != null && !state.isError()) ? mMediaPlayer.getCurrentPosition() :0;
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
                    mSeekPos = newPos;
                    mWaitingForSeek = true;
                    notifyChange(SEEKING);
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

    /* package */
    public boolean isPlaying() {
        try {
            return mMediaPlayer != null && mMediaPlayer.isPlaying() && state.isSupposedToBePlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public void restartTrack() {
        openCurrent();
    }

    public PlaylistManager getPlaylistManager() {
        return mPlaylistManager;
    }

    public void setAutoAdvance(boolean autoAdvance) {
        mAutoAdvance = autoAdvance;
    }

    private String getUserName() {
        return currentTrack != null ? currentTrack.getUserName() : null;
    }

    private long getUserId() {
        return currentTrack != null ? currentTrack.user_id : -1;
    }

    private long getTrackId() {
        return currentTrack == null ? -1 : currentTrack.id;
    }

    private String getTrackName() {
        return currentTrack == null ? null : currentTrack.title;
    }

    private boolean getIsFavorite() {
        return currentTrack != null && currentTrack.user_favorite;
    }

    private boolean isPastBuffer(long pos) {
        return (pos / (double) currentTrack.duration) * 100 > mLoadPercent;
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

    private void addFavorite() {
        onFavoriteStatusSet(currentTrack.id, true);
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
        f.execute(currentTrack);
    }

    private void removeFavorite() {
        onFavoriteStatusSet(currentTrack.id, false);
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
        f.execute(currentTrack);
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

        if (currentTrack.id == trackId && currentTrack.user_favorite != isFavorite) {
            currentTrack.user_favorite = isFavorite;
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

                if (state != STOPPED) {
                    saveQueue();
                }

                stopSelf(mServiceStartId);
            }
        }
    };

    private final BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            pause();
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "BroadcastReceiver#onReceive("+action+")");
            }
            if (NEXT_ACTION.equals(action)) {
                next();
            } else if (PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (TOGGLEPAUSE_ACTION.equals(action)) {
                togglePlayback();
            } else if (PAUSE_ACTION.equals(action)) {
                pause();
            } else if (UPDATE_WIDGET_ACTION.equals(action)) {
                // Someone asked us to executeRefreshTask a set of specific widgets,
                // probably because they were just added.
                int[] appWidgetIds = intent
                        .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

                mAppWidgetProvider.performUpdate(CloudPlaybackService.this, appWidgetIds,
                        new Intent(PLAYSTATE_CHANGED));

            } else if (ADD_FAVORITE_ACTION.equals(action)) {
                setFavoriteStatus(intent.getLongExtra(EXTRA_TRACK_ID, -1), true);
            } else if (REMOVE_FAVORITE_ACTION.equals(action)) {
                setFavoriteStatus(intent.getLongExtra(EXTRA_TRACK_ID, -1), false);
            } else if (PLAY_ACTION.equals(action)) {
                handlePlayAction(intent);
            } else if (RESET_ALL.equals(action)) {
                stop();
                mPlaylistManager.clear();
            } else if (STOP_ACTION.equals(action)) {
                if (state.isSupposedToBePlaying()) pause();
                mResumeTime = getProgress();
                mResumeTrackId = currentTrack.id;
                stop();

            } else if (PLAYLIST_CHANGED.equals(action)) {
                if (state == EMPTY_PLAYLIST) {
                    openCurrent();
                }
            }
        }
    };

    private void handlePlayAction(Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "handlePlayAction("+intent+")");
        }

        if (intent.getBooleanExtra(EXTRA_UNMUTE, false)) {
            final int volume = (int) Math.round(
                    mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    * 0.75d);
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setting volume to "+volume);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
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
            play();
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
                    if (currentTrack != null) {
                        if (state.isSupposedToBePlaying()) {
                            int refresh = Media.refresh(currentTrack.duration);
                            if (refresh > 0) {
                                long now = System.currentTimeMillis();
                                if (now - mLastRefresh > refresh) {
                                    track(Media.fromTrack(currentTrack), Media.Action.Refresh);
                                    mLastRefresh = now;
                                }
                            }
                        }
                        mPlayerHandler.sendEmptyMessageDelayed(CHECK_TRACK_EVENT, CHECK_TRACK_EVENT_DELAY);
                    } else {
                        mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
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
                    mPlayerHandler.removeMessages(CLEAR_LAST_SEEK);
                    state = PAUSED_FOR_BUFFERING;
                    notifyChange(BUFFERING);
                    break;

                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    if (mSeekPos != -1 && !mWaitingForSeek) {
                        mPlayerHandler.removeMessages(CLEAR_LAST_SEEK);
                        mPlayerHandler.sendEmptyMessageDelayed(CLEAR_LAST_SEEK, 3000);
                    } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Not clearing seek, waiting for seek to finish");
                    }

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
            //noinspection ObjectEquality
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
            //noinspection ObjectEquality
            if (mMediaPlayer == mp) {
                // only clear seek if we are not buffering. If we are buffering, it will be cleared after buffering completes
                if (state != State.PAUSED_FOR_BUFFERING){
                    // keep the last seek time for 3000 ms because getCurrentPosition will be incorrect at first
                    mPlayerHandler.removeMessages(CLEAR_LAST_SEEK);
                    mPlayerHandler.sendEmptyMessageDelayed(CLEAR_LAST_SEEK, 3000);
                } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Not clearing seek, waiting for buffer");
                }


                mWaitingForSeek = false;
                notifyChange(SEEK_COMPLETE);
            }
        }
    };

    final MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCompletion(state="+state+")");
            }
            // mediaplayer seems to reset itself to 0 before this is called in certain builds, so if so,
            // pretend it's finished
            final long targetPosition = (mSeekPos != -1) ? mSeekPos :
                                        (mResumeTime > -1 && mResumeTrackId == getTrackId()) ? mResumeTime :
                                        (mp.getCurrentPosition() <= 0 && state == PLAYING) ? getDuration() : mp.getCurrentPosition();
            // premature track end ?
            if (isSeekable() && getDuration() - targetPosition > 3000) {
                Log.w(TAG, "premature end of track (targetpos="+targetPosition+")");
                // track ended prematurely (probably end of buffer, unreported IO error),
                // so try to resume at last time
                mResumeTrackId = currentTrack.id;
                mResumeTime = targetPosition;
                errorListener.onError(mp, MediaPlayer.MEDIA_ERROR_UNKNOWN, Errors.STAGEFRIGHT_ERROR_BUFFER_EMPTY);
            } else if (!state.isError()) {
                track(Media.fromTrack(currentTrack), Media.Action.Stop);
                mPlayerHandler.sendEmptyMessage(TRACK_ENDED);
            } else {
                // onComplete must have been called in error state
                stop();
            }
        }
    };

    MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            //noinspection ObjectEquality
            if (mp == mMediaPlayer) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onPrepared(state="+state+")");
                }
                if (state == PREPARING) {
                    state = PREPARED;
                    // do we need to resume a track position ?
                    if (currentTrack.id == mResumeTrackId && mResumeTime > 0) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "resuming to "+mResumeTime);
                        }

                        // play before seek to prevent ANR
                        play();
                        seek(mResumeTime, true);
                        mResumeTime = mResumeTrackId = -1;


                    // normal play, unless first start (autopause=true)
                    } else {
                        setVolume(0);
                        //  FADE_IN will call play()
                        notifyChange(BUFFERING_COMPLETE);
                        if (!mAutoPause && mFocus.requestMusicFocus(CloudPlaybackService.this, IAudioManager.FOCUS_GAIN)) {
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
            //noinspection ObjectEquality
            if (mp == mMediaPlayer && state != STOPPED) {
                // when the proxy times out it will just close the connection - different implementations
                // return different error codes. try to reconnect at least twice before giving up.
                if (mConnectRetries++ < 4) {
                    Log.d(TAG, "stream disconnected, retrying (try=" + mConnectRetries + ")");
                    state = ERROR_RETRYING;
                    openCurrent();
                    return true;
                } else {
                    StreamItem item = currentTrack != null ? mProxy.getStreamItem(currentTrack.stream_url) : null;
                    Log.d(TAG, "stream disconnected, giving up");
                    mConnectRetries = 0;
                    DebugUtils.reportMediaPlayerError(CloudPlaybackService.this, item, what, extra);

                    mMediaPlayer.release();
                    mMediaPlayer = null;
                    gotoIdleState(ERROR);

                    if (IOUtils.isConnected(CloudPlaybackService.this)) {
                        notifyChange(item != null && !item.isAvailable() ? TRACK_UNAVAILABLE : PLAYBACK_ERROR);
                    } else {
                        notifyChange(STREAM_DIED);
                    }
                    notifyChange(PLAYBACK_COMPLETE);
                }
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

    public void track(Event event, Object... args) {
        getApp().track(event, args);
    }

    @Override
    public void track(Class<?> klazz, Object... args) {
        getApp().track(klazz, args);
    }


}
