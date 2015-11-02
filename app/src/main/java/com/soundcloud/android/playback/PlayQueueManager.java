package com.soundcloud.android.playback;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdFunctions;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
public class PlayQueueManager implements OriginProvider {

    private static final String UI_ASSERTION_MESSAGE = "Play queues must be set from the main thread only.";

    private final PlayQueueOperations playQueueOperations;
    private final PolicyOperations policyOperations;
    private final EventBus eventBus;
    private int currentPosition;
    private boolean currentItemIsUserTriggered;

    private PlayQueue playQueue = PlayQueue.empty();
    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
    private Pair<Urn, Long> lastPlayedTrackAndPosition = Pair.of(Urn.NOT_SET, (long) Consts.NOT_SET);

    @Inject
    public PlayQueueManager(PlayQueueOperations playQueueOperations,
                            EventBus eventBus,
                            PolicyOperations policyOperations) {
        this.playQueueOperations = playQueueOperations;
        this.eventBus = eventBus;
        this.policyOperations = policyOperations;
    }

    public void setNewPlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        setNewPlayQueue(playQueue, playSessionSource, 0);
    }

    public void setNewPlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource, int startPosition) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        logEmptyPlayQueues(playQueue, playSessionSource);

        if (this.playQueue.equals(playQueue) && this.playSessionSource.equals(playSessionSource)) {
            this.currentPosition = startPosition;
            eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(getCurrentPlayQueueItem(), getCollectionUrn(), getCurrentPosition()));
        } else {
            currentPosition = startPosition;
            setNewPlayQueueInternal(playQueue, playSessionSource);
        }
        saveQueue();
        saveCurrentProgress(0L);

        fireAndForget(policyOperations.updatePolicies(playQueue.getTrackItemUrns()));
    }

    private void logEmptyPlayQueues(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        if (playQueue.isEmpty()) {
            ErrorUtils.handleSilentException(new IllegalStateException("Setting empty play queue"),
                    "PlaySessionSource", playSessionSource.toString());
        }
    }

    void appendPlayQueueItems(Iterable<PlayQueueItem> playQueueItems) {
        this.playQueue.addAllPlayQueueItems(playQueueItems);

        publishQueueUpdate();
        saveQueue();
    }

    void appendUniquePlayQueueItems(Iterable<PlayQueueItem> playQueueItems) {
        final List<Integer> queueHashes = this.playQueue.getQueueHashes();

        for (PlayQueueItem playQueueItem : playQueueItems) {
            if (!queueHashes.contains(playQueueItem.hashCode())) {
                this.playQueue.addPlayQueueItem(playQueueItem);
            }
        }
        publishQueueUpdate();
        saveQueue();
    }

    public PlayQueueItem getCurrentPlayQueueItem() {
        return getPlayQueueItemAtPosition(currentPosition);
    }

    public PlayQueueItem getNextPlayQueueItem() {
        return getPlayQueueItemAtPosition(getNextPosition());
    }

    public PlayQueueItem getLastPlayQueueItem() {
        return getPlayQueueItemAtPosition(getQueueSize() - 1);
    }

    public PlayQueueItem getPlayQueueItemAtPosition(int position) {
        if (position >= 0 && position < getQueueSize()) {
            return playQueue.getPlayQueueItem(position);
        } else {
            return PlayQueueItem.EMPTY;
        }
    }

    public boolean isCurrentTrack(@NotNull Urn trackUrn) {
        return isTrackAt(trackUrn, getCurrentPosition());
    }

    public boolean isTrackAt(@NotNull Urn trackUrn, int currentPosition) {
        return currentPosition < getQueueSize() &&
                getPlayQueueItemAtPosition(currentPosition).isTrack() &&
                getPlayQueueItemAtPosition(currentPosition).getUrn().equals(trackUrn);
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public boolean wasLastSavedTrack(Urn urn) {
        return lastPlayedTrackAndPosition.first().equals(urn);
    }

    public long getLastSavedPosition() {
        return lastPlayedTrackAndPosition.second();
    }

    private int getNextPosition() {
        return getCurrentPosition() + 1;
    }

    public boolean isCurrentPosition(int position) {
        return position == getCurrentPosition();
    }

    public boolean isQueueEmpty() {
        return playQueue.isEmpty();
    }

    public int getQueueSize() {
        return playQueue.size();
    }

    public int getPositionForUrn(final Urn urn) {
        return playQueue.indexOfTrackUrn(urn);
    }

    public int getUpcomingPositionForUrn(Urn urn) {
        return playQueue.indexOfTrackUrn(currentPosition, urn);
    }

    public void setPosition(int position) {
        if (position != currentPosition && position < playQueue.size()) {
            this.currentPosition = position;
            currentItemIsUserTriggered = true;
            publishPositionUpdate();
        }
    }

    public void moveToPreviousItem() {
        if (playQueue.hasPreviousItem(currentPosition)) {
            currentPosition--;
            currentItemIsUserTriggered = true;
            publishPositionUpdate();
        }
    }

    public boolean autoNextItem() {
        return nextItemInternal(false);
    }

    public boolean nextItem() {
        return nextItemInternal(true);
    }

    public boolean hasNextItem() {
        return playQueue.hasNextItem(currentPosition);
    }

    private boolean nextItemInternal(boolean manual) {
        if (playQueue.hasNextItem(currentPosition)) {
            currentPosition++;
            currentItemIsUserTriggered = manual;
            publishPositionUpdate();
            return true;
        } else {
            return false;
        }
    }

    public List<Urn> getCurrentQueueTrackUrns() {
        return playQueue.getTrackItemUrns();
    }

    public boolean hasSameTrackList(List<Urn> remoteTrackList) {
        return playQueue.getTrackItemUrns().equals(remoteTrackList);
    }

    private void setNewPlayQueueInternal(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        this.playQueue = checkNotNull(playQueue, "Playqueue to update should not be null");
        this.currentItemIsUserTriggered = true;
        this.playSessionSource = playSessionSource;

        broadcastNewPlayQueue();
    }

    public void saveCurrentProgress(long currentTrackProgress) {
        if (playQueue.hasItems() && getCurrentPlayQueueItem().isTrack()) {
            final int savePosition = getPositionToBeSaved();
            final long progress = getProgressToBeSaved(currentTrackProgress);
            final Urn trackUrn = getCurrentPlayQueueItem().getUrn();
            playQueueOperations.savePositionInfo(savePosition, trackUrn, playSessionSource, progress);
            setLastPlayedTrackAndPosition(trackUrn, progress);
        }
    }

    private int getPositionToBeSaved() {
        int adjustedPosition = currentPosition;
        for (int i = 0; i < currentPosition; i++) {
            if (!playQueue.shouldPersistItemAt(i)) {
                adjustedPosition--;
            }
        }
        return adjustedPosition;
    }

    private long getProgressToBeSaved(long currentTrackProgress) {
        // we will always have a next track when playing an ad. Start at the beginning of that.
        return playQueue.shouldPersistItemAt(currentPosition) ? currentTrackProgress : 0;
    }

    public Observable<PlayQueue> loadPlayQueueAsync() {
        assertOnUiThread(UI_ASSERTION_MESSAGE);

        Observable<PlayQueue> playQueueObservable = playQueueOperations.getLastStoredPlayQueue();
        if (playQueueObservable != null) {
            return playQueueObservable
                    .doOnSubscribe(new Action0() {
                        @Override
                        public void call() {
                            // return so player can have the resume information while load is in progress
                            setLastPlayedTrackAndPosition(
                                    Urn.forTrack(playQueueOperations.getLastStoredPlayingTrackId()),
                                    playQueueOperations.getLastStoredSeekPosition());
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(new Action1<PlayQueue>() {
                        @Override
                        public void call(PlayQueue savedQueue) {
                            currentPosition = playQueueOperations.getLastStoredPlayPosition();
                            setNewPlayQueueInternal(savedQueue, playQueueOperations.getLastStoredPlaySessionSource());
                        }
                    });
        } else {
            return Observable.empty();
        }
    }

    private void setLastPlayedTrackAndPosition(Urn urn, long lastStoredSeekPosition) {
        lastPlayedTrackAndPosition = Pair.of(urn, lastStoredSeekPosition);
    }

    private void publishPositionUpdate() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(getCurrentPlayQueueItem(), getCollectionUrn(), getCurrentPosition()));
    }

    public boolean isTrackFromCurrentPromotedItem(Urn trackUrn) {
        final TrackSourceInfo trackSourceInfo = getCurrentTrackSourceInfo();
        if (trackSourceInfo == null) {
            return false;
        }

        if (trackSourceInfo.isFromPromoted()) {
            final PromotedSourceInfo promotedSourceInfo = trackSourceInfo.getPromotedSourceInfo();
            // Track is from a promoted playlist and not a recommendation
            if (trackSourceInfo.isFromPlaylist() && trackSourceInfo.getPlaylistPosition() < playSessionSource.getCollectionSize()) {
                return trackSourceInfo.getCollectionUrn().equals(promotedSourceInfo.getPromotedItemUrn());
            } else if (isCurrentPosition(0)) { // Track is a promoted track?
                return trackUrn.equals(promotedSourceInfo.getPromotedItemUrn());
            }
        }

        return false;
    }

    @Nullable
    public TrackSourceInfo getCurrentTrackSourceInfo() {
        if (playQueue.isEmpty()) {
            return null;
        }

        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(playSessionSource.getOriginScreen(), currentItemIsUserTriggered);

        PlayQueueItem currentPlayQueueItem = getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isTrack()) {
            TrackQueueItem trackQueueItem = (TrackQueueItem) currentPlayQueueItem;
            trackSourceInfo.setSource(trackQueueItem.getSource(), trackQueueItem.getSourceVersion());
            trackSourceInfo.setReposter(trackQueueItem.getReposter());
        }

        if (playSessionSource.isFromQuery()) {
            trackSourceInfo.setSearchQuerySourceInfo(playSessionSource.getSearchQuerySourceInfo());
        }

        if (playSessionSource.isFromPromotedItem()) {
            trackSourceInfo.setPromotedSourceInfo(playSessionSource.getPromotedSourceInfo());
        }

        if (playSessionSource.isFromStations()) {
            trackSourceInfo.setOriginStation(playSessionSource.getCollectionUrn());
        }

        final Urn collectionUrn = playSessionSource.getCollectionUrn();
        if (collectionUrn.isPlaylist()) {
            trackSourceInfo.setOriginPlaylist(collectionUrn, getCurrentPosition(), playSessionSource.getCollectionOwnerUrn());
        }

        return trackSourceInfo;
    }

    public PlaySessionSource getCurrentPlaySessionSource() {
        return playSessionSource;
    }

    public Urn getCollectionUrn() {
        return playSessionSource.getCollectionUrn();
    }

    public boolean isCurrentCollection(Urn collection) {
        return getCollectionUrn().equals(collection) &&
                (playQueue.isEmpty() || isTrackQueueItemSourceEmpty(getCurrentPlayQueueItem()));
    }

    public boolean isTrackQueueItemSourceEmpty(PlayQueueItem playQueueItem) {
        return playQueueItem.isTrack()
                && Strings.isBlank(((TrackQueueItem) playQueueItem).getSource());
    }

    public boolean isCurrentCollectionOrRecommendation(Urn collection) {
        return getCollectionUrn().equals(collection);
    }

    @Override
    public String getScreenTag() {
        return playSessionSource.getOriginScreen();
    }

    public void clearAll() {
        assertOnUiThread(UI_ASSERTION_MESSAGE);

        playQueueOperations.clear();
        playQueue = PlayQueue.empty();
        playSessionSource = PlaySessionSource.EMPTY;
    }

    public void performPlayQueueUpdateOperations(QueueUpdateOperation... operations) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        for (QueueUpdateOperation operation : operations) {
            operation.execute(playQueue);
        }
        publishQueueUpdate();
    }

    @VisibleForTesting
    public void removeAds() {
        removeAds(PlayQueueEvent.fromQueueUpdate(getCollectionUrn()));
    }

    public void removeAds(PlayQueueEvent updateEvent) {
        boolean queueUpdated = false;
        int i = 0;
        for (final Iterator<PlayQueueItem> iterator = playQueue.iterator(); iterator.hasNext(); ) {
            final PlayQueueItem item = iterator.next();
            if (AdFunctions.IS_PLAYER_AD_ITEM.apply(item)) {
                iterator.remove();
                queueUpdated = true;
                if (i <= currentPosition) {
                    currentPosition--;
                }
            } else {
                i++;
            }
        }

        if (queueUpdated) {
            eventBus.publish(EventQueue.PLAY_QUEUE, updateEvent);
        }
    }

    public List<PlayQueueItem> filterAdQueueItems() {
        List<PlayQueueItem> matchingQueueItems = new ArrayList<>();
        for (PlayQueueItem playQueueItem : playQueue) {
            if (!AdFunctions.IS_PLAYER_AD_ITEM.apply(playQueueItem)) {
                matchingQueueItems.add(playQueueItem);
            }
        }
        return matchingQueueItems;
    }

    @Nullable
    public PromotedSourceInfo getCurrentPromotedSourceInfo(Urn trackUrn) {
        if (isTrackFromCurrentPromotedItem(trackUrn)) {
            final TrackSourceInfo trackSourceInfo = getCurrentTrackSourceInfo();

            if (trackSourceInfo != null) {
                return trackSourceInfo.getPromotedSourceInfo();
            }
        }

        return null;
    }

    private void saveQueue() {
        if (playQueue.hasItems()) {
            playQueueOperations.saveQueue(playQueue.copy());
        }
    }

    private void publishQueueUpdate() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(getCollectionUrn()));
    }

    private void broadcastNewPlayQueue() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(getCollectionUrn()));

        if (playQueue.hasItems()) {
            eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(getCurrentPlayQueueItem(), getCollectionUrn(), getCurrentPosition()));
        }
    }

    public interface QueueUpdateOperation {
        void execute(PlayQueue playQueue);
    }

    public static class InsertAudioOperation implements QueueUpdateOperation {

        private final int position;
        private final Urn trackUrn;
        private final AudioAd audioAd;
        private final boolean shouldPersist;

        public InsertAudioOperation(int position, Urn trackUrn, AudioAd audioAd, boolean shouldPersist) {
            this.position = position;
            this.trackUrn = trackUrn;
            this.audioAd = audioAd;
            this.shouldPersist = shouldPersist;
        }

        @Override
        public void execute(PlayQueue playQueue) {
            playQueue.insertAudioAd(position, trackUrn, audioAd, shouldPersist);
        }
    }

    public static class InsertVideoOperation implements QueueUpdateOperation {

        private final int position;
        private final VideoAd videoAd;

        public InsertVideoOperation(int position, VideoAd videoAd) {
            this.position = position;
            this.videoAd = videoAd;
        }

        @Override
        public void execute(PlayQueue playQueue) {
            playQueue.insertVideo(position, videoAd);
        }
    }

    public static class SetAdDataOperation implements QueueUpdateOperation {

        private final int position;
        private final Optional<AdData> adData;

        public SetAdDataOperation(int position, Optional<AdData> adData) {
            this.position = position;
            this.adData = adData;
        }

        @Override
        public void execute(PlayQueue playQueue) {
            playQueue.setAdData(position, adData);
        }
    }
}
