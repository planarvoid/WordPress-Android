package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.PlayQueueManager.ResumeInfo;
import static com.soundcloud.android.service.playback.State.COMPLETED;
import static com.soundcloud.android.service.playback.State.ERROR;
import static com.soundcloud.android.service.playback.State.ERROR_RETRYING;
import static com.soundcloud.android.service.playback.State.PAUSED;
import static com.soundcloud.android.service.playback.State.PAUSED_FOCUS_LOST;
import static com.soundcloud.android.service.playback.State.PAUSED_FOR_BUFFERING;
import static com.soundcloud.android.service.playback.State.PLAYING;
import static com.soundcloud.android.service.playback.State.PREPARED;
import static com.soundcloud.android.service.playback.State.PREPARING;
import static com.soundcloud.android.service.playback.State.STOPPED;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.AnalyticsEngine;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.api.OldCloudAPI;
import com.soundcloud.android.audio.managers.AudioManagerFactory;
import com.soundcloud.android.audio.managers.IAudioManager;
import com.soundcloud.android.audio.managers.IRemoteAudioManager;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.streaming.StreamItem;
import com.soundcloud.android.streaming.StreamProxy;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.Media;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.tracking.eventlogger.Action;
import com.soundcloud.android.tracking.eventlogger.PlayEventTracker;
import com.soundcloud.android.tracking.eventlogger.PlayEventTrackingApi;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DebugUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.PlaybackOperations;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.android.view.play.NotificationPlaybackRemoteViews;
import org.jetbrains.annotations.Nullable;
import rx.android.concurrency.AndroidSchedulers;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;

public class CloudPlaybackService extends Service implements IAudioManager.MusicFocusable, Tracker {
    public static final String TAG = "CloudPlaybackService";

    private static @Nullable CloudPlaybackService instance;
    private static State state = STOPPED;

    // static convenience accessors
    public static @Nullable Track getCurrentTrack()  { return instance == null ? null : instance.mCurrentTrack; }
    public static long getCurrentTrackId() { return instance == null || instance.mCurrentTrack == null ? -1L : instance.mCurrentTrack.getId(); }
    public static boolean isTrackPlaying(long id) { return getCurrentTrackId() == id && state.isSupposedToBePlaying(); }
    public static PlayQueue getPlayQueue() { return instance == null ? PlayQueue.EMPTY : instance.clonePlayQueue(); }
    public static @Nullable Uri getPlayQueueUri() { return instance == null ? null : instance.getPlayQueueInternal().getSourceUri(); }
    public static int getPlayPosition()   { return instance == null ? -1 : instance.getPlayQueueInternal().getPosition(); }
    public static long getCurrentProgress() { return instance == null ? -1 : instance.getProgress(); }
    public static int getLoadingPercent()   { return instance == null ? -1 : instance.loadPercent(); }
    public static State getPlaybackState() { return state; }
    public static boolean isBuffering() {  return instance != null && instance._isBuffering(); }
    public static boolean isSeekable() {  return instance != null && instance._isSeekable(); }

    // public service actions
    public interface Actions {
        String PLAY_ACTION              = "com.soundcloud.android.playback.start";
        String TOGGLEPLAYBACK_ACTION    = "com.soundcloud.android.playback.toggleplayback";
        String PAUSE_ACTION             = "com.soundcloud.android.playback.pause";
        String NEXT_ACTION              = "com.soundcloud.android.playback.next";
        String PREVIOUS_ACTION          = "com.soundcloud.android.playback.previous";
        String RESET_ALL                = "com.soundcloud.android.playback.reset"; // used on logout
        String STOP_ACTION              = "com.soundcloud.android.playback.stop"; // from the notification
        String ADD_LIKE_ACTION          = "com.soundcloud.android.like.add";
        String REMOVE_LIKE_ACTION       = "com.soundcloud.android.like.remove";
        String ADD_REPOST_ACTION        = "com.soundcloud.android.repost.add";
        String REMOVE_REPOST_ACTION     = "com.soundcloud.android.repost.remove";
        String RELOAD_QUEUE             = "com.soundcloud.android.reloadqueue";
        String LOAD_TRACK_INFO          = "com.soundcloud.android.loadTrackInfo";
        String RETRY_RELATED_TRACKS     = "com.soundcloud.android.retryRelatedTracks";
    }

