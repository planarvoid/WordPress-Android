package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.soundcloud.android.model.Playable;
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

public class PlayQueueManager implements Observer<RelatedTracksCollection> {

    private final Context mContext;

    private final ScModelManager mModelManager;
    private final PlayQueueOperations mPlayQueueOperations;

    private PlayQueue mPlayQueue = PlayQueue.empty();
    private PlaySessionSource mPlaySessionSource = PlaySessionSource.EMPTY;

    private Subscription mFetchRelatedSubscription = Subscriptions.empty();
    private Subscription mPlayQueueSubscription = Subscriptions.empty();
    private Observable<RelatedTracksCollection> mRelatedTracksObservable;

    private boolean mGotRelatedTracks;
    private PlaybackOperations.AppendState mAppendState = PlaybackOperations.AppendState.IDLE;

    @Inject
    public PlayQueueManager(Context context, PlayQueueOperations playQueueOperations, ScModelManager modelManager) {
        mContext = context;
        mPlayQueueOperations = playQueueOperations;
        mModelManager = modelManager;
    }

    public void setNewPlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        setNewPlayQueueInternal(playQueue, playSessionSource);
        saveCurrentPosition(0L);
    }

    private void setNewPlayQueueInternal(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        stopLoadingOperations();

        mPlayQueue = checkNotNull(playQueue, "Playqueue to update should not be null");
        mPlayQueue.setCurrentTrackToUserTriggered();
        mPlaySessionSource = playSessionSource;

        broadcastPlayQueueChanged();
    }

    public void saveCurrentPosition(long currentTrackProgress) {
        if (!mPlayQueue.isEmpty()) {
            mPlayQueueOperations.saveQueue(mPlayQueue, mPlaySessionSource, currentTrackProgress);
        }
    }

    /**
     * @return last stored seek pos of the current track in queue, or -1 if there is no reload
     */
    public PlaybackProgressInfo loadPlayQueue() {

        Observable<PlayQueue> playQueueObservable = mPlayQueueOperations.getLastStoredPlayQueue();
        if (playQueueObservable != null){
            mPlayQueueSubscription = playQueueObservable.subscribe(new Action1<PlayQueue>() {
                @Override
                public void call(PlayQueue playQueue) {
                    setNewPlayQueueInternal(playQueue, mPlayQueueOperations.getLastStoredPlaySessionSource());
                }
            });
            // return so player can have the resume information while load is in progress
            return new PlaybackProgressInfo(mPlayQueueOperations.getLastStoredPlayingTrackId(), mPlayQueueOperations.getLastStoredSeekPosition());
        } else {
            // this is so the player can finish() instead of display waiting to the user
            broadcastPlayQueueChanged();
            return null;
        }
    }

    @Nullable
    public TrackSourceInfo getCurrentTrackSourceInfo() {
        return mPlayQueue.getCurrentTrackSourceInfo(mPlaySessionSource);
    }

    public long getPlaylistId(){
        return mPlaySessionSource.getPlaylistId();
    }

    public String getOriginScreen() {
        return mPlaySessionSource.getOriginScreen();
    }

    private boolean isPlayingPlaylist() {
        return getPlaylistId() > Playable.NOT_SET;
    }

    public boolean shouldReloadQueue(){
        return mPlayQueue.isEmpty();
    }

    public void fetchRelatedTracks(long trackId){
        mRelatedTracksObservable = mPlayQueueOperations.getRelatedTracks(trackId);
        loadRelatedTracks();
    }

    public void retryRelatedTracksFetch(){
        loadRelatedTracks();
    }

    public void clearAll(){
        mPlayQueueOperations.clear();
        mPlayQueue = PlayQueue.empty();
    }

    public PlayQueue getCurrentPlayQueue() {
        return mPlayQueue;
    }

    public PlayQueueView getPlayQueueView() {
        return mPlayQueue.getViewWithAppendState(mAppendState);
    }

    private void loadRelatedTracks() {
        setNewRelatedLoadingState(PlaybackOperations.AppendState.LOADING);
        mGotRelatedTracks = false;
        mFetchRelatedSubscription = mRelatedTracksObservable.subscribe(this);
    }

    @Override
    public void onNext(RelatedTracksCollection relatedTracks) {
        for (TrackSummary item : relatedTracks) {
            final Track track = new Track(item);
            mModelManager.cache(track);
            mPlayQueue.addTrack(track.getId(), PlaySessionSource.DiscoverySource.RECOMMENDER.value(),
                    relatedTracks.getSourceVersion());
        }
        mGotRelatedTracks = true;
    }

    @Override
    public void onCompleted() {
        // TODO, save new tracks to database
        setNewRelatedLoadingState(mGotRelatedTracks ? PlaybackOperations.AppendState.IDLE : PlaybackOperations.AppendState.EMPTY);
    }

    @Override
    public void onError(Throwable e) {
        setNewRelatedLoadingState(PlaybackOperations.AppendState.ERROR);
    }

    private void setNewRelatedLoadingState(PlaybackOperations.AppendState appendState) {
        mAppendState = appendState;
        final Intent intent = new Intent(PlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED)
                .putExtra(PlayQueueView.EXTRA, mPlayQueue.getViewWithAppendState(appendState));
        mContext.sendBroadcast(intent);
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED)
                .putExtra(PlayQueueView.EXTRA, mPlayQueue.getViewWithAppendState(mAppendState));
        mContext.sendBroadcast(intent);
    }

    private void stopLoadingOperations() {
        mFetchRelatedSubscription.unsubscribe();
        mFetchRelatedSubscription = Subscriptions.empty();

        mPlayQueueSubscription.unsubscribe();
    }
}
