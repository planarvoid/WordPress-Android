package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.managers.IAudioManager;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.track.LegacyTrackOperations;
import dagger.Lazy;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class PlaybackService extends Service implements IAudioManager.MusicFocusable, Playa.PlayaListener {
    public static final String TAG = "CloudPlaybackService";

    @Nullable
    static PlaybackService instance;

    @Inject EventBus eventBus;
    @Inject PlayQueueManager playQueueManager;
    @Inject LegacyTrackOperations trackOperations;
    @Inject AccountOperations accountOperations;
    @Inject StreamPlaya streamPlayer;
    @Inject PlaybackReceiver.Factory playbackReceiverFactory;
    @Inject Lazy<IRemoteAudioManager> remoteAudioManagerProvider;
    @Inject FeatureFlags featureFlags;
    @Inject PlaybackNotificationController playbackNotificationController;

    // XXX : would be great to not have these boolean states
    private boolean waitingForPlaylist;

    private @Nullable Track currentTrack;

    private int serviceStartId = -1;
    private boolean serviceInUse;

    private static final int IDLE_DELAY = 180*1000;  // interval after which we stop the service when idle

    // audio focus related
    private IAudioManager audioManager;
    private FocusLossState focusLossState = FocusLossState.NONE;
    private enum FocusLossState {
        NONE, TRANSIENT, LOST
    }

    private final IBinder binder = new LocalBinder<PlaybackService>() {
        @Override public PlaybackService getService() {
            return PlaybackService.this;
        }
    };

    private final Action1<Notification> startForegroundAction = new Action1<Notification>() {
        @Override
        public void call(Notification notification) {
            startForeground(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
        }
    };

    private boolean suppressNotifications;

    private PlaybackReceiver playbackReceiver;
    private final Handler fadeHandler = new FadeHandler(this);
    private final Handler delayedStopHandler = new DelayedStopHandler(this);

    private Subscription streamableTrackSubscription = Subscriptions.empty();
    private Subscription loadTrackSubscription = Subscriptions.empty();

    @Deprecated
    public interface BroadcastExtras{
        String ID = "id";
        String USER_ID = "user_id";
        String PROGRESS_POSITION = "progress_position";
        String QUEUE_POSITION = "queuePosition";
    }

    // public service actions
    public interface Actions {
        String TOGGLEPLAYBACK_ACTION    = "com.soundcloud.android.playback.toggleplayback";
        String PLAY_CURRENT             = "com.soundcloud.android.playback.playcurrent";
        String PLAY_ACTION              = "com.soundcloud.android.playback.playcurrent";
        String PAUSE_ACTION             = "com.soundcloud.android.playback.pause";
        String SEEK                     = "com.soundcloud.android.playback.seek";
        String RESET_ALL                = "com.soundcloud.android.playback.reset"; // used on logout
        String STOP_ACTION              = "com.soundcloud.android.playback.stop"; // from the notification
    }

    public interface ActionsExtras {
        String SEEK_POSITION = "seek_position";
    }

    // broadcast notifications
    public interface Broadcasts {
        String PLAYSTATE_CHANGED        = "com.soundcloud.android.playstatechanged";
        @Deprecated
        String META_CHANGED             = "com.soundcloud.android.metachanged";
    }

    public PlaybackService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    PlaybackService(PlayQueueManager playQueueManager,
                    EventBus eventBus, LegacyTrackOperations trackOperations,
                    AccountOperations accountOperations,
                    StreamPlaya streamPlaya,
                    PlaybackReceiver.Factory playbackReceiverFactory, Lazy<IRemoteAudioManager> remoteAudioManagerProvider,
                    FeatureFlags featureFlags, PlaybackNotificationController playbackNotificationController) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.accountOperations = accountOperations;
        this.streamPlayer = streamPlaya;
        this.playbackReceiverFactory = playbackReceiverFactory;
        this.remoteAudioManagerProvider = remoteAudioManagerProvider;
        this.featureFlags = featureFlags;
        this.playbackNotificationController = playbackNotificationController;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        streamPlayer.setListener(this);

        playbackReceiver = playbackReceiverFactory.create(this, accountOperations, playQueueManager, eventBus);
        audioManager = remoteAudioManagerProvider.get();

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(Actions.TOGGLEPLAYBACK_ACTION);
        playbackFilter.addAction(Actions.PLAY_CURRENT);
        playbackFilter.addAction(Actions.PAUSE_ACTION);
        playbackFilter.addAction(Actions.SEEK);
        playbackFilter.addAction(Actions.RESET_ALL);
        playbackFilter.addAction(Actions.STOP_ACTION);
        playbackFilter.addAction(PlayQueueManager.PLAYQUEUE_CHANGED_ACTION);
        playbackFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(playbackReceiver, playbackFilter);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        scheduleServiceShutdownCheck();
        instance = this;
    }

    @Override
    public void onDestroy() {
        stop();
        streamPlayer.destroy();

        // make sure there aren't any other messages coming
        delayedStopHandler.removeCallbacksAndMessages(null);
        fadeHandler.removeCallbacksAndMessages(null);
        audioManager.abandonMusicFocus(false);
        unregisterReceiver(playbackReceiver);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        delayedStopHandler.removeCallbacksAndMessages(null);
        serviceInUse = true;
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        delayedStopHandler.removeCallbacksAndMessages(null);
        serviceInUse = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        serviceInUse = false;

        if (streamPlayer.isPlaying() || focusLossState != FocusLossState.NONE) {
            // something is currently playing, or will be playing once
            // an in-progress call ends, so don't stop the service now.
            return true;

        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between
        // tracks.
        } else if (!getPlayQueueInternal().isEmpty()) {
            delayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
            return true;

        } else {
            // No active playlist, OK to stop the service right now
            stopSelf(serviceStartId);
            return true;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceStartId = startId;
        delayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {
            boolean hasAccount = accountOperations.isUserLoggedIn();
            if (hasAccount && playQueueManager.shouldReloadQueue()){
                playQueueManager.loadPlayQueue();
            }
            playbackReceiver.onReceive(this, intent);
        }
        scheduleServiceShutdownCheck();
        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        return START_STICKY;
    }

    @Override
    public void focusGained() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "focusGained");
        if (focusLossState == FocusLossState.TRANSIENT) {
            fadeHandler.sendEmptyMessage(FadeHandler.FADE_IN);
        } else {
            streamPlayer.setVolume(1.0f);
        }

        focusLossState = FocusLossState.NONE;
    }

    @Override
    public void focusLost(boolean isTransient, boolean canDuck) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "focusLost(" + isTransient + ", canDuck=" + canDuck + ")");
        }
        if (streamPlayer.isPlaying()) {

            if (isTransient){
                focusLossState = FocusLossState.TRANSIENT;
                fadeHandler.sendEmptyMessage(canDuck ? FadeHandler.DUCK : FadeHandler.FADE_OUT);
            } else {
                focusLossState = FocusLossState.LOST;
                pause();
            }
        }
    }

    public void resetAll() {
        stop();
        currentTrack = null;
        audioManager.abandonMusicFocus(false); // kills lockscreen
    }

    boolean isWaitingForPlaylist() {
        return waitingForPlaylist;
    }

    private void scheduleServiceShutdownCheck() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "scheduleServiceShutdownCheck()");
        }
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
    }

    @Override
    public void onPlaystateChanged(Playa.StateTransition stateTransition) {

        if (!stateTransition.isPlaying()) {
            onIdleState();
        }

        if (currentTrack != null) {
            stateTransition.setTrackUrn(currentTrack.getUrn());
        }

        notifyChange(Broadcasts.PLAYSTATE_CHANGED, stateTransition);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateTransition);
    }

    private void onIdleState() {
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forIdle());
        scheduleServiceShutdownCheck();
        fadeHandler.removeMessages(FadeHandler.FADE_OUT);
        fadeHandler.removeMessages(FadeHandler.FADE_IN);
    }

    @Override
    public void onProgressEvent(long position, long duration) {
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)){
            final PlaybackProgress playbackProgress = new PlaybackProgress(position, duration);
            final PlaybackProgressEvent event = new PlaybackProgressEvent(playbackProgress, currentTrack.getUrn());
            eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);
        }
    }

    @Override
    public boolean requestAudioFocus() {
        return audioManager.requestMusicFocus(PlaybackService.this, IAudioManager.FOCUS_GAIN);
    }

    void notifyChange(String what) {
        notifyChange(what, streamPlayer.getLastStateTransition());
    }

    void notifyChange(String what, Playa.StateTransition stateTransition) {

        Log.d(TAG, "notifyChange(" + what + ", " + stateTransition.getNewState() + ")");
        Intent intent = new Intent(what)
            .putExtra(BroadcastExtras.ID, getTrackId())
            .putExtra(BroadcastExtras.USER_ID, getTrackUserId())
            .putExtra(BroadcastExtras.PROGRESS_POSITION, getProgress())
            .putExtra(BroadcastExtras.QUEUE_POSITION, getPlayQueueInternal().getPosition());

        stateTransition.addToIntent(intent);
        sendBroadcast(intent);

        if (what.equals(Broadcasts.PLAYSTATE_CHANGED) && !suppressNotifications) {
            if (stateTransition.playSessionIsActive()) {
                playbackNotificationController.playingNotification().doOnNext(startForegroundAction).subscribe();
            } else {
                if (!playbackNotificationController.notifyIdleState()){
                    stopForeground(true);
                }
            }
        }
    }

    // TODO : Handle tracks that are not in local storage (quicksearch)
    /* package */ void openCurrent() {
        if (!getPlayQueueInternal().isEmpty()) {

            streamPlayer.startBufferingMode();

            final long currentTrackId = getPlayQueueInternal().getCurrentTrackId();
            loadTrackSubscription.unsubscribe();
            loadTrackSubscription = trackOperations.loadTrack(currentTrackId, AndroidSchedulers.mainThread())
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
            suppressNotifications = false;
            waitingForPlaylist = false;

            if (track.equals(currentTrack) && track.isStreamable()) {
                if (!streamPlayer.isPlayerPlaying()) {
                    startTrack(track);
                }
            } else { // new track
                currentTrack = track;

                notifyChange(Broadcasts.META_CHANGED);
                final Observable<Track> trackObservable = trackOperations.loadStreamableTrack(currentTrack.getId(), AndroidSchedulers.mainThread());

                streamableTrackSubscription.unsubscribe();
                streamableTrackSubscription = trackObservable
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
            fireAndForget(trackOperations.markTrackAsPlayed(track));
            startTrack(track);
        }
    };

    private void startTrack(Track track) {
        // set current track again as it may have been fetched from the api. This is not necessary with modelmanager, but will be going forward
        currentTrack = track;
        Log.d(TAG, "startTrack("+track.title+")");

        final PlaybackProgressInfo resumeInfo = playQueueManager.getPlayProgressInfo();
        if (resumeInfo != null && resumeInfo.getTrackId() == track.getId()){
            streamPlayer.play(currentTrack, resumeInfo.getTime());
        } else {
            streamPlayer.play(currentTrack);
        }
    }



    /* package */ void play() {
        if (!streamPlayer.isPlaying() && currentTrack != null && audioManager.requestMusicFocus(this, IAudioManager.FOCUS_GAIN)) {
            if (!(currentTrack.getUrn().equals(playQueueManager.getCurrentTrackUrn()) && streamPlayer.playbackHasPaused() && streamPlayer.resume())) {
                openCurrent();
            } else {
                suppressNotifications = false;
            }
        }
    }

    // Pauses playback (call play() to resume)
    /* package */ void pause() {
        streamPlayer.pause();
    }

    public void togglePlayback() {
        if (streamPlayer.isPlaying()) {
            pause();
        } else if (currentTrack != null) {
            play();
        } else if (!getPlayQueueInternal().isEmpty()) {
            openCurrent();
        } else {
            waitingForPlaylist = true;
        }
    }

    /* package */ void stop() {
        streamPlayer.stop();
        suppressNotifications = true;
        stopForeground(true);
    }


    /* package */ int getDuration() {
        return currentTrack == null ? -1 : currentTrack.duration;
    }

    /* package */ boolean isBuffering() {
        return streamPlayer.isBuffering();
    }

    /*
     * Returns the current playback position in milliseconds
     */
    public long getProgress() {
        return streamPlayer.getProgress();
    }

    /* package */ boolean isSeekable() {
        return streamPlayer.isSeekable();
    }

    public long seek(float percent, boolean performSeek) {
        final int duration = getDuration();
        final boolean invalidSeek = duration <= 0 || percent < 0 || percent > 1;
        if (invalidSeek){
            final String message = "Invalid Seek [percent=" + percent + ", duration=" + duration +"]";
            SoundCloudApplication.handleSilentException(message, new IllegalStateException(message));
            return 0;
        } else {
            return seek((long) (duration * percent), performSeek);
        }
    }

    public long seek(long pos, boolean performSeek) {
        return streamPlayer.seek(pos, performSeek);
    }

    public boolean isPlaying() {
        return streamPlayer.isPlaying();
    }

    public boolean isPlayerPlaying() {
        return streamPlayer.isPlayerPlaying();
    }

    public boolean isSupposedToBePlaying() {
        return streamPlayer.isPlaying();
    }

    public void restartTrack() {
        openCurrent();
    }

    PlayQueue getPlayQueueInternal() {
        return playQueueManager.getCurrentPlayQueue();
    }

    private long getTrackUserId() {
        return currentTrack != null ? currentTrack.user_id : -1;
    }

    Track getCurrentTrack() {
        return currentTrack;
    }

    long getTrackId() {
        return currentTrack == null ? -1 : currentTrack.getId();
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
            if (service != null && !service.streamPlayer.isPlaying()
                    && service.focusLossState == FocusLossState.NONE
                    && !service.serviceInUse) {

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "DelayedStopHandler: stopping service");
                }
                service.stopSelf(service.serviceStartId);
            }
        }
    }



    private static final class FadeHandler extends Handler {

        private static final int FADE_IN          = 1;
        private static final int FADE_OUT         = 2;
        private static final int DUCK             = 3;

        private WeakReference<PlaybackService> serviceRef;
        private float currentVolume;

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
                    if (!service.streamPlayer.isPlaying()) {
                        currentVolume = 0f;
                        service.streamPlayer.setVolume(0f);
                        service.play();
                        sendEmptyMessageDelayed(FADE_IN, 10);
                    } else {
                        currentVolume += FADE_CHANGE;
                        if (currentVolume < 1.0f) {
                            sendEmptyMessageDelayed(FADE_IN, 10);
                        } else {
                            currentVolume = 1.0f;
                        }
                        service.streamPlayer.setVolume(currentVolume);
                    }
                    break;
                case FADE_OUT:
                    removeMessages(FADE_IN);
                    if (service.streamPlayer.isPlaying()) {
                        currentVolume -= FADE_CHANGE;
                        if (currentVolume > 0f) {
                            sendEmptyMessageDelayed(FADE_OUT, 10);
                        } else {
                            if (service != null) {
                                service.pause();
                            }
                            currentVolume = 0f;
                        }
                        service.streamPlayer.setVolume(currentVolume);
                    } else {
                        service.streamPlayer.setVolume(0f);
                    }
                    break;
                case DUCK:
                    removeMessages(FADE_IN);
                    removeMessages(FADE_OUT);
                    service.streamPlayer.setVolume(DUCK_VOLUME);
                    break;
                default: // NO-OP
                    break;
            }
        }
    }

}
