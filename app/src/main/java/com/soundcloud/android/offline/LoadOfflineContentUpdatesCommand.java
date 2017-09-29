package com.soundcloud.android.offline;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.MoreCollections;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class LoadOfflineContentUpdatesCommand extends Command<ExpectedOfflineContent, OfflineContentUpdates> {

    private final TrackDownloadsStorage trackDownloadsStorage;

    private static <T> Collection<T> add(Collection<T> items, Collection<T>... collectionsToAdd) {
        final ArrayList<T> result = new ArrayList<>(items);
        for (Collection<T> itemsToAdd : collectionsToAdd) {
            result.addAll(itemsToAdd);
        }
        return result;
    }

    private static <T> Collection<T> subtract(Collection<T> items, Collection<T>... collectionsToSubtract) {
        final ArrayList<T> result = new ArrayList<>(items);
        for (Collection<T> itemsToSubtract : collectionsToSubtract) {
            result.removeAll(itemsToSubtract);
        }
        return result;
    }

    @Inject
    public LoadOfflineContentUpdatesCommand(TrackDownloadsStorage trackDownloadsStorage) {
        this.trackDownloadsStorage = trackDownloadsStorage;
    }

    @Override
    public OfflineContentUpdates call(final ExpectedOfflineContent userExpectedContent) {
        final Collection<DownloadRequest> expectedTracks = userExpectedContent.requests;
        final List<DownloadRequest> expectedTracksSyncable = newArrayList(MoreCollections.filter(expectedTracks, input -> input.isSyncable() && !input.isSnipped()));

        final List<Urn> actualRequestedTracks = getTrackDownloadRequests();
        final List<Urn> actualDownloadedTracks = getTracksDownloaded();
        final List<Urn> actualPendingRemovalsTracks = getTracksToRemove();
        final List<Urn> actualUnavailableTracks = getTracksMarkedAsUnavailable();

        final Collection<Urn> expectedTracksSyncableUrns = toUrns(expectedTracksSyncable);
        final Collection<Urn> tracksToRestore = getTracksToRestore(expectedTracksSyncableUrns, actualPendingRemovalsTracks);
        final Collection<Urn> newTracksToDownload = getNewPendingDownloads(expectedTracksSyncableUrns,
                                                                           actualRequestedTracks,
                                                                           actualDownloadedTracks,
                                                                           tracksToRestore);

        final Collection<Urn> unavailableTracks = toUrns(MoreCollections.filter(expectedTracks, input -> !input.isSyncable() || input.isSnipped()));
        final Collection<DownloadRequest> tracksToDownload = getAllDownloadRequests(expectedTracksSyncable,
                                                                                    actualPendingRemovalsTracks,
                                                                                    tracksToRestore,
                                                                                    actualDownloadedTracks);
        final List<Urn> tracksToRemove = getNewTrackPendingRemovals(expectedTracks,
                                                                    actualDownloadedTracks,
                                                                    actualUnavailableTracks,
                                                                    actualRequestedTracks);

        return OfflineContentUpdates.builder()
                                    .unavailableTracks(newArrayList(unavailableTracks))
                                    .tracksToDownload(newArrayList(tracksToDownload))
                                    .newTracksToDownload(newArrayList(newTracksToDownload))
                                    .tracksToRestore(newArrayList(tracksToRestore))
                                    .tracksToRemove(tracksToRemove)
                                    .userExpectedOfflineContent(userExpectedContent)
                                    .build();
    }

    private List<Urn> getTracksMarkedAsUnavailable() {
        return trackDownloadsStorage.getUnavailableTracks().blockingGet();
    }

    private List<Urn> getTrackDownloadRequests() {
        return trackDownloadsStorage.getRequestedTracks().blockingGet();
    }

    private List<Urn> getTracksDownloaded() {
        return trackDownloadsStorage.getDownloadedTracks().blockingGet();
    }

    private List<Urn> getTracksToRemove() {
        return trackDownloadsStorage.getTracksToRemove().blockingGet();
    }

    private List<Urn> getNewTrackPendingRemovals(Collection<DownloadRequest> expectedContent, List<Urn> downloaded,
                                                 Collection<Urn> unavailable, Collection<Urn> requested) {
        return newArrayList(subtract(add(downloaded, requested, unavailable), toUrns(expectedContent)));
    }

    private Collection<Urn> toUrns(Collection<DownloadRequest> expectedContent) {
        return MoreCollections.transform(expectedContent, DownloadRequest::getUrn);
    }

    private Collection<Urn> getTracksToRestore(Collection<Urn> expectedContent, final List<Urn> pendingRemovals) {
        return MoreCollections.filter(expectedContent, pendingRemovals::contains);
    }

    private Collection<Urn> getNewPendingDownloads(Collection<Urn> expectedContent,
                                                   final List<Urn> pendingDownloads,
                                                   final List<Urn> downloadedTracks,
                                                   final Collection<Urn> tracksToRestore) {
        return MoreCollections.filter(expectedContent, track -> !pendingDownloads.contains(track) &&
                !downloadedTracks.contains(track) &&
                !tracksToRestore.contains(track));
    }

    private Collection<DownloadRequest> getAllDownloadRequests(Collection<DownloadRequest> expectedRequests,
                                                               final List<Urn> downloadedTracks,
                                                               final Collection<Urn> tracksToRestore,
                                                               final List<Urn> downloadedContent) {
        return MoreCollections.filter(expectedRequests, request -> !downloadedTracks.contains(request.getUrn()) &&
                !tracksToRestore.contains(request.getUrn()) &&
                !downloadedContent.contains(request.getUrn()));
    }
}
