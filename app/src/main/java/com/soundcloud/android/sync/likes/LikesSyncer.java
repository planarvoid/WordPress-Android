package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.PropertySetComparator;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.PropertySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public class LikesSyncer<ApiModel> implements Callable<Boolean> {

    private static final Comparator<PropertySet> LIKES_COMPARATOR = new PropertySetComparator<>(LikeProperty.TARGET_URN);

    private final FetchLikesCommand fetchLikes;
    private final BulkFetchCommand<ApiModel> fetchLikedResources;
    private final PushLikeAdditionsCommand pushLikeAdditions;
    private final PushLikeDeletionsCommand pushLikeDeletions;
    private final LoadLikesCommand loadLikes;
    private final LoadLikesPendingAdditionCommand loadLikesPendingAddition;
    private final LoadLikesPendingRemovalCommand loadLikesPendingRemoval;
    private final StoreCommand<Iterable<ApiModel>> storeLikedResources;
    private final StoreLikesCommand storeLikes;
    private final ApiEndpoints apiEndpoints;
    private final RemoveLikesCommand removeLikes;

    @SuppressWarnings("PMD.ExcessiveParameterList") // We will run into this a lot with commands...
    LikesSyncer(FetchLikesCommand fetchLikes,
                BulkFetchCommand<ApiModel> fetchLikedResources,
                PushLikeAdditionsCommand pushLikeAdditions,
                PushLikeDeletionsCommand pushLikeDeletions,
                LoadLikesCommand loadLikes,
                LoadLikesPendingAdditionCommand loadLikesPendingAddition,
                LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
                StoreCommand<Iterable<ApiModel>> storeLikedResources,
                StoreLikesCommand storeLikes,
                RemoveLikesCommand removeLikes,
                ApiEndpoints apiEndpoints) {
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
        this.apiEndpoints = apiEndpoints;
    }

    @Override
    public Boolean call() throws Exception {
        final NavigableSet<PropertySet> remoteLikes = fetchLikes
                .with(apiEndpoints).call();

        final Set<PropertySet> localLikes = new TreeSet<>(LIKES_COMPARATOR);
        localLikes.addAll(loadLikes.call());
        final Set<PropertySet> localAdditions = new TreeSet<>(LIKES_COMPARATOR);
        localAdditions.addAll(loadLikesPendingAddition.call());

        final Set<PropertySet> localRemovals = new TreeSet<>(LIKES_COMPARATOR);
        localRemovals.addAll(loadLikesPendingRemoval.call());

        final Set<PropertySet> pendingRemoteAdditions = getSetDifference(localAdditions, remoteLikes);
        final Set<PropertySet> pendingLocalAdditions = getSetDifference(remoteLikes, localLikes, localRemovals);

        final Set<PropertySet> localLikesWithoutAdditions = getSetDifference(localLikes, localAdditions);
        // clean items that no longer exist remotely
        final Set<PropertySet> pendingLocalRemovals = getSetDifference(localLikesWithoutAdditions, remoteLikes);
        // dirty local removals that do not need removing remotely
        pendingLocalRemovals.addAll(getSetDifference(localRemovals, remoteLikes));

        // For likes that are flagged for removal locally, but that have actually been re-added remotely, we have
        // we have to switch them from the remote removals set over to the local additions set.
        final Set<PropertySet> pendingRemoteRemovals = getSetIntersection(localRemovals, remoteLikes);
        for (PropertySet candidate : pendingRemoteRemovals) {
            final Date localRemovalDate = candidate.get(LikeProperty.REMOVED_AT);
            final PropertySet remoteLike = remoteLikes.tailSet(candidate).first();
            final Date remoteCreationDate = remoteLike.get(LikeProperty.CREATED_AT);
            if (remoteCreationDate.after(localRemovalDate)) {
                pendingRemoteRemovals.remove(candidate);
                pendingLocalAdditions.add(remoteLike);
            }
        }

        pushPendingAdditionsToApi(pendingRemoteAdditions, pendingLocalAdditions);
        pushPendingRemovalsToApi(pendingRemoteRemovals, pendingLocalRemovals);
        writePendingRemovalsToLocalStorage(pendingLocalRemovals);

        fetchAndWriteNewLikedEntities(pendingLocalAdditions);
        writePendingAdditionsToLocalStorage(pendingLocalAdditions);

        return !(pendingLocalAdditions.isEmpty() && pendingLocalRemovals.isEmpty());
    }

    private void fetchAndWriteNewLikedEntities(Set<PropertySet> pendingLocalAdditions) throws Exception {
        if (!pendingLocalAdditions.isEmpty()) {
            final ArrayList<Urn> urns = new ArrayList<>(pendingLocalAdditions.size());
            for (PropertySet like : pendingLocalAdditions) {
                urns.add(like.get(LikeProperty.TARGET_URN));
            }
            fetchLikedResources.with(urns).andThen(storeLikedResources).call();
        }
    }

    @SafeVarargs
    private final Set<PropertySet> getSetDifference(Set<PropertySet> set, Set<PropertySet>... without) {
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

    private void pushPendingAdditionsToApi(Set<PropertySet> pendingRemoteAdditions, Set<PropertySet> pendingLocalAdditions) throws Exception {
        if (!pendingRemoteAdditions.isEmpty()) {
            final Collection<PropertySet> successfulAdditions = pushLikeAdditions.with(pendingRemoteAdditions).call();
            // make sure we replace the existing local records with server side data
            pendingLocalAdditions.addAll(successfulAdditions);
        }
    }

    private void pushPendingRemovalsToApi(Set<PropertySet> pendingRemoteRemovals, Set<PropertySet> pendingLocalRemovals) throws Exception {
        if (!pendingRemoteRemovals.isEmpty()) {
            final Collection<PropertySet> successfulDeletions = pushLikeDeletions.with(pendingRemoteRemovals).call();
            // make sure we also remove successful remote deletions from local storage
            pendingLocalRemovals.addAll(successfulDeletions);
        }
    }

    private void writePendingAdditionsToLocalStorage(Set<PropertySet> pendingLocalAdditions) throws PropellerWriteException {
        if (!pendingLocalAdditions.isEmpty()) {
            storeLikes.with(pendingLocalAdditions).call();
        }
    }

    private void writePendingRemovalsToLocalStorage(Set<PropertySet> pendingLocalRemovals) throws PropellerWriteException {
        if (!pendingLocalRemovals.isEmpty()) {
            removeLikes.with(pendingLocalRemovals).call();
        }
    }
}
