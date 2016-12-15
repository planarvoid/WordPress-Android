package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.CastProtocol.TAG;
import static com.soundcloud.android.playback.AudioPlaybackItem.create;
import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioPlaybackItem;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStatePublisher;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
class DefaultCastPlayer implements ProgressReporter.ProgressPuller, RemoteMediaClient.Listener, CastPlayer {

    private final DefaultCastOperations castOperations;
    private final ProgressReporter progressReporter;
    private final PlayQueueManager playQueueManager;
    private final PlayStatePublisher playStatePublisher;
    private final DateProvider dateProvider;
    private final EventBus eventBus;
    private final CastProtocol castProtocol;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriber;

    private Subscription playCurrentSubscription = RxUtils.invalidSubscription();
    @Nullable private RemoteMediaClient remoteMediaClient;

    @Inject
    DefaultCastPlayer(DefaultCastOperations castOperations,
                      ProgressReporter progressReporter,
                      PlayQueueManager playQueueManager,
                      EventBus eventBus,
                      PlayStatePublisher playStatePublisher,
                      CurrentDateProvider dateProvider,
                      CastProtocol castProtocol,
                      PlaySessionStateProvider playSessionStateProvider,
                      Provider<ExpandPlayerSubscriber> expandPlayerSubscriber) {
        this.castOperations = castOperations;
        this.progressReporter = progressReporter;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.playStatePublisher = playStatePublisher;
        this.dateProvider = dateProvider;
        this.castProtocol = castProtocol;
        this.playSessionStateProvider = playSessionStateProvider;
        this.expandPlayerSubscriber = expandPlayerSubscriber;

        progressReporter.setProgressPuller(this);
    }

    public void onDisconnected() {
        if (remoteMediaClient != null) {
            reportStateChange(getStateTransition(PlaybackState.IDLE,
                                                 PlayStateReason.NONE)); // possibly show disconnect error here instead?
            this.remoteMediaClient.removeListener(this);
            this.remoteMediaClient = null;
        }
    }

    public void onConnected(RemoteMediaClient client) {
        Log.d(TAG, "On Connected with client " + client);
        remoteMediaClient = client;
        remoteMediaClient.addListener(this);
    }

    @Override
    public void onStatusUpdated() {
        Log.d(TAG, "On Status updated, status: " + remoteMediaClient.getPlayerState());

        onMediaPlayerStatusUpdatedListener(remoteMediaClient.getPlayerState(),
                                           remoteMediaClient.getIdleReason());
    }

    @Override
    public void onMetadataUpdated() {
        Log.d(TAG, "On metadata updated, status: " + remoteMediaClient.getPlayerState());

        pullRemotePlayQueueAndUpdateLocalState();
    }

    @Override
    public void onQueueStatusUpdated() {
        Log.d(TAG, "On queue updated, status: " + remoteMediaClient.getPlayerState());
    }

    @Override
    public void onPreloadStatusUpdated() {
        // no-op
    }

    @Override
    public void onSendingRemoteMediaRequest() {
        // no-op
    }

    @Override
    public void onAdBreakStatusUpdated() {
        // no-op
    }

