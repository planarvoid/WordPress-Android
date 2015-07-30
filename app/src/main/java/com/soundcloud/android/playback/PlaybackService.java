package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.notification.PlaybackNotificationController;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;
import dagger.Lazy;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

// remove this once we remove PlayQueueManager + TrackOperations by moving url loading out
@SuppressWarnings({"PMD.ExcessiveParameterList"})
public class PlaybackService extends Service implements IAudioManager.MusicFocusable, Playa.PlayaListener {
    public static final String TAG = "PlaybackService";
    private static final int IDLE_DELAY = 180 * 1000;  // interval after which we stop the service when idle
    @Nullable static PlaybackService instance;
    private final Handler fadeHandler = new FadeHandler(this);
    private final Handler delayedStopHandler = new DelayedStopHandler(this);
    @Inject EventBus eventBus;
    @Inject PlayQueueManager playQueueManager;
    @Inject TrackRepository trackRepository;
    @Inject AccountOperations accountOperations;
    @Inject StreamPlaya streamPlayer;
    @Inject PlaybackReceiver.Factory playbackReceiverFactory;
    @Inject Lazy<IRemoteAudioManager> remoteAudioManagerProvider;
    @Inject PlaybackNotificationController playbackNotificationController;
    @Inject PlaybackSessionAnalyticsController analyticsController;
    @Inject AdsOperations adsOperations;
    // XXX : would be great to not have these boolean states
    private boolean waitingForPlaylist;
    @Nullable private PropertySet currentTrack;
    // audio focus related
    private IAudioManager audioManager;
    private FocusLossState focusLossState = FocusLossState.NONE;
    private PlaybackReceiver playbackReceiver;
    private Subscription loadTrackSubscription = RxUtils.invalidSubscription();

    public PlaybackService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    PlaybackService(PlayQueueManager playQueueManager,
                    EventBus eventBus,
                    TrackRepository trackRepository,
                    AccountOperations accountOperations,
                    StreamPlaya streamPlaya,
                    PlaybackReceiver.Factory playbackReceiverFactory, Lazy<IRemoteAudioManager> remoteAudioManagerProvider,
                    PlaybackNotificationController playbackNotificationController,
                    PlaybackSessionAnalyticsController analyticsController, AdsOperations adsOperations) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.accountOperations = accountOperations;
        this.streamPlayer = streamPlaya;
        this.playbackReceiverFactory = playbackReceiverFactory;
        this.remoteAudioManagerProvider = remoteAudioManagerProvider;
        this.playbackNotificationController = playbackNotificationController;
        this.analyticsController = analyticsController;
        this.adsOperations = adsOperations;
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

        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
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
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        playbackNotificationController.subscribe(this);

