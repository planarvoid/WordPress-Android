package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackDownloadsStorageTest extends StorageIntegrationTest {

    private TrackDownloadsStorage storage;

    private final Urn TRACK_URN1 = Urn.forTrack(123L);
    private final Urn TRACK_URN2 = Urn.forTrack(124L);
    private final List<Urn> TRACK_URNS = Arrays.asList(TRACK_URN1, TRACK_URN2);
    private final DownloadResult DOWNLOAD_RESULT = new DownloadResult(true, TRACK_URN1);

    @Before
    public void setUp() {
        storage = new TrackDownloadsStorage(propeller(), testScheduler());

        insertTrackToSounds(TRACK_URN1, "http://streamUrl1");
        insertTrackToSounds(TRACK_URN2, "http://streamUrl2");
    }

    @Test
    public void filterAndStoreNewDownloadRequestsSavesRequestedDownloads() {
        storage.filterAndStoreNewDownloadRequests(TRACK_URNS).subscribe();

        databaseAssertions().assertDownloadRequestsInserted(TRACK_URNS);
    }

    @Test
    public void filterAndStoreNewDownloadRequestsDoesNotOverrideExistingRecords() {
        final long timestamp = 100;
        testFixtures().insertRequestedTrackDownload(timestamp, TRACK_URN1);

        storage.filterAndStoreNewDownloadRequests(Arrays.asList(TRACK_URN1, TRACK_URN2)).subscribe();

        databaseAssertions().assertExistingDownloadRequest(timestamp, TRACK_URN1);
        databaseAssertions().assertDownloadRequestsInserted(Arrays.asList(TRACK_URN2));
    }

    @Test
    public void updatesDownloadTracksWithDownloadResults() {
        storage.filterAndStoreNewDownloadRequests(TRACK_URNS).subscribe();

        storage.updateDownload(DOWNLOAD_RESULT);

        databaseAssertions().assertDownloadResultsInserted(DOWNLOAD_RESULT);
        databaseAssertions().assertDownloadRequestsInserted(TRACK_URNS);
    }

    @Test
    public void getPendingDownloadsReturnsEmptyListForAllDownloadsCompleted() {
        final TestObserver<List<DownloadRequest>> observer = new TestObserver<>();
        storage.filterAndStoreNewDownloadRequests(Arrays.asList(TRACK_URN1));

        storage.updateDownload(DOWNLOAD_RESULT);
        storage.getPendingDownloads().subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toNumber(0);
    }

    @Test
    public void getPendingDownloadsReturnsOnlyNotDownloadedTracks() {
        final TestObserver<List<DownloadRequest>> observer = new TestObserver<>();
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        testFixtures().insertTrack(apiTrack);
        storage.filterAndStoreNewDownloadRequests(Arrays.asList(apiTrack.getUrn())).subscribe();

        storage.getPendingDownloads().subscribe(observer);

        final List<DownloadRequest> result = observer.getOnNextEvents().get(0);
        expect(result.size()).toBe(1);
        expect(result.get(0).urn).toEqual(apiTrack.getUrn());
        expect(result.get(0).fileUrl).toEqual(apiTrack.getStreamUrl());
    }

    private void insertTrackToSounds(Urn urn, String streamUrl) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setUrn(urn.toString());
        track.setStreamUrl(streamUrl);
        testFixtures().insertTrack(track);
    }

}