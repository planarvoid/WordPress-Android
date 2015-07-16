package com.soundcloud.android.playback;

import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class DefaultPlaybackStrategy implements PlaybackStrategy {

    private final PlayQueueManager playQueueManager;
    private final ServiceInitiator serviceInitiator;

    public DefaultPlaybackStrategy(PlayQueueManager playQueueManager, ServiceInitiator serviceInitiator) {
        this.playQueueManager = playQueueManager;
        this.serviceInitiator = serviceInitiator;
    }

    @Override
    public void togglePlayback() {
        serviceInitiator.togglePlayback();
    }

    @Override
    public void resume() {
        serviceInitiator.resume();
    }

    @Override
    public void pause() {
        serviceInitiator.pause();
    }

    @Override
    public void playCurrent() {
        serviceInitiator.playCurrent();
    }

    @Override
    public Observable<PlaybackResult> playNewQueue(final PlayQueue playQueue,
                                                   final Urn initialTrackUrn,
                                                   final int initialTrackPosition,
                                                   final boolean loadRelated,
                                                   final PlaySessionSource playSessionSource) {
        return Observable
                .create(new Observable.OnSubscribe<PlaybackResult>() {
                    @Override
                    public void call(Subscriber<? super PlaybackResult> subscriber) {
                        final int updatedPosition = PlaybackUtils.correctStartPositionAndDeduplicateList(playQueue, initialTrackPosition, initialTrackUrn);
                        setAndPlayNewQueue(playQueue, updatedPosition, playSessionSource);
                        subscriber.onNext(PlaybackResult.success());
                        subscriber.onCompleted();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread());
    }

    private void setAndPlayNewQueue(PlayQueue playQueue, int startPosition, PlaySessionSource playSessionSource) {
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource, startPosition);
        playCurrent();
    }

    @Override
    public void seek(long position) {
        serviceInitiator.seek(position);
    }
}
