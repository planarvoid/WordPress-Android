package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadLikedTracksCommandTest extends StorageIntegrationTest {

    private static final Date LIKED_DATE_1 = new Date(100);
    private static final Date LIKED_DATE_2 = new Date(200);
    private static final Date LIKED_DATE_3 = new Date(300);

    private static final Date DOWNLOADED_DATE = new Date(1000);
    private static final Date REMOVED_DATE = new Date(2000);
    private static final Date REQUESTED_DATE = new Date(3000);

    private LoadLikedTracksCommand command;
    private PropertySet track1;
    private PropertySet track2;
    private PropertySet track3;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedTracksCommand(propeller());

        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        track2 = testFixtures().insertLikedTrack(LIKED_DATE_2).toPropertySet();
        track3 = testFixtures().insertLikedTrack(LIKED_DATE_3).toPropertySet();

        testFixtures().insertCompletedTrackDownload(track1.get(TrackProperty.URN), REQUESTED_DATE.getTime(), DOWNLOADED_DATE.getTime());
        testFixtures().insertTrackDownloadPendingRemoval(track2.get(TrackProperty.URN), REQUESTED_DATE.getTime(), REMOVED_DATE.getTime());
        testFixtures().insertRequestedTrackDownload(track3.get(TrackProperty.URN), REQUESTED_DATE.getTime());
    }

    @Test
    public void shouldLoadAllTrackLikes() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(3, Long.MAX_VALUE)).call();

        expect(result).toContainExactly(
                expectedRequestedLikedTrackFor(track3, LIKED_DATE_3, REQUESTED_DATE),
                expectedRemovedLikedTrackFor(track2, LIKED_DATE_2, REQUESTED_DATE, REMOVED_DATE),
                expectedDownloadedLikedTrackFor(track1, LIKED_DATE_1, REQUESTED_DATE, DOWNLOADED_DATE));
    }

    @Test
    public void shouldAdhereToLimit() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(1, Long.MAX_VALUE)).call();

        expect(result).toContainExactly(expectedRequestedLikedTrackFor(track3, LIKED_DATE_3, REQUESTED_DATE));
    }

    @Test
    public void shouldAdhereToTimestamp() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(3, LIKED_DATE_3.getTime())).call();

        expect(result).toContainExactly(
                expectedRemovedLikedTrackFor(track2, LIKED_DATE_2, REQUESTED_DATE, REMOVED_DATE),
                expectedDownloadedLikedTrackFor(track1, LIKED_DATE_1, REQUESTED_DATE, DOWNLOADED_DATE));
    }

    private PropertySet expectedRequestedLikedTrackFor(PropertySet track, Date likedAt, Date requestedAt) {
        return expectedLikedTrackFor(track, likedAt).put(TrackProperty.OFFLINE_REQUESTED_AT, requestedAt);
    }

    private PropertySet expectedDownloadedLikedTrackFor(PropertySet track, Date likedAt, Date requestedAt, Date downloadedAt) {
        return expectedLikedTrackFor(track, likedAt)
                .put(TrackProperty.OFFLINE_REQUESTED_AT, requestedAt)
                .put(TrackProperty.OFFLINE_DOWNLOADED_AT, downloadedAt);
    }

    private PropertySet expectedRemovedLikedTrackFor(PropertySet track, Date likedAt, Date requestedAt, Date removedAt) {
        return expectedLikedTrackFor(track, likedAt)
                .put(TrackProperty.OFFLINE_REQUESTED_AT, requestedAt)
                .put(TrackProperty.OFFLINE_REMOVED_AT, removedAt);
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