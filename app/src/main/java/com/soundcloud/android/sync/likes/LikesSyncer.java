package com.soundcloud.android.sync.likes;

import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.PropertySetComparator;
import com.soundcloud.java.collections.PropertySet;
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

    private static final Comparator<PropertySet> LIKES_COMPARATOR = new PropertySetComparator<>(LikeProperty.TARGET_URN);

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
        final NavigableSet<PropertySet> remoteLikes = fetchLikes.call();

        final Set<PropertySet> localLikes = new TreeSet<>(LIKES_COMPARATOR);
        localLikes.addAll(loadLikes.call(soundType));

        final Set<PropertySet> localAdditions = new TreeSet<>(LIKES_COMPARATOR);
        localAdditions.addAll(loadLikesPendingAddition.call(soundType));

        final Set<PropertySet> localRemovals = new TreeSet<>(LIKES_COMPARATOR);
        localRemovals.addAll(loadLikesPendingRemoval.call(soundType));

        final Set<PropertySet> pendingRemoteAdditions = getSetDifference(localAdditions, remoteLikes);
        final Set<PropertySet> newLocalAdditions = getSetDifference(remoteLikes, localLikes, localRemovals);

        final Set<PropertySet> localLikesWithoutAdditions = getSetDifference(localLikes, localAdditions);
        // clean items that no longer exist remotely
        final Set<PropertySet> newLocalRemovals = getSetDifference(localLikesWithoutAdditions, remoteLikes);

        final Set<PropertySet> pendingLocalAdditions = new HashSet<>(newLocalAdditions);
        final Set<PropertySet> pendingLocalRemovals = new HashSet<>(newLocalRemovals);

        // dirty local removals that do not need removing remotely
        pendingLocalRemovals.addAll(getSetDifference(localRemovals, remoteLikes));

        // Local removals, that still exist remotely
        final Set<PropertySet> pendingRemoteRemovals = getSetIntersection(localRemovals, remoteLikes);

        pushPendingAdditionsToApi(pendingRemoteAdditions, pendingLocalAdditions);
        pushPendingRemovalsToApi(pendingRemoteRemovals, pendingLocalRemovals);
        writePendingRemovalsToLocalStorage(pendingLocalRemovals);

        fetchAndWriteNewLikedEntities(getSetDifference(pendingLocalAdditions, pendingRemoteAdditions));
        writePendingAdditionsToLocalStorage(pendingLocalAdditions);

        publishLikeChanged(newLocalAdditions, true);
        publishLikeChanged(newLocalRemovals, false);

        return !(pendingLocalAdditions.isEmpty() && pendingLocalRemovals.isEmpty());
    }

    private void publishLikeChanged(Set<PropertySet> newlocalChanges, boolean isAddition) {
        final Map<Urn, LikesStatusEvent.LikeStatus> changedEntities = new HashMap<>(newlocalChanges.size());

        for (PropertySet like : newlocalChanges) {
            final Urn urn = like.get(PlayableProperty.URN);
            changedEntities.put(urn, LikesStatusEvent.LikeStatus.create(urn, isAddition));
        }

        if (!changedEntities.isEmpty()) {
            eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.createFromSync(changedEntities));
        }
    }

    private void fetchAndWriteNewLikedEntities(Set<PropertySet> pendingLocalAdditions) throws Exception {
        if (!pendingLocalAdditions.isEmpty()) {
            final ArrayList<Urn> urns = new ArrayList<>(pendingLocalAdditions.size());
            for (PropertySet like : pendingLocalAdditions) {
                urns.add(like.get(LikeProperty.TARGET_URN));
            }
            final Collection<ApiModel> apiModels = fetchLikedResources.with(urns).call();
            storeLikedResources.call(apiModels);
        }
    }

    private Set<PropertySet> getSetDifference(Set<PropertySet> set, Set<PropertySet>... without) {
        final Set<PropertySet> difference = new TreeSet<>(LIKES_COMPARATOR);
        difference.addAll(set);
        for (Set<PropertySet> s : without) {
            difference.removeAll(s);
        }
        return difference;
    }

    private Set<PropertySet> getSetIntersection(Set<PropertySet> set, Set<PropertySet> toIntersectWith) {
        final Set<PropertySet> intersection = new TreeSet<>(LIKES_COMPARATOR);
        intersection.addAll(set);
        intersection.retainAll(toIntersectWith);
        return intersection;
    }

    private void pushPendingAdditionsToApi(Set<PropertySet> pendingRemoteAdditions,
                                           Set<PropertySet> pendingLocalAdditions) throws Exception {
        if (!pendingRemoteAdditions.isEmpty()) {
            final Collection<PropertySet> successfulAdditions = pushLikeAdditions.with(pendingRemoteAdditions).call();
            // make sure we replace the existing local records with server side data
            pendingLocalAdditions.addAll(successfulAdditions);
        }
    }

    private void pushPendingRemovalsToApi(Set<PropertySet> pendingRemoteRemovals,
                                          Set<PropertySet> pendingLocalRemovals) throws Exception {
        if (!pendingRemoteRemovals.isEmpty()) {
            final Collection<PropertySet> successfulDeletions = pushLikeDeletions.with(pendingRemoteRemovals).call();
            // make sure we also remove successful remote deletions from local storage
            pendingLocalRemovals.addAll(successfulDeletions);
        }
    }

    private void writePendingAdditionsToLocalStorage(Set<PropertySet> pendingLocalAdditions) throws PropellerWriteException {
        if (!pendingLocalAdditions.isEmpty()) {
            storeLikes.call(pendingLocalAdditions);
        }
    }

    private void writePendingRemovalsToLocalStorage(Set<PropertySet> pendingLocalRemovals) throws PropellerWriteException {
        if (!pendingLocalRemovals.isEmpty()) {
            removeLikes.call(pendingLocalRemovals);
        }
    }
}
