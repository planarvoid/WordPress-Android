package com.soundcloud.android.offline;

import static com.soundcloud.android.storage.Tables.TrackDownloads;
import static com.soundcloud.android.storage.Tables.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.REMOVED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.REQUESTED_AT;
import static com.soundcloud.android.utils.CollectionUtils.add;
import static com.soundcloud.android.utils.CollectionUtils.subtract;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

class LoadOfflineContentUpdatesCommand extends Command<Collection<DownloadRequest>, OfflineContentUpdates> {

    private static final long PENDING_REMOVAL_DELAY = TimeUnit.MINUTES.toMillis(3);

    private final PropellerDatabase propellerDatabase;
    private final DateProvider dateProvider;

    private final Function<DownloadRequest, Urn> toUrn = new Function<DownloadRequest, Urn>() {
        @Override
        public Urn apply(DownloadRequest request) {
            return request.track;
        }
    };

    @Inject
    public LoadOfflineContentUpdatesCommand(PropellerDatabase propellerDatabase, DateProvider dateProvider) {
        this.propellerDatabase = propellerDatabase;
        this.dateProvider = dateProvider;
    }

    @Override
    public OfflineContentUpdates call(final Collection<DownloadRequest> userExpectedContent) {
        final List<DownloadRequest> expectedRequests = newArrayList(MoreCollections.filter(userExpectedContent, downloadablePredicate(true)));
        final Collection<DownloadRequest> creatorOptOut = MoreCollections.filter(userExpectedContent, downloadablePredicate(false));

        final Collection<Urn> expectedTracks = MoreCollections.transform(expectedRequests, toUrn);
        final List<Urn> requested = getDownloadRequests();
        final List<Urn> downloaded = getDownloaded();
        final List<Urn> pendingRemovals = getPendingRemovals();
        final List<Urn> unavailable = getMarkedAsUnavailable();

        final Collection<DownloadRequest> tracksToRestore = getTracksToRestore(expectedRequests, pendingRemovals);
        final Collection<DownloadRequest> newPendingDownloads = getNewPendingDownloads(expectedRequests, requested, downloaded, tracksToRestore);
        final Collection<DownloadRequest> allDownloadRequests = getAllDownloadRequests(expectedRequests, pendingRemovals, tracksToRestore, downloaded);
        final List<Urn> newPendingRemovals = getNewPendingRemovals(expectedTracks, downloaded, unavailable, requested);

        return new OfflineContentUpdates(
                newArrayList(allDownloadRequests),
                newArrayList(newPendingDownloads),
                newArrayList(tracksToRestore),
                newArrayList(creatorOptOut),
                newPendingRemovals
        );
    }

    private static Predicate<DownloadRequest> downloadablePredicate(final boolean isDownloadable) {
        return new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest input) {
                return input.downloadable == isDownloadable;
            }
        };
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

    private List<Urn> getNewPendingRemovals(Collection<Urn> expectedContent, List<Urn> downloaded,
                                            Collection<Urn> unavailable, Collection<Urn> requested) {
        return newArrayList(subtract(add(downloaded, requested, unavailable), expectedContent));
    }

    private Collection<DownloadRequest> getTracksToRestore(Collection<DownloadRequest> expectedContent,
                                                           final List<Urn> pendingRemovals) {
        return MoreCollections.filter(expectedContent, new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest request) {
                return pendingRemovals.contains(request.track);
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
                return !pendingDownloads.contains(request.track) &&
                        !downloadedTracks.contains(request.track) &&
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
                return !downloadedTracks.contains(request.track) &&
                        !tracksToRestore.contains(request) &&
                        !downloadedContent.contains(request.track);
            }
        });
    }
}