    // broadcast notifications
    public interface Broadcasts {
        String UPDATE_WIDGET_ACTION     = "com.soundcloud.android.playback.updatewidgetaction";
        String PLAYSTATE_CHANGED        = "com.soundcloud.android.playstatechanged";
        String META_CHANGED             = "com.soundcloud.android.metachanged";
        String RELATED_LOAD_STATE_CHANGED = "com.soundcloud.android.related.changed";
        String PLAYQUEUE_CHANGED        = "com.soundcloud.android.playlistchanged";
        String PLAYBACK_COMPLETE        = "com.soundcloud.android.playbackcomplete";
        String PLAYBACK_ERROR           = "com.soundcloud.android.trackerror";
        String STREAM_DIED              = "com.soundcloud.android.streamdied";
        String TRACK_UNAVAILABLE        = "com.soundcloud.android.trackunavailable";
        String COMMENTS_LOADED          = "com.soundcloud.android.commentsloaded";
        String SEEKING                  = "com.soundcloud.android.seeking";
        String SEEK_COMPLETE            = "com.soundcloud.android.seekcomplete";
        String BUFFERING                = "com.soundcloud.android.buffering";
        String BUFFERING_COMPLETE       = "com.soundcloud.android.bufferingcomplete";
        String RESET_ALL                = "com.soundcloud.android.resetAll";
    }


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
    private static final int CHECK_BUFFERING   = 10;

    private static final int PLAYBACKSERVICE_STATUS_ID = 1;

    private static final float FADE_CHANGE = 0.02f; // change to fade faster/slower

    private @Nullable MediaPlayer mMediaPlayer;
    private int mLoadPercent = 0;       // track buffer indicator
    private boolean mAutoPause = true;  // used when svc is first created and playlist is resumed on start
    private boolean mAutoAdvance = true;// automatically skip to next track
    /* package */ AccountOperations mAccountOperations;
    private PlayQueueManager mPlayQueueManager;
    private PlaybackOperations mPlaybackOperations;

    // TODO: this doesn't really belong here. It's only used to PUT likes and reposts, and isn't playback specific.
    /* package */ AssociationManager mAssociationManager;

    private AudioManager mAudioManager;
    private @Nullable Track mCurrentTrack;
    private AndroidCloudAPI mOldCloudApi;

    @Nullable
    private ResumeInfo mResumeInfo;      // info to resume a previous play session
    private long mSeekPos = -1;         // desired seek position
    private int mConnectRetries = 0;
    private long mLastRefresh;          // time last refresh hit was sent

    private int mServiceStartId = -1;
    private boolean mServiceInUse;
    private PlayerAppWidgetProvider mAppWidgetProvider = PlayerAppWidgetProvider.getInstance();

    private static final int IDLE_DELAY = 60*1000;  // interval after which we stop the service when idle
    private static final long CHECK_TRACK_EVENT_DELAY = Media.REFRESH_MIN; // check for track timestamp events at this frequency

    private boolean mWaitingForSeek;

    private StreamProxy mProxy;

    // audio focus related
    private IRemoteAudioManager mFocus;
    private boolean mTransientFocusLoss;

    private final IBinder mBinder = new LocalBinder<CloudPlaybackService>() {
        @Override public CloudPlaybackService getService() {
            return CloudPlaybackService.this;
        }
    };

    private Notification status;
    private PlaybackReceiver mIntentReceiver;

    // for play duration tracking
    private PlayEventTracker mPlayEventTracker;

    private AnalyticsEngine mAnalyticsEngine;

    public PlayEventTracker getPlayEventTracker() {
        return mPlayEventTracker;
    }

    public interface PlayExtras{
        String track = Track.EXTRA;
        String trackId = Track.EXTRA_ID;
        String unmute = "unmute"; // used by alarm clock
        String fetchRelated = "fetch_related";
        String trackingInfo = "tracking_info";
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
        String isLike = "isLike";
        String isRepost = "isRepost";
    }



