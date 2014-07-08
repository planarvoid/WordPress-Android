package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.RecommendedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayQueueManager implements Observer<RecommendedTracksCollection>, OriginProvider {

    public static final String PLAYQUEUE_CHANGED_ACTION = "com.soundcloud.android.playlistchanged";
    public static final String RELATED_LOAD_STATE_CHANGED_ACTION = "com.soundcloud.android.related.changed";

    private final Context context;

    private final ScModelManager modelManager;
    private final PlayQueueOperations playQueueOperations;
    private final EventBus eventBus;

    private PlayQueue playQueue = PlayQueue.empty();
    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

    private Subscription fetchRecommendedSubscription = Subscriptions.empty();
    private Subscription playQueueSubscription = Subscriptions.empty();
    private Observable<RecommendedTracksCollection> recommendedTracksObservable;

    private PlaybackProgressInfo playbackProgressInfo;

    private boolean gotRecommendedTracks;
    private FetchRecommendedState fetchState = FetchRecommendedState.IDLE;

    public enum FetchRecommendedState {
        IDLE, LOADING, ERROR, EMPTY;
    }

    @Inject
    public PlayQueueManager(Context context,
                            PlayQueueOperations playQueueOperations,
                            EventBus eventBus,
                            ScModelManager modelManager) {
        this.context = context;
        this.playQueueOperations = playQueueOperations;
        this.eventBus = eventBus;
        this.modelManager = modelManager;
    }

    public void setNewPlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        Preconditions.checkState(Looper.getMainLooper().getThread() == Thread.currentThread(),
                "Play queues must be set from the main thread only");

        setNewPlayQueueInternal(playQueue, playSessionSource);
        saveCurrentPosition(0L);
    }

    public long getCurrentTrackId() {
        return playQueue.getCurrentTrackId();
    }

    public boolean isCurrentTrack(TrackUrn trackUrn) {
        return playQueue.getCurrentTrackId() == trackUrn.numericId;
    }

    public int getCurrentPosition() {
        return playQueue.getPosition();
    }

    public boolean isCurrentPosition(int position) {
        return position == getCurrentPosition();
    }

    public boolean isQueueEmpty() {
        return getQueueSize() == 0;
    }

    public int getQueueSize() {
        return playQueue.getItems().size();
    }

    public long getIdAtPosition(int position) {
        return playQueue.getItems().get(position).getTrackId();
    }

    public TrackUrn getUrnAtPosition(int position) {
        return TrackUrn.forTrack(getIdAtPosition(position));
    }

    public PlaybackProgressInfo getPlayProgressInfo() {
        return playbackProgressInfo;
    }

    public void setPosition(int position) {
        if (position != playQueue.getPosition() && position < playQueue.getItems().size()) {
            playQueue.setPosition(position);
            eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(playQueue.getCurrentTrackId()));
        }
    }

    public void previousTrack() {
        if (playQueue.hasPreviousTrack()) {
            playQueue.moveToPrevious();
            eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(playQueue.getCurrentTrackId()));
        }
    }

    public boolean autoNextTrack() {
        return nextTrackInternal(false);
    }

    public boolean nextTrack() {
        return nextTrackInternal(true);
    }

    public boolean hasNextTrack() {
        return playQueue.hasNextTrack();
    }

    private boolean nextTrackInternal(boolean manual) {
        if (playQueue.hasNextTrack()) {
            playQueue.moveToNext(manual);
            eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(playQueue.getCurrentTrackId()));
            return true;
        } else {
            return false;
        }

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
            playbackProgressInfo = new PlaybackProgressInfo(playQueue.getCurrentTrackId(), currentTrackProgress);
        }
    }

    /**
     * @return last stored seek pos of the current track in queue, or -1 if there is no reload
     */
    public void loadPlayQueue() {
        Observable<PlayQueue> playQueueObservable = playQueueOperations.getLastStoredPlayQueue();
        if (playQueueObservable != null) {
            playQueueSubscription = playQueueObservable.subscribe(new DefaultSubscriber<PlayQueue>() {
                @Override
                public void onNext(PlayQueue savedQueue) {
                    setNewPlayQueueInternal(savedQueue, playQueueOperations.getLastStoredPlaySessionSource());
                }
            });
            // return so player can have the resume information while load is in progress
            playbackProgressInfo = new PlaybackProgressInfo(playQueueOperations.getLastStoredPlayingTrackId(), playQueueOperations.getLastStoredSeekPosition());
        } else {
            // this is so the player can finish() instead of display waiting to the user
            broadcastPlayQueueChanged();
        }
    }

    @Nullable
    public TrackSourceInfo getCurrentTrackSourceInfo() {
        return playQueue.getCurrentTrackSourceInfo(playSessionSource);
    }

    public long getPlaylistId() {
        return playSessionSource.getPlaylistId();
    }

    public boolean isPlaylist() {
        return getPlaylistId() != Playable.NOT_SET;
    }

    public boolean isCurrentPlaylist(long playlistId) {
        return getPlaylistId() == playlistId;
    }

    public boolean isAudioAdAtPosition(int position) {
        return playQueue.isAudioAd(position);
    }

    @Override
    public String getScreenTag() {
        return playSessionSource.getOriginScreen();
    }

    public boolean shouldReloadQueue() {
        return playQueue.isEmpty();
    }

    public void fetchRelatedTracks(long trackId) {
        recommendedTracksObservable = playQueueOperations.getRelatedTracks(trackId).observeOn(AndroidSchedulers.mainThread());
        loadRecommendedTracks();
    }

    public void retryRelatedTracksFetch() {
        loadRecommendedTracks();
    }

    public void clearAll() {
        playQueueOperations.clear();
        playQueue = PlayQueue.empty();
        playSessionSource = PlaySessionSource.EMPTY;
    }

    public PlayQueue getCurrentPlayQueue() {
        return playQueue;
    }

    public PlayQueueView getPlayQueueView() {
        return playQueue.getViewWithAppendState(fetchState);
    }

    private void loadRecommendedTracks() {
        setNewRelatedLoadingState(FetchRecommendedState.LOADING);
        gotRecommendedTracks = false;
        fetchRecommendedSubscription = recommendedTracksObservable.subscribe(this);
    }

    @Override
    public void onNext(RecommendedTracksCollection relatedTracks) {
        for (TrackSummary item : relatedTracks) {
            final Track track = new Track(item);
            modelManager.cache(track);
            playQueue.addTrack(track.getId(), PlaySessionSource.DiscoverySource.RECOMMENDER.value(),
                    relatedTracks.getSourceVersion());
        }
        gotRecommendedTracks = true;
    }

    @Override
    public void onCompleted() {
        // TODO, save new tracks to database
        setNewRelatedLoadingState(gotRecommendedTracks ? FetchRecommendedState.IDLE : FetchRecommendedState.EMPTY);
    }

    @Override
    public void onError(Throwable e) {
        setNewRelatedLoadingState(FetchRecommendedState.ERROR);
    }

    private void setNewRelatedLoadingState(FetchRecommendedState fetchState) {
        this.fetchState = fetchState;
        final Intent intent = new Intent(RELATED_LOAD_STATE_CHANGED_ACTION)
                .putExtra(PlayQueueView.EXTRA, playQueue.getViewWithAppendState(fetchState));
        context.sendBroadcast(intent);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(playQueue.getCurrentTrackId()));
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(PLAYQUEUE_CHANGED_ACTION)
                .putExtra(PlayQueueView.EXTRA, playQueue.getViewWithAppendState(fetchState));
        context.sendBroadcast(intent);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(playQueue.getCurrentTrackId()));
    }

    private void stopLoadingOperations() {
        fetchRecommendedSubscription.unsubscribe();
        fetchRecommendedSubscription = Subscriptions.empty();

        playQueueSubscription.unsubscribe();
    }

}
