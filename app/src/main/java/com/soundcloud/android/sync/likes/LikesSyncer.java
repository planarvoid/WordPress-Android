package com.soundcloud.android.sync.likes;

import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.rx.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public class LikesSyncer<ApiModel> implements Callable<Boolean> {

    private static final Comparator<LikeRecord> LIKES_COMPARATOR = FetchLikesCommand.LIKES_COMPARATOR;

    private final FetchLikesCommand fetchLikes;
    private final BulkFetchCommand<ApiModel> fetchLikedResources;
    private final PushLikesCommand<ApiLike> pushLikeAdditions;
    private final PushLikesCommand<ApiDeletedLike> pushLikeDeletions;
    private final LoadLikesCommand loadLikes;
    private final LoadLikesPendingAdditionCommand loadLikesPendingAddition;
    private final LoadLikesPendingRemovalCommand loadLikesPendingRemoval;
    private final DefaultWriteStorageCommand<Iterable<ApiModel>, WriteResult> storeLikedResources;
    private final StoreLikesCommand storeLikes;
    private final RemoveLikesCommand removeLikes;
    private final EventBus eventBus;
    private final int soundType;

    @SuppressWarnings("PMD.ExcessiveParameterList")
    LikesSyncer(FetchLikesCommand fetchLikes,
                BulkFetchCommand<ApiModel> fetchLikedResources,
                PushLikesCommand<ApiLike> pushLikeAdditions,
                PushLikesCommand<ApiDeletedLike> pushLikeDeletions,
                LoadLikesCommand loadLikes,
                LoadLikesPendingAdditionCommand loadLikesPendingAddition,
                LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
                DefaultWriteStorageCommand storeLikedResources,
                StoreLikesCommand storeLikes,
                RemoveLikesCommand removeLikes,
                EventBus eventBus,
                int soundType) {
        this.fetchLikes = fetchLikes;
        this.pushLikeAdditions = pushLikeAdditions;
        this.pushLikeDeletions = pushLikeDeletions;
        this.loadLikes = loadLikes;
        this.loadLikesPendingAddition = loadLikesPendingAddition;
        this.loadLikesPendingRemoval = loadLikesPendingRemoval;
        this.removeLikes = removeLikes;
        this.fetchLikedResources = fetchLikedResources;
        this.storeLikedResources = storeLikedResources;
        this.storeLikes = storeLikes;
        this.eventBus = eventBus;
        this.soundType = soundType;
    }

    @Override
    public Boolean call() throws Exception {
        final NavigableSet<LikeRecord> remoteLikes = fetchLikes.call();

        final Set<LikeRecord> localLikes = new TreeSet<>(LIKES_COMPARATOR);
        localLikes.addAll(loadLikes.call(soundType));

        final Set<LikeRecord> localAdditions = new TreeSet<>(LIKES_COMPARATOR);
        localAdditions.addAll(loadLikesPendingAddition.call(soundType));

        final Set<LikeRecord> localRemovals = new TreeSet<>(LIKES_COMPARATOR);
        localRemovals.addAll(loadLikesPendingRemoval.call(soundType));

        final Set<LikeRecord> pendingRemoteAdditions = getSetDifference(localAdditions, remoteLikes);
        final Set<LikeRecord> newLocalAdditions = getSetDifference(remoteLikes, localLikes, localRemovals);

        final Set<LikeRecord> localLikesWithoutAdditions = getSetDifference(localLikes, localAdditions);
        // clean items that no longer exist remotely
        final Set<LikeRecord> newLocalRemovals = getSetDifference(localLikesWithoutAdditions, remoteLikes);

        final Set<LikeRecord> pendingLocalAdditions = new HashSet<>(newLocalAdditions);
        final Set<LikeRecord> pendingLocalRemovals = new HashSet<>(newLocalRemovals);

        // dirty local removals that do not need removing remotely
        pendingLocalRemovals.addAll(getSetDifference(localRemovals, remoteLikes));

        // Local removals, that still exist remotely
        final Set<LikeRecord> pendingRemoteRemovals = getSetIntersection(localRemovals, remoteLikes);

        pushPendingAdditionsToApi(pendingRemoteAdditions, pendingLocalAdditions);
        pushPendingRemovalsToApi(pendingRemoteRemovals, pendingLocalRemovals);
        writePendingRemovalsToLocalStorage(pendingLocalRemovals);

        fetchAndWriteNewLikedEntities(getSetDifference(pendingLocalAdditions, pendingRemoteAdditions));
        writePendingAdditionsToLocalStorage(pendingLocalAdditions);

        publishLikeChanged(newLocalAdditions, true);
        publishLikeChanged(newLocalRemovals, false);

        return !(pendingLocalAdditions.isEmpty() && pendingLocalRemovals.isEmpty());
    }

    private void publishLikeChanged(Set<LikeRecord> newlocalChanges, boolean isAddition) {
        final Map<Urn, LikesStatusEvent.LikeStatus> changedEntities = new HashMap<>(newlocalChanges.size());

        for (LikeRecord like : newlocalChanges) {
            final Urn urn = like.getTargetUrn();
            changedEntities.put(urn, LikesStatusEvent.LikeStatus.create(urn, isAddition));
        }

        if (!changedEntities.isEmpty()) {
            eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.createFromSync(changedEntities));
        }
    }

    private void fetchAndWriteNewLikedEntities(Set<LikeRecord> pendingLocalAdditions) throws Exception {
        if (!pendingLocalAdditions.isEmpty()) {
            final ArrayList<Urn> urns = new ArrayList<>(pendingLocalAdditions.size());
            for (LikeRecord like : pendingLocalAdditions) {
                urns.add(like.getTargetUrn());
            }
            final Collection<ApiModel> apiModels = fetchLikedResources.with(urns).call();
            storeLikedResources.call(apiModels);
        }
    }

    private Set<LikeRecord> getSetDifference(Set<LikeRecord> set, Set<LikeRecord>... without) {
        final Set<LikeRecord> difference = new TreeSet<>(LIKES_COMPARATOR);
        difference.addAll(set);
        for (Set<LikeRecord> s : without) {
            difference.removeAll(s);
        }
        return difference;
    }

    private Set<LikeRecord> getSetIntersection(Set<LikeRecord> set, Set<LikeRecord> toIntersectWith) {
        final Set<LikeRecord> intersection = new TreeSet<>(LIKES_COMPARATOR);
        intersection.addAll(set);
        intersection.retainAll(toIntersectWith);
        return intersection;
    }

    private void pushPendingAdditionsToApi(Set<LikeRecord> pendingRemoteAdditions,
                                           Set<LikeRecord> pendingLocalAdditions) throws Exception {
        if (!pendingRemoteAdditions.isEmpty()) {
            final Collection<ApiLike> successfulAdditions = pushLikeAdditions.with(pendingRemoteAdditions).call();
            // make sure we replace the existing local records with server side data
            pendingLocalAdditions.addAll(successfulAdditions);
        }
    }

    private void pushPendingRemovalsToApi(Set<LikeRecord> pendingRemoteRemovals,
                                          Set<LikeRecord> pendingLocalRemovals) throws Exception {
        if (!pendingRemoteRemovals.isEmpty()) {
            final Collection<ApiDeletedLike> successfulDeletions = pushLikeDeletions.with(pendingRemoteRemovals).call();
            // make sure we also remove successful remote deletions from local storage
            pendingLocalRemovals.addAll(successfulDeletions);
        }
    }

    private void writePendingAdditionsToLocalStorage(Set<LikeRecord> pendingLocalAdditions) throws PropellerWriteException {
        if (!pendingLocalAdditions.isEmpty()) {
            storeLikes.call(pendingLocalAdditions);
        }
    }

    private void writePendingRemovalsToLocalStorage(Set<LikeRecord> pendingLocalRemovals) throws PropellerWriteException {
        if (!pendingLocalRemovals.isEmpty()) {
            removeLikes.call(pendingLocalRemovals);
        }
    }
}
