package com.soundcloud.android.offline.commands;

import static com.google.common.collect.Lists.newArrayList;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REQUESTED_AT;
import static com.soundcloud.android.utils.CollectionUtils.add;
import static com.soundcloud.android.utils.CollectionUtils.subtract;
import static com.soundcloud.propeller.query.Filter.filter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineContentRequests;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoadOfflineContentUpdatesCommand extends Command<Collection<DownloadRequest>, OfflineContentRequests> {

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
    public OfflineContentRequests call(final Collection<DownloadRequest> expectedRequests) {
        final Collection<Urn> expectedTracks = Collections2.transform(expectedRequests, toUrn);
        final List<Urn> downloadRequests = getDownloadRequests();
        final List<Urn> downloadedContent = getDownloaded();
        final List<Urn> pendingRemovals = getPendingRemovals();


        final Collection<DownloadRequest> tracksToRestore = getTracksToRestore(expectedRequests, pendingRemovals);
        final Collection<DownloadRequest> newPendingDownloads = getNewPendingDownloads(expectedRequests, downloadRequests, downloadedContent, tracksToRestore);
        final Collection<DownloadRequest> allDownloadRequests = getAllDownloadRequests(expectedRequests, pendingRemovals, tracksToRestore, downloadedContent);
        final List<Urn> newPendingRemovals = getNewPendingRemovals(expectedTracks, downloadedContent, downloadRequests);

        return new OfflineContentRequests(
                newArrayList(allDownloadRequests),
                newArrayList(newPendingDownloads),
                newArrayList(tracksToRestore),
                newPendingRemovals
        );
    }

    private List<Urn> getDownloadRequests() {
        final Where isPendingDownloads = filter()
                .whereNull(REMOVED_AT)
                .whereNull(DOWNLOADED_AT)
                .whereNotNull(REQUESTED_AT);

        return propellerDatabase.query(Query.from(TrackDownloads.name())
                .where(isPendingDownloads))
                .toList(new TrackUrnMapper());
    }

    private List<Urn> getDownloaded() {
        final Query query = Query.from(TrackDownloads.name()).whereNotNull(DOWNLOADED_AT).whereNull(REMOVED_AT);
        return propellerDatabase.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> getPendingRemovals() {
        final long pendingRemovalThreshold = dateProvider.getCurrentDate().getTime() - PENDING_REMOVAL_DELAY;
        final Query query = Query.from(TrackDownloads.name()).whereNotNull(DOWNLOADED_AT).whereGt(REMOVED_AT, pendingRemovalThreshold);
        return propellerDatabase.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> getNewPendingRemovals(Collection<Urn> expectedContent, List<Urn> downloadedTracks, Collection<Urn> downloadRequests) {
        return newArrayList(subtract(add(downloadedTracks, downloadRequests), expectedContent));
    }

    private Collection<DownloadRequest> getTracksToRestore(Collection<DownloadRequest> expectedContent, final List<Urn> pendingRemovals) {
        return Collections2.filter(expectedContent, new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest request) {
                return pendingRemovals.contains(request.track);
            }
        });
    }

    private Collection<DownloadRequest> getNewPendingDownloads(Collection<DownloadRequest> expectedContent, final List<Urn> pendingDownloads, final List<Urn> downloadedTracks, final Collection<DownloadRequest> tracksToRestore) {
        return Collections2.filter(expectedContent, new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest request) {
                return !pendingDownloads.contains(request.track) &&
                        !downloadedTracks.contains(request.track) &&
                        !tracksToRestore.contains(request);
            }
        });
    }

    private Collection<DownloadRequest> getAllDownloadRequests(Collection<DownloadRequest> expectedRequests, final List<Urn> downloadedTracks, final Collection<DownloadRequest> tracksToRestore, final List<Urn> downloadedContent) {
        return Collections2.filter(expectedRequests, new Predicate<DownloadRequest>() {
            @Override
            public boolean apply(DownloadRequest request) {
                return !downloadedTracks.contains(request.track) &&
                        !tracksToRestore.contains(request) &&
                        !downloadedContent.contains(request.track);
            }
        });
    }
}
