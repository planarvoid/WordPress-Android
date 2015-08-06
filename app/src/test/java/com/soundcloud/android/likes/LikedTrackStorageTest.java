package com.soundcloud.android.likes;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LikedTrackStorageTest extends StorageIntegrationTest {

    private static final Date LIKED_DATE_1 = new Date(100);
    private static final Date LIKED_DATE_2 = new Date(200);
    private static final Date LIKED_DATE_3 = new Date(300);
    private static final Date LIKED_DATE_4 = new Date(400);

    private PropertySet track1;
    private PropertySet track2;
    private PropertySet track3;
    private PropertySet track4;

    private LikedTrackStorage storage;
    private TestObserver<PropertySet> testObserver = new TestObserver<>();
    private TestObserver<List<PropertySet>> testListObserver = new TestObserver<>();

    @Before
    public void setUp() {
        storage = new LikedTrackStorage(propellerRx());

        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        track2 = testFixtures().insertLikedTrack(LIKED_DATE_2).toPropertySet();
        track3 = testFixtures().insertLikedTrack(LIKED_DATE_3).toPropertySet();
        track4 = testFixtures().insertLikedTrack(LIKED_DATE_4).toPropertySet();

        testFixtures().insertCompletedTrackDownload(track1.get(TrackProperty.URN), 100L, 300L);
        testFixtures().insertTrackDownloadPendingRemoval(track2.get(TrackProperty.URN), 100L, 300L);
        testFixtures().insertTrackPendingDownload(track3.get(TrackProperty.URN), 200L);
        testFixtures().insertPolicyMidTierMonetizable(track4.get(TrackProperty.URN));
    }

    @Test
    public void loadTrackLikesLoadsAllTrackLikes() {
        storage.loadTrackLikes(4, Long.MAX_VALUE).subscribe(testListObserver);

        assertThat(testListObserver.getOnNextEvents()).containsExactly(Arrays.asList(
                expectedMidTierMonetizableLikedTrackFor(track4, LIKED_DATE_4),
                expectedRequestedLikedTrackFor(track3, LIKED_DATE_3),
                expectedRemovedLikedTrackFor(track2, LIKED_DATE_2),
                expectedDownloadedLikedTrackFor(track1, LIKED_DATE_1)));
    }

    @Test
    public void loadTrackLikesAdheresToLimit() {
        storage.loadTrackLikes(1, Long.MAX_VALUE).subscribe(testListObserver);

        assertThat(testListObserver.getOnNextEvents()).containsExactly(Collections.singletonList(expectedMidTierMonetizableLikedTrackFor(track4, LIKED_DATE_4)));
    }

    @Test
    public void loadTrackLikesAdheresToTimestamp() {
        storage.loadTrackLikes(3, LIKED_DATE_3.getTime()).subscribe(testListObserver);

        assertThat(testListObserver.getOnNextEvents()).containsExactly(Arrays.asList(
                expectedRemovedLikedTrackFor(track2, LIKED_DATE_2),
                expectedDownloadedLikedTrackFor(track1, LIKED_DATE_1)));
    }

    @Test
    public void loadLikedTrackEmitsNoPropertySetsIfLikeDoesNotExist() {
        track1 = testFixtures().insertTrack().toPropertySet();

        storage.loadTrackLike(track1.get(TrackProperty.URN)).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).isEmpty();
    }

    @Test
    public void loadLikedTrackLoadsTrackLike() {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();

        storage.loadTrackLike(track1.get(TrackProperty.URN)).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).containsExactly(expectedLikedTrackFor(track1, LIKED_DATE_1));
    }

    @Test
    public void loadLikedTrackLoadsCompletedTrackLike() {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        testFixtures().insertCompletedTrackDownload(track1.get(TrackProperty.URN), 100L, 300L);

        storage.loadTrackLike(track1.get(TrackProperty.URN)).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).containsExactly(expectedDownloadedLikedTrackFor(track1, LIKED_DATE_1));
    }

    @Test
    public void loadLikedTrackLoadsRequestedTrackLike() {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        testFixtures().insertTrackPendingDownload(track1.get(TrackProperty.URN), 100L);

        storage.loadTrackLike(track1.get(TrackProperty.URN)).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).containsExactly(expectedRequestedLikedTrackFor(track1, LIKED_DATE_1));
    }

    @Test
    public void loadLikedTrackLoadsRemovedTrackLike() {
        track1 = testFixtures().insertLikedTrack(LIKED_DATE_1).toPropertySet();
        testFixtures().insertTrackDownloadPendingRemoval(track1.get(TrackProperty.URN), 100L, 200L);

        storage.loadTrackLike(track1.get(TrackProperty.URN)).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).containsExactly(expectedRemovedLikedTrackFor(track1, LIKED_DATE_1));
    }

    @Test
    public void loadLikedTrackLoadsMidTierMonetizableTrackIncludingPolicy() {
        storage.loadTrackLike(track4.get(TrackProperty.URN)).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).containsExactly(expectedMidTierMonetizableLikedTrackFor(track4, LIKED_DATE_4));
    }

    private PropertySet expectedRequestedLikedTrackFor(PropertySet track, Date likedAt) {
        return expectedLikedTrackFor(track, likedAt).put(OfflineProperty.OFFLINE_STATE, OfflineState.REQUESTED);
    }

    private PropertySet expectedDownloadedLikedTrackFor(PropertySet track, Date likedAt) {
        return expectedLikedTrackFor(track, likedAt).put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);
    }

    private PropertySet expectedRemovedLikedTrackFor(PropertySet track, Date likedAt) {
        return expectedLikedTrackFor(track, likedAt).put(OfflineProperty.OFFLINE_STATE, OfflineState.NO_OFFLINE);
    }

    private PropertySet expectedMidTierMonetizableLikedTrackFor(PropertySet track, Date likedAt) {
        return expectedLikedTrackFor(track, likedAt).put(TrackProperty.SUB_MID_TIER, true);
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
                TrackProperty.IS_PRIVATE.bind(track.get(TrackProperty.IS_PRIVATE)),
                TrackProperty.SUB_MID_TIER.bind(false));
    }
}