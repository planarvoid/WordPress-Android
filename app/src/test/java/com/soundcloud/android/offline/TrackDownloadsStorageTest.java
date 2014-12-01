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

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackDownloadsStorageTest extends StorageIntegrationTest {

    private TrackDownloadsStorage storage;
    private DownloadResult downloadResult;
    private List<Urn> tracksToDownload;

    private final Urn track1 = Urn.forTrack(123L);
    private final Urn track2 = Urn.forTrack(124L);

    @Before
    public void setUp() {
        storage = new TrackDownloadsStorage(propeller());
        downloadResult = DownloadResult.forSuccess(Urn.forTrack(123));
        tracksToDownload = getTracksToDownload();

        insertTrackToSounds(track1, "http://streamUrl1");
        insertTrackToSounds(track2, "http://streamUrl2");
    }

    @Test
    public void storeRequestedDownloads() {
        storage.storeRequestedDownloads(tracksToDownload);
        databaseAssertions().assertDownloadRequestsInserted(tracksToDownload);
    }

    @Test
    public void storeRequestedDownloadDoesNotOverrideExistingRecords() {
        long timestamp = 100;
        testFixtures().insertRequestedTrackDownload(timestamp, track1);

        storage.storeRequestedDownloads(Arrays.asList(track1));

        databaseAssertions().assertExistingDownloadRequest(timestamp, track1);
    }

    @Test
    public void updatesDownloadTracksWithDownloadResults() {
        storage.storeRequestedDownloads(tracksToDownload);

        storage.updateDownload(downloadResult);

        databaseAssertions().assertDownloadResultsInserted(downloadResult);
        databaseAssertions().assertDownloadRequestsInserted(tracksToDownload);
    }

    @Test
    public void getRequestedDownloadsEmptyListWhenAllDownloadsCompleted() {
        storage.storeRequestedDownloads(tracksToDownload);
        storage.updateDownload(downloadResult);

        List<DownloadRequest> result = storage.getPendingDownloads();
        expect(result).toNumber(1);
    }

    @Test
    public void getRequestedDownloadsReturnsNotDownloadedTracks() {
        ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        testFixtures().insertTrack(apiTrack);
        storage.storeRequestedDownloads(Arrays.asList(apiTrack.getUrn()));

        List<DownloadRequest> result = storage.getPendingDownloads();
        expect(result.size()).toBe(1);
        expect(result.get(0).urn).toEqual(apiTrack.getUrn());
        expect(result.get(0).fileUrl).toEqual(apiTrack.getStreamUrl());
    }

    private List<Urn> getTracksToDownload() {
        return Arrays.asList(track1, track2);
    }

    private void insertTrackToSounds(Urn urn, String streamUrl) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setUrn(urn.toString());
        track.setStreamUrl(streamUrl);
        testFixtures().insertTrack(track);
    }

}