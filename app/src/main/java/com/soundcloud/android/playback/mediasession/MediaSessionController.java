package com.soundcloud.android.playback.mediasession;

import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.external.PlaybackActionController;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

@AutoFactory(allowSubclasses = true)
public class MediaSessionController {

    private static final String TAG = "MediaSessionCtrl";
    private static final float PLAYBACK_SPEED = 1f;
    private static final long AVAILABLE_ACTIONS =
            ACTION_PLAY_PAUSE | ACTION_PAUSE | ACTION_PLAY | ACTION_STOP | ACTION_SKIP_TO_NEXT | ACTION_SKIP_TO_PREVIOUS;

    private final AudioFocusListener audioFocusListener;
    private final Context context;
    private final Listener listener;
    private final MediaSessionWrapper mediaSessionWrapper;
    private final PlayQueueManager playQueueManager;
    private final MetadataOperations metadataOperations;
    private final AdsOperations adsOperations;
    private final MediaSessionCompat mediaSession;
    private final AudioManager audioManager;
    private Subscription subscription = RxUtils.invalidSubscription();

    private int playbackState;
    private long playbackPosition;

    public MediaSessionController(Context context,
                                  Listener listener,
                                  @Provided MediaSessionWrapper mediaSessionWrapper,
                                  @Provided PlaybackActionController playbackActionController,
                                  @Provided MetadataOperations metadataOperations,
                                  @Provided PlayQueueManager playQueueManager,
                                  @Provided AdsOperations adsOperations) {
        this.context = context;
        this.listener = listener;
        this.mediaSessionWrapper = mediaSessionWrapper;
        this.playQueueManager = playQueueManager;
        this.metadataOperations = metadataOperations;
        this.adsOperations = adsOperations;

        audioFocusListener = new AudioFocusListener(listener);
        audioManager = mediaSessionWrapper.getAudioManager(context);
        mediaSession = mediaSessionWrapper.getMediaSession(context, TAG);
        mediaSession.setCallback(new MediaSessionListener(this,
                                                          playbackActionController,
                                                          context.getApplicationContext()));
        mediaSession.setFlags(FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS);
        updatePlaybackState();
    }

    public void onStartCommand(Intent intent) {
        mediaSessionWrapper.handleIntent(mediaSession, intent);
    }

    public boolean onPlay() {
        boolean audioFocusAcquired = requestAudioFocus();

        if (audioFocusAcquired) {
            showNotification();
        }
        return audioFocusAcquired;
    }

    public boolean onPlay(PlaybackItem playbackItem) {
        boolean audioFocusAcquired = requestAudioFocus();

        if (audioFocusAcquired) {
            updateMetadata(playbackItem.getUrn(), AdUtils.isAd(playbackItem));
        }
        return audioFocusAcquired;
    }

    public void onPause() {
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
        showNotification();
    }

    public void onStop() {
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
        audioManager.abandonAudioFocus(audioFocusListener);
        mediaSession.setActive(false);
    }

    public void onSkip() {
        final PlayQueueItem queueItem = playQueueManager.getCurrentPlayQueueItem();

        if (!queueItem.isEmpty()) {
            updateMetadata(queueItem.getUrn(), queueItem.isAd());
        }
    }

    private void updateMetadata(Urn urn, boolean isAd) {
        subscription.unsubscribe();
        subscription = metadataOperations
                .metadata(urn, isAd, getMetadata())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new MetadataSubscriber());
    }


    public void onPreload(Urn urn) {
        metadataOperations.preload(urn);
    }

    public void onBuffering(long position) {
        this.playbackPosition = position;
        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING);
    }

    public void onPlaying(long position) {
        this.playbackPosition = position;
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
    }

    public void onSeek(long playbackPosition) {
        this.playbackPosition = playbackPosition;
        updatePlaybackState();
    }

    public void onProgress(long playbackPosition) {
        this.playbackPosition = playbackPosition;
        updatePlaybackState();
    }

    public void onDestroy() {
        mediaSession.release();
    }

    private Optional<MediaMetadataCompat> getMetadata() {
        MediaControllerCompat controller = mediaSession.getController();

        if (controller != null) {
            return Optional.fromNullable(controller.getMetadata());
        }
        return Optional.absent();
    }

    boolean isPlayingVideoAd() {
        return adsOperations.isCurrentItemVideoAd();
    }

    private void updatePlaybackState(int playbackState) {
        this.playbackState = playbackState;
        updatePlaybackState();
    }

    private void updatePlaybackState() {
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(AVAILABLE_ACTIONS)
                .setState(playbackState, playbackPosition, PLAYBACK_SPEED)
                .build();

        mediaSession.setPlaybackState(state);
    }

    private boolean requestAudioFocus() {
        int status = audioManager.requestAudioFocus(audioFocusListener,
                                                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        boolean focusAcquired = status == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;

        if (focusAcquired) {
            mediaSession.setActive(true);
            updatePlaybackState(STATE_PLAYING);
            listener.onFocusGain();
        }

        return focusAcquired;
    }

    private void showNotification() {
        Optional<NotificationCompat.Builder> builderOpt =
                MediaNotificationHelper.from(context, mediaSession, isPlaying());

        if (builderOpt.isPresent()) {
            listener.showNotification(builderOpt.get().build());
        }
    }

    private boolean isPlaying() {
        return playbackState == STATE_PLAYING || playbackState == STATE_BUFFERING;
    }

    public interface Listener {
        void showNotification(Notification notification);

        void onFocusGain();

        void onFocusLoss(boolean isTransient, boolean canDuck);
    }

    class MetadataSubscriber extends DefaultSubscriber<MediaMetadataCompat> {
        @Override
        public void onNext(MediaMetadataCompat metadata) {
            mediaSession.setMetadata(metadata);
            showNotification();
        }
    }

}
