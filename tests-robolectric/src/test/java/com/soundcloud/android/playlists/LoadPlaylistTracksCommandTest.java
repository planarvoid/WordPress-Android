package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class LoadPlaylistTracksCommandTest extends StorageIntegrationTest{

    private LoadPlaylistTracksCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadPlaylistTracksCommand(propeller());
    }

    @Test
    public void returnsPlaylistTracks() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);

        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrack(testFixtures().insertPlaylist(), 2);

        expect(command.with(apiPlaylist.getUrn()).call()).toContainExactly(
                fromApiTrack(apiTrack1),
                fromApiTrack(apiTrack2)
        );
    }

    @Test
    public void returnsPlaylistTracksWithOfflineInfo() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrack(apiPlaylist, 2);

        testFixtures().insertCompletedTrackDownload(apiTrack1.getUrn(), 100L);
        testFixtures().insertRequestedTrackDownload(apiTrack2.getUrn(), 200L);
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack3.getUrn(), 300L);

        expect(command.with(apiPlaylist.getUrn()).call()).toContain(
                fromApiTrack(apiTrack1)
                        .put(TrackProperty.OFFLINE_DOWNLOADED_AT, new Date(100L))
                        .put(TrackProperty.OFFLINE_REQUESTED_AT, new Date(100L)),
                fromApiTrack(apiTrack2)
                        .put(TrackProperty.OFFLINE_REQUESTED_AT, new Date(200L)),
                fromApiTrack(apiTrack3)
                        .put(TrackProperty.OFFLINE_REMOVED_AT, new Date(300L))
                        .put(TrackProperty.OFFLINE_REQUESTED_AT, new Date(300L))
        );
    }

    private PropertySet fromApiTrack(ApiTrack apiTrack){
        return PropertySet.from(
                TrackProperty.URN.bind(apiTrack.getUrn()),
                TrackProperty.TITLE.bind(apiTrack.getTitle()),
                TrackProperty.DURATION.bind(apiTrack.getDuration()),
                TrackProperty.PLAY_COUNT.bind(apiTrack.getStats().getPlaybackCount()),
                TrackProperty.LIKES_COUNT.bind(apiTrack.getStats().getLikesCount()),
                TrackProperty.IS_PRIVATE.bind(apiTrack.isPrivate()),
                TrackProperty.CREATOR_NAME.bind(apiTrack.getUserName()),
                TrackProperty.CREATOR_URN.bind(apiTrack.getUser().getUrn())
        );
    }
}