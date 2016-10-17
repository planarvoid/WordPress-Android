package com.soundcloud.android.playback;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.playback.mediasession.MediaSessionController;
import com.soundcloud.android.playback.mediasession.MediaSessionControllerFactory;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.app.Notification;
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
public class PlaybackService extends Service
        implements Player.PlayerListener, VolumeController.Listener, MediaSessionController.Listener {

    public static final String TAG = "PlaybackService";

    private static final int IDLE_DELAY = 180 * 1000;  // interval after which we stop the service when idle
    private static final int SHORT_FADE_DURATION_MS = 600;
    private static final int LONG_FADE_DURATION_MS = 2000;
    private static final int LONG_FADE_PRELOAD_MS = 1000; // interval to start fade before fade duration

    private final Handler delayedStopHandler = new DelayedStopHandler(this);

    @Nullable static PlaybackService instance;

    @Inject EventBus eventBus;
    @Inject AccountOperations accountOperations;
    @Inject StreamPlayer streamPlayer;
    @Inject PlaybackReceiver.Factory playbackReceiverFactory;
    @Inject PlaybackAnalyticsController analyticsDispatcher;
    @Inject VolumeControllerFactory volumeControllerFactory;
    @Inject MediaSessionControllerFactory mediaSessionControllerFactory;
    @Inject PlaySessionStateProvider playSessionStateProvider;
    @Inject PlayStatePublisher playStatePublisher;

    private Optional<PlaybackItem> currentPlaybackItem = Optional.absent();
    private boolean pauseRequested;

    // audio focus related
    private FocusLossState focusLossState = FocusLossState.NONE;
    private PlaybackReceiver playbackReceiver;
    private VolumeController volumeController;
    private MediaSessionController mediaSessionController;

    public PlaybackService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    PlaybackService(EventBus eventBus,
                    AccountOperations accountOperations,
                    StreamPlayer streamPlayer,
                    PlaybackReceiver.Factory playbackReceiverFactory,
                    PlaybackAnalyticsController analyticsDispatcher,
                    VolumeControllerFactory volumeControllerFactory,
                    MediaSessionControllerFactory mediaSessionControllerFactory,
                    PlaySessionStateProvider playSessionStateProvider,
                    PlayStatePublisher playStatePublisher) {
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.streamPlayer = streamPlayer;
        this.playbackReceiverFactory = playbackReceiverFactory;
        this.analyticsDispatcher = analyticsDispatcher;
        this.volumeControllerFactory = volumeControllerFactory;
        this.mediaSessionControllerFactory = mediaSessionControllerFactory;
        this.playSessionStateProvider = playSessionStateProvider;
        this.playStatePublisher = playStatePublisher;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        streamPlayer.setListener(this);
        mediaSessionController = mediaSessionControllerFactory.create(this, this);
        volumeController = volumeControllerFactory.create(streamPlayer, this);
        playbackReceiver = playbackReceiverFactory.create(this, accountOperations);

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(Action.TOGGLE_PLAYBACK);
        playbackFilter.addAction(Action.PLAY_CURRENT);
        playbackFilter.addAction(Action.PAUSE);
        playbackFilter.addAction(Action.SEEK);
        playbackFilter.addAction(Action.RESET_ALL);
        playbackFilter.addAction(Action.STOP);
        playbackFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        playbackFilter.addAction(Action.FADE_AND_PAUSE);
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
        volumeController.resetVolume();
        mediaSessionController.onDestroy();
        streamPlayer.destroy();

        // make sure there aren't any other messages coming
        delayedStopHandler.removeCallbacksAndMessages(null);
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
        delayedStopHandler.removeCallbacksAndMessages(null);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStarted());

        if (intent != null) {
            playbackReceiver.onReceive(this, intent);
            scheduleServiceShutdownCheck();
            mediaSessionController.onStartCommand(intent);
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
    public void showNotification(Notification notification) {
        startForeground(NotificationConstants.PLAYBACK_NOTIFY_ID, notification);
    }

    public void onFocusGain() {
        Log.d(TAG, "[FOCUS] focusGained with state " + focusLossState);
        volumeController.unMute(SHORT_FADE_DURATION_MS);
        focusLossState = FocusLossState.NONE;
    }

    public void onFocusLoss(boolean isTransient, boolean canDuck) {
        Log.d(TAG,
              "[FOCUS] focusLost(playing=" + streamPlayer.isPlaying() + ", transient=" + isTransient + ", canDuck=" + canDuck + ")");
        if (streamPlayer.isPlaying()) {
            if (isTransient) {
                focusLossState = FocusLossState.TRANSIENT;
                if (canDuck) {
                    volumeController.duck(SHORT_FADE_DURATION_MS);
                } else {
                    volumeController.mute(SHORT_FADE_DURATION_MS);
                }
            } else {
                focusLossState = FocusLossState.LOST;
                volumeController.mute(SHORT_FADE_DURATION_MS);
                pause();
            }
        }
    }

    public void resetAll() {
        stop();
        currentPlaybackItem = Optional.absent();
    }

    @Override
    public void onPlaystateChanged(PlaybackStateTransition stateTransition) {
        Log.d(TAG, "Received new playState " + stateTransition);

        // TODO : Fix threading in Skippy so we can never receive delayed messages
        if (currentPlaybackItem.isPresent()) {
            if (currentPlaybackItem.get().getUrn().equals(stateTransition.getUrn())) {
                if (!stateTransition.isPlaying()) {
                    onIdleState();
                }

                playStatePublisher.publish(stateTransition, currentPlaybackItem.get(), true);
                long position = stateTransition.getProgress().getPosition();
                if (stateTransition.isBuffering()) {
                    mediaSessionController.onBuffering(position);
                } else if (stateTransition.isPlaying()) {
                    mediaSessionController.onPlaying(position);
                }

            } else {
                Log.d(TAG, "resetting volume after playstate changed on different track");
                resetVolume(0);
            }
        }
    }

    @Override
    public void onProgressEvent(long position, long duration) {
        if (currentPlaybackItem.isPresent()) {
            final PlaybackItem playbackItem = currentPlaybackItem.get();
            final PlaybackProgressEvent playbackProgress = PlaybackProgressEvent.create(new PlaybackProgress(position,
                                                                                                             duration,
                                                                                                             playbackItem.getUrn()),
                                                                                        playbackItem.getUrn());

            if (currentPlaybackItem.isPresent()) {
                playSessionStateProvider.onProgressEvent(playbackProgress);
                analyticsDispatcher.onProgressEvent(currentPlaybackItem.get(), playbackProgress);
            }

            eventBus.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgress);
            mediaSessionController.onProgress(position);
            fadeOutIfNecessary(position);
        }
    }

    private void fadeOutIfNecessary(long position) {
        if (isAudioSnippet()) {
            long duration = currentPlaybackItem.get().getDuration();
            long fadeOffset = LONG_FADE_DURATION_MS - (duration - position);

            if (isWithinSnippetFadeOut(LONG_FADE_DURATION_MS, fadeOffset)) {
                volumeController.fadeOut(LONG_FADE_DURATION_MS, fadeOffset);
            }
        }
    }

    private boolean isAudioSnippet() {
        return currentPlaybackItem.isPresent()
                && currentPlaybackItem.get().getPlaybackType() == PlaybackType.AUDIO_SNIPPET;
    }

    private boolean isWithinSnippetFadeOut(long fadeDuration, long fadeOffset) {
        return fadeOffset <= fadeDuration && fadeOffset >= -LONG_FADE_PRELOAD_MS;
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
        resetVolume(pos);
        mediaSessionController.onSeek(pos);
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
    }

    void preload(PreloadItem preloadItem) {
        streamPlayer.preload(preloadItem);
        mediaSessionController.onPreload(preloadItem.getUrn());
    }

    void play(PlaybackItem playbackItem) {
        Log.d(TAG, "Play item (playbackItem=" + playbackItem + ")");
        currentPlaybackItem = Optional.of(playbackItem);
        if (mediaSessionController.onPlay(playbackItem)) {
            resetVolume(playbackItem.getStartPosition());
            streamPlayer.play(playbackItem);
        }
    }

    public void play() {
        Log.d(TAG, "Playing");
        if (!streamPlayer.isPlaying() && currentPlaybackItem.isPresent()) {
            if (mediaSessionController.onPlay()) {
                resetVolume(streamPlayer.getProgress());
                streamPlayer.resume(currentPlaybackItem.get());
            }
        }
    }

    private void resetVolume(long position) {
        volumeController.resetVolume();
        fadeOutIfNecessary(position);
    }

    public void pause() {
        Log.d(TAG, "Pausing");
        streamPlayer.pause();
        mediaSessionController.onPause();
        unpinNotification(false);
    }

    public void fadeAndPause() {
        pauseRequested = true;
        volumeController.fadeOut(LONG_FADE_DURATION_MS, 0);
    }

    public void stop() {
        Log.d(TAG, "Stopping");
        streamPlayer.stop();
        mediaSessionController.onStop();
        unpinNotification(true);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStopped());
    }

    @Override
    public void onFadeFinished() {
        if (pauseRequested) {
            pauseRequested = false;
            pause();
        }
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
        String FADE_AND_PAUSE = BuildConfig.APPLICATION_ID + ".playback.fadeandpause";
        String SEEK = BuildConfig.APPLICATION_ID + ".playback.seek";
        String RESET_ALL = BuildConfig.APPLICATION_ID + ".playback.reset"; // used on logout
        String STOP = BuildConfig.APPLICATION_ID + ".playback.stop"; // from the notification
        String PLAY = BuildConfig.APPLICATION_ID + ".playback.play";
    }

    public interface ActionExtras {
        String POSITION = "seek_position";
        String PLAYBACK_ITEM = "playback_item";
        String PRELOAD_ITEM = "preload_item";
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

    private void unpinNotification(boolean remove) {
        stopForeground(remove);
    }

}
