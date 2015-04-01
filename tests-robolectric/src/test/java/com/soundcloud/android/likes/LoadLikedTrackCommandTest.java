package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;

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

    private static final Date DOWNLOADED_DATE = new Date(1000);
    private static final Date REMOVED_DATE = new Date(2000);
    private static final Date REQUESTED_DATE = new Date(3000);

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
        testFixtures().insertCompletedTrackDownload(track1.get(TrackProperty.URN), REQUESTED_DATE.getTime(), DOWNLOADED_DATE.getTime());

        PropertySet result = command.with(track1.get(TrackProperty.URN)).call();

        expect(result).toEqual(expectedDownloadedLikedTrackFor(track1, LIKED_DATE_1, REQUESTED_DATE, DOWNLOADED_DATE));
    }

    @Test
    public void loadsRequestedTrackLike() throws Exception {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        testFixtures().insertTrackPendingDownload(track1.get(TrackProperty.URN), REQUESTED_DATE.getTime());

        PropertySet result = command.with(track1.get(TrackProperty.URN)).call();

        expect(result).toEqual(expectedRequestedLikedTrackFor(track1, LIKED_DATE_1, REQUESTED_DATE));
    }

    @Test
    public void loadsRemovedTrackLike() throws Exception {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        testFixtures().insertTrackDownloadPendingRemoval(track1.get(TrackProperty.URN), REQUESTED_DATE.getTime(), REMOVED_DATE.getTime());

        PropertySet result = command.with(track1.get(TrackProperty.URN)).call();

        expect(result).toEqual(expectedRemovedLikedTrackFor(track1, LIKED_DATE_1, REQUESTED_DATE, REMOVED_DATE));
    }

    private PropertySet expectedRequestedLikedTrackFor(PropertySet track, Date likedAt, Date requestedAt) {
        return expectedLikedTrackFor(track, likedAt).put(OfflineProperty.Track.REQUESTED_AT, requestedAt);
    }

    private PropertySet expectedDownloadedLikedTrackFor(PropertySet track, Date likedAt, Date requestedAt, Date downloadedAt) {
        return expectedLikedTrackFor(track, likedAt)
                .put(OfflineProperty.Track.REQUESTED_AT, requestedAt)
                .put(OfflineProperty.Track.DOWNLOADED_AT, downloadedAt);
    }

    private PropertySet expectedRemovedLikedTrackFor(PropertySet track, Date likedAt, Date requestedAt, Date removedAt) {
        return expectedLikedTrackFor(track, likedAt)
                .put(OfflineProperty.Track.DOWNLOADED_AT, requestedAt)
                .put(OfflineProperty.Track.REQUESTED_AT, requestedAt)
                .put(OfflineProperty.Track.REMOVED_AT, removedAt);
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