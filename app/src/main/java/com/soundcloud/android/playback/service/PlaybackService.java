package com.soundcloud.android.playback.service;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.peripherals.PeripheralsOperations;
import com.soundcloud.android.playback.service.managers.AudioManagerFactory;
import com.soundcloud.android.playback.service.managers.IAudioManager;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class PlaybackService extends Service implements IAudioManager.MusicFocusable, Playa.PlayaListener {
    public static final String TAG = "CloudPlaybackService";

    @Nullable
    static PlaybackService instance;

    // public service actions
    public interface Actions {
        String PLAY_ACTION              = "com.soundcloud.android.playback.start";
        String TOGGLEPLAYBACK_ACTION    = "com.soundcloud.android.playback.toggleplayback";
        String PAUSE_ACTION             = "com.soundcloud.android.playback.pause";
        String NEXT_ACTION              = "com.soundcloud.android.playback.next";
        String PREVIOUS_ACTION          = "com.soundcloud.android.playback.previous";
        String RESET_ALL                = "com.soundcloud.android.playback.reset"; // used on logout
        String STOP_ACTION              = "com.soundcloud.android.playback.stop"; // from the notification
        String RELOAD_QUEUE             = "com.soundcloud.android.reloadqueue";
        String RETRY_RELATED_TRACKS     = "com.soundcloud.android.retryRelatedTracks";
        String WIDGET_LIKE_CHANGED     = "com.soundcloud.android.widgetLike";
    }

    // broadcast notifications
    public interface Broadcasts {
        String UPDATE_WIDGET_ACTION     = "com.soundcloud.android.playback.updatewidgetaction";
        String PLAYSTATE_CHANGED        = "com.soundcloud.android.playstatechanged";
        String META_CHANGED             = "com.soundcloud.android.metachanged";
        String RELATED_LOAD_STATE_CHANGED = "com.soundcloud.android.related.changed";
        String PLAYQUEUE_CHANGED        = "com.soundcloud.android.playlistchanged";
        String COMMENTS_LOADED          = "com.soundcloud.android.commentsloaded";
        String RESET_ALL                = "com.soundcloud.android.resetAll";
    }

    private static final int PLAYBACKSERVICE_STATUS_ID = 1;


    @Inject
    EventBus mEventBus;
    @Inject
    PlayQueueManager mPlayQueueManager;
    @Inject
    TrackOperations mTrackOperations;
    @Inject
    PeripheralsOperations mPeripheralsOperations;
    @Inject
    PlaybackEventSource mPlaybackEventSource;
    @Inject
    AccountOperations mAccountOperations;
    @Inject
    ImageOperations mImageOperations;
    @Inject
    PlayerAppWidgetProvider mAppWidgetProvider;
    @Inject
    StreamPlaya mStreamPlayer;

    // XXX : would be great to not have these boolean states
    private boolean mWaitingForPlaylist;

    private @Nullable Track mCurrentTrack;
    private @Nullable TrackSourceInfo mCurrentTrackSourceInfo;

    @Nullable
    private PlaybackProgressInfo mResumeInfo;      // info to resume a previous play session

    private int mServiceStartId = -1;
    private boolean mServiceInUse;

    private static final int IDLE_DELAY = 180*1000;  // interval after which we stop the service when idle

    // audio focus related
    private IRemoteAudioManager mFocus;
    private FocusLossState mFocusLossState = FocusLossState.NONE;
    private enum FocusLossState {
        NONE, TRANSIENT, LOST
    }

    private final IBinder mBinder = new LocalBinder<PlaybackService>() {
        @Override public PlaybackService getService() {
            return PlaybackService.this;
        }
    };

    private Notification status;
    private PlaybackReceiver mPlaybackReceiver;
    private final Handler mFadeHandler = new FadeHandler(this);
    private final Handler mDelayedStopHandler = new DelayedStopHandler(this);

    private final BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            pause();
        }
    };

    private Subscription mStreamableTrackSubscription = Subscriptions.empty();
    private Subscription mLoadTrackSubscription = Subscriptions.empty();

    public interface PlayExtras{
        String TRACK = Track.EXTRA;
        String TRACK_ID = Track.EXTRA_ID;
        String TRACK_ID_LIST = "track_id_list";
        String START_POSITION = "start_position";
        String PLAY_SESSION_SOURCE = "play_session_source";
        String LOAD_RECOMMENDED = "load_recommended";
    }

    public interface BroadcastExtras{
        String ID = "id";
        String TITLE = "title";
        String USER_ID = "user_id";
        String USERNAME = "username";
        String POSITION = "position";
        String QUEUE_POSITION = "queuePosition";
        String IS_LIKE = "isLike";
        String IS_REPOST = "isRepost";
    }

    public PlaybackService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mPlaybackReceiver = new PlaybackReceiver(this, mAccountOperations, mPlayQueueManager, mEventBus);
        mStreamPlayer.setListener(this);

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(Actions.PLAY_ACTION);
        playbackFilter.addAction(Actions.TOGGLEPLAYBACK_ACTION);
        playbackFilter.addAction(Actions.PAUSE_ACTION);
        playbackFilter.addAction(Actions.NEXT_ACTION);
        playbackFilter.addAction(Actions.PREVIOUS_ACTION);
        playbackFilter.addAction(Actions.RESET_ALL);
        playbackFilter.addAction(Actions.STOP_ACTION);
        playbackFilter.addAction(Broadcasts.PLAYQUEUE_CHANGED);
        playbackFilter.addAction(Actions.RELOAD_QUEUE);
        playbackFilter.addAction(Actions.RETRY_RELATED_TRACKS);

        registerReceiver(mPlaybackReceiver, playbackFilter);
        registerReceiver(mNoisyReceiver, new IntentFilter(Consts.AUDIO_BECOMING_NOISY));

        mFocus = AudioManagerFactory.createRemoteAudioManager(this);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        scheduleServiceShutdownCheck();
        instance = this;
    }

    @Override
    public void onDestroy() {
        stop();
        mStreamPlayer.destroy();

        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mFadeHandler.removeCallbacksAndMessages(null);
        mFocus.abandonMusicFocus(false);
        unregisterReceiver(mPlaybackReceiver);
        unregisterReceiver(mNoisyReceiver);
        mEventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());
        instance = null;
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

        if (mStreamPlayer.isPlaying() || mFocusLossState != FocusLossState.NONE) {
            // something is currently playing, or will be playing once
            // an in-progress call ends, so don't stop the service now.
            return true;

        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between
        // tracks.
        } else if (!getPlayQueueInternal().isEmpty()) {
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
            mPlaybackReceiver.onReceive(this, intent);
        }
        scheduleServiceShutdownCheck();
        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        return START_STICKY;
    }

    @Override
    public void focusGained() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "focusGained");
        if (mFocusLossState == FocusLossState.TRANSIENT) {
            mFadeHandler.sendEmptyMessage(FadeHandler.FADE_IN);
        } else {
            mStreamPlayer.setVolume(1.0f);
        }

        mFocusLossState = FocusLossState.NONE;
    }

    @Override
    public void focusLost(boolean isTransient, boolean canDuck) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "focusLost(" + isTransient + ", canDuck=" + canDuck + ")");
        }
        if (mStreamPlayer.isPlaying()) {

            if (isTransient){
                mFocusLossState = FocusLossState.TRANSIENT;
                mFadeHandler.sendEmptyMessage(canDuck ? FadeHandler.DUCK : FadeHandler.FADE_OUT);
            } else {
                mFocusLossState = FocusLossState.LOST;
                pause();
            }
        }
    }

    public void resetAll() {
        stop();
        mCurrentTrack = null;
        mAppWidgetProvider.performUpdate(this, new Intent(Broadcasts.RESET_ALL));
        mFocus.abandonMusicFocus(false); // kills lockscreen
    }

    public void saveProgressAndStop() {
        mResumeInfo = new PlaybackProgressInfo(getProgress(), instance == null || instance.getPlayQueueInternal().isEmpty() ?
                -1L : instance.getPlayQueueInternal().getCurrentTrackId());
        stop();
    }

    boolean isWaitingForPlaylist() {
        return mWaitingForPlaylist;
    }

    private void scheduleServiceShutdownCheck() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "scheduleServiceShutdownCheck()");
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
    }

    @Override
    public void onPlaystateChanged(Playa.StateTransition stateTransition) {
        publishPlaybackEventFromState(stateTransition);

        if (stateTransition.trackEnded() && autoNext()){
            // advanced successfully
            openCurrent();
        } else {
            if (stateTransition.getNewState() == Playa.PlayaState.IDLE){
                gotoIdleState(false);
            }
            notifyChange(Broadcasts.PLAYSTATE_CHANGED, stateTransition);
        }
    }

    @Override
    public boolean requestAudioFocus() {
        return mFocus.requestMusicFocus(PlaybackService.this, IAudioManager.FOCUS_GAIN);
    }

    void notifyChange(String what) {
        notifyChange(what, mStreamPlayer.getLastStateTransition());
    }

    void notifyChange(String what, Playa.StateTransition stateTransition) {

        Log.d(TAG, "notifyChange(" + what + ", " + stateTransition.getNewState() + ")");
        final boolean isPlaying = stateTransition.isPlaying();
        Intent intent = new Intent(what)
            .putExtra(BroadcastExtras.ID, getTrackId())
            .putExtra(BroadcastExtras.TITLE, getTrackName())
            .putExtra(BroadcastExtras.USER_ID, getTrackUserId())
            .putExtra(BroadcastExtras.USERNAME, getUserName())
            .putExtra(BroadcastExtras.POSITION, getProgress())
            .putExtra(BroadcastExtras.QUEUE_POSITION, getPlayQueueInternal().getPosition())
            .putExtra(BroadcastExtras.IS_LIKE, getIsLike())
            .putExtra(BroadcastExtras.IS_REPOST, getIsRepost());

        stateTransition.addToIntent(intent);

        sendBroadcast(intent);
        mAppWidgetProvider.performUpdate(this, intent); // Share this notification directly with our widgets

        if (Consts.SdkSwitches.useRichNotifications) {
            if (what.equals(Broadcasts.PLAYSTATE_CHANGED)) {
                mFocus.setPlaybackState(isPlaying);
                setPlayingNotification(mCurrentTrack);
                mPeripheralsOperations.notifyPlayStateChanged(this, getCurrentTrack(), isPlaying);
            } else if (what.equals(Broadcasts.META_CHANGED)) {
                onTrackChanged(mCurrentTrack);
                mPeripheralsOperations.notifyMetaChanged(this, getCurrentTrack(), isPlaying);
            }
        }

        if (what.equals(Broadcasts.META_CHANGED) || stateTransition.playbackHasStopped()) {
            saveQueue();
        }
    }

    private void saveQueue(){
        mPlayQueueManager.saveCurrentPosition(mCurrentTrack == null ? 0 : getProgress());
    }

    private void onTrackChanged(final Track track) {
        if (mFocus.isTrackChangeSupported()) {
            // set initial data without bitmap so it doesn't have to wait
            mFocus.onTrackChanged(track, null);

            // Loads the current track artwork into the lock screen controls
            // this is quite ugly; should move into ImageOperations really!
            final ImageUtils.ViewlessLoadingListener lockScreenImageListener = new ImageUtils.ViewlessLoadingListener() {
                @Override
                public void onLoadingFailed(String s, View view, String failedReason) {
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    if (track == mCurrentTrack) {
                        // use a copy of the bitmap because it is going to get recycled afterwards
                        try {
                            mFocus.onTrackChanged(track, loadedImage.copy(Bitmap.Config.ARGB_8888, false));
                        } catch (OutOfMemoryError e) {
                            mFocus.onTrackChanged(track, null);
                        }
                    }

                }
            };
            mImageOperations.load(track.getUrn(), ImageSize.getFullImageSize(getResources()), lockScreenImageListener);
        }
    }

    // TODO : Handle tracks that are not in local storage (quicksearch)
    /* package */ void openCurrent() {
        if (!getPlayQueueInternal().isEmpty()) {

            mStreamPlayer.startBufferingMode();

            final long currentTrackId = getPlayQueueInternal().getCurrentTrackId();
            mLoadTrackSubscription.unsubscribe();
            mLoadTrackSubscription = mTrackOperations.loadTrack(currentTrackId, AndroidSchedulers.mainThread())
                    .subscribe(new TrackInformationSubscriber());
        }
    }

    private class TrackInformationSubscriber extends DefaultSubscriber<Track> {
        @Override
        public void onError(Throwable throwable) {
            onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED));
        }

        @Override
        public void onNext(Track track) {
            openCurrent(track);
        }
    };

    /* package */ void openCurrent(Track track) {
        if (track != null) {
            if (track.equals(mCurrentTrack) && track.isStreamable()) {
                if (!mStreamPlayer.isPlayerPlaying()) {
                    startTrack(track);
                }
            } else { // new track
                final TrackSourceInfo newTrackSourceInfo = mPlayQueueManager.getCurrentTrackSourceInfo();

                if (mStreamPlayer.isPlayerPlaying()) {
                    final boolean changedContext = newTrackSourceInfo != null && !newTrackSourceInfo.sharesSameOrigin(mCurrentTrackSourceInfo);
                    mPlaybackEventSource.publishStopEvent(mCurrentTrack, mCurrentTrackSourceInfo, getCurrentUserId(),
                            changedContext ? PlaybackEvent.STOP_REASON_NEW_QUEUE : PlaybackEvent.STOP_REASON_SKIP);
                }

                mWaitingForPlaylist = false;
                mCurrentTrack = track;
                mCurrentTrackSourceInfo = newTrackSourceInfo;

                notifyChange(Broadcasts.META_CHANGED);
                mStreamableTrackSubscription = mTrackOperations.loadStreamableTrack(mCurrentTrack.getId(), AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(new StreamableTrackInformationSubscriber());
            }
        } else {
            Log.e(TAG, "openCurrent with no available track");
        }
    }

    private class StreamableTrackInformationSubscriber extends DefaultSubscriber<Track> {
        @Override
        public void onError(Throwable e) {
            onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED));
        }

        @Override
        public void onNext(Track track) {
            fireAndForget(mTrackOperations.markTrackAsPlayed(track));
            startTrack(track);
        }
    };

    private void publishPlaybackEventFromState(Playa.StateTransition stateTransition) {
        final long currentUserId = getCurrentUserId();
        switch (stateTransition.getNewState()) {
            case PLAYING:
                mPlaybackEventSource.publishPlayEvent(mCurrentTrack, mCurrentTrackSourceInfo, currentUserId);
                break;
            case BUFFERING:
                mPlaybackEventSource.publishStopEvent(mCurrentTrack, mCurrentTrackSourceInfo, currentUserId, PlaybackEvent.STOP_REASON_BUFFERING);
                break;
            case IDLE:
                if (stateTransition.getReason() == Playa.Reason.COMPLETE){
                    final int stopReason = getPlayQueueInternal().hasNextTrack()
                            ? PlaybackEvent.STOP_REASON_TRACK_FINISHED
                            : PlaybackEvent.STOP_REASON_END_OF_QUEUE;
                    mPlaybackEventSource.publishStopEvent(mCurrentTrack, mCurrentTrackSourceInfo, currentUserId, stopReason);
                } else if (stateTransition.wasError()){
                    mPlaybackEventSource.publishStopEvent(mCurrentTrack, mCurrentTrackSourceInfo, currentUserId, PlaybackEvent.STOP_REASON_ERROR);
                } else {
                    mPlaybackEventSource.publishStopEvent(mCurrentTrack, mCurrentTrackSourceInfo, currentUserId, PlaybackEvent.STOP_REASON_PAUSE);
                }
                break;
            default:
                throw new IllegalArgumentException("Unexpected state when trying to publish play event" + stateTransition.getNewState());
        }
    }

    private void startTrack(Track track) {
        // set current track again as it may have been fetched from the api. This is not necessary with modelmanager, but will be going forward
        mCurrentTrack = track;
        Log.d(TAG, "startTrack("+track.title+")");

        if (mResumeInfo != null && mResumeInfo.getTrackId() == track.getId()){
            mStreamPlayer.play(mCurrentTrack, mResumeInfo.getTime());
        } else {
            mStreamPlayer.play(mCurrentTrack);
        }
        mResumeInfo = null;
    }



    /* package */ void play() {
        if (!mStreamPlayer.isPlaying() && mCurrentTrack != null && mFocus.requestMusicFocus(this, IAudioManager.FOCUS_GAIN)) {
            if (!mStreamPlayer.resume()) {
                // must have been a playback error or we are in stop state
                openCurrent();
            }
        }
    }

    // Pauses playback (call play() to resume)
    /* package */ void pause() {
        mStreamPlayer.pause();
    }

    public void togglePlayback() {
        if (mStreamPlayer.isPlaying()) {
            pause();
        } else if (mCurrentTrack != null) {
            play();
        } else if (!getPlayQueueInternal().isEmpty()) {
            openCurrent();
        } else {
            mWaitingForPlaylist = true;
        }
    }

    /* package */ void stop() {
        saveQueue();
        mStreamPlayer.stop();
        gotoIdleState(true);
    }


    private void gotoIdleState(boolean stopService) {
        mEventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forIdle());
        scheduleServiceShutdownCheck();

        mFadeHandler.removeMessages(FadeHandler.FADE_OUT);
        mFadeHandler.removeMessages(FadeHandler.FADE_IN);

        if (Consts.SdkSwitches.useRichNotifications && mCurrentTrack != null && !stopService){
            stopForeground(false);
            setPlayingNotification(mCurrentTrack);
        } else {
            stopForeground(true);
            status = null;
        }
    }

    /* package */ boolean prev() {
        if (getPlayQueueInternal().moveToPrevious()) {
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
            return true;
        } else {
            return false;
        }
    }

    private void setPlayingNotification(final Track track) {

        final boolean supposedToBePlaying = mStreamPlayer.isPlaying();
        if (track == null ||
                (Consts.SdkSwitches.useRichNotifications && status != null && status.contentView != null &&
                    ((NotificationPlaybackRemoteViews) status.contentView).isAlreadyNotifying(track, supposedToBePlaying))){
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

            playbackRemoteViews.setNotification(track, supposedToBePlaying);
            playbackRemoteViews.linkButtonsNotification(this);
            playbackRemoteViews.setPlaybackStatus(supposedToBePlaying);
            notification.contentView = playbackRemoteViews;
            notification.contentIntent = pi;

            final String artworkUri = track.getListArtworkUrl(this);
            if (ImageUtils.checkIconShouldLoad(artworkUri)) {
                playbackRemoteViews.clearIcon();
                mImageOperations.load(artworkUri, new ImageUtils.ViewlessLoadingListener() {
                    @Override
                    public void onLoadingFailed(String s, View view, String failedReason) {}

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
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

        final PlayQueue playQueue = getPlayQueueInternal();
        if (playQueue.getPosition() != pos && playQueue.setPosition(pos)) {
            playQueue.setCurrentTrackToUserTriggered();
            openCurrent();
        }
    }

    /* package */ int getDuration() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.duration;
    }

    /* package */ boolean isBuffering() {
        return mStreamPlayer.isBuffering();
    }

    /*
     * Returns the current playback position in milliseconds
     */
    public long getProgress() {
        return mStreamPlayer.getProgress();
    }

    /* package */ boolean isSeekable() {
        return mStreamPlayer.isSeekable();
    }

    public long seek(float percent, boolean performSeek) {
        return seek((long) (getDuration() * percent), performSeek);
    }

    public long seek(long pos, boolean performSeek) {
        return mStreamPlayer.seek(pos, performSeek);
    }

    public boolean isPlaying() {
        return mStreamPlayer.isPlaying();
    }

    public boolean isPlayerPlaying() {
        return mStreamPlayer.isPlayerPlaying();
    }

    public boolean isSupposedToBePlaying() {
        return mStreamPlayer.isPlaying();
    }

    public long getPlayQueuePlaylistId() {
        return mPlayQueueManager.getPlaylistId();
    }

    public String getPlayQueueOriginScreen() {
        return mPlayQueueManager.getOriginScreen();
    }

    public void restartTrack() {
        openCurrent();
    }

    PlayQueue getPlayQueueInternal() {
        return mPlayQueueManager.getCurrentPlayQueue();
    }

    PlayQueueView getPlayQueueView(){
        return mPlayQueueManager.getPlayQueueView();
    }

    private long getCurrentUserId() {
        return mAccountOperations.getLoggedInUserId();
    }

    private String getUserName() {
        return mCurrentTrack != null ? mCurrentTrack.getUserName() : null;
    }

    private long getTrackUserId() {
        return mCurrentTrack != null ? mCurrentTrack.user_id : -1;
    }

    Track getCurrentTrack() {
        return mCurrentTrack;
    }

    long getTrackId() {
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


    private static final class DelayedStopHandler extends Handler {
        private WeakReference<PlaybackService> serviceRef;

        private DelayedStopHandler(PlaybackService service) {
            serviceRef = new WeakReference<PlaybackService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaybackService service = serviceRef.get();
            // Check again to make sure nothing is playing right now
            if (service != null && !service.mStreamPlayer.isPlaying()
                    && service.mFocusLossState == FocusLossState.NONE
                    && !service.mServiceInUse) {

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "DelayedStopHandler: stopping service");
                }
                service.stopSelf(service.mServiceStartId);
            }
        }
    }



    private static final class FadeHandler extends Handler {

        private static final int FADE_IN          = 1;
        private static final int FADE_OUT         = 2;
        private static final int DUCK             = 3;

        private WeakReference<PlaybackService> serviceRef;
        private float mCurrentVolume;

        private static final float FADE_CHANGE = 0.02f; // change to fade faster/slower
        private static final float DUCK_VOLUME = 0.1f;

        private FadeHandler(PlaybackService service) {
            this.serviceRef = new WeakReference<PlaybackService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            final PlaybackService service = serviceRef.get();
            if (service == null) {
                return;
            }


           switch (msg.what) {
                case FADE_IN:
                    removeMessages(FADE_OUT);
                    if (!service.mStreamPlayer.isPlaying()) {
                        mCurrentVolume = 0f;
                        service.mStreamPlayer.setVolume(0f);
                        service.play();
                        sendEmptyMessageDelayed(FADE_IN, 10);
                    } else {
                        mCurrentVolume += FADE_CHANGE;
                        if (mCurrentVolume < 1.0f) {
                            sendEmptyMessageDelayed(FADE_IN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        service.mStreamPlayer.setVolume(mCurrentVolume);
                    }
                    break;
                case FADE_OUT:
                    removeMessages(FADE_IN);
                    if (service.mStreamPlayer.isPlaying()) {
                        mCurrentVolume -= FADE_CHANGE;
                        if (mCurrentVolume > 0f) {
                            sendEmptyMessageDelayed(FADE_OUT, 10);
                        } else {
                            if (service != null) {
                                service.pause();
                            }
                            mCurrentVolume = 0f;
                        }
                        service.mStreamPlayer.setVolume(mCurrentVolume);
                    } else {
                        service.mStreamPlayer.setVolume(0f);
                    }
                    break;
                case DUCK:
                    removeMessages(FADE_IN);
                    removeMessages(FADE_OUT);
                    service.mStreamPlayer.setVolume(DUCK_VOLUME);
                    break;
                default: // NO-OP
                    break;
            }
        }
    }

}
