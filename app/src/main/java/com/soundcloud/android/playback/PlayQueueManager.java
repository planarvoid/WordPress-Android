package com.soundcloud.android.playback;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
public class PlayQueueManager implements OriginProvider {

    public static final String PLAYQUEUE_CHANGED_ACTION = "com.soundcloud.android.playlistchanged";
    private static final String UI_ASSERTION_MESSAGE = "Play queues must be set from the main thread only.";
    private static final String TAG = "PlayQueueManager";

    private final Context context;

    private final PlayQueueOperations playQueueOperations;
    private final PolicyOperations policyOperations;
    private final EventBus eventBus;
    private int currentPosition;
    private boolean currentTrackIsUserTriggered;

    private PlayQueue playQueue = PlayQueue.empty();
    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
    private Subscription playQueueSubscription = RxUtils.invalidSubscription();
    private PlaybackProgressInfo playbackProgressInfo;

    @Inject
    public PlayQueueManager(Context context,
                            PlayQueueOperations playQueueOperations,
                            EventBus eventBus,
                            PolicyOperations policyOperations) {
        this.context = context;
        this.playQueueOperations = playQueueOperations;
        this.eventBus = eventBus;
        this.policyOperations = policyOperations;
    }

    public void setNewPlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        setNewPlayQueue(playQueue, playSessionSource, 0);
    }

    public void setNewPlayQueue(PlayQueue playQueue, PlaySessionSource playSessionSource, int startPosition) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);

        if (this.playQueue.equals(playQueue) && this.playSessionSource.equals(playSessionSource)) {
            this.currentPosition = startPosition;
            eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(getCurrentTrackUrn(), getCurrentMetaData()));
        } else {
            currentPosition = startPosition;
            setNewPlayQueueInternal(playQueue, playSessionSource);
        }
        saveQueue();
        saveCurrentProgress(0L);

        fireAndForget(policyOperations.updatePolicies(playQueue.getTrackUrns()));
    }

    public void appendUniquePlayQueueItems(Iterable<PlayQueueItem> playQueueItems) {
        final List<Urn> trackUrns = this.playQueue.getTrackUrns();
        for (PlayQueueItem playQueueItem : playQueueItems){
            if (!trackUrns.contains(playQueueItem.getTrackUrn())){
                this.playQueue.addPlayQueueItem(playQueueItem);
            }
        }
        publishQueueUpdate();
        saveQueue();
    }

    @Deprecated // use URNs instead
    public long getCurrentTrackId() {
        return playQueue.getTrackId(currentPosition);
    }

    public Urn getCurrentTrackUrn() {
        return playQueue.getUrn(currentPosition);
    }

    public Urn getLastTrackUrn() {
        return playQueue.getUrn(playQueue.size() - 1);
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
        return playQueue.isEmpty();
    }

    public int getQueueSize() {
        return playQueue.size();
    }

    public Urn getUrnAtPosition(int position) {
        return playQueue.getUrn(position);
    }

    public int getPositionForUrn(final Urn trackUrn) {
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
            currentTrackIsUserTriggered = true;
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

    public Urn getNextTrackUrn() {
        return hasNextTrack() ? getUrnAtPosition(getNextPosition()) : Urn.NOT_SET;
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

    public List<Urn> getCurrentQueueAsUrnList() {
        return playQueue.getTrackUrns();
    }

    public boolean hasSameTrackList(List<Urn> remoteTrackList) {
        return playQueue.getTrackUrns().equals(remoteTrackList);
    }

    public PropertySet getCurrentMetaData() {
        return playQueue.getMetaData(getCurrentPosition());
    }

    public PropertySet getMetaDataAt(int position) {
        return playQueue.getMetaData(position);
    }

    private void setNewPlayQueueInternal(PlayQueue playQueue, PlaySessionSource playSessionSource) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        playQueueSubscription.unsubscribe();

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
        int adjustedPosition = currentPosition;
        for (int i = 0; i < currentPosition; i++) {
            if (!playQueue.shouldPersistTrackAt(i)) {
                adjustedPosition--;
            }
        }
        return adjustedPosition;
    }

    private long getProgressToBeSaved(long currentTrackProgress) {
        // we will always have a next track when playing an ad. Start at the beginning of that.
        return playQueue.shouldPersistTrackAt(currentPosition) ? currentTrackProgress : 0;
    }

    public void loadPlayQueueAsync() {
        loadPlayQueueAsync(false);
    }

    public void loadPlayQueueAsync(final boolean showPlayerAfterLoad) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);

        Observable<PlayQueue> playQueueObservable = playQueueOperations.getLastStoredPlayQueue();
        if (playQueueObservable != null) {
            final long lastStoredPlayingTrackId = playQueueOperations.getLastStoredPlayingTrackId();
            playQueueSubscription = playQueueObservable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DefaultSubscriber<PlayQueue>() {
                        @Override
                        public void onNext(PlayQueue savedQueue) {
                            if (!savedQueue.isEmpty()) {
                                currentPosition = playQueueOperations.getLastStoredPlayPosition();
                                setNewPlayQueueInternal(savedQueue, playQueueOperations.getLastStoredPlaySessionSource());
                                if (showPlayerAfterLoad) {
                                    eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
                                }
                            } else {
                                Log.e(TAG, "Not setting empty playqueue on reload, last played id : " + lastStoredPlayingTrackId);
                            }
                        }
                    });
            // return so player can have the resume information while load is in progress
            playbackProgressInfo = new PlaybackProgressInfo(lastStoredPlayingTrackId, playQueueOperations.getLastStoredSeekPosition());
        }
    }

    private void publishPositionUpdate() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(getCurrentTrackUrn(), getCurrentMetaData()));
    }

    @Nullable
    public TrackSourceInfo getCurrentTrackSourceInfo() {
        if (playQueue.isEmpty()) {
            return null;
        }

        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(playSessionSource.getOriginScreen(), currentTrackIsUserTriggered);
        trackSourceInfo.setSource(getCurrentTrackSource(), getCurrentTrackSourceVersion());

        if (playSessionSource.isFromQuery()) {
            trackSourceInfo.setSearchQuerySourceInfo(playSessionSource.getSearchQuerySourceInfo());
        }

        if (playSessionSource.isFromPlaylist()) {
            trackSourceInfo.setOriginPlaylist(playSessionSource.getPlaylistUrn(), getCurrentPosition(), playSessionSource.getPlaylistOwnerUrn());
        }
        return trackSourceInfo;
    }

    public PlaySessionSource getCurrentPlaySessionSource() {
        return playSessionSource;
    }

    private String getCurrentTrackSource() {
        return playQueue.getTrackSource(currentPosition);
    }

    private String getCurrentTrackSourceVersion() {
        return playQueue.getSourceVersion(currentPosition);
    }

    public Urn getPlaylistUrn() {
        return playSessionSource.getPlaylistUrn();
    }

    public boolean isPlaylist() {
        return getPlaylistUrn() != Urn.NOT_SET;
    }

    public boolean isCurrentPlaylist(Urn playlistUrn) {
        return getPlaylistUrn().equals(playlistUrn) && ScTextUtils.isBlank(getCurrentTrackSource());
    }

    @Override
    public String getScreenTag() {
        return playSessionSource.getOriginScreen();
    }

    public boolean shouldReloadQueue() {
        return playQueue.isEmpty() && playQueueSubscription == RxUtils.invalidSubscription();
    }

    public void clearAll() {
        assertOnUiThread(UI_ASSERTION_MESSAGE);

        playQueueOperations.clear();
        playQueue = PlayQueue.empty();
        playSessionSource = PlaySessionSource.EMPTY;
        clearCurrentPlayingTrack();
    }

    private void clearCurrentPlayingTrack() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(Urn.NOT_SET));
    }

    public void performPlayQueueUpdateOperations(QueueUpdateOperation... operations) {
        assertOnUiThread(UI_ASSERTION_MESSAGE);
        for (QueueUpdateOperation operation : operations) {
            operation.execute(playQueue);
        }
        publishQueueUpdate();
    }

    @VisibleForTesting
    public void removeTracksWithMetaData(Predicate<PropertySet> predicate) {
        removeTracksWithMetaData(predicate, PlayQueueEvent.fromQueueUpdate());
    }

    public void removeTracksWithMetaData(Predicate<PropertySet> predicate, PlayQueueEvent updateEvent) {
        boolean queueUpdated = false;
        int i = 0;
        for (final Iterator<PlayQueueItem> iterator = playQueue.iterator(); iterator.hasNext(); ) {
            final PlayQueueItem item = iterator.next();
            if (predicate.apply(item.getMetaData())) {
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

    public List<Urn> filterTrackUrnsWithMetadata(Predicate<PropertySet> predicate) {
        List<Urn> trackUrns = new ArrayList<>();
        for (PlayQueueItem playQueueItem : playQueue) {
            if (predicate.apply(playQueueItem.getMetaData())) {
                trackUrns.add(playQueueItem.getTrackUrn());
            }
        }
        return trackUrns;
    }

    private void saveQueue() {
        if (playQueue.hasItems()) {
            playQueueOperations.saveQueue(playQueue.copy());
        }
    }

    private void publishQueueUpdate() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());
    }

    private void broadcastNewPlayQueue() {
        context.sendBroadcast(new Intent(PLAYQUEUE_CHANGED_ACTION));

        final Urn currentTrackUrn = getCurrentTrackUrn();
        if (!Urn.NOT_SET.equals(currentTrackUrn)) {
            eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue());
            eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(currentTrackUrn, getCurrentMetaData()));
        }
    }

    public interface QueueUpdateOperation {
        void execute(PlayQueue playQueue);
    }

    public static class InsertOperation implements QueueUpdateOperation {

        private final int position;
        private final Urn trackUrn;
        private final PropertySet metaData;
        private final boolean shouldPersist;

        public InsertOperation(int position, Urn trackUrn, PropertySet metaData, boolean shouldPersist) {
            this.position = position;
            this.trackUrn = trackUrn;
            this.metaData = metaData;
            this.shouldPersist = shouldPersist;
        }

        @Override
        public void execute(PlayQueue playQueue) {
            playQueue.insertTrack(position, trackUrn, metaData, shouldPersist);
        }
    }

    public static class MergeMetadataOperation implements QueueUpdateOperation {

        private final int position;
        private final PropertySet metadata;

        public MergeMetadataOperation(int position, PropertySet metadata) {
            this.position = position;
            this.metadata = metadata;
        }

        @Override
        public void execute(PlayQueue playQueue) {
            playQueue.mergeMetaData(position, metadata);
        }
    }
}
