package com.soundcloud.android.playback;

import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;
import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.discovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.TrackOfflineStateProvider;
import com.soundcloud.android.playback.PlaybackContext.Bucket;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.java.strings.Strings;
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

    public enum RepeatMode {
        REPEAT_NONE(""),
        REPEAT_ONE("one"),
        REPEAT_ALL("all");

        private final String repeatMode;

        RepeatMode(String repeatMode) {
            this.repeatMode = repeatMode;
        }

        public String get() {
            return repeatMode;
        }
    }

    private static final String UI_ASSERTION_MESSAGE = "Play queues must be set from the main thread only.";

    private final PlayQueueOperations playQueueOperations;
    private final EventBus eventBus;
    private final NetworkConnectionHelper networkConnectionHelper;
    private final TrackOfflineStateProvider offlineStateProvider;

    private Action1<PlayQueue> onPlayQueueLoaded = new Action1<PlayQueue>() {
        @Override
        public void call(PlayQueue savedQueue) {
            currentPosition = playQueueOperations.getLastStoredPlayPosition();
            setNewPlayQueueInternal(savedQueue, playQueueOperations.getLastStoredPlaySessionSource());
            eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(getCollectionUrn()));
        }
    };

    private int currentPosition;
    private boolean currentItemIsUserTriggered;
    private RepeatMode repeatMode = RepeatMode.REPEAT_NONE;
    private boolean autoPlay = true;

    private PlayQueue playQueue = PlayQueue.empty();
    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

    @Inject
    public PlayQueueManager(PlayQueueOperations playQueueOperations,
                            EventBus eventBus,
                            NetworkConnectionHelper networkConnectionHelper,
                            TrackOfflineStateProvider offlineStateProvider) {
        this.playQueueOperations = playQueueOperations;
        this.eventBus = eventBus;
        this.networkConnectionHelper = networkConnectionHelper;
        this.offlineStateProvider = offlineStateProvider;
    }

    public void shuffle() {
        final int pivot = currentPosition + 1 >= playQueue.size() ? 0 : currentPosition + 1;
        setPlayQueueKeepPosition(playQueue.shuffle(pivot));
    }

    public void unshuffle() {
        checkState(playQueue instanceof ShuffledPlayQueue, "unshuffle must be called on a shuffled play queue.");
        setPlayQueueKeepPosition(((ShuffledPlayQueue) playQueue).unshuffle());
    }

    public boolean isShuffled() {
        return playQueue.isShuffled();
    }

    private void setPlayQueueKeepPosition(PlayQueue newPlayQueue) {
        final PlayQueueItem currentPlayQueueItem = getCurrentPlayQueueItem();
        final int startPosition = newPlayQueue.indexOfPlayQueueItem(currentPlayQueueItem);

        checkState(startPosition != -1, "The current play queue item must be present in the new play queue.");

        setNewPlayQueueInternal(newPlayQueue, playSessionSource);
        currentPosition = startPosition;
        saveQueueForUpdate();
    }

    void setNewPlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        setNewPlayQueue(playQueue, playSessionSource, 0);
    }

    public void setNewPlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource, int startPosition) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        logEmptyPlayQueues(playQueue, playSessionSource);
        currentPosition = startPosition;

        if (isSamePlayQueue(playQueue, playSessionSource) && isSamePlayQueueType(playQueue)) {
            publishCurrentQueueItemChanged();
        } else {
            setNewPlayQueueInternal(playQueue, playSessionSource);
            saveQueue(PlayQueueEvent.fromNewQueue(getCollectionUrn()));
        }
        saveCurrentPosition();
    }

    private boolean isSamePlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        return this.playQueue.hasSameTracks(playQueue) && this.playSessionSource.equals(playSessionSource);
    }

    private boolean isSamePlayQueueType(PlayQueue newPlayQueue) {
        return newPlayQueue.isShuffled() == this.playQueue.isShuffled();
    }

    void saveCurrentPosition() {
        if (playQueue.hasItems()) {
            playQueueOperations.savePlayInfo(getPositionToBeSaved(), playSessionSource);
        }
    }

    public boolean isCurrentItem(PlayQueueItem playQueueItem) {
        return getCurrentPlayQueueItem().equals(playQueueItem);
    }

    public boolean isNextItem(PlayQueueItem playQueueItem) {
        return getNextPlayQueueItem().equals(playQueueItem);
    }

    public void setCurrentPlayQueueItem(PlayQueueItem playQueueItem) {
        setPositionInternal(playQueue.indexOfPlayQueueItem(playQueueItem), true);
        saveCurrentPosition();
    }

    public void setCurrentPlayQueueItem(Urn urn) {
        setPositionInternal(playQueue.indexOfTrackUrn(urn), true);
    }

    void setCurrentPlayQueueItem(Urn urn, int trackPosition) {
        int queuePosition = getQueuePositionFromTrack(trackPosition);
        PlayQueueItem playQueueItem = getPlayQueueItemAtPosition(queuePosition);

        if (!playQueueItem.isEmpty() && playQueueItem.getUrn().equals(urn)) {
            setPositionInternal(queuePosition, true);
        } else {
            setCurrentPlayQueueItem(urn);
        }
    }

    private int getQueuePositionFromTrack(int position) {
        for (int queuePosition = 0, trackPosition = 0; queuePosition < playQueue.size(); queuePosition++) {
            if (getPlayQueueItemAtPosition(queuePosition).getUrn().isTrack()) {
                if (trackPosition == position) {
                    return queuePosition;
                }
                trackPosition++;
            }
        }
        return Consts.NOT_SET;
    }

    public List<PlayQueueItem> getPlayQueueItems(Predicate<PlayQueueItem> predicate) {
        return Lists.newArrayList(Iterables.filter(playQueue, predicate));
    }

    public List<Urn> getUpcomingPlayQueueItems(int count) {
        if (hasNextItem()) {
            final int nextPosition = currentPosition + 1;
            return playQueue.getItemUrns(nextPosition, count);
        } else {
            return Collections.emptyList();
        }
    }

    List<Urn> getPreviousPlayQueueItems(int count) {
        if (hasPreviousItem()) {
            final int firstPosition = Math.max(0, currentPosition - count);
            return playQueue.getItemUrns(firstPosition, currentPosition - firstPosition);
        } else {
            return Collections.emptyList();
        }
    }

    void insertPlaylistTracks(Urn playlistUrn, List<Urn> tracks) {
        for (PlayQueueItem item : playQueue.itemsWithUrn(playlistUrn)) {
            PlayableQueueItem playableQueueItem = (PlayableQueueItem) item;

            final int index = playQueue.indexOfPlayQueueItem(item);
            if (index < currentPosition) {
                currentPosition += tracks.size() - 1;
            }

            List<PlayQueueItem> items = new ArrayList<>(tracks.size());
            for (Urn track : tracks) {
                items.add(new TrackQueueItem.Builder(track).copySourceAndPlaybackContext(playableQueueItem).build());
            }
            playQueue.replaceItem(index, items);
        }
        saveQueueForUpdate();
    }

    public void replace(PlayQueueItem oldItem, List<PlayQueueItem> newItems) {
        playQueue.replaceItem(playQueue.indexOfPlayQueueItem(oldItem), newItems);
        saveQueueForUpdate();
    }

    public void insertNext(List<Urn> trackUrns) {
        if (!playQueue.isEmpty()) {
            int nextExplicitPosition = nextExplicitPositionFromCurrent();
            for (int i = 0; i < trackUrns.size(); i++) {
                TrackQueueItem queueItem = new TrackQueueItem.Builder(trackUrns.get(i))
                        .fromSource(DiscoverySource.PLAY_NEXT.value(), Strings.EMPTY)
                        .withPlaybackContext(PlaybackContext.create(Bucket.EXPLICIT))
                        .build();
                playQueue.insertPlayQueueItem(nextExplicitPosition + i, queueItem);
            }
            saveQueue(PlayQueueEvent.fromQueueInsert(getCollectionUrn()));
        } else {
            throw new IllegalStateException("It is not possible to insert when the play queue is empty");
        }
    }

    public void insertNext(Urn trackUrn) {
        if (!playQueue.isEmpty()) {
            int nextExplicitPosition = nextExplicitPositionFromCurrent();
            TrackQueueItem queueItem = new TrackQueueItem.Builder(trackUrn)
                    .fromSource(DiscoverySource.PLAY_NEXT.value(), Strings.EMPTY)
                    .withPlaybackContext(PlaybackContext.create(Bucket.EXPLICIT))
                    .build();
            playQueue.insertPlayQueueItem(nextExplicitPosition, queueItem);
            saveQueue(PlayQueueEvent.fromQueueInsert(getCollectionUrn()));
        } else {
            throw new IllegalStateException("It is not possible to insert when the play queue is empty");
        }
    }

    private final Predicate<PlayQueueItem> NOT_TRACK_ADDED_EXPLICITLY = new Predicate<PlayQueueItem>() {
        @Override
        public boolean apply(PlayQueueItem item) {
            if (item.isTrack()) {
                final PlayableQueueItem queueItem = (PlayableQueueItem) item;
                return !queueItem.isBucket(Bucket.EXPLICIT);
            } else {
                return true;
            }
        }
    };

    private int nextExplicitPositionFromCurrent() {
        final int startPosition = currentPosition + 1;

        if (startPosition >= playQueue.size()) {
            return startPosition;
        }

        final List<PlayQueueItem> upcomingItems = playQueue.items().subList(startPosition, playQueue.size());
        return startPosition + Iterables.indexOf(upcomingItems, NOT_TRACK_ADDED_EXPLICITLY);
    }

    private void logEmptyPlayQueues(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        if (playQueue.isEmpty()) {
            ErrorUtils.handleSilentException(new IllegalStateException("Setting empty play queue"),
                                             "PlaySessionSource", playSessionSource.toString());
        }
    }

    void appendPlayQueueItems(Iterable<PlayQueueItem> playQueueItems) {
        this.playQueue.addAllPlayQueueItems(playQueueItems);
        saveQueueForUpdate();
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

    PlayQueueItem getLastPlayQueueItem() {
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
        return trackUrn.isTrack() && isCurrentItem(trackUrn);
    }

    public boolean isCurrentItem(@NotNull Urn itemUrn) {
        int currentPosition1 = getCurrentPosition();
        return currentPosition1 < getQueueSize() &&
                !getPlayQueueItemAtPosition(currentPosition1).isEmpty() &&
                getPlayQueueItemAtPosition(currentPosition1).getUrn().equals(itemUrn);
    }

    public boolean isQueueEmpty() {
        return playQueue.isEmpty();
    }

    int getQueueSize() {
        return playQueue.size();
    }

    int getPlayableQueueItemsRemaining() {
        int playableCount = 0;
        for (int i = currentPosition + 1, size = getQueueSize(); i < size; i++) {
            if (isPlayableAtPosition(i)) {
                playableCount++;
            }
        }
        return playableCount;
    }

    @Deprecated
    // this should not be used outside of casting, which uses it as an optimisation. Use setCurrentPlayQueueItem instead
    public void setPosition(int position, boolean isUserTriggered) {
        setPositionInternal(position, isUserTriggered);
    }

    private void setPositionInternal(int position, boolean isUserTriggered) {
        if (position != currentPosition && position < playQueue.size()) {
            this.currentPosition = position;
            PlayQueueItem playQueueItem = getCurrentPlayQueueItem();
            if (playQueueItem.isPlayable()) {
                PlayableQueueItem playableQueueItem = (PlayableQueueItem) playQueueItem;
                playableQueueItem.setPlayed();
            }
            currentItemIsUserTriggered = isUserTriggered;
            publishPositionUpdate();
        }
    }

    public boolean hasNextItem() {
        return playQueue.hasNextItem(currentPosition);
    }

    public boolean hasTrackAsNextItem() {
        return playQueue.hasTrackAsNextItem(currentPosition);
    }

    boolean hasPreviousItem() {
        return playQueue.hasPreviousItem(currentPosition);
    }

    public List<Urn> getCurrentQueueTrackUrns() {
        return playQueue.getTrackItemUrns();
    }

    public boolean moveToNextPlayableItem() {
        return moveToNextPlayableItemInternal(true);
    }

    public void moveToNextRecommendationItem() {
        for (PlayQueueItem playQueueItem : playQueue.items()) {
            if (playQueueItem.isTrack()) {
                PlayableQueueItem playableQueueItem = (PlayableQueueItem) playQueueItem;
                if (!playableQueueItem.isPlayed() && playableQueueItem.isBucket(Bucket.AUTO_PLAY)) {
                    setPositionInternal(playQueue.indexOfPlayQueueItem(playQueueItem), true);
                    return;
                }
            }
        }
    }

    public boolean autoMoveToNextPlayableItem() {
        switch (repeatMode) {
            case REPEAT_ONE:
                return repeatCurrentPlayableItemInternal();
            case REPEAT_ALL:
                return moveToNextOrFirstPlayableItemInternal(false);
            case REPEAT_NONE:
            default:
                return moveToNextPlayableItemInternal(false);
        }
    }

    private boolean moveToNextOrFirstPlayableItemInternal(boolean userTriggered) {
        if (isQueueEmpty()) {
            return false;
        } else if (getNextPlayableItem() != Consts.NOT_SET && canRepeatNext()) {
            return moveToNextPlayableItemInternal(userTriggered);
        } else if (currentPosition == 0) {
            return repeatCurrentPlayableItemInternal();
        } else if (isPlayableAtPosition(0)) {
            setPositionInternal(0, userTriggered);
            return true;
        } else {
            currentPosition = 0;
            return moveToNextPlayableItemInternal(userTriggered);
        }
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public void setAutoPlay(boolean isAutoPlay) {
        this.autoPlay = isAutoPlay;
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAutoPlayEnabled(getCollectionUrn()));
    }

    private boolean moveToNextPlayableItemInternal(boolean userTriggered) {
        if (hasNextItem()) {
            final int nextPlayableItem = getNextPlayableItem();
            final int newPosition = nextPlayableItem == Consts.NOT_SET
                                    ? currentPosition + 1 // next track
                                    : nextPlayableItem; // next playable track
            if (canAutoPlayNext()) {
                setPositionInternal(newPosition, userTriggered);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean canRepeatNext() {
        if (getNextPlayQueueItem().isTrack()) {
            PlayableQueueItem playableQueueItem = (PlayableQueueItem) getNextPlayQueueItem();
            return !playableQueueItem.isBucket(Bucket.AUTO_PLAY) || playableQueueItem.isPlayed();
        }
        return true;
    }

    private boolean canAutoPlayNext() {
        if (getNextPlayQueueItem().isTrack()) {
            PlayableQueueItem playableQueueItem = (PlayableQueueItem) getNextPlayQueueItem();
            return !playableQueueItem.isBucket(Bucket.AUTO_PLAY) || autoPlay || playableQueueItem.isPlayed();
        }
        return true;
    }

    private boolean repeatCurrentPlayableItemInternal() {
        currentItemIsUserTriggered = false;
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionRepeat(getCurrentPlayQueueItem(),
                                                                      getCollectionUrn(),
                                                                      getCurrentPosition()));
        return true;
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
        return isNotBlockedTrackOrAd(playQueueItem) &&
                (networkConnectionHelper.isNetworkConnected() || isOfflineAvailable(playQueueItem));
    }

    private boolean isNotBlockedTrackOrAd(PlayQueueItem playQueueItem) {
        return playQueueItem.isAd() || (playQueueItem.isTrack() && !((TrackQueueItem) playQueueItem).isBlocked());
    }

    private boolean isOfflineAvailable(PlayQueueItem playQueueItem) {
        return offlineStateProvider.getOfflineState(playQueueItem.getUrn()) == OfflineState.DOWNLOADED;
    }

    boolean moveToPreviousPlayableItem() {
        if (hasPreviousItem()) {
            final int previousPlayable = getPreviousPlayableItem();
            final int newPosition = previousPlayable == Consts.NOT_SET
                                    ? 0 // first track
                                    : previousPlayable; // next playable track
            setPositionInternal(newPosition, true);
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

        if (!playSessionSource.equals(this.playSessionSource)) {
            this.repeatMode = RepeatMode.REPEAT_NONE;
            this.autoPlay = true;
        }
        this.playQueue = checkNotNull(playQueue, "Playqueue to update should not be null");
        this.currentItemIsUserTriggered = true;
        this.playSessionSource = playSessionSource;
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

    Observable<PlayQueue> loadPlayQueueAsync() {
        assertOnUiThread(UI_ASSERTION_MESSAGE);

        return playQueueOperations.getLastStoredPlayQueue()
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .doOnNext(onPlayQueueLoaded);
    }

    private void publishPositionUpdate() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(getCurrentPlayQueueItem(),
                                                                       getCollectionUrn(),
                                                                       getCurrentPosition()));
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

        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(playSessionSource.getOriginScreen(),
                                                                    currentItemIsUserTriggered);

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
            Urn queryUrn = currentPlayQueueItem.isTrack() ?
                           ((TrackQueueItem) currentPlayQueueItem).getQueryUrn() :
                           Urn.NOT_SET;
            trackSourceInfo.setStationSourceInfo(
                    playSessionSource.getCollectionUrn(),
                    StationsSourceInfo.create(queryUrn)
            );
        }

        if (playSessionSource.hasQuerySourceInfo()) {
            final QuerySourceInfo sourceInfo = playSessionSource.getQuerySourceInfo();
            if (sourceInfo != null) {
                trackSourceInfo.setQuerySourceInfo(sourceInfo);
            }
        }

        final Urn collectionUrn = playSessionSource.getCollectionUrn();
        if (collectionUrn.isPlaylist()) {
            trackSourceInfo.setOriginPlaylist(collectionUrn,
                                              getCurrentPosition(),
                                              playSessionSource.getCollectionOwnerUrn());
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

    boolean isCurrentCollectionOrRecommendation(Urn collection) {
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
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));
        publishCurrentQueueItemChanged();

    }

    @VisibleForTesting
    void removeAds() {
        removeAds(PlayQueueEvent.fromQueueUpdate(getCollectionUrn()));
    }

    public void removeAds(PlayQueueEvent updateEvent) {
        boolean queueUpdated = false;
        int i = 0;
        for (final Iterator<PlayQueueItem> iterator = playQueue.iterator(); iterator.hasNext(); ) {
            final PlayQueueItem item = iterator.next();
            if (AdUtils.IS_PLAYER_AD_ITEM.apply(item)) {
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
            if (!AdUtils.IS_PLAYER_AD_ITEM.apply(playQueueItem)) {
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

    private void saveQueue(PlayQueueEvent event) {
        if (playQueue.hasItems()) {
            saveQueueAndPublishEvent(event);
        }
    }

    private void publishCurrentQueueItemChanged() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(getCurrentPlayQueueItem(),
                                                                getCollectionUrn(),
                                                                getCurrentPosition()));
    }

    public void removeUpcomingItem(PlayQueueItem item, boolean shouldPublishQueueChange) {
        final int indexToRemove = playQueue.indexOfPlayQueueItem(item);
        if (indexToRemove > currentPosition) {
            playQueue.removeItemAtPosition(indexToRemove);
            if (shouldPublishQueueChange) {
                saveQueueForUpdate();
            }
        }
    }

    public void insertItemAtPosition(int position, PlayQueueItem item) {
        playQueue.insertPlayQueueItem(position, item);
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueInsert(getCollectionUrn()));
    }

    public void removeItem(PlayQueueItem item) {
        playQueue.removeItem(item);
        saveQueue(PlayQueueEvent.fromQueueUpdateRemoved(getCollectionUrn()));
    }

    public void moveItem(int fromPosition, int toPosition) {
        playQueue.moveItem(fromPosition, toPosition);
        saveQueue(PlayQueueEvent.fromQueueUpdateMoved(getCollectionUrn()));
    }

    List<PlayQueueItem> getExplicitQueueItems() {
        List<PlayQueueItem> explicitPlayQueueItems = new ArrayList<>();
        for (PlayQueueItem playQueueItem : playQueue) {
            if (playQueueItem.isPlayable()) {
                PlayableQueueItem playableQueueItem = (PlayableQueueItem) playQueueItem;
                if (playableQueueItem.isBucket(Bucket.EXPLICIT)) {
                    explicitPlayQueueItems.add(playQueueItem);
                }
            }
        }
        return explicitPlayQueueItems;
    }

    private void saveQueueForUpdate() {
        saveQueue(PlayQueueEvent.fromQueueUpdate(getCollectionUrn()));
    }

    private void saveQueueAndPublishEvent(PlayQueueEvent event) {
        playQueueOperations.saveQueue(playQueue.copy())
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe(new SaveQueueSubscriber(event));
    }

    private class SaveQueueSubscriber extends DefaultSubscriber<TxnResult> {
        private final PlayQueueEvent event;

        SaveQueueSubscriber(PlayQueueEvent event) {
            this.event = event;
        }

        @Override
        public void onNext(TxnResult ignored) {
            eventBus.publish(EventQueue.PLAY_QUEUE, event);
        }
    }
}
