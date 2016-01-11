package com.soundcloud.android.playback;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.Consts;
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
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
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

        if (this.playQueue.hasSameTracks(playQueue) && this.playSessionSource.equals(playSessionSource)) {
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

    public boolean isCurrentItem(PlayQueueItem playQueueItem) {
        return getCurrentPlayQueueItem().equals(playQueueItem);
    }

    public boolean isNextItem(PlayQueueItem playQueueItem) {
        return getNextPlayQueueItem().equals(playQueueItem);
    }

    public void setCurrentPlayQueueItem(PlayQueueItem playQueueItem) {
        setPosition(playQueue.indexOfPlayQueueItem(playQueueItem), true);
    }

    public List<PlayQueueItem> getPlayQueueItems(Predicate<PlayQueueItem> predicate) {
        return Lists.newArrayList(Iterables.filter(playQueue, predicate));
    }

    public List<Urn> getUpcomingPlayQueueItems(int count) {
        if (hasNextItem()) {
            final int nextPosition = currentPosition + 1;
            return playQueue.getItemUrns(nextPosition, nextPosition + count);
        } else {
            return Collections.emptyList();
        }
    }

    public void insertPlaylistTracks(Urn playlistUrn, List<Urn> tracks) {
        for (PlayQueueItem item : playQueue.itemsWithUrn(playlistUrn)) {
            PlayableQueueItem playableQueueItem = (PlayableQueueItem) item;
            List<PlayQueueItem> items = new ArrayList<>(tracks.size());
            for (Urn track : tracks) {
                items.add(new TrackQueueItem.Builder(track).copySource(playableQueueItem).build());
            }
            playQueue.replaceItem(playQueue.indexOfPlayQueueItem(item), items);
        }
        publishQueueUpdate();
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

    public PlayQueueItem getCurrentPlayQueueItem() {
        return getPlayQueueItemAtPosition(currentPosition);
    }

    public PlayQueueItem getNextPlayQueueItem() {
        if (hasNextItem()) {
            return playQueue.getPlayQueueItem(getCurrentPosition() + 1);
        } else {
            return PlayQueueItem.EMPTY;
        }
    }

    public PlayQueueItem getLastPlayQueueItem() {
        return getPlayQueueItemAtPosition(getQueueSize() - 1);
    }

    @VisibleForTesting
    PlayQueueItem getPlayQueueItemAtPosition(int position) {
        if (position >= 0 && position < getQueueSize()) {
            return playQueue.getPlayQueueItem(position);
        } else {
            return PlayQueueItem.EMPTY;
        }
    }

    @VisibleForTesting
    int getCurrentPosition() {
        return currentPosition;
    }

    public boolean isCurrentTrack(@NotNull Urn trackUrn) {
        int currentPosition1 = getCurrentPosition();
        return currentPosition1 < getQueueSize() &&
                getPlayQueueItemAtPosition(currentPosition1).isTrack() &&
                getPlayQueueItemAtPosition(currentPosition1).getUrn().equals(trackUrn);
    }

    public boolean wasLastSavedTrack(Urn urn) {
        return lastPlayedTrackAndPosition.first().equals(urn);
    }

    public long getLastSavedProgressPosition() {
        return lastPlayedTrackAndPosition.second();
    }

    public boolean isQueueEmpty() {
        return playQueue.isEmpty();
    }

    public int getQueueSize() {
        return playQueue.size();
    }

    public int getQueueItemsRemaining() {
        return getQueueSize() - playQueue.indexOfPlayQueueItem(getCurrentPlayQueueItem()) - 1;
    }

    @Deprecated // this should not be used outside of casting, which uses it as an optimisation. Use setCurrentPlayQueueItem instead
    public void setPosition(int position, boolean isUserTriggered) {
        if (position != currentPosition && position < playQueue.size()) {
            this.currentPosition = position;
            currentItemIsUserTriggered = isUserTriggered;
            publishPositionUpdate();
        }
    }

    public boolean hasNextItem() {
        return playQueue.hasNextItem(currentPosition);
    }

    public boolean hasPreviousItem() {
        return playQueue.hasPreviousItem(currentPosition);
    }

    public List<Urn> getCurrentQueueTrackUrns() {
        return playQueue.getTrackItemUrns();
    }

    public boolean moveToNextPlayableItem() {
        return moveToNextPlayableItemInternal(true);
    }

    public boolean autoMoveToNextPlayableItem() {
        return moveToNextPlayableItemInternal(false);
    }

    private boolean moveToNextPlayableItemInternal(boolean userTriggered) {
        if (hasNextItem()) {
            final int nextPlayableItem = getNextPlayableItem();
            final int newPosition = nextPlayableItem == Consts.NOT_SET
                    ? getQueueSize() - 1 // last track
                    : nextPlayableItem; // next playable track
            setPosition(newPosition, userTriggered);
            return true;
        } else {
            return false;
        }
    }

    private int getNextPlayableItem() {
        for (int i = currentPosition + 1, size = getQueueSize(); i < size; i++) {
            if (isPlayableAtPosition(i)) {
                return i;
            }
        }
        return Consts.NOT_SET;
    }

    private boolean isPlayableAtPosition(int i) {
        final PlayQueueItem playQueueItem = playQueue.getPlayQueueItem(i);
        return playQueueItem.isVideo() || !((TrackQueueItem) playQueueItem).isBlocked();
    }

    public boolean moveToPreviousPlayableItem() {
        if (hasPreviousItem()) {
            final int previousPlayable = getPreviousPlayableItem();
            final int newPosition = previousPlayable == Consts.NOT_SET
                    ? 0 // first track
                    : previousPlayable; // next playable track
            setPosition(newPosition, true);
            return true;
        } else {
            return false;
        }
    }

    private int getPreviousPlayableItem() {
        for (int i = currentPosition - 1; i > 0; i--) {
            if (isPlayableAtPosition(i)) {
                return i;
            }
        }
        return Consts.NOT_SET;
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

        return playQueueOperations.getLastStoredPlayQueue()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Action1<PlayQueue>() {
                    @Override
                    public void call(PlayQueue savedQueue) {
                        setLastPlayedTrackAndPosition(
                                Urn.forTrack(playQueueOperations.getLastStoredPlayingTrackId()),
                                playQueueOperations.getLastStoredSeekPosition());

                        currentPosition = playQueueOperations.getLastStoredPlayPosition();
                        setNewPlayQueueInternal(savedQueue, playQueueOperations.getLastStoredPlaySessionSource());
                    }
                });
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
            } else if (0 == getCurrentPosition()) { // Track is a promoted track?
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

        if (playSessionSource.isFromSearchQuery()) {
            trackSourceInfo.setSearchQuerySourceInfo(playSessionSource.getSearchQuerySourceInfo());
        }

        if (playSessionSource.isFromPromotedItem()) {
            trackSourceInfo.setPromotedSourceInfo(playSessionSource.getPromotedSourceInfo());
        }

        if (playSessionSource.isFromStations()) {
            TrackQueueItem trackQueueItem = (TrackQueueItem) currentPlayQueueItem;
            trackSourceInfo.setStationSourceInfo(
                    playSessionSource.getCollectionUrn(),
                    StationsSourceInfo.create(trackQueueItem.getQueryUrn())
            );
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
        return getCollectionUrn().equals(collection);
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

    public void removeUpcomingItem(PlayQueueItem item, boolean shouldPublishQueueChange) {
        final int indexToRemove = playQueue.indexOfPlayQueueItem(item);
        if (indexToRemove > currentPosition) {
            playQueue.removeItemAtPosition(indexToRemove);
            if (shouldPublishQueueChange) {
                publishQueueUpdate();
            }
        }
    }

    public void insertVideo(PlayQueueItem beforeItem, VideoAd videoAd){
        playQueue.insertVideo(playQueue.indexOfPlayQueueItem(beforeItem), videoAd);
        publishQueueUpdate();
    }

    public void insertAudioAd(PlayQueueItem beforeItem, Urn trackUrn, AudioAd audioAd, boolean shouldPersist){
        playQueue.insertAudioAd(playQueue.indexOfPlayQueueItem(beforeItem), trackUrn, audioAd, shouldPersist);
        publishQueueUpdate();
    }
}
