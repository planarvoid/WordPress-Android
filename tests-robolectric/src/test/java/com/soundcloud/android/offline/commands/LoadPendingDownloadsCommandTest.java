package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadPendingDownloadsCommandTest extends StorageIntegrationTest {

    private LoadPendingDownloadsCommand command;
    private ApiTrack apiTrack;

    @Before
    public void setUp() {
        command = new LoadPendingDownloadsCommand(propeller());
        apiTrack = ModelFixtures.create(ApiTrack.class);

        testFixtures().insertTrack(apiTrack);
        testFixtures().insertLike(apiTrack.getUrn().getNumericId(), TableColumns.Sounds.TYPE_TRACK, new Date(0));
    }

    @Test
    public void returnsLikedTracksAsPendingDownloads() throws Exception {
        testFixtures().insertRequestedTrackDownload(apiTrack.getUrn(), 100);

        List<DownloadRequest> pending = command.call();

        expect(pending).toNumber(1);
        expectDownloadRequestMatchApiTrack(pending.get(0), apiTrack);
    }

    @Test
    public void returnsOfflinePlaylistTracksAsPendingDownloads() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();
        final ApiTrack track1 = testFixtures().insertPlaylistTrack(playlist, 0);
        testFixtures().insertRequestedTrackDownload(track1.getUrn(), 100);

        List<DownloadRequest> pending = command.call();
        expect(pending).toNumber(1);
        expectDownloadRequestMatchApiTrack(pending.get(0), track1);
    }

    @Test
    public void returnsEmptyListWhenAllDownloadsCompleted() throws Exception {
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 100);

        List<DownloadRequest> pending = command.call();
        expect(pending).toBeEmpty();
    }

    @Test
    public void doesNotReturnTrackDownloadsPendingRemoval() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);

        List<DownloadRequest> pending = command.call();
        expect(pending).toBeEmpty();
    }

    @Test
    public void doesNotReturnDownloadedTrackPendingRemoval() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 100);
        database().execSQL("UPDATE TrackDownloads SET downloaded_at=100"
                + " WHERE _id=" + apiTrack.getUrn().getNumericId());

        List<DownloadRequest> pending = command.call();
        expect(pending).toBeEmpty();
    }

    private void expectDownloadRequestMatchApiTrack(DownloadRequest request, ApiTrack track) {
        expect(request.urn).toEqual(track.getUrn());
        expect(request.fileUrl).toEqual(track.getStreamUrl());
    }

}