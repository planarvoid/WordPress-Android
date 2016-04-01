package com.soundcloud.android.playback;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.playback.notification.PlaybackNotificationController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import org.jetbrains.annotations.Nullable;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

// remove this once we remove PlayQueueManager + TrackOperations by moving url loading out
@SuppressWarnings({"PMD.ExcessiveParameterList"})
public class PlaybackService extends Service implements IAudioManager.MusicFocusable, Player.PlayerListener {
    public static final String TAG = "PlaybackService";
    private static final int IDLE_DELAY = 180 * 1000;  // interval after which we stop the service when idle
    @Nullable static PlaybackService instance;
    private final Handler fadeHandler = new FadeHandler(this);
    private final Handler delayedStopHandler = new DelayedStopHandler(this);
    @Inject EventBus eventBus;
    @Inject AccountOperations accountOperations;
    @Inject StreamPlayer streamPlayer;
    @Inject PlaybackReceiver.Factory playbackReceiverFactory;
    @Inject Lazy<IRemoteAudioManager> remoteAudioManagerProvider;
    @Inject PlaybackNotificationController playbackNotificationController;
    @Inject PlaybackSessionAnalyticsController analyticsController;
    @Inject AdsController adsController;
    @Inject AdsOperations adsOperations;

    private Optional<PlaybackItem> currentPlaybackItem = Optional.absent();
    // audio focus related
    private IAudioManager audioManager;
    private FocusLossState focusLossState = FocusLossState.NONE;
    private PlaybackReceiver playbackReceiver;


    public PlaybackService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    PlaybackService(EventBus eventBus,
                    AccountOperations accountOperations,
                    StreamPlayer streamPlayer,
                    PlaybackReceiver.Factory playbackReceiverFactory, Lazy<IRemoteAudioManager> remoteAudioManagerProvider,
                    PlaybackNotificationController playbackNotificationController,
                    PlaybackSessionAnalyticsController analyticsController,
                    AdsOperations adsOperations,
                    AdsController adsController) {
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.streamPlayer = streamPlayer;
        this.playbackReceiverFactory = playbackReceiverFactory;
        this.remoteAudioManagerProvider = remoteAudioManagerProvider;
        this.playbackNotificationController = playbackNotificationController;
        this.analyticsController = analyticsController;
        this.adsOperations = adsOperations;
        this.adsController = adsController;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        streamPlayer.setListener(this);

        playbackReceiver = playbackReceiverFactory.create(this, accountOperations, eventBus);
        audioManager = remoteAudioManagerProvider.get();

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(Action.TOGGLE_PLAYBACK);
        playbackFilter.addAction(Action.PLAY_CURRENT);
        playbackFilter.addAction(Action.PAUSE);
        playbackFilter.addAction(Action.SEEK);
        playbackFilter.addAction(Action.RESET_ALL);
        playbackFilter.addAction(Action.STOP);
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
        audioManager.abandonMusicFocus();
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
        Log.d(TAG, "[FOCUS] focusGained with state " + focusLossState);
        if (focusLossState == FocusLossState.TRANSIENT) {
            fadeHandler.sendEmptyMessage(FadeHandler.FADE_IN);
        } else {
            streamPlayer.setVolume(1.0f);
        }

        focusLossState = FocusLossState.NONE;
    }

    @Override
    public void focusLost(boolean isTransient, boolean canDuck) {
        Log.d(TAG, "[FOCUS] focusLost(playing=" + streamPlayer.isPlaying() + ", transient=" + isTransient + ", canDuck=" + canDuck + ")");
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
        currentPlaybackItem = Optional.absent();
        audioManager.abandonMusicFocus(); // kills lockscreen
    }

    @Override
    public void onPlaystateChanged(PlaybackStateTransition stateTransition) {
        Log.d(TAG, "Received new playState " + stateTransition);

        // TODO : Fix threading in Skippy so we can never receive delayed messages
        if (currentPlaybackItem.isPresent() && currentPlaybackItem.get().getUrn().equals(stateTransition.getUrn())) {
            if (!stateTransition.isPlaying()) {
                onIdleState();
            }

            analyticsController.onStateTransition(stateTransition);
            adsController.onPlayStateTransition(stateTransition);
            eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, correctUnknownDuration(stateTransition, currentPlaybackItem.get()));
        }
    }

    @Override
    public void onProgressEvent(long position, long duration) {
        if (currentPlaybackItem.isPresent()) {
            final PlaybackItem playbackItem = currentPlaybackItem.get();
            final PlaybackProgressEvent playbackProgress = PlaybackProgressEvent.create(new PlaybackProgress(position, duration), playbackItem.getUrn());
            analyticsController.onProgressEvent(playbackProgress);
            eventBus.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgress);
        }
    }

    private static PlaybackStateTransition correctUnknownDuration(PlaybackStateTransition stateTransition, PlaybackItem playbackItem) {
        final PlaybackProgress progress = stateTransition.getProgress();
        if (!progress.isDurationValid()) {
            progress.setDuration(playbackItem.getDuration());
        }
        return stateTransition;
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
        } else if (currentPlaybackItem.isPresent()) {
            play();
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

    void preload(PreloadItem preloadItem) {
        streamPlayer.preload(preloadItem);
    }

    void play(PlaybackItem playbackItem) {
        currentPlaybackItem = Optional.of(playbackItem);
        streamPlayer.play(playbackItem);
    }

    /* package */ void play() {
        Log.d(TAG, "Playing");
        if (!streamPlayer.isPlaying() && currentPlaybackItem.isPresent() && audioManager.requestMusicFocus(this, IAudioManager.FOCUS_GAIN)) {
            streamPlayer.resume();
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
    public interface Action {
        String TOGGLE_PLAYBACK = BuildConfig.APPLICATION_ID + ".playback.toggleplayback";
        String PRELOAD = BuildConfig.APPLICATION_ID + ".playback.preload";
        String PLAY_CURRENT = BuildConfig.APPLICATION_ID + ".playback.playcurrent";
        String RESUME = BuildConfig.APPLICATION_ID + ".playback.playcurrent";
        String PAUSE = BuildConfig.APPLICATION_ID + ".playback.pause";
        String SEEK = BuildConfig.APPLICATION_ID + ".playback.seek";
        String RESET_ALL = BuildConfig.APPLICATION_ID + ".playback.reset"; // used on logout
        String STOP = BuildConfig.APPLICATION_ID + ".playback.stop"; // from the notification
        String PLAY = BuildConfig.APPLICATION_ID + ".playback.play";
    }

    public interface ActionExtras {
        String POSITION = "seek_position";
        String PLAYBACK_ITEM = "playback_item";
        String PRELOAD_ITEM = "preload_item";
        String URN = "urn";
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
}
