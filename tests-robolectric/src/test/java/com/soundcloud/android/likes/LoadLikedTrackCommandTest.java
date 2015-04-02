package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class LoadLikedTrackCommandTest extends StorageIntegrationTest {

    private static final Date LIKED_DATE_1 = new Date(100);

    private LoadLikedTrackCommand command;
    private PropertySet track1;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedTrackCommand(propeller());
    }

    @Test
    public void emitsEmptyPropertySetIfLikeDoesNotExist() throws Exception {
        track1 = testFixtures().insertTrack().toPropertySet();

        PropertySet result = command.with(track1.get(TrackProperty.URN)).call();

        expect(result).toEqual(PropertySet.create());
    }

    @Test
    public void loadsTrackLike() throws Exception {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();

        PropertySet result = command.with(track1.get(TrackProperty.URN)).call();

        expect(result).toEqual(expectedLikedTrackFor(track1, LIKED_DATE_1));
    }

    @Test
    public void loadsCompletedTrackLike() throws Exception {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        testFixtures().insertCompletedTrackDownload(track1.get(TrackProperty.URN), 100L, 300L);

        PropertySet result = command.with(track1.get(TrackProperty.URN)).call();

        expect(result).toEqual(expectedDownloadedLikedTrackFor(track1, LIKED_DATE_1));
    }

    @Test
    public void loadsRequestedTrackLike() throws Exception {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        testFixtures().insertTrackPendingDownload(track1.get(TrackProperty.URN), 100L);

        PropertySet result = command.with(track1.get(TrackProperty.URN)).call();

        expect(result).toEqual(expectedRequestedLikedTrackFor(track1, LIKED_DATE_1));
    }

    @Test
    public void loadsRemovedTrackLike() throws Exception {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        testFixtures().insertTrackDownloadPendingRemoval(track1.get(TrackProperty.URN), 100L, 200L);

        PropertySet result = command.with(track1.get(TrackProperty.URN)).call();

        expect(result).toEqual(expectedRemovedLikedTrackFor(track1, LIKED_DATE_1));
    }

    private PropertySet expectedRequestedLikedTrackFor(PropertySet track, Date likedAt) {
        return expectedLikedTrackFor(track, likedAt).put(OfflineProperty.DOWNLOAD_STATE, DownloadState.REQUESTED);
    }

    private PropertySet expectedDownloadedLikedTrackFor(PropertySet track, Date likedAt) {
        return expectedLikedTrackFor(track, likedAt).put(OfflineProperty.DOWNLOAD_STATE, DownloadState.DOWNLOADED);
    }

    private PropertySet expectedRemovedLikedTrackFor(PropertySet track, Date likedAt) {
        return expectedLikedTrackFor(track, likedAt).put(OfflineProperty.DOWNLOAD_STATE, DownloadState.NO_OFFLINE);
    }

    private PropertySet expectedLikedTrackFor(PropertySet track, Date likedAt) {
        return PropertySet.from(
                TrackProperty.URN.bind(track.get(TrackProperty.URN)),
                TrackProperty.CREATOR_NAME.bind(track.get(TrackProperty.CREATOR_NAME)),
                TrackProperty.TITLE.bind(track.get(TrackProperty.TITLE)),
                TrackProperty.DURATION.bind(track.get(TrackProperty.DURATION)),
                TrackProperty.PLAY_COUNT.bind(track.get(TrackProperty.PLAY_COUNT)),
                TrackProperty.LIKES_COUNT.bind(track.get(TrackProperty.LIKES_COUNT)),
                LikeProperty.CREATED_AT.bind((likedAt)),
                TrackProperty.IS_PRIVATE.bind(track.get(TrackProperty.IS_PRIVATE)));
    }
}