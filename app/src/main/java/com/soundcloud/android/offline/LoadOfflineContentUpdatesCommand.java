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
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
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
        final Collection<DownloadRequest> creatorOptOut = updateCreatorOptOut(userExpectedContent);
        final List<DownloadRequest> expectedRequests = getExpectedRequest(userExpectedContent);

        final Collection<Urn> expectedTracks = MoreCollections.transform(expectedRequests, toUrn);
        final List<Urn> downloadRequests = getDownloadRequests();
        final List<Urn> downloadedContent = getDownloaded();
        final List<Urn> pendingRemovals = getPendingRemovals();

        final Collection<DownloadRequest> tracksToRestore = getTracksToRestore(expectedRequests, pendingRemovals);
        final Collection<DownloadRequest> newPendingDownloads = getNewPendingDownloads(expectedRequests, downloadRequests, downloadedContent, tracksToRestore);
        final Collection<DownloadRequest> allDownloadRequests = getAllDownloadRequests(expectedRequests, pendingRemovals, tracksToRestore, downloadedContent);
        final List<Urn> newPendingRemovals = getNewPendingRemovals(expectedTracks, downloadedContent, downloadRequests);

        return new OfflineContentUpdates(
                newArrayList(allDownloadRequests),
                newArrayList(newPendingDownloads),
                newArrayList(tracksToRestore),
                newArrayList(creatorOptOut),
                newPendingRemovals
        );
    }

    private List<DownloadRequest> getExpectedRequest(Collection<DownloadRequest> userExpectedContent) {
        return newArrayList(MoreCollections.filter(userExpectedContent, syncablePredicate(true)));
    }

    private Collection<DownloadRequest> updateCreatorOptOut(Collection<DownloadRequest> userExpectedContent) {
        Collection<DownloadRequest> outOuts = MoreCollections.filter(userExpectedContent, syncablePredicate(false));

        if (!outOuts.isEmpty()) {
            propellerDatabase.bulkUpsert(TrackDownloads.TABLE, creatorOptOutContentValues(outOuts));
        }
        return outOuts;
    }

    private List<ContentValues> creatorOptOutContentValues(Collection<DownloadRequest> creatorOptOut) {
        List<ContentValues> contentValues = new ArrayList<>();
        for (DownloadRequest outOut : creatorOptOut) {
            contentValues.add(ContentValuesBuilder.values()
                    .put(TrackDownloads.UNAVAILABLE_AT, dateProvider.getCurrentTime())
                    .put(TrackDownloads.REQUESTED_AT, null)
                    .put(TrackDownloads._ID, outOut.track.getNumericId())
                    .get());
        }
        return contentValues;
    }

    private Predicate<DownloadRequest> syncablePredicate(final boolean isSyncable) {
        return new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest input) {
                return input.syncable == isSyncable;
            }
        };
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
        final Query query = Query.from(TrackDownloads.TABLE).whereNotNull(DOWNLOADED_AT).whereNull(REMOVED_AT);
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

    private List<Urn> getNewPendingRemovals(Collection<Urn> expectedContent, List<Urn> downloadedTracks,
                                            Collection<Urn> downloadRequests) {
        return newArrayList(subtract(add(downloadedTracks, downloadRequests), expectedContent));
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