    void onMediaPlayerStatusUpdatedListener(int playerState, int idleReason) {
        switch (playerState) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                reportStateChange(getStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_PAUSED:
                // we have to suppress pause events while we should be playing.
                // The receiver sends thes back often, as in when the track first loads, even if autoplay is true
                reportStateChange(getStateTransition(PlaybackState.IDLE, PlayStateReason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_BUFFERING:
                reportStateChange(getStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_IDLE:
                final PlayStateReason translatedIdleReason = getTranslatedIdleReason(idleReason);
                if (translatedIdleReason != null) {
                    reportStateChange(getStateTransition(PlaybackState.IDLE, translatedIdleReason));
                }
                break;

            case MediaStatus.PLAYER_STATE_UNKNOWN:
                Log.e(this, "received an unknown media status"); // not sure when this happens yet
                break;
            default:
                throw new IllegalArgumentException("Unknown Media State code returned " + playerState);
        }
    }

    private void reportStateChange(PlaybackStateTransition stateTransition) {
        final MediaInfo mediaInfo = remoteMediaClient.getMediaInfo();
        final AudioPlaybackItem playbackItem = create(castProtocol.getRemoteCurrentTrackUrn(mediaInfo), 0,
                                                      getDuration(), PlaybackType.AUDIO_DEFAULT);
        playStatePublisher.publish(stateTransition, playbackItem, false);

        final boolean playerPlaying = stateTransition.isPlayerPlaying();
        if (playerPlaying) {
            progressReporter.start();
        } else {
            progressReporter.stop();
        }
    }

    @Override
    public void pullProgress() {
        final MediaInfo mediaInfo = remoteMediaClient.getMediaInfo();
        final PlaybackProgress playbackProgress = new PlaybackProgress(remoteMediaClient.getApproximateStreamPosition(),
                                                                       remoteMediaClient.getStreamDuration(),
                                                                       castProtocol.getRemoteCurrentTrackUrn(mediaInfo));
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS,
                         PlaybackProgressEvent.create(playbackProgress,
                                                      castProtocol.getRemoteCurrentTrackUrn(mediaInfo)));
    }

    private PlaybackStateTransition getStateTransition(PlaybackState state, PlayStateReason reason) {
        final MediaInfo mediaInfo = remoteMediaClient.getMediaInfo();
        return new PlaybackStateTransition(state,
                                           reason,
                                           castProtocol.getRemoteCurrentTrackUrn(mediaInfo),
                                           getProgress(),
                                           getDuration(),
                                           dateProvider);
    }

    @Nullable
    private PlayStateReason getTranslatedIdleReason(int idleReason) {
        switch (idleReason) {
            case MediaStatus.IDLE_REASON_ERROR:
                return PlayStateReason.ERROR_FAILED;
            case MediaStatus.IDLE_REASON_FINISHED:
                return PlayStateReason.PLAYBACK_COMPLETE;
            case MediaStatus.IDLE_REASON_CANCELED:
                return PlayStateReason.NONE;
            default:
                // do not fail fast, we want to ignore non-handled codes
                return null;
        }
    }

    private Func1<LocalPlayQueue, Observable<PlaybackResult>> playNewLocalQueueOnRemote(final Urn initialTrackUrnCandidate,
                                                                                        final PlaySessionSource playSessionSource) {
        return new Func1<LocalPlayQueue, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(LocalPlayQueue localPlayQueue) {
                if (localPlayQueue.isEmpty() || isInitialTrackDifferent(localPlayQueue)) {
                    return Observable.just(PlaybackResult.error(TRACK_UNAVAILABLE_CAST));
                } else {
                    reportStateChange(createStateTransition(localPlayQueue.currentTrackUrn,
                                                            PlaybackState.BUFFERING,
                                                            PlayStateReason.NONE));
                    castOperations.setNewPlayQueue(localPlayQueue, playSessionSource);
                    return Observable.just(PlaybackResult.success());
                }
            }

            private boolean isInitialTrackDifferent(LocalPlayQueue localPlayQueue) {
                return initialTrackUrnCandidate != Urn.NOT_SET &&
                        !initialTrackUrnCandidate.equals(localPlayQueue.currentTrackUrn);
            }
        };
    }

    @Override
    public void pullRemotePlayQueueAndUpdateLocalState() {
        final RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue(remoteMediaClient.getMediaInfo());
        Log.d(TAG, "Loading " + remotePlayQueue.toString());

        if (!remotePlayQueue.isEmpty()) {
            updateLocalPlayQueueAndPlayState(remotePlayQueue);

            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
        }
    }

    private void updateLocalPlayQueueAndPlayState(RemotePlayQueue remotePlayQueue) {
        final int remotePosition = remotePlayQueue.getCurrentPosition();

        if (playQueueManager.hasSameTrackList(remotePlayQueue)) {
            if (isNotInterruptedIdle()) {
                updatePositionAndPlay(remotePosition);
            }
        } else {
            Log.d(TAG, "Does not have the same tracklist, updating locally");
            final PlayQueue playQueue = remotePlayQueue.toPlayQueue(PlaySessionSource.forCast(), Collections.emptyMap());
            playQueueManager.setNewPlayQueue(playQueue, PlaySessionSource.forCast(), remotePosition);
            playCurrent();
        }
    }

    private boolean isNotInterruptedIdle() {
        // when moving manually to the next track receiver will send an idle interrupted event for the previous
        // track first before buffering state of the current track. This causes player to move back and forward, that
        // is why we decided to ignore this particular event
        return !(remoteMediaClient.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE
                && remoteMediaClient.getIdleReason() == MediaStatus.IDLE_REASON_INTERRUPTED);
    }

    private void updatePositionAndPlay(int remotePosition) {
        Log.d(TAG, "Has the same tracklist, setting remotePosition");
        playQueueManager.setPosition(remotePosition, true);
        if (remoteMediaClient.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING) {
            playCurrent();
        }
    }

    public void playCurrent() {
        playCurrent(0L);
    }

    public void playCurrent(long position) {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        final Urn currentTrackUrn = currentPlayQueueItem.getUrn();
        if (isCurrentlyLoadedOnRemotePlayer(currentTrackUrn)) {
            reconnectToExistingSession();
        } else {
            reportStateChange(createStateTransition(currentTrackUrn, PlaybackState.BUFFERING, PlayStateReason.NONE));
            playCurrentSubscription.unsubscribe();
            playCurrentSubscription = castOperations
                    .loadLocalPlayQueue(currentTrackUrn, playQueueManager.getCurrentQueueTrackUrns())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new PlayCurrentLocalQueueOnRemote(currentTrackUrn, position));
        }
    }

