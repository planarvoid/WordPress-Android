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
import java.util.concurrent.TimeUnit;

class LoadOfflineContentUpdatesCommand extends Command<Collection<DownloadRequest>, OfflineContentUpdates> {

    private static final long PENDING_REMOVAL_DELAY = TimeUnit.MINUTES.toMillis(3);

    private final PropellerDatabase propellerDatabase;
    private final DateProvider dateProvider;

    private final Function<DownloadRequest, Urn> TO_URN = new Function<DownloadRequest, Urn>() {
        @Override
        public Urn apply(DownloadRequest request) {
            return request.getTrack();
        }
    };

    private final Predicate<DownloadRequest> IS_SYNCABLE = new Predicate<DownloadRequest>() {
        @Override
        public boolean apply(DownloadRequest input) {
            return input.isSyncable();
        }
    };

    private final Predicate<DownloadRequest> IS_NOT_SYNCABLE = new Predicate<DownloadRequest>() {
        @Override
        public boolean apply(DownloadRequest input) {
            return !input.isSyncable();
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
    public OfflineContentUpdates call(final Collection<DownloadRequest> userExpectedContent) {
        final List<DownloadRequest> downloadable = newArrayList(MoreCollections.filter(userExpectedContent, IS_SYNCABLE));
        final Collection<DownloadRequest> creatorOptOut = MoreCollections.filter(userExpectedContent, IS_NOT_SYNCABLE);
        final Collection<Urn> downloadableUrns = MoreCollections.transform(downloadable, TO_URN);

        final List<Urn> requested = getDownloadRequests();
        final List<Urn> downloaded = getDownloaded();
        final List<Urn> pendingRemovals = getPendingRemovals();
        final List<Urn> previousUnavailable = getMarkedAsUnavailable();

        final Collection<DownloadRequest> tracksToRestore = getTracksToRestore(downloadable, pendingRemovals);
        final Collection<DownloadRequest> newPendingDownloads = getNewPendingDownloads(downloadable, requested, downloaded, tracksToRestore);
        final Collection<DownloadRequest> allDownloadRequests = getAllDownloadRequests(downloadable, pendingRemovals, tracksToRestore, downloaded);
        final List<Urn> newPendingRemovals = getNewPendingRemovals(userExpectedContent, downloaded, previousUnavailable, requested);

        return new OfflineContentUpdates(
                newArrayList(allDownloadRequests),
                newArrayList(newPendingDownloads),
                newArrayList(tracksToRestore),
                newArrayList(creatorOptOut),
                newPendingRemovals
        );
    }

    private List<Urn> getMarkedAsUnavailable() {
        Query query = Query
                .from(TrackDownloads.TABLE)
                .whereNotNull(TrackDownloads.UNAVAILABLE_AT);

        return propellerDatabase.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> getDownloadRequests() {
        final Where isPendingDownloads = filter()
                .whereNull(REMOVED_AT)
                .whereNull(DOWNLOADED_AT)
                .whereNotNull(REQUESTED_AT);

        return propellerDatabase.query(Query.from(TrackDownloads.TABLE)
                .where(isPendingDownloads))
                .toList(new TrackUrnMapper());
    }

    private List<Urn> getDownloaded() {
        final Query query = Query.from(TrackDownloads.TABLE)
                .whereNotNull(DOWNLOADED_AT)
                .whereNull(REMOVED_AT);
        return propellerDatabase.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> getPendingRemovals() {
        final long pendingRemovalThreshold = dateProvider.getCurrentDate().getTime() - PENDING_REMOVAL_DELAY;

        final Query query = Query
                .from(TrackDownloads.TABLE)
                .whereNotNull(DOWNLOADED_AT)
                .whereGt(REMOVED_AT, pendingRemovalThreshold);

        return propellerDatabase.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> getNewPendingRemovals(Collection<DownloadRequest> expectedContent, List<Urn> downloaded,
                                            Collection<Urn> unavailable, Collection<Urn> requested) {
        Collection<Urn> expectedTracks = MoreCollections.transform(expectedContent, TO_URN);
        return newArrayList(subtract(add(downloaded, requested, unavailable), expectedTracks));
    }

    private Collection<DownloadRequest> getTracksToRestore(Collection<DownloadRequest> expectedContent,
                                                           final List<Urn> pendingRemovals) {
        return MoreCollections.filter(expectedContent, new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest request) {
                return pendingRemovals.contains(request.getTrack());
            }
        });
    }

    private Collection<DownloadRequest> getNewPendingDownloads(Collection<DownloadRequest> expectedContent,
                                                               final List<Urn> pendingDownloads,
                                                               final List<Urn> downloadedTracks,
                                                               final Collection<DownloadRequest> tracksToRestore) {
        return MoreCollections.filter(expectedContent, new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest request) {
                return !pendingDownloads.contains(request.getTrack()) &&
                        !downloadedTracks.contains(request.getTrack()) &&
                        !tracksToRestore.contains(request);
            }
        });
    }

    private Collection<DownloadRequest> getAllDownloadRequests(Collection<DownloadRequest> expectedRequests,
                                                               final List<Urn> downloadedTracks,
                                                               final Collection<DownloadRequest> tracksToRestore,
                                                               final List<Urn> downloadedContent) {
        return MoreCollections.filter(expectedRequests, new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest request) {
                return !downloadedTracks.contains(request.getTrack()) &&
                        !tracksToRestore.contains(request) &&
                        !downloadedContent.contains(request.getTrack());
            }
        });
    }
}
