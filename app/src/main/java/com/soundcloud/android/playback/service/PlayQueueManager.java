package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayQueueManager implements Observer<RecommendedTracksCollection>, OriginProvider {

    public static final String PLAYQUEUE_CHANGED_ACTION = "com.soundcloud.android.playlistchanged";
    public static final String RELATED_LOAD_STATE_CHANGED_ACTION = "com.soundcloud.android.related.changed";
    private static final String UI_ASSERTION_MESSAGE = "Play queues must be set from the main thread only.";

    private final Context context;

    private final ScModelManager modelManager;
    private final PlayQueueOperations playQueueOperations;
    private final EventBus eventBus;

    private int currentPosition;
    private int adTrackPosition = Consts.NOT_SET;
    private PropertySet adMetaData;
    private boolean currentTrackIsUserTriggered;

    private PlayQueue playQueue = PlayQueue.empty();
    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

    private Subscription fetchRecommendedSubscription = Subscriptions.empty();
    private Subscription playQueueSubscription = Subscriptions.empty();
    private Observable<RecommendedTracksCollection> recommendedTracksObservable;

    private PlaybackProgressInfo playbackProgressInfo;
    private boolean gotRecommendedTracks;
    private FetchRecommendedState fetchState = FetchRecommendedState.IDLE;

    public enum FetchRecommendedState {
        IDLE, LOADING, ERROR, EMPTY
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
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        setNewPlayQueue(playQueue, 0, playSessionSource);
    }

    public void setNewPlayQueue(PlayQueue playQueue, int position, PlaySessionSource playSessionSource) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);

        if (this.playQueue.equals(playQueue) && this.playSessionSource.equals(playSessionSource)) {
            this.currentPosition = position;
            eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(getCurrentTrackUrn()));
        } else {
            currentPosition = position;
            setNewPlayQueueInternal(playQueue, playSessionSource);
        }
        saveQueue();
        saveCurrentProgress(0L);

        fireAndForget(playQueueOperations.fetchAndStorePolicies(playQueue.getTrackUrns()));
    }

    @Deprecated
    public PlayQueueView getViewWithAppendState(FetchRecommendedState fetchState) {
        return new PlayQueueView(playQueue.getTrackIds(), currentPosition, fetchState);
    }

    @Deprecated // use URNs instead
    public long getCurrentTrackId() {
        return playQueue.getTrackId(currentPosition);
    }

    public TrackUrn getCurrentTrackUrn() {
        return playQueue.getUrn(currentPosition);
    }

    public boolean isCurrentTrack(@NotNull Urn trackUrn) {
        return trackUrn.equals(getCurrentTrackUrn());
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    private int getNextPosition() {
        return getCurrentPosition() + 1;
    }

    public boolean isCurrentPosition(int position) {
        return position == getCurrentPosition();
    }

    public boolean isQueueEmpty() {
        return getQueueSize() == 0;
    }

    public int getQueueSize() {
        return playQueue.size();
    }

    public TrackUrn getUrnAtPosition(int position) {
        return playQueue.getUrn(position);
    }

    public int getPositionForUrn(final TrackUrn trackUrn) {
        return Iterables.indexOf(playQueue, new Predicate<PlayQueueItem>() {
            @Override
            public boolean apply(PlayQueueItem input) {
                return input.getTrackUrn().equals(trackUrn);
            }
        });
    }

    public PlaybackProgressInfo getPlayProgressInfo() {
        return playbackProgressInfo;
    }

    public void setPosition(int position) {
        if (position != currentPosition && position < playQueue.size()) {
            this.currentPosition = position;
            publishPositionUpdate();
        }
    }

    public void moveToPreviousTrack() {
        if (playQueue.hasPreviousTrack(currentPosition)) {
            currentPosition--;
            currentTrackIsUserTriggered = true;
            publishPositionUpdate();
        }
    }

    public boolean autoNextTrack() {
        return nextTrackInternal(false);
    }

    public boolean nextTrack() {
        return nextTrackInternal(true);
    }

    public boolean hasNextTrack() {
        return playQueue.hasNextTrack(currentPosition);
    }

    public TrackUrn getNextTrackUrn() {
        return hasNextTrack() ? getUrnAtPosition(getNextPosition()) : TrackUrn.NOT_SET;
    }

    private boolean nextTrackInternal(boolean manual) {
        if (playQueue.hasNextTrack(currentPosition)) {
            currentPosition++;
            currentTrackIsUserTriggered = manual;
            publishPositionUpdate();
            return true;
        } else {
            return false;
        }
    }

    private void setNewPlayQueueInternal(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        stopLoadingOperations();

        this.adTrackPosition = Consts.NOT_SET;
        this.playQueue = checkNotNull(playQueue, "Playqueue to update should not be null");
        this.currentTrackIsUserTriggered = true;
        this.playSessionSource = playSessionSource;

        broadcastNewPlayQueue();
    }

    public void saveCurrentProgress(long currentTrackProgress) {
        if (playQueue.hasItems()) {
            final int savePosition = getPositionToBeSaved();
            final long progress = getProgressToBeSaved(currentTrackProgress);
            playQueueOperations.savePositionInfo(savePosition, getCurrentTrackUrn(), playSessionSource, progress);
            playbackProgressInfo = new PlaybackProgressInfo(getCurrentTrackId(), progress);
        }
    }

    private int getPositionToBeSaved() {
        if (adTrackPosition != Consts.NOT_SET && adTrackPosition < currentPosition) {
            return currentPosition - 1;
        }
        return currentPosition;
    }

    private long getProgressToBeSaved(long currentTrackProgress) {
        // we will always have a next track when playing an ad. Start at the beginning of that.
        return adTrackPosition == currentPosition ? 0 : currentTrackProgress;
    }

    public void loadPlayQueue() {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        
        Observable<PlayQueue> playQueueObservable = playQueueOperations.getLastStoredPlayQueue();
        currentPosition = playQueueOperations.getLastStoredPlayPosition();
        if (playQueueObservable != null) {
            playQueueSubscription = playQueueObservable.subscribe(new DefaultSubscriber<PlayQueue>() {
                @Override
                public void onNext(PlayQueue savedQueue) {
                    setNewPlayQueueInternal(savedQueue, playQueueOperations.getLastStoredPlaySessionSource());
                }
            });
            // return so player can have the resume information while load is in progress
            playbackProgressInfo = new PlaybackProgressInfo(playQueueOperations.getLastStoredPlayingTrackId(), playQueueOperations.getLastStoredSeekPosition());
        }
    }

    private void publishPositionUpdate() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(getCurrentTrackUrn()));
    }

    @Nullable
    public TrackSourceInfo getCurrentTrackSourceInfo() {
        if (playQueue.isEmpty()) {
            return null;
        }

        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(playSessionSource.getOriginScreen(), currentTrackIsUserTriggered);
        trackSourceInfo.setSource(getCurrentTrackSource(), getCurrentTrackSourceVersion());
        if (playSessionSource.isFromPlaylist()) {
            trackSourceInfo.setOriginPlaylist(playSessionSource.getPlaylistId(), getCurrentPosition(), playSessionSource.getPlaylistOwnerId());
        }
        return trackSourceInfo;
    }

    private String getCurrentTrackSource() {
        return playQueue.getTrackSource(currentPosition);
    }

    private String getCurrentTrackSourceVersion() {
        return playQueue.getSourceVersion(currentPosition);
    }

    @Deprecated // use URNs
    public long getPlaylistId() {
        return playSessionSource.getPlaylistId();
    }

    public boolean isPlaylist() {
        return getPlaylistId() != Consts.NOT_SET;
    }

    @Deprecated // use URNs
    public boolean isCurrentPlaylist(long playlistId) {
        return getPlaylistId() == playlistId;
    }

    public boolean isAudioAdAtPosition(int position) {
        return playQueue.isAudioAd(position);
    }

    public boolean isNextTrackAudioAd() {
        return playQueue.isAudioAd(getNextPosition());
    }

    public boolean isCurrentTrackAudioAd() {
        return isAudioAdAtPosition(getCurrentPosition());
    }

    @Override
    public String getScreenTag() {
        return playSessionSource.getOriginScreen();
    }

    public boolean shouldReloadQueue() {
        return playQueue.isEmpty();
    }

    public void fetchTracksRelatedToCurrentTrack() {
        recommendedTracksObservable = playQueueOperations.getRelatedTracks(getCurrentTrackUrn()).observeOn(AndroidSchedulers.mainThread());
        loadRecommendedTracks();
    }

    public void retryRelatedTracksFetch() {
        loadRecommendedTracks();
    }

    public void clearAll() {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        
        playQueueOperations.clear();
        playQueue = PlayQueue.empty();
        playSessionSource = PlaySessionSource.EMPTY;
    }

    public PlayQueueView getPlayQueueView() {
        return getViewWithAppendState(fetchState);
    }

    public void insertAudioAd(AudioAd audioAd) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        
        if (adTrackPosition != Consts.NOT_SET) {
            throw new IllegalStateException("Existing AudioAd must be cleared before inserting a new one");
        }
        adTrackPosition = getNextPosition();
        playQueue.insertAudioAd(audioAd, adTrackPosition);
        this.adMetaData = audioAd
                .toPropertySet()
                .put(AdProperty.MONETIZABLE_TRACK_URN, playQueue.getUrn(adTrackPosition + 1));
        publishQueueUpdate();
    }

    public void clearAudioAd() {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        
        if (adTrackPosition != Consts.NOT_SET && adTrackPosition != currentPosition) {
            removeAd(adTrackPosition);
            publishQueueUpdate();
        }
    }

    private void removeAd(int position) {
        playQueue.remove(position);
        if (position < currentPosition) {
            currentPosition--;
        }
        adTrackPosition = Consts.NOT_SET;
        this.adMetaData = null;
    }

    public PropertySet getAudioAd() {
        if (adMetaData == null) {
            throw new IllegalStateException("No audio ad available.");
        }
        return adMetaData;
    }

    private void loadRecommendedTracks() {
        setNewRelatedLoadingState(FetchRecommendedState.LOADING);
        gotRecommendedTracks = false;
        fetchRecommendedSubscription = recommendedTracksObservable.subscribe(this);
    }

    @Override
    public void onNext(RecommendedTracksCollection relatedTracks) {
        for (ApiTrack item : relatedTracks) {
            final PublicApiTrack track = new PublicApiTrack(item);
            modelManager.cache(track);
            playQueue.addTrack(track.getUrn(), PlaySessionSource.DiscoverySource.RECOMMENDER.value(),
                    relatedTracks.getSourceVersion());
        }
        gotRecommendedTracks = true;
    }

    @Override
    public void onCompleted() {
        if (gotRecommendedTracks){
            setNewRelatedLoadingState(FetchRecommendedState.IDLE);
            publishQueueUpdate();
            saveQueue();
        } else {
            setNewRelatedLoadingState(FetchRecommendedState.EMPTY);
        }
    }

    @Override
    public void onError(Throwable e) {
        setNewRelatedLoadingState(FetchRecommendedState.ERROR);
    }

    private void saveQueue() {
        if (playQueue.hasItems()) {
            PlayQueue saveQueue = playQueue.copy();
            if (adTrackPosition != Consts.NOT_SET) {
                saveQueue.remove(adTrackPosition);
            }
            playQueueOperations.saveQueue(saveQueue);
        }
    }

    private void setNewRelatedLoadingState(FetchRecommendedState fetchState) {
        this.fetchState = fetchState;
        broadcastRelatedLoadStateChanged();
    }

    private void publishQueueUpdate() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());
    }

    private void broadcastRelatedLoadStateChanged() {
        final Intent intent = new Intent(RELATED_LOAD_STATE_CHANGED_ACTION)
                .putExtra(PlayQueueView.EXTRA, getViewWithAppendState(fetchState));
        context.sendBroadcast(intent);
    }

    private void broadcastNewPlayQueue() {
        Intent intent = new Intent(PLAYQUEUE_CHANGED_ACTION)
                .putExtra(PlayQueueView.EXTRA, getViewWithAppendState(fetchState));
        context.sendBroadcast(intent);

        final TrackUrn currentTrackUrn = getCurrentTrackUrn();
        if (!TrackUrn.NOT_SET.equals(currentTrackUrn)){
            eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue());
            eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(currentTrackUrn));
        }
    }

    private void stopLoadingOperations() {
        fetchRecommendedSubscription.unsubscribe();
        fetchRecommendedSubscription = Subscriptions.empty();

        playQueueSubscription.unsubscribe();
    }

}