    private boolean isCurrentlyLoadedOnRemotePlayer(Urn urn) {
        final Urn currentPlayingUrn = castProtocol.getRemoteCurrentTrackUrn(remoteMediaClient.getMediaInfo());
        return currentPlayingUrn != Urn.NOT_SET && currentPlayingUrn.equals(urn);
    }

    private void reconnectToExistingSession() {
        onMediaPlayerStatusUpdatedListener(remoteMediaClient.getPlayerState(), remoteMediaClient.getIdleReason());
    }

    private class PlayCurrentLocalQueueOnRemote extends DefaultSubscriber<LocalPlayQueue> {
        private final Urn currentTrackUrn;
        private final long position;

        private PlayCurrentLocalQueueOnRemote(Urn currentTrackUrn, long position) {
            this.currentTrackUrn = currentTrackUrn;
            this.position = position;
        }

        @Override
        public void onNext(LocalPlayQueue localPlayQueue) {
            playLocalQueueOnRemote(localPlayQueue, position);
        }

        @Override
        public void onError(Throwable e) {
            reportStateChange(createStateTransition(currentTrackUrn, PlaybackState.IDLE, PlayStateReason.ERROR_FAILED));
        }
    }

    private void playLocalQueueOnRemote(LocalPlayQueue localPlayQueue, long progressPosition) {
        Log.d(TAG, "Play local queue on the remote");
        remoteMediaClient.load(localPlayQueue.mediaInfo, true, progressPosition, localPlayQueue.playQueueTracksJSON);
    }

    public void playLocalPlayQueueOnRemote() {
        Log.d(TAG, "Sending current track and queue to cast receiver");

        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        final PlaybackProgress lastProgressForTrack = currentPlayQueueItem.isEmpty() ? PlaybackProgress.empty() :
                                                      playSessionStateProvider.getLastProgressForItem(currentPlayQueueItem.getUrn());
        reloadCurrentQueue()
                .doOnNext(playbackResult -> playCurrent(lastProgressForTrack.getPosition()))
                .subscribe(expandPlayerSubscriber.get());
    }

    Observable<PlaybackResult> reloadCurrentQueue() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isTrack()) {
            return setNewQueue(
                    castOperations.getCurrentQueueUrnsWithoutAds(),
                    currentPlayQueueItem.getUrn(),
                    playQueueManager.getCurrentPlaySessionSource());
        } else {
            return Observable.just(PlaybackResult.error(TRACK_UNAVAILABLE_CAST));
        }
    }

    public Observable<PlaybackResult> setNewQueue(List<Urn> unfilteredLocalPlayQueueTracks,
                                                  final Urn initialTrackUrnCandidate,
                                                  final PlaySessionSource playSessionSource) {
        return castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(initialTrackUrnCandidate,
                                                                                   unfilteredLocalPlayQueueTracks)
                             .observeOn(AndroidSchedulers.mainThread())
                             .flatMap(playNewLocalQueueOnRemote(initialTrackUrnCandidate, playSessionSource))
                             .doOnError(throwable -> reportStateChange(createStateTransition(initialTrackUrnCandidate, PlaybackState.IDLE, PlayStateReason.ERROR_FAILED)));
    }

    private PlaybackStateTransition createStateTransition(Urn initialTrackUrnCandidate,
                                                          PlaybackState newState, PlayStateReason reason) {
        return new PlaybackStateTransition(newState, reason, initialTrackUrnCandidate, 0, 0, dateProvider);
    }

    public boolean resume() {
        remoteMediaClient.play();
        return true;
    }

    public void pause() {
        remoteMediaClient.pause();
    }

    public void togglePlayback() {
        remoteMediaClient.togglePlayback();
    }

    public long seek(long ms) {
        remoteMediaClient.seek((int) ms);
        progressReporter.stop();
        return ms;
    }

    public long getProgress() {
        return remoteMediaClient.getApproximateStreamPosition();
    }

    private long getDuration() {
        return remoteMediaClient.getStreamDuration();
    }

    public void stop() {
        pause(); // stop has more long-running implications in cast. pause is sufficient
    }

}
