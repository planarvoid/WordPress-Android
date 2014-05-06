package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.playback.PlaybackOperations;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayQueueManager implements Observer<RelatedTracksCollection> {

    private final Context context;

    private final ScModelManager modelManager;
    private final PlayQueueOperations playQueueOperations;

    private PlayQueue playQueue = PlayQueue.empty();
    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

    private Subscription fetchRelatedSubscription = Subscriptions.empty();
    private Subscription playQueueSubscription = Subscriptions.empty();
    private Observable<RelatedTracksCollection> relatedTracksObservable;

    private boolean gotRelatedTracks;
    private PlaybackOperations.AppendState appendState = PlaybackOperations.AppendState.IDLE;

    @Inject
    public PlayQueueManager(Context context, PlayQueueOperations playQueueOperations, ScModelManager modelManager) {
        this.context = context;
        this.playQueueOperations = playQueueOperations;
        this.modelManager = modelManager;
    }

    public void setNewPlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        setNewPlayQueueInternal(playQueue, playSessionSource);
        saveCurrentPosition(0L);
    }

    private void setNewPlayQueueInternal(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        stopLoadingOperations();

        this.playQueue = checkNotNull(playQueue, "Playqueue to update should not be null");
        this.playQueue.setCurrentTrackToUserTriggered();
        this.playSessionSource = playSessionSource;

        broadcastPlayQueueChanged();
    }

    public void saveCurrentPosition(long currentTrackProgress) {
        if (!playQueue.isEmpty()) {
            playQueueOperations.saveQueue(playQueue, playSessionSource, currentTrackProgress);
        }
    }

    /**
     * @return last stored seek pos of the current track in queue, or -1 if there is no reload
     */
    public PlaybackProgressInfo loadPlayQueue() {

        Observable<PlayQueue> playQueueObservable = playQueueOperations.getLastStoredPlayQueue();
        if (playQueueObservable != null){
            playQueueSubscription = playQueueObservable.subscribe(new Action1<PlayQueue>() {
                @Override
                public void call(PlayQueue playQueue) {
                    setNewPlayQueueInternal(playQueue, playQueueOperations.getLastStoredPlaySessionSource());
                }
            });
            // return so player can have the resume information while load is in progress
            return new PlaybackProgressInfo(playQueueOperations.getLastStoredPlayingTrackId(), playQueueOperations.getLastStoredSeekPosition());
        } else {
            // this is so the player can finish() instead of display waiting to the user
            broadcastPlayQueueChanged();
            return null;
        }
    }

    @Nullable
    public TrackSourceInfo getCurrentTrackSourceInfo() {
        return playQueue.getCurrentTrackSourceInfo(playSessionSource);
    }

    public long getPlaylistId(){
        return playSessionSource.getPlaylistId();
    }

    public String getOriginScreen() {
        return playSessionSource.getOriginScreen();
    }

    public boolean shouldReloadQueue(){
        return playQueue.isEmpty();
    }

    public void fetchRelatedTracks(long trackId){
        relatedTracksObservable = playQueueOperations.getRelatedTracks(trackId);
        loadRelatedTracks();
    }

    public void retryRelatedTracksFetch(){
        loadRelatedTracks();
    }

    public void clearAll(){
        playQueueOperations.clear();
        playQueue = PlayQueue.empty();
        playSessionSource = PlaySessionSource.EMPTY;
    }

    public PlayQueue getCurrentPlayQueue() {
        return playQueue;
    }

    public PlayQueueView getPlayQueueView() {
        return playQueue.getViewWithAppendState(appendState);
    }

    private void loadRelatedTracks() {
        setNewRelatedLoadingState(PlaybackOperations.AppendState.LOADING);
        gotRelatedTracks = false;
        fetchRelatedSubscription = relatedTracksObservable.subscribe(this);
    }

    @Override
    public void onNext(RelatedTracksCollection relatedTracks) {
        for (TrackSummary item : relatedTracks) {
            final Track track = new Track(item);
            modelManager.cache(track);
            playQueue.addTrack(track.getId(), PlaySessionSource.DiscoverySource.RECOMMENDER.value(),
                    relatedTracks.getSourceVersion());
        }
        gotRelatedTracks = true;
    }

    @Override
    public void onCompleted() {
        // TODO, save new tracks to database
        setNewRelatedLoadingState(gotRelatedTracks ? PlaybackOperations.AppendState.IDLE : PlaybackOperations.AppendState.EMPTY);
    }

    @Override
    public void onError(Throwable e) {
        setNewRelatedLoadingState(PlaybackOperations.AppendState.ERROR);
    }

    private void setNewRelatedLoadingState(PlaybackOperations.AppendState appendState) {
        this.appendState = appendState;
        final Intent intent = new Intent(PlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED)
                .putExtra(PlayQueueView.EXTRA, playQueue.getViewWithAppendState(appendState));
        context.sendBroadcast(intent);
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED)
                .putExtra(PlayQueueView.EXTRA, playQueue.getViewWithAppendState(appendState));
        context.sendBroadcast(intent);
    }

    private void stopLoadingOperations() {
        fetchRelatedSubscription.unsubscribe();
        fetchRelatedSubscription = Subscriptions.empty();

        playQueueSubscription.unsubscribe();
    }
}
