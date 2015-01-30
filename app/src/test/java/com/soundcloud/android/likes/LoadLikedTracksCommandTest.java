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

    private static final Date DOWNLOADED_DATE_1 = new Date();

    private LoadLikedTracksCommand command;
    private PropertySet track1;
    private PropertySet track2;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedTracksCommand(propeller());

        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        track2 = testFixtures().insertLikedTrack(LIKED_DATE_2).toPropertySet();

        testFixtures().insertCompletedTrackDownload(track1.get(TrackProperty.URN), DOWNLOADED_DATE_1.getTime());
    }

    @Test
    public void shouldLoadAllTrackLikes() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(2, Long.MAX_VALUE)).call();

        expect(result).toContainExactly(
                expectedLikedTrackFor(track2, LIKED_DATE_2),
                expectedDownloadedLikedTrackFor(track1, LIKED_DATE_1, DOWNLOADED_DATE_1));
    }

    @Test
    public void shouldAdhereToLimit() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(1, Long.MAX_VALUE)).call();

        expect(result).toContainExactly(expectedLikedTrackFor(track2, LIKED_DATE_2));
    }

    @Test
    public void shouldAdhereToTimestamp() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(2, LIKED_DATE_2.getTime())).call();

        expect(result).toContainExactly(expectedLikedTrackFor(track1, LIKED_DATE_1));
    }

    private PropertySet expectedDownloadedLikedTrackFor(PropertySet track, Date likedAt, Date downloadedAt) {
        return expectedLikedTrackFor(track, likedAt).put(TrackProperty.OFFLINE_DOWNLOADED_AT, downloadedAt);
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