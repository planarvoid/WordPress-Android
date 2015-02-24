package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class UpdateLikeCommandTrackTest extends StorageIntegrationTest {

    private UpdateLikeCommand command;
    private PropertySet track;

    @Before
    public void setUp() throws Exception {
        command = new UpdateLikeCommand(propeller());
    }

    @Test
    public void insertLikedTrack() throws Exception {
        setUpTrackForLiking();

        command.with(track).call();

        databaseAssertions().assertLikedTrackPendingAddition(track);
    }

    @Test
    public void updatesLikesCountInSoundsWhenLiked() throws Exception {
        setUpTrackForLiking();

        PropertySet changeSet = command.with(track).call();

        final Integer newLikesCount = changeSet.get(PlayableProperty.LIKES_COUNT);
        final Integer oldLikesCount = track.get(PlayableProperty.LIKES_COUNT);

        expect(newLikesCount).toBe(oldLikesCount + 1);
        databaseAssertions().assertLikesCount(track.get(PlayableProperty.URN), newLikesCount);
    }

    @Test
    public void upsertRemovedLikedTrack() throws Exception {
        setUpTrackForLiking(testFixtures().insertLikedTrackPendingRemoval(new Date()));

        command.with(track).call();

        databaseAssertions().assertLikedTrackPendingAddition(track);
    }

    @Test
    public void insertUnlikedTrack() throws Exception {
        setUpTrackForUnLiking();

        command.with(track).call();

        databaseAssertions().assertLikedTrackPendingRemoval(track);
    }

    @Test
    public void updatesLikesCountInSoundsWhenUnliked() throws Exception {
        setUpTrackForUnLiking();

        PropertySet changeSet = command.with(track).call();

        final Integer newLikesCount = changeSet.get(PlayableProperty.LIKES_COUNT);
        final Integer oldLikesCount = track.get(PlayableProperty.LIKES_COUNT);

        expect(newLikesCount).toBe(oldLikesCount - 1);
        databaseAssertions().assertLikesCount(track.get(PlayableProperty.URN), newLikesCount);
    }

    @Test
    public void updatesTrackLikeToBePendingRemoval() throws Exception {
        setUpTrackForUnLiking(testFixtures().insertLikedTrack(new Date()));

        command.with(track).call();

        databaseAssertions().assertLikedTrackPendingRemoval(track);
    }

    private void setUpTrackForLiking() {
        setUpTrackForLiking(testFixtures().insertTrack());
    }

    private void setUpTrackForLiking(ApiTrack apiTrack) {
        final Date created = new Date();
        track = apiTrack.toPropertySet()
                .put(LikeProperty.CREATED_AT, created)
                .put(LikeProperty.ADDED_AT, created)
                .put(PlayableProperty.IS_LIKED, true);
    }

    private void setUpTrackForUnLiking() {
        setUpTrackForUnLiking(testFixtures().insertTrack());
    }

    private void setUpTrackForUnLiking(ApiTrack apiTrack) {
        final Date created = new Date();
        track = apiTrack.toPropertySet()
                .put(LikeProperty.CREATED_AT, created)
                .put(LikeProperty.REMOVED_AT, created)
                .put(PlayableProperty.IS_LIKED, false);
    }

}