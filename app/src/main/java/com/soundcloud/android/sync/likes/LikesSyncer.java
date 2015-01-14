package com.soundcloud.android.sync.likes;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.ApiResourceCommand;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.likes.ApiLike;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.utils.PropertySetComparator;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public class LikesSyncer implements SyncStrategy {

    private static final Comparator<PropertySet> LIKES_COMPARATOR = new PropertySetComparator<>(LikeProperty.TARGET_URN);

    private final ApiClient apiClient;
    private final ApiResourceCommand fetchLikedResources;
    private final LoadLikesCommand loadLikes;
    private final LoadLikesPendingRemovalCommand loadLikesPendingRemoval;
    private final StoreCommand storeLikedResources;
    private final StoreLikesCommand storeLikes;
    private final RemoveLikesCommand removeLikes;
    private final AccountOperations accountOperations;
    private final ApiEndpoints fetchLikesEndpoint;
    private final ApiEndpoints writeLikesEndpoint;

    @Inject
    @SuppressWarnings("PMD.ExcessiveParameterList") // We will run into this a lot with commands...
    LikesSyncer(ApiClient apiClient, ApiResourceCommand fetchLikedResources, LoadLikesCommand loadLikes,
                LoadLikesPendingRemovalCommand loadLikesPendingRemoval, StoreCommand storeLikedResources,
                StoreLikesCommand storeLikes, RemoveLikesCommand removeLikes, AccountOperations accountOperations,
                ApiEndpoints fetchLikesEndpoint, ApiEndpoints writeLikesEndpoint) {
        this.apiClient = apiClient;
        this.loadLikes = loadLikes;
        this.loadLikesPendingRemoval = loadLikesPendingRemoval;
        this.removeLikes = removeLikes;
        this.fetchLikedResources = fetchLikedResources;
        this.storeLikedResources = storeLikedResources;
        this.storeLikes = storeLikes;
        this.accountOperations = accountOperations;
        this.fetchLikesEndpoint = fetchLikesEndpoint;
        this.writeLikesEndpoint = writeLikesEndpoint;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        final LikesSyncResult result = performSync();

        if (result.hasLocalAdditions()) {
            final ArrayList<Urn> urns = new ArrayList<>(result.localAdditions.size());
            for (PropertySet like : result.localAdditions) {
                urns.add(like.get(LikeProperty.TARGET_URN));
            }
            fetchLikedResources.with(urns).andThen(storeLikedResources).call();
        }

        return (result.hasChanged())
                ? ApiSyncResult.fromSuccessfulChange(uri)
                : ApiSyncResult.fromSuccessWithoutChange(uri);
    }

    private LikesSyncResult performSync() throws Exception {
        final NavigableSet<PropertySet> remoteLikes = fetchLikes(fetchLikesEndpoint);

        final Set<PropertySet> localLikes = new TreeSet<>(LIKES_COMPARATOR);
        localLikes.addAll(loadLikes.call());
        final Set<PropertySet> localRemovals = new TreeSet<>(LIKES_COMPARATOR);
        localRemovals.addAll(loadLikesPendingRemoval.call());

        final Set<PropertySet> pendingRemoteAdditions = getSetDifference(localLikes, remoteLikes);
        final Set<PropertySet> pendingLocalAdditions = getSetDifference(remoteLikes, localLikes, localRemovals);
        final Set<PropertySet> pendingLocalRemovals = getSetDifference(localRemovals, remoteLikes);

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

        pushPendingAdditionsToApi(pendingRemoteAdditions);
        pushPendingRemovalsToApi(pendingLocalRemovals, pendingRemoteRemovals);
        writePendingUpdatesToLocalStorage(pendingLocalAdditions, pendingLocalRemovals);

        return new LikesSyncResult(pendingLocalAdditions, pendingLocalRemovals);
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

    private void pushPendingAdditionsToApi(Set<PropertySet> pendingRemoteAdditions) {
        for (PropertySet like : pendingRemoteAdditions) {
            final String path = writeLikesEndpoint.path(like.get(LikeProperty.TARGET_URN).getNumericId());
            final ApiRequest request = ApiRequest.Builder.put(path).forPublicApi().build();
            // We're not checking the result here, relying on eventual consistency. Anything that fails pushing,
            // will simply be re-attempted during the next sync.
            // TODO: should we check for things like unexpected status codes?
            apiClient.fetchResponse(request);
        }
    }

    private void pushPendingRemovalsToApi(Set<PropertySet> pendingLocalRemovals,
                                          Set<PropertySet> pendingRemoteRemovals) {
        for (PropertySet like : pendingRemoteRemovals) {
            final String path = writeLikesEndpoint.path(like.get(LikeProperty.TARGET_URN).getNumericId());
            final ApiRequest request = ApiRequest.Builder.delete(path).forPublicApi().build();
            final ApiResponse response = apiClient.fetchResponse(request);
            // make sure we'll drop successful removals from the local database
            if (response.isSuccess()) {
                pendingLocalRemovals.add(like);
            }
        }
    }

    private void writePendingUpdatesToLocalStorage(Set<PropertySet> pendingLocalAdditions,
                                                   Set<PropertySet> pendingLocalRemovals) throws PropellerWriteException {
        // TODO: we should check whether these are succesful
        if (!pendingLocalRemovals.isEmpty()) {
            removeLikes.with(pendingLocalRemovals).call();
        }

        if (!pendingLocalAdditions.isEmpty()) {
            storeLikes.with(pendingLocalAdditions).call();
        }
    }

    private NavigableSet<PropertySet> fetchLikes(ApiEndpoints endpoint)
            throws ApiMapperException, IOException, ApiRequestException {
        final Urn userUrn = accountOperations.getLoggedInUserUrn();
        final ApiRequest<ModelCollection<ApiLike>> request =
                ApiRequest.Builder.<ModelCollection<ApiLike>>get(endpoint.path(userUrn))
                        .forPrivateApi(1)
                        .forResource(new TypeToken<ModelCollection<ApiLike>>() {
                        })
                        .build();
        final ModelCollection<ApiLike> apiLikes = apiClient.fetchMappedResponse(request);
        final NavigableSet<PropertySet> result = new TreeSet<>(LIKES_COMPARATOR);
        for (ApiLike like : apiLikes) {
            result.add(like.toPropertySet());
        }
        return result;
    }

    private static final class LikesSyncResult {
        private final Collection<PropertySet> localAdditions;
        private final Collection<PropertySet> localRemovals;

        private LikesSyncResult(Collection<PropertySet> localAdditions, Collection<PropertySet> localRemovals) {
            this.localAdditions = localAdditions;
            this.localRemovals = localRemovals;
        }

        boolean hasChanged() {
            return !(localAdditions.isEmpty() && localRemovals.isEmpty());
        }

        boolean hasLocalAdditions() {
            return !localAdditions.isEmpty();
        }
    }
}