        delayedStopHandler.removeCallbacksAndMessages(null);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStarted());

        if (intent != null) {
            boolean hasAccount = accountOperations.isUserLoggedIn();
            if (hasAccount && playQueueManager.shouldReloadQueue()) {
                playQueueManager.loadPlayQueueAsync();
            }
            playbackReceiver.onReceive(this, intent);
            scheduleServiceShutdownCheck();
            return START_STICKY;
        } else {

            // the system will automatically re-start a service that was killed by the user.
            // Shutdown to go through normal shutdown logic
            delayedStopHandler.removeCallbacksAndMessages(null);
            delayedStopHandler.sendEmptyMessage(0);
            return START_NOT_STICKY;
        }
    }

    @Override
    public void focusGained() {
        Log.d(TAG, "focusGained with state " + focusLossState);
        if (focusLossState == FocusLossState.TRANSIENT) {
            fadeHandler.sendEmptyMessage(FadeHandler.FADE_IN);
        } else {
            streamPlayer.setVolume(1.0f);
        }

        focusLossState = FocusLossState.NONE;
    }

    @Override
    public void focusLost(boolean isTransient, boolean canDuck) {
        Log.d(TAG, "focusLost(" + isTransient + ", canDuck=" + canDuck + ")");
        if (streamPlayer.isPlaying()) {

            if (isTransient) {
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

    @Override
    public void onPlaystateChanged(Playa.StateTransition stateTransition) {
        Log.d(TAG, "Received new playState " + stateTransition);

        // TODO : Fix threading in Skippy so we can never receive delayed messages
        if (getCurrentTrackUrn().equals(stateTransition.getTrackUrn())) {
            if (!stateTransition.isPlaying()) {
                onIdleState();
            }

            analyticsController.onStateTransition(stateTransition);
            eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateTransition);
        }
    }

    @Override
    public void onProgressEvent(long position, long duration) {
        final PlaybackProgress playbackProgress = new PlaybackProgress(position, duration);
        final Urn trackUrn = checkNotNull(currentTrack, "Current track is null.").get(TrackProperty.URN);
        final PlaybackProgressEvent event = new PlaybackProgressEvent(playbackProgress, trackUrn);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);
    }

    @Override
    public boolean requestAudioFocus() {
        Log.d(TAG, "Requesting audio focus");
        return audioManager.requestMusicFocus(PlaybackService.this, IAudioManager.FOCUS_GAIN);
    }

    public void togglePlayback() {
        Log.d(TAG, "Toggling playback");
        if (streamPlayer.isPlaying()) {
            pause();
        } else if (currentTrack != null) {
            play();
        } else if (!playQueueManager.isQueueEmpty()) {
            openCurrent();
        } else {
            waitingForPlaylist = true;
        }
    }

    public long seek(long pos, boolean performSeek) {
        Log.d(TAG, "Seeking to " + pos);
        return streamPlayer.seek(pos, performSeek);
    }

    public boolean isPlaying() {
        return streamPlayer.isPlaying();
    }

    public boolean isPlayerPlaying() {
        return streamPlayer.isPlayerPlaying();
    }

    private void scheduleServiceShutdownCheck() {
        Log.d(TAG, "scheduleServiceShutdownCheck()");
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
    }

    private void onIdleState() {
        Log.d(TAG, "On Idle State (focusLossState=" + focusLossState + ")");
        scheduleServiceShutdownCheck();
        fadeHandler.removeMessages(FadeHandler.FADE_OUT);
        fadeHandler.removeMessages(FadeHandler.FADE_IN);
    }

    private void playCurrentTrackFromStart(boolean playUninterrupted) {
        if (playUninterrupted) {
            streamPlayer.playUninterrupted(currentTrack);
        } else {
            streamPlayer.play(currentTrack);
        }
    }

    private Urn getCurrentTrackUrn() {
        return currentTrack == null ? Urn.NOT_SET : currentTrack.get(TrackProperty.URN);
    }

    boolean isWaitingForPlaylist() {
        return waitingForPlaylist;
    }

    // TODO : Handle tracks that are not in local storage (quicksearch)
    /* package */ void openCurrent() {
        if (!playQueueManager.isQueueEmpty()) {
            streamPlayer.startBufferingMode(playQueueManager.getCurrentTrackUrn());

            loadTrackSubscription.unsubscribe();
            loadTrackSubscription = trackRepository.track(playQueueManager.getCurrentTrackUrn())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new TrackInformationSubscriber(adsOperations.isCurrentTrackAudioAd()));
        }
    }

    @VisibleForTesting
    void openCurrent(PropertySet track, boolean playUninterrupted) {
        waitingForPlaylist = false;
        currentTrack = track;

        final PlaybackProgressInfo resumeInfo = playQueueManager.getPlayProgressInfo();
        if (!playUninterrupted && resumeInfo != null && resumeInfo.shouldResumeTrack(getCurrentTrackUrn())) {
            Log.d(TAG, "Resuming track at " + resumeInfo.getTime());
            streamPlayer.play(currentTrack, resumeInfo.getTime());
        } else {
            playCurrentTrackFromStart(playUninterrupted);
        }
    }

    /* package */ void play() {
        Log.d(TAG, "Playing");
        if (!streamPlayer.isPlaying() && currentTrack != null && audioManager.requestMusicFocus(this, IAudioManager.FOCUS_GAIN)) {
            if (!(playQueueManager.isCurrentTrack(getCurrentTrackUrn()) && streamPlayer.playbackHasPaused() && streamPlayer.resume())) {
                Log.d(TAG, "Re-opening current track as it is not resumable");
                openCurrent();
            }
            resetVolume();
        }
    }

    private void resetVolume() {
        Log.d(TAG, "Resetting volume");
        fadeHandler.removeCallbacksAndMessages(null);
        streamPlayer.setVolume(1);
    }

    // Pauses playback (call play() to resume)
    /* package */ void pause() {
        Log.d(TAG, "Pausing");
        streamPlayer.pause();
    }

    /* package */ void stop() {
        Log.d(TAG, "Stopping");
        playbackNotificationController.unsubscribe();
        streamPlayer.stop();
        stopForeground(true);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStopped());
    }

    private enum FocusLossState {
        NONE, TRANSIENT, LOST
    }

    // public service actions
    public interface Actions {
        String TOGGLEPLAYBACK_ACTION = "com.soundcloud.android.playback.toggleplayback";
        String PLAY_CURRENT = "com.soundcloud.android.playback.playcurrent";
        String PLAY_ACTION = "com.soundcloud.android.playback.playcurrent";
        String PAUSE_ACTION = "com.soundcloud.android.playback.pause";
        String SEEK = "com.soundcloud.android.playback.seek";
        String RESET_ALL = "com.soundcloud.android.playback.reset"; // used on logout
        String STOP_ACTION = "com.soundcloud.android.playback.stop"; // from the notification
    }

    public interface ActionsExtras {
        String SEEK_POSITION = "seek_position";
    }

    // broadcast notifications
    public interface Broadcasts {
        String PLAYSTATE_CHANGED = "com.soundcloud.android.playstatechanged";
        @Deprecated
        String META_CHANGED = "com.soundcloud.android.metachanged";
    }

    private static final class DelayedStopHandler extends Handler {
        private final WeakReference<PlaybackService> serviceRef;

        private DelayedStopHandler(PlaybackService service) {
            serviceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaybackService service = serviceRef.get();
            // Check again to make sure nothing is playing right now
            if (service != null && !service.streamPlayer.isPlaying()
                    && service.focusLossState == FocusLossState.NONE) {

                Log.d(TAG, "DelayedStopHandler: stopping service");
                service.stopSelf();
            }
        }
    }

    private static final class FadeHandler extends Handler {

        private static final int FADE_IN = 1;
        private static final int FADE_OUT = 2;
        private static final int DUCK = 3;
        private static final float FADE_CHANGE = 0.02f; // change to fade faster/slower
        private static final float DUCK_VOLUME = 0.1f;
        private final WeakReference<PlaybackService> serviceRef;
        private float currentVolume;

        private FadeHandler(PlaybackService service) {
            this.serviceRef = new WeakReference<>(service);
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
                        Log.d(TAG, "Skipping fade in as we are not playing");
                        currentVolume = 0f;
                        service.streamPlayer.setVolume(0f);
                        service.play();
                        sendEmptyMessageDelayed(FADE_IN, 10);
                    } else {
                        currentVolume += FADE_CHANGE;
                        Log.d(TAG, "Fading volume in at : " + currentVolume);
                        if (currentVolume < 1.0f) {
                            sendEmptyMessageDelayed(FADE_IN, 10);
                        } else {
                            Log.d(TAG, "Done fading volume in");
                            currentVolume = 1.0f;
                        }
                        service.streamPlayer.setVolume(currentVolume);
                    }
                    break;
                case FADE_OUT:
                    removeMessages(FADE_IN);
                    if (service.streamPlayer.isPlaying()) {
                        currentVolume -= FADE_CHANGE;
                        Log.d(TAG, "Fading out at " + currentVolume);
                        if (currentVolume > 0f) {
                            sendEmptyMessageDelayed(FADE_OUT, 10);
                        } else {
                            Log.d(TAG, "Done fading out, pausing ");
                            service.pause();
                            currentVolume = 0f;
                        }
                        service.streamPlayer.setVolume(currentVolume);
                    } else {
                        Log.d(TAG, "Fading out without playing, setting volume to 0");
                        service.streamPlayer.setVolume(0f);
                    }
                    break;
                case DUCK:
                    Log.d(TAG, "Ducking");
                    removeMessages(FADE_IN);
                    removeMessages(FADE_OUT);
                    service.streamPlayer.setVolume(DUCK_VOLUME);
                    break;
                default: // NO-OP
                    break;
            }
        }
    }

    private class TrackInformationSubscriber extends DefaultSubscriber<PropertySet> {
        private final boolean playUninterrupted;

        TrackInformationSubscriber(boolean playUninterrupted) {
            this.playUninterrupted = playUninterrupted;
        }

        @Override
        public void onError(Throwable throwable) {
            Log.d(TAG, "Unable to get track information " + throwable);
            onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED, getCurrentTrackUrn()));
        }

        @Override
        public void onNext(PropertySet track) {
            openCurrent(track, playUninterrupted);
        }
    }
}