    @Override
    public void onCreate() {
        super.onCreate();
        mAssociationManager = new AssociationManager(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mPlayEventTracker = new PlayEventTracker(this, new PlayEventTrackingApi(getString(R.string.app_id)));
        mOldCloudApi = new OldCloudAPI(this);
        mAnalyticsEngine = new AnalyticsEngine(getApplicationContext());
        mAccountOperations = new AccountOperations(this);
        mPlaybackOperations = new PlaybackOperations();

        mPlayQueueManager = new PlayQueueManager(this, new PlayQueueStorage(), new ExploreTracksOperations(),
                PreferenceManager.getDefaultSharedPreferences(this), SoundCloudApplication.MODEL_MANAGER);
        mIntentReceiver = new PlaybackReceiver(this, mAssociationManager, mAudioManager, mPlayQueueManager);

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(Actions.PLAY_ACTION);
        commandFilter.addAction(Actions.TOGGLEPLAYBACK_ACTION);
        commandFilter.addAction(Actions.PAUSE_ACTION);
        commandFilter.addAction(Actions.NEXT_ACTION);
        commandFilter.addAction(Actions.PREVIOUS_ACTION);
        commandFilter.addAction(Actions.RESET_ALL);
        commandFilter.addAction(Actions.STOP_ACTION);
        commandFilter.addAction(Broadcasts.PLAYQUEUE_CHANGED);
        commandFilter.addAction(Actions.RELOAD_QUEUE);
        commandFilter.addAction(Actions.LOAD_TRACK_INFO);
        commandFilter.addAction(Actions.RETRY_RELATED_TRACKS);

        registerReceiver(mIntentReceiver, commandFilter);
        registerReceiver(mNoisyReceiver, new IntentFilter(Consts.AUDIO_BECOMING_NOISY));

        mFocus = AudioManagerFactory.createRemoteAudioManager(this);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        scheduleServiceShutdownCheck();

        initializeProxyIfNeeded();
        instance = this;
    }

    @Override
    public void onDestroy() {
        instance = null;
        stop();
        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mPlayerHandler.removeCallbacksAndMessages(null);
        mPlayEventTracker.stop();

        mFocus.abandonMusicFocus(false);
        unregisterReceiver(mIntentReceiver);
        unregisterReceiver(mNoisyReceiver);
        if (mProxy != null && mProxy.isRunning()) mProxy.stop();
        super.onDestroy();
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
        } else if (!getPlayQueueInternal().isEmpty() || mPlayerHandler.hasMessages(TRACK_ENDED)) {
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
            boolean hasAccount = mAccountOperations.soundCloudAccountExists();
            if (hasAccount && !Actions.PLAY_ACTION.equals(intent.getAction()) && mPlayQueueManager.shouldReloadQueue()){
                mResumeInfo = mPlayQueueManager.loadPlayQueue();
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
            if (isTransient){
                mPlayerHandler.sendEmptyMessage(canDuck ? DUCK : FADE_OUT);
            } else {
                pause();
            }
        }
        mTransientFocusLoss = isTransient;
    }

    State getState() {
        return state;
    }

    public PlayerAppWidgetProvider getAppWidgetProvider() {
        return mAppWidgetProvider;
    }

    public void resetAll() {
        stop();
        mCurrentTrack = null;
        mAppWidgetProvider.notifyChange(this, new Intent(Broadcasts.RESET_ALL));
    }

    public void saveProgressAndStop() {
        pause();
        mResumeInfo = new ResumeInfo(getProgress(), getCurrentTrackId());
        stop();
    }

    public AndroidCloudAPI getOldCloudApi() {
        return mOldCloudApi;
    }

    public FetchModelTask.Listener<Track> getInfoListener() {
        return mInfoListener;
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
            .putExtra(BroadcastExtras.isBuffering, _isBuffering())
            .putExtra(BroadcastExtras.position, getProgress())
            .putExtra(BroadcastExtras.queuePosition, getPlayQueueInternal().getPosition())
            .putExtra(BroadcastExtras.isLike, getIsLike())
            .putExtra(BroadcastExtras.isRepost, getIsRepost());

        sendBroadcast(i);

        if (Consts.SdkSwitches.useRichNotifications) {
            if (what.equals(Broadcasts.PLAYSTATE_CHANGED)) {
                mFocus.setPlaybackState(state);
                setPlayingNotification(mCurrentTrack);
            } else if (what.equals(Broadcasts.META_CHANGED)) {
                onTrackChanged(mCurrentTrack);
            }
        }

        if (what.equals(Broadcasts.META_CHANGED) || what.equals(Broadcasts.PLAYBACK_ERROR) || what.equals(Broadcasts.PLAYBACK_COMPLETE)) {
            saveQueue();
        }
        // Share this notification directly with our widgets
        mAppWidgetProvider.notifyChange(this, i);
    }

    private void saveQueue(){
        mPlayQueueManager.saveCurrentPosition(mCurrentTrack == null ? 0 : getProgress());
    }

    private void onTrackChanged(final Track track) {
        if (mFocus.isTrackChangeSupported()) {
            final String artworkUri = track.getPlayerArtworkUri(this);
            if (ImageUtils.checkIconShouldLoad(artworkUri)) {
                ImageLoader.getInstance().loadImage(artworkUri, new SimpleImageLoadingListener(){
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        super.onLoadingComplete(imageUri, view, loadedImage);
                        if (track == mCurrentTrack){
                            // use a copy of the bitmap because it is going to get recycled afterwards
                            try {
                                mFocus.onTrackChanged(track, loadedImage.copy(Bitmap.Config.ARGB_8888, false));
                            } catch (OutOfMemoryError e) {
                                mFocus.onTrackChanged(track, null);
                                System.gc();
                                // retry?
                            }
                        }

                    }
                });
            }
        }
    }

