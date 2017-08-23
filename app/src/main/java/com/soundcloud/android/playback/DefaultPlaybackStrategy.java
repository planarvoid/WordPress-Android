package com.soundcloud.android.playback;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.functions.Action;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class DefaultPlaybackStrategy implements PlaybackStrategy {

    private final PlayQueueManager playQueueManager;
    private final PlaybackServiceController serviceController;
    private final TrackItemRepository trackItemRepository;
    private final OfflinePlaybackOperations offlinePlaybackOperations;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final EventBusV2 eventBus;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final FeedbackController feedbackController;

    public DefaultPlaybackStrategy(PlayQueueManager playQueueManager, PlaybackServiceController serviceController,
                                   TrackItemRepository trackItemRepository, OfflinePlaybackOperations offlinePlaybackOperations,
                                   PlaySessionStateProvider playSessionStateProvider, EventBusV2 eventBus, OfflineSettingsStorage offlineSettingsStorage,
                                   FeedbackController feedbackController) {
        this.playQueueManager = playQueueManager;
        this.serviceController = serviceController;
        this.trackItemRepository = trackItemRepository;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.eventBus = eventBus;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.feedbackController = feedbackController;
    }

    @Override
    public void togglePlayback() {
        eventBus.queue(EventQueue.PLAYER_LIFE_CYCLE).firstElement()
                .flatMapCompletable(playerLifeCycleEvent -> completeIfServiceRunning(playerLifeCycleEvent, serviceController::togglePlayback))
                .subscribe(new DefaultDisposableCompletableObserver());

    }

    @Override
    public void resume() {
        eventBus.queue(EventQueue.PLAYER_LIFE_CYCLE).firstElement()
                .flatMapCompletable(playerLifeCycleEvent -> completeIfServiceRunning(playerLifeCycleEvent, serviceController::resume))
                .subscribe(new DefaultDisposableCompletableObserver());
    }

    private Completable completeIfServiceRunning(PlayerLifeCycleEvent playerLifeCycleEvent, Action action) {
        if (playerLifeCycleEvent.isServiceRunning()) {
            action.run();
            return Completable.complete();
        } else {
            return playCurrent();
        }
    }

    @Override
    public void pause() {
        serviceController.pause();
    }

    @Override
    public Completable playCurrent() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isTrack()) {
            return trackItemRepository.track(currentPlayQueueItem.getUrn())
                                      .switchIfEmpty(Maybe.error(new MissingTrackException(currentPlayQueueItem.getUrn())))
                                      .flatMapCompletable(trackItem -> {
                                                              final Urn trackUrn = trackItem.getUrn();
                                                              Track track = trackItem.track();
                                                              if (track.blocked()) {
                                                                  return Completable.error(new BlockedTrackException(trackUrn));
                                                              } else {
                                                                  if (offlinePlaybackOperations.shouldPlayOffline(trackItem)) {
                                                                      handleOfflineTrackPlayback(track, trackUrn);
                                                                  } else if (track.snipped()) {
                                                                      serviceController.play(AudioPlaybackItem.forSnippet(track, getPosition(trackUrn)));
                                                                  } else {
                                                                      serviceController.play(AudioPlaybackItem.create(track, getPosition(trackUrn)));
                                                                  }
                                                                  return Completable.complete();
                                                              }
                                                          }
                                      );
        } else if (currentPlayQueueItem.isAd()) {
            return playCurrentAd(currentPlayQueueItem);
        } else {
            return Completable.complete();
        }
    }

    private void handleOfflineTrackPlayback(Track track, Urn trackUrn) {
        if (offlineSettingsStorage.isOfflineContentAccessible()) {
            serviceController.play(AudioPlaybackItem.forOffline(track, getPosition(trackUrn)));
        } else {
            feedbackController.showFeedback(Feedback.create(R.string.sd_card_cannot_be_found));
            serviceController.play(AudioPlaybackItem.create(track, getPosition(trackUrn)));
        }
    }

    private Completable playCurrentAd(PlayQueueItem currentPlayQueueItem) {
        final AdData adData = currentPlayQueueItem.getAdData().get();
        final long position = getPosition(adData.adUrn());
        final PlaybackItem playbackItem = currentPlayQueueItem.isVideoAd()
                                          ? VideoAdPlaybackItem.create((VideoAd) adData, position)
                                          : AudioAdPlaybackItem.create((AudioAd) adData);

        serviceController.play(playbackItem);
        return Completable.complete();
    }

    private long getPosition(Urn urn) {
        return playSessionStateProvider.getLastProgressForItem(urn).getPosition();
    }

    @Override
    public Single<PlaybackResult> setNewQueue(final PlayQueue playQueue,
                                              final Urn initialTrackUrn,
                                              final int initialTrackPosition,
                                              final PlaySessionSource playSessionSource) {
        return Single.defer(() -> {
            int updatedPosition = PlaybackUtils.correctStartPosition(playQueue,
                                                                     initialTrackPosition,
                                                                     initialTrackUrn,
                                                                     playSessionSource);
            playQueueManager.setNewPlayQueue(playQueue, playSessionSource, updatedPosition);

            return Single.just(PlaybackResult.success());
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void fadeAndPause() {
        serviceController.fadeAndPause();
    }

    @Override
    public void seek(long position) {
        serviceController.seek(position);
    }
}
