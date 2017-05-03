package com.soundcloud.android.playback.playqueue;


import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.rx.observers.LambdaSubscriber;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;

public class ArtworkPresenter {

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final PlayQueueManager playQueueManager;
    private CompositeSubscription subscriptions = new CompositeSubscription();

    private Optional<ArtworkView> artworkViewContract = Optional.absent();
    private Urn lastUrn = Urn.NOT_SET;

    @Inject
    public ArtworkPresenter(EventBus eventBus, TrackRepository trackRepository, PlayQueueManager playQueueManager) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.playQueueManager = playQueueManager;
    }

    void attachView(ArtworkView artworkView) {
        this.artworkViewContract = Optional.fromNullable(artworkView);
        setArtworkStreams();
    }

    void detachView() {
        artworkViewContract = Optional.absent();
        subscriptions.clear();
    }

    public void artworkSizeChanged(int width, int imageViewWidth) {
        if (width > 0 && imageViewWidth > 0) {
            if (artworkViewContract.isPresent()) {
                artworkViewContract.get().setProgressControllerValues(0, Math.min(0, -(imageViewWidth - width)));
            }
        }
    }

    private void setArtworkStreams() {
        subscriptions.add(eventBus.queue(EventQueue.PLAYBACK_PROGRESS)
                                  .map(PlaybackProgressEvent::getPlaybackProgress)
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .filter(event -> sameAsLast(event.getUrn()))
                                  .filter(event -> artworkViewContract.isPresent())
                                  .subscribe(LambdaSubscriber.onNext(event -> artworkViewContract.get().setPlaybackProgress(event, event.getDuration()))));
        subscriptions.add(eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .filter(event -> artworkViewContract.isPresent())
                                  .subscribe(new PlaybackStateSubscriber()));
        subscriptions.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                  .map(CurrentPlayQueueItemEvent::getCurrentPlayQueueItem)
                                  .startWith(Observable.just(playQueueManager.getCurrentPlayQueueItem()))
                                  .filter(PlayQueueItem::isTrack)
                                  .flatMap(playQueueItem -> RxJava.toV1Observable(trackRepository.track(playQueueItem.getUrn())))
                                  .map(track -> SimpleImageResource.create(track.urn(), track.imageUrlTemplate()))
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .filter(event -> artworkViewContract.isPresent())
                                  .subscribe(LambdaSubscriber.onNext(image -> {
                                      lastUrn = image.getUrn();
                                      artworkViewContract.get().setImage(image);
                                      artworkViewContract.get().resetProgress();
                                  })));
    }

    private boolean sameAsLast(Urn urn) {
        return urn.equals(lastUrn);
    }

    private class PlaybackStateSubscriber extends DefaultSubscriber<PlayStateEvent> {

        @Override
        public void onNext(PlayStateEvent stateEvent) {

            if (sameAsLast(stateEvent.getPlayingItemUrn())) {
                if (stateEvent.isPaused()) {
                    artworkViewContract.get().cancelProgressAnimation();
                } else {
                    artworkViewContract.get().startProgressAnimation(stateEvent.getProgress(),
                                                                     stateEvent.getProgress().getDuration());
                }
            } else {
                artworkViewContract.get().cancelProgressAnimation();
            }
        }
    }

}
