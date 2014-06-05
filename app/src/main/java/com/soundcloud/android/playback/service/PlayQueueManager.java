package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayQueueManager implements Observer<RelatedTracksCollection>, OriginProvider {

    public static final String PLAYQUEUE_CHANGED_ACTION = "com.soundcloud.android.playlistchanged";
    public static final String RELATED_LOAD_STATE_CHANGED_ACTION = "com.soundcloud.android.related.changed";

    private final Context context;

    private final ScModelManager modelManager;
    private final PlayQueueOperations playQueueOperations;
    private final EventBus eventBus;

    private PlayQueue playQueue = PlayQueue.empty();
    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

    private Subscription fetchRelatedSubscription = Subscriptions.empty();
    private Subscription playQueueSubscription = Subscriptions.empty();
    private Observable<RelatedTracksCollection> relatedTracksObservable;

    private PlaybackProgressInfo playbackProgressInfo;

    private boolean gotRelatedTracks;
    private PlaybackServiceOperations.AppendState appendState = PlaybackServiceOperations.AppendState.IDLE;

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
        setNewPlayQueueInternal(playQueue, playSessionSource);
        saveCurrentPosition(0L);
    }

    public long getCurrentTrackId() {
        return playQueue.getCurrentTrackId();
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

    public PlaybackProgressInfo getPlayProgressInfo() {
        return playbackProgressInfo;
    }

    public void setPosition(int position) {
        if (position != playQueue.getPosition()
                && position < playQueue.getItems().size()) {
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

    public boolean autoNextTrack(){
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
    public PlaybackProgressInfo loadPlayQueue() {
        Observable<PlayQueue> playQueueObservable = playQueueOperations.getLastStoredPlayQueue();
        if (playQueueObservable != null) {
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

    public long getPlaylistId() {
        return playSessionSource.getPlaylistId();
    }

    public boolean isPlaylist() {
        return getPlaylistId() != Playable.NOT_SET;
    }

    public boolean isCurrentPlaylist(long playlistId) {
        return getPlaylistId() == playlistId;
    }

    @Override
    public String getScreenTag() {
        return playSessionSource.getOriginScreen();
    }

    public boolean shouldReloadQueue() {
        return playQueue.isEmpty();
    }

    public void fetchRelatedTracks(long trackId) {
        relatedTracksObservable = playQueueOperations.getRelatedTracks(trackId).observeOn(AndroidSchedulers.mainThread());
        loadRelatedTracks();
    }

    public void retryRelatedTracksFetch() {
        loadRelatedTracks();
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
        return playQueue.getViewWithAppendState(appendState);
    }

    private void loadRelatedTracks() {
        setNewRelatedLoadingState(PlaybackServiceOperations.AppendState.LOADING);
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
        setNewRelatedLoadingState(gotRelatedTracks ? PlaybackServiceOperations.AppendState.IDLE : PlaybackServiceOperations.AppendState.EMPTY);
    }

    @Override
    public void onError(Throwable e) {
        setNewRelatedLoadingState(PlaybackServiceOperations.AppendState.ERROR);
    }

    private void setNewRelatedLoadingState(PlaybackServiceOperations.AppendState appendState) {
        this.appendState = appendState;
        final Intent intent = new Intent(RELATED_LOAD_STATE_CHANGED_ACTION)
                .putExtra(PlayQueueView.EXTRA, playQueue.getViewWithAppendState(appendState));
        context.sendBroadcast(intent);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(playQueue.getCurrentTrackId()));
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(PLAYQUEUE_CHANGED_ACTION)
                .putExtra(PlayQueueView.EXTRA, playQueue.getViewWithAppendState(appendState));
        context.sendBroadcast(intent);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(playQueue.getCurrentTrackId()));
    }

    private void stopLoadingOperations() {
        fetchRelatedSubscription.unsubscribe();
        fetchRelatedSubscription = Subscriptions.empty();

        playQueueSubscription.unsubscribe();
    }

}
