package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;

public class DefaultPlaybackStrategy implements PlaybackStrategy {

    private final Context context;
    private final PlayQueueManager playQueueManager;

    public DefaultPlaybackStrategy(Context context,
                                   PlayQueueManager playQueueManager) {
        this.context = context;
        this.playQueueManager = playQueueManager;
    }

    @Override
    public void togglePlayback() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
    }

    @Override
    public void resume() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.PLAY_ACTION));
    }

    @Override
    public void pause() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.PAUSE_ACTION));
    }

    @Override
    public void playCurrent() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.PLAY_CURRENT));
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
                        fetchRelatedTracks(loadRelated);
                        subscriber.onNext(PlaybackResult.success());
                        subscriber.onCompleted();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread());
    }

    private void setAndPlayNewQueue(PlayQueue playQueue, int startPosition, PlaySessionSource playSessionSource) {
        playQueueManager.setNewPlayQueue(playQueue, startPosition, playSessionSource);
        playCurrent();
    }

    private void fetchRelatedTracks(boolean loadRelated) {
        if (loadRelated) {
            playQueueManager.fetchTracksRelatedToCurrentTrack();
        }
    }

    @Override
    public void seek(long position) {
        Intent intent = createExplicitServiceIntent(PlaybackService.Actions.SEEK);
        intent.putExtra(PlaybackService.ActionsExtras.SEEK_POSITION, position);
        context.startService(intent);
    }

    private Intent createExplicitServiceIntent(String action) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        return intent;
    }
}
