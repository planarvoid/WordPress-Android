package com.soundcloud.android.offline;

import static com.soundcloud.android.storage.Tables.TrackDownloads;
import static com.soundcloud.android.storage.Tables.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.REMOVED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.REQUESTED_AT;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class LoadOfflineContentUpdatesCommand extends Command<ExpectedOfflineContent, OfflineContentUpdates> {

    private final PropellerDatabase propellerDatabase;
    private final DateProvider dateProvider;

    private final Function<DownloadRequest, Urn> TO_URN = new Function<DownloadRequest, Urn>() {
        @Override
        public Urn apply(DownloadRequest request) {
            return request.getUrn();
        }
    };

    private final Predicate<DownloadRequest> IS_SYNCABLE = new Predicate<DownloadRequest>() {
        @Override
        public boolean apply(DownloadRequest input) {
            return input.isSyncable() && !input.isSnipped();
        }
    };

    private final Predicate<DownloadRequest> IS_NOT_SYNCABLE = new Predicate<DownloadRequest>() {
        @Override
        public boolean apply(DownloadRequest input) {
            return !input.isSyncable() || input.isSnipped();
        }
    };

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
    public LoadOfflineContentUpdatesCommand(PropellerDatabase propellerDatabase, CurrentDateProvider dateProvider) {
        this.propellerDatabase = propellerDatabase;
        this.dateProvider = dateProvider;
    }

    @Override
    public OfflineContentUpdates call(final ExpectedOfflineContent userExpectedContent) {
        final Collection<DownloadRequest> expectedTracks = userExpectedContent.requests;
        final List<DownloadRequest> expectedTracksSyncable = newArrayList(MoreCollections.filter(expectedTracks,
                                                                                                 IS_SYNCABLE));

        final List<Urn> actualRequestedTracks = getTrackDownloadRequests();
        final List<Urn> actualDownloadedTracks = getTracksDownloaded();
        final List<Urn> actualPendingRemovalsTracks = getTrackPendingRemovals();
        final List<Urn> actualUnavailableTracks = getTracksMarkedAsUnavailable();

        final Collection<Urn> expectedTracksSyncableUrns = toUrns(expectedTracksSyncable);
        final Collection<Urn> tracksToRestore = getTracksToRestore(expectedTracksSyncableUrns,
                                                                   actualPendingRemovalsTracks);
        final Collection<Urn> newTracksToDownload = getNewPendingDownloads(expectedTracksSyncableUrns,
                                                                           actualRequestedTracks,
                                                                           actualDownloadedTracks,
                                                                           tracksToRestore);
        final Collection<Urn> unavailableTracks = toUrns(MoreCollections.filter(expectedTracks, IS_NOT_SYNCABLE));
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
        Query query = Query
                .from(TrackDownloads.TABLE)
                .whereNotNull(TrackDownloads.UNAVAILABLE_AT);

        return propellerDatabase.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> getTrackDownloadRequests() {
        final Where isPendingDownloads = filter()
                .whereNull(REMOVED_AT)
                .whereNull(DOWNLOADED_AT)
                .whereNotNull(REQUESTED_AT);

        return propellerDatabase.query(Query.from(TrackDownloads.TABLE)
                                            .where(isPendingDownloads))
                                .toList(new TrackUrnMapper());
    }

    private List<Urn> getTracksDownloaded() {
        final Query query = Query.from(TrackDownloads.TABLE)
                                 .whereNotNull(DOWNLOADED_AT)
                                 .whereNull(REMOVED_AT);
        return propellerDatabase.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> getTrackPendingRemovals() {
        final long pendingRemovalThreshold = dateProvider.getCurrentDate()
                                                         .getTime() - OfflineConstants.PENDING_REMOVAL_DELAY;

        final Query query = Query
                .from(TrackDownloads.TABLE)
                .whereNotNull(DOWNLOADED_AT)
                .whereGe(REMOVED_AT, pendingRemovalThreshold);

        return propellerDatabase.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> getNewTrackPendingRemovals(Collection<DownloadRequest> expectedContent, List<Urn> downloaded,
                                                 Collection<Urn> unavailable, Collection<Urn> requested) {
        return newArrayList(subtract(add(downloaded, requested, unavailable), toUrns(expectedContent)));
    }

    private Collection<Urn> toUrns(Collection<DownloadRequest> expectedContent) {
        return MoreCollections.transform(expectedContent, TO_URN);
    }

    private Collection<Urn> getTracksToRestore(Collection<Urn> expectedContent, final List<Urn> pendingRemovals) {
        return MoreCollections.filter(expectedContent, new Predicate<Urn>() {
            @Override
            public boolean apply(Urn track) {
                return pendingRemovals.contains(track);
            }
        });
    }

    private Collection<Urn> getNewPendingDownloads(Collection<Urn> expectedContent,
                                                   final List<Urn> pendingDownloads,
                                                   final List<Urn> downloadedTracks,
                                                   final Collection<Urn> tracksToRestore) {
        return MoreCollections.filter(expectedContent, new Predicate<Urn>() {
            @Override
            public boolean apply(Urn track) {
                return !pendingDownloads.contains(track) &&
                        !downloadedTracks.contains(track) &&
                        !tracksToRestore.contains(track);
            }
        });
    }

    private Collection<DownloadRequest> getAllDownloadRequests(Collection<DownloadRequest> expectedRequests,
                                                               final List<Urn> downloadedTracks,
                                                               final Collection<Urn> tracksToRestore,
                                                               final List<Urn> downloadedContent) {
        return MoreCollections.filter(expectedRequests, new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest request) {
                return !downloadedTracks.contains(request.getUrn()) &&
                        !tracksToRestore.contains(request.getUrn()) &&
                        !downloadedContent.contains(request.getUrn());
            }
        });
    }
}
