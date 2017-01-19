package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;

public class ArtworkPresenter {

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private CompositeSubscription subscriptions = new CompositeSubscription();

    private Optional<ArtworkView> artworkViewContract = Optional.absent();
    private Optional<Urn> lastItem = Optional.absent();

    @Inject
    public ArtworkPresenter(EventBus eventBus, TrackRepository trackRepository) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
    }

    void attachView(ArtworkView artworkView) {
        this.artworkViewContract = Optional.fromNullable(artworkView);
        setArtworkStreams();
    }

    void detachView() {
        artworkViewContract = Optional.absent();
        subscriptions.unsubscribe();
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
                                  .filter(event -> artworkViewContract.isPresent())
                                  .subscribe(new ProgressSubscriber()));
        subscriptions.add(eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .filter(PlayStateEvent::isPlayerPlaying)
                                  .filter(event -> artworkViewContract.isPresent())
                                  .subscribe(new PlaybackStateSubscriber()));
        subscriptions.add(eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                  .filter(event -> artworkViewContract.isPresent())
                                  .map(CurrentPlayQueueItemEvent::getCurrentPlayQueueItem)
                                  .filter(PlayQueueItem::isTrack)
                                  .flatMap(playQueueItem -> trackRepository.track(playQueueItem.getUrn()))
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(new ImageSetterSubscriber()));
    }

    private boolean sameAsLast(Urn urn) {
        return lastItem.isPresent() && urn.equals(lastItem.get());
    }

    private class PlaybackStateSubscriber extends DefaultSubscriber<PlayStateEvent> {

        @Override
        public void onNext(PlayStateEvent stateEvent) {
            if (sameAsLast(stateEvent.getPlayingItemUrn())) {
                artworkViewContract.get().startProgressAnimation(stateEvent.getProgress(),
                                                                 stateEvent.getProgress().getDuration());
            } else {
                artworkViewContract.get().cancelProgressAnimation();
            }
        }
    }

    private class ProgressSubscriber extends DefaultSubscriber<PlaybackProgress> {

        @Override
        public void onNext(PlaybackProgress progress) {
            if (sameAsLast(progress.getUrn())) {
                artworkViewContract.get().setPlaybackProgress(progress, progress.getDuration());
            }
        }
    }

    private class ImageSetterSubscriber extends DefaultSubscriber<Track> {

        @Override
        public void onNext(Track track) {
            artworkViewContract.get().setImage(SimpleImageResource.create(track.urn(), track.imageUrlTemplate()));
            lastItem = Optional.of(track.urn());

        }
    }


}