    /* package */ void openCurrent() {
        openCurrent(Media.Action.Stop);
    }

    // TODO : Handle tracks that are not in local storage (quicksearch)
    /* package */ void openCurrent(final Media.Action action) {
        final long currentTrackId = getPlayQueueInternal().getCurrentTrackId();
        mPlaybackOperations.loadTrack(currentTrackId).subscribe(new DefaultObserver<Track>() {
            @Override
            public void onNext(Track track) {
                openCurrent(track, action);
            }
        });
    }

    /* package */ void openCurrent(Track track, Media.Action action) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "openCurrent(state="+state+")");
        }

        if (track != null) {
            if (mAutoPause) {
                mAutoPause = false;
            }
            mLoadPercent = 0;
            if (track.equals(mCurrentTrack) && track.isStreamable()) {
                if (!isPlaying()) {
                    notifyChange(Broadcasts.META_CHANGED);
                    startTrack(track);
                }
            } else { // new track
                if (isMediaPlayerPlaying()) {
                    trackStopEvent(); // track stop event for previous track, if any
                }
                track(Media.fromTrack(mCurrentTrack), action);

                mCurrentTrack = track;
                notifyChange(Broadcasts.META_CHANGED);
                mConnectRetries = 0; // new track, reset connection attempts

                if (track.isStreamable()) {
                    onStreamableTrack(track);
                } else if (track.load_info_task == null || !AndroidUtils.isTaskFinished(track.load_info_task)) {
                    track.refreshInfoAsync(mOldCloudApi,mInfoListener);
                } else {
                    onUnstreamableTrack(track.getId());
                }
            }
        } else {
            Log.e(TAG, "openCurrent with no available track");
        }
    }

    private void trackPlayEvent(Track newTrack) {
        final long userId = SoundCloudApplication.getUserId();
        mPlayEventTracker.trackEvent(newTrack, Action.PLAY, userId, getPlayQueueInternal().getCurrentEventLoggerParams());
    }

    private void trackStopEvent() {
        final long userId = SoundCloudApplication.getUserId();
        mPlayEventTracker.trackEvent(mCurrentTrack, Action.STOP, userId, getPlayQueueInternal().getCurrentEventLoggerParams());
    }

    private FetchModelTask.Listener<Track> mInfoListener = new FetchModelTask.Listener<Track>() {
        @Override
        public void onSuccess(Track track) {
            // TODO
            // this used to write the track back to storage - this should happen as
            // part of the sync/task
            sendBroadcast(new Intent(Playable.ACTION_SOUND_INFO_UPDATED)
                                        .putExtra(CloudPlaybackService.BroadcastExtras.id, track.getId()));

            if (track.equals(mCurrentTrack) && (!isPlaying() && state.isSupposedToBePlaying())){
                // we were waiting on this track
                if (track.isStreamable()) {
                    onStreamableTrack(track);
                } else {
                    onUnstreamableTrack(track.getId());
                }
            }
        }

        @Override
        public void onError(Object context) {
            long id = context instanceof Number ? ((Number)context).longValue() : -1;
            sendBroadcast(new Intent(Playable.ACTION_SOUND_INFO_ERROR)
                                .putExtra(CloudPlaybackService.BroadcastExtras.id, id));
            onUnstreamableTrack(id);
        }
    };

    private void onUnstreamableTrack(long trackId){
        if (getCurrentTrackId() != trackId) return;

        mPlayerHandler.sendEmptyMessage(STREAM_EXCEPTION);
        gotoIdleState(STOPPED);
    }

    private void onStreamableTrack(Track track){
        if (getCurrentTrackId() != track.getId()) return;

        mPlaybackOperations.markTrackAsPlayed(mCurrentTrack).subscribe(DefaultObserver.NOOP_OBSERVER);
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
                default: // NO-OP
                    break;
            }
        }
        state = PREPARING;
        setPlayingNotification(track);

        try {
            initializeProxyIfNeeded();
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.reset");
            mMediaPlayer.reset();
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(preparedlistener);
            mMediaPlayer.setOnSeekCompleteListener(seekListener);
            mMediaPlayer.setOnCompletionListener(completionListener);
            mMediaPlayer.setOnErrorListener(errorListener);
            mMediaPlayer.setOnBufferingUpdateListener(bufferingListener);
            mMediaPlayer.setOnInfoListener(infolistener);
            notifyChange(Broadcasts.BUFFERING);

            // TODO, re-enable this
            Track next = null;//mPlayQueueManager.getNext();

            // if this comes from a shortcut, we may not have the stream url yet. we should get it on info load
            if (mCurrentTrack != null && mCurrentTrack.isStreamable()) {
                mProxy.uriObservable(mCurrentTrack.getStreamUrlWithAppendedId(), next == null ? null : next.getStreamUrlWithAppendedId())
                      .subscribe(new MediaPlayerDataSourceObserver(mMediaPlayer, errorListener), AndroidSchedulers.mainThread());
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "error", e);
            gotoIdleState(ERROR);
        }
    }

    private void releaseMediaPlayer(boolean refresh) {
        Log.w(TAG, "stuck in preparing state!");
        final MediaPlayer old = mMediaPlayer;
        if (old != null){
            new Thread() {
                @Override
                public void run() {
                    old.reset();
                    old.release();
                }
            }.start();
        }
        mMediaPlayer = refresh ? new MediaPlayer() : null;
    }


    /* package */ void play() {
        if (state.isSupposedToBePlaying()) return;

        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "play(state=" + state + ")");
        track(Media.fromTrack(mCurrentTrack), Media.Action.Play);
        mLastRefresh = System.currentTimeMillis();

        if (mCurrentTrack != null && mFocus.requestMusicFocus(this, IAudioManager.FOCUS_GAIN)) {
            if (mMediaPlayer != null && state.isStartable()) {
                // resume
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.start");
                mMediaPlayer.start();
                state = PLAYING;
                mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                mPlayerHandler.sendEmptyMessageDelayed(CHECK_TRACK_EVENT, CHECK_TRACK_EVENT_DELAY);
                notifyChange(Broadcasts.PLAYSTATE_CHANGED);
                if (!Consts.SdkSwitches.useRichNotifications) setPlayingNotification(mCurrentTrack);

                trackPlayEvent(mCurrentTrack);
                mAnalyticsEngine.openSessionForPlayer();

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

        trackStopEvent();
        track(Media.fromTrack(mCurrentTrack), Media.Action.Pause);

        safePause();
        notifyChange(Broadcasts.PLAYSTATE_CHANGED);
    }

    private void safePause() {
        if (mMediaPlayer != null) {
            if (state.isPausable()) {
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                gotoIdleState(PAUSED);
            } else {
                // get into a determined state
                stop();
            }
        }
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
        } else if (mCurrentTrack != null) {
            play();
        } else {
            openCurrent();
        }
    }


    private void gotoIdleState(State newState) {
        if (!newState.isInIdleState()) throw new IllegalArgumentException(newState + " is not a valid idle state");
        mAnalyticsEngine.closeSessionForPlayer();
        state = newState;
        mPlayerHandler.removeMessages(FADE_OUT);
        mPlayerHandler.removeMessages(FADE_IN);
        mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
        scheduleServiceShutdownCheck();

        if (Consts.SdkSwitches.useRichNotifications && mCurrentTrack != null && state != STOPPED){
            stopForeground(false);
            setPlayingNotification(mCurrentTrack);
        } else {
            stopForeground(true);
            status = null;
        }
    }

    /* package */ boolean prev() {
        if (getPlayQueueInternal().moveToPrevious()) {
            openCurrent(Media.Action.Backward);
            return true;
        } else {
            return false;
        }
    }

    /* package */ boolean next() {
        return next(true);
    }

    private boolean autoNext() {
        return next(false);
    }

    private boolean next(boolean userTriggered) {
        if (getPlayQueueInternal().moveToNext(userTriggered)) {
            openCurrent(Media.Action.Forward);
            return true;
        } else {
            return false;
        }
    }

    private void setPlayingNotification(final Track track) {

        if (track == null || state == STOPPED ||
                (Consts.SdkSwitches.useRichNotifications && status != null && status.contentView != null &&
                    ((NotificationPlaybackRemoteViews) status.contentView).isAlreadyNotifying(track, state.isSupposedToBePlaying()))){
            return;
        }

        final Notification notification = new Notification();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.icon = R.drawable.ic_notification_cloud;

        Intent intent = new Intent(com.soundcloud.android.Actions.PLAYER)
            .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

        if (!Consts.SdkSwitches.useRichNotifications) {
            notification.setLatestEventInfo(this, track.getUserName(), track.title, pi);
        } else {
            final NotificationPlaybackRemoteViews playbackRemoteViews = new NotificationPlaybackRemoteViews(getPackageName());

            playbackRemoteViews.setNotification(track, state.isSupposedToBePlaying());
            playbackRemoteViews.linkButtonsNotification(this);
            playbackRemoteViews.setPlaybackStatus(state.isSupposedToBePlaying());
            notification.contentView = playbackRemoteViews;
            notification.contentIntent = pi;

            final String artworkUri = track.getListArtworkUrl(this);
            if (ImageUtils.checkIconShouldLoad(artworkUri)) {
                playbackRemoteViews.clearIcon();
                ImageLoader.getInstance().loadImage(artworkUri, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        super.onLoadingComplete(imageUri, view, loadedImage);
                        if (mCurrentTrack == track) {
                            playbackRemoteViews.setIcon(loadedImage);
                            startForeground(PLAYBACKSERVICE_STATUS_ID, notification);
                        }
                    }
                });

            } else {
                playbackRemoteViews.clearIcon();
            }
        }
        startForeground(PLAYBACKSERVICE_STATUS_ID, notification);
        status = notification;
    }

    public void setQueuePosition(int pos) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setQueuePosition("+pos+")");

        if (getPlayQueueInternal().getPosition() != pos && getPlayQueueInternal().setPosition(pos)) {
            openCurrent();
        }
    }

    /* package */ int getDuration() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.duration;
    }

    /* package */ boolean _isBuffering() {
        return state == PAUSED_FOR_BUFFERING || state == PREPARING || mWaitingForSeek;
    }

    /*
     * Returns the current playback position in milliseconds
     */
    /* package */
    public long getProgress() {

        if (mCurrentTrack != null && mResumeInfo != null && mResumeInfo.getTrackId() == mCurrentTrack.getId()) {
            return mResumeInfo.getTime(); // either -1 or a valid resume time
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

    /* package */ boolean _isSeekable() {
        return (mMediaPlayer != null
                && state.isSeekable()
                && mCurrentTrack != null);
    }

    /* package */ boolean isNotSeekablePastBuffer() {
        // Some phones on 2.2 ship with broken opencore
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && StreamProxy.isOpenCore();
    }

    public long seek(float percent, boolean performSeek) {
        return seek((long) (getDuration() * percent), performSeek);
    }

    /* package */
    public long seek(long pos, boolean performSeek) {
        if (isSeekable()) {
            if (pos <= 0) {
                pos = 0;
            }

            final long currentPos = (mMediaPlayer != null && !state.isError()) ? mMediaPlayer.getCurrentPosition() :0;
            // workaround for devices which can't do content-range requests
            if ((isNotSeekablePastBuffer() && isPastBuffer(pos)) || mMediaPlayer == null) {
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
                    notifyChange(Broadcasts.SEEKING);

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

    private boolean isMediaPlayerPlaying() {
        try {
            return mMediaPlayer != null && mMediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
    }

    /* package */
    public boolean isPlaying() {
        return isMediaPlayerPlaying() && state.isSupposedToBePlaying();
    }

    public void restartTrack() {
        openCurrent();
    }

    private PlayQueue getPlayQueueInternal() {
        return mPlayQueueManager.getCurrentPlayQueue();
    }

    private PlayQueue clonePlayQueue(){
        PlayQueue original = mPlayQueueManager.getCurrentPlayQueue();
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return PlayQueue.CREATOR.createFromParcel(parcel);
    }


    private String getUserName() {
        return mCurrentTrack != null ? mCurrentTrack.getUserName() : null;
    }

    private long getUserId() {
        return mCurrentTrack != null ? mCurrentTrack.user_id : -1;
    }

    private long getTrackId() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.getId();
    }

    private String getTrackName() {
        return mCurrentTrack == null ? null : mCurrentTrack.title;
    }

    private boolean getIsLike() {
        return mCurrentTrack != null && mCurrentTrack.user_like;
    }

    private boolean getIsRepost() {
        return mCurrentTrack != null && mCurrentTrack.user_repost;
    }

    private boolean isPastBuffer(long pos) {
        return (pos / (double) getDuration()) * 100 > mLoadPercent;
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


    private static final class DelayedStopHandler extends Handler {
        private WeakReference<CloudPlaybackService> serviceRef;

        private DelayedStopHandler(CloudPlaybackService service) {
            serviceRef = new WeakReference<CloudPlaybackService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            CloudPlaybackService service = serviceRef.get();
            // Check again to make sure nothing is playing right now
            if (service != null && !state.isSupposedToBePlaying()
                    && state != PAUSED_FOCUS_LOST
                    && !service.mServiceInUse
                    && !service.mPlayerHandler.hasMessages(TRACK_ENDED)) {

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "DelayedStopHandler: stopping service");
                }

                if (state != STOPPED) {
                    service.saveQueue();
                }

                service.stopSelf(service.mServiceStartId);
            }
        }
    }

    private StreamProxy initializeProxyIfNeeded() {
        if (mProxy == null) {
            mProxy = new StreamProxy(getApplicationContext()).start();
        }
        return mProxy;
    }

    private final Handler mDelayedStopHandler = new DelayedStopHandler(this);

    private final BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            pause();
        }
    };


    private static final class PlayerHandler extends Handler {
        private static final float DUCK_VOLUME = 0.1f;

        private WeakReference<CloudPlaybackService> serviceRef;
        private float mCurrentVolume = 1.0f;

        private PlayerHandler(CloudPlaybackService service) {
            this.serviceRef = new WeakReference<CloudPlaybackService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            final CloudPlaybackService service = serviceRef.get();
            if (service == null) {
                return;
            }

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage(" + msg.what + ", state=" + state + ")");
            }

            switch (msg.what) {
                case CHECK_BUFFERING:
                    if (!state.equals(State.PAUSED_FOR_BUFFERING)) {
                        service.notifyChange(Broadcasts.BUFFERING_COMPLETE);
                    }
                    break;

                case NOTIFY_META_CHANGED:
                    service.notifyChange(Broadcasts.META_CHANGED);
                    break;
                case FADE_IN:
                    removeMessages(FADE_OUT);

                    if (!state.isSupposedToBePlaying()) {
                        mCurrentVolume = 0f;
                        service.setVolume(0f);
                        service.play();
                        sendEmptyMessageDelayed(FADE_IN, 10);
                    } else {
                        mCurrentVolume += FADE_CHANGE;
                        if (mCurrentVolume < 1.0f) {
                            sendEmptyMessageDelayed(FADE_IN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        service.setVolume(mCurrentVolume);
                    }
                    break;
                case FADE_OUT:
                    removeMessages(FADE_IN);
                    if (service.isPlaying()) {
                        mCurrentVolume -= FADE_CHANGE;
                        if (mCurrentVolume > 0f) {
                            sendEmptyMessageDelayed(FADE_OUT, 10);
                        } else {
                            if (service != null && service.mMediaPlayer != null) service.mMediaPlayer.pause();
                            mCurrentVolume = 0f;
                            state = PAUSED_FOCUS_LOST;
                        }
                        service.setVolume(mCurrentVolume);
                    } else {
                        service.setVolume(0f);
                    }
                    break;
                case DUCK:
                    removeMessages(FADE_IN);
                    removeMessages(FADE_OUT);
                    service.setVolume(DUCK_VOLUME);
                    break;
                case SERVER_DIED:
                    if (state == PLAYING && service.mAutoAdvance) service.next();
                    break;
                case TRACK_ENDED:
                    if (!service.mAutoAdvance || !service.autoNext()) {
                        service.notifyChange(Broadcasts.PLAYBACK_COMPLETE);
                        service.gotoIdleState(COMPLETED);
                    }
                    break;
                case CLEAR_LAST_SEEK:
                    service.mSeekPos = -1;
                    break;
                case CHECK_TRACK_EVENT:
                    final Track currentTrack = getCurrentTrack();
                    if (currentTrack != null) {
                        if (state.isSupposedToBePlaying()) {
                            int refresh = Media.refresh(currentTrack.duration);
                            if (refresh > 0) {
                                long now = System.currentTimeMillis();
                                if (now - service.mLastRefresh > refresh) {
                                    service.track(Media.fromTrack(currentTrack), Media.Action.Refresh);
                                    service.mLastRefresh = now;
                                }
                            }
                        }
                        sendEmptyMessageDelayed(CHECK_TRACK_EVENT, CHECK_TRACK_EVENT_DELAY);
                    } else {
                        removeMessages(CHECK_TRACK_EVENT);
                    }
                    break;
                default: // NO-OP
                    break;
            }
        }
    }

    private final Handler mPlayerHandler = new PlayerHandler(this);

    final MediaPlayer.OnInfoListener infolistener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onInfo(" + what + "," + extra + ", state=" + state + ")");
            }

            switch (what) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    mPlayerHandler.removeMessages(CLEAR_LAST_SEEK);
                    state = PAUSED_FOR_BUFFERING;
                    trackStopEvent();
                    notifyChange(Broadcasts.BUFFERING);
                    break;

                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    if (mSeekPos != -1 && !mWaitingForSeek) {
                        mPlayerHandler.removeMessages(CLEAR_LAST_SEEK);
                        mPlayerHandler.sendEmptyMessageDelayed(CLEAR_LAST_SEEK, 3000);
                    } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Not clearing seek, waiting for seek to finish");
                    }
                    if (!state.isSupposedToBePlaying()) {
                        safePause();
                    } else {
                        // still playing back, set proper state after buffering state
                        state = PLAYING;
                        trackPlayEvent(mCurrentTrack);
                    }
                    notifyChange(Broadcasts.BUFFERING_COMPLETE);
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
                notifyChange(Broadcasts.SEEK_COMPLETE);

                // respect pauses during seeks
                if (!state.isSupposedToBePlaying()) safePause();
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
                                        (mResumeInfo != null && mResumeInfo.getTrackId() == getTrackId()) ? mResumeInfo.getTime() :
                                        (mp.getCurrentPosition() <= 0 && state == PLAYING) ? getDuration() : mp.getCurrentPosition();
            // premature track end ?
            if (isSeekable() && getDuration() - targetPosition > 3000) {
                Log.w(TAG, "premature end of track (targetpos="+targetPosition+")");
                // track ended prematurely (probably end of buffer, unreported IO error),
                // so try to resume at last time
                mResumeInfo = new ResumeInfo(getCurrentTrackId(), targetPosition);
                errorListener.onError(mp, MediaPlayer.MEDIA_ERROR_UNKNOWN, Errors.STAGEFRIGHT_ERROR_BUFFER_EMPTY);
            } else if (!state.isError()) {
                trackStopEvent();
                track(Media.fromTrack(mCurrentTrack), Media.Action.Stop);
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
                    if (mResumeInfo != null && getCurrentTrackId() == mResumeInfo.getTrackId() && mResumeInfo.getTime() > 0) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "resuming to "+mResumeInfo.getTrackId());
                        }

                        // play before seek to prevent ANR
                        play();
                        seek(mResumeInfo.getTime(), true);
                        mResumeInfo = null;


                    // normal play, unless first start (autopause=true)
                    } else {
                        // sometimes paused for buffering happens right after prepare, so check buffering on a delay
                        mPlayerHandler.sendEmptyMessageDelayed(CHECK_BUFFERING,500);

                        //  FADE_IN will call play()
                        if (!mAutoPause && mFocus.requestMusicFocus(CloudPlaybackService.this, IAudioManager.FOCUS_GAIN)) {
                            mPlayerHandler.removeMessages(FADE_OUT);
                            mPlayerHandler.removeMessages(FADE_IN);
                            setVolume(1.0f);
                            play();
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
                trackStopEvent();
                // when the proxy times out it will just close the connection - different implementations
                // return different error codes. try to reconnect at least twice before giving up.
                if (mConnectRetries++ < 4) {
                    Log.d(TAG, "stream disconnected, retrying (try=" + mConnectRetries + ")");
                    state = ERROR_RETRYING;
                    openCurrent();
                    return true;
                } else {
                    StreamItem item = mCurrentTrack != null ? mProxy.getStreamItem(mCurrentTrack.getStreamUrlWithAppendedId()) : null;
                    Log.d(TAG, "stream disconnected, giving up");
                    mConnectRetries = 0;
                    DebugUtils.reportMediaPlayerError(CloudPlaybackService.this, item, what, extra);

                    mp.release();
                    mMediaPlayer = null;
                    gotoIdleState(ERROR);

                    if (IOUtils.isConnected(CloudPlaybackService.this)) {
                        notifyChange(item != null && !item.isAvailable() ? Broadcasts.TRACK_UNAVAILABLE : Broadcasts.PLAYBACK_ERROR);
                    } else {
                        notifyChange(Broadcasts.STREAM_DIED);
                    }
                    notifyChange(Broadcasts.PLAYBACK_COMPLETE);
                }
            }
            return true;
        }
    };

    public void track(Event event, Object... args) {
        getTracker().track(event, args);
    }

    @Override
    public void track(Class<?> klazz, Object... args) {
        getTracker().track(klazz, args);
    }

    private Tracker getTracker() {
        return (Tracker) getApplication();
    }
}
