package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.UpdateLikeCommand.UpdateLikeParams;
import static com.soundcloud.propeller.query.Filter.filter;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackStats;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.TrackFixtures;
import org.junit.Before;
import org.junit.Test;

import android.content.ContentValues;

import java.util.Date;

public class UpdateLikeCommandTrackTest extends StorageIntegrationTest {

    private UpdateLikeCommand command;
    private Urn targetUrn;

    @Before
    public void setUp() throws Exception {
        command = new UpdateLikeCommand(propeller());
        targetUrn = testFixtures().insertTrack().getUrn();
    }

    @Test
    public void updatesLikeStatusWhenLikingTrack() throws Exception {
        command.call(new UpdateLikeParams(targetUrn, true));

        databaseAssertions().assertLikedTrackPendingAddition(targetUrn);
    }

    @Test
    public void updatesLikesCountInSoundsWhenLiked() throws Exception {
        databaseAssertions().assertLikesCount(targetUrn, TrackFixtures.LIKE_COUNT);

        final int newLikesCount = command.call(new UpdateLikeParams(targetUrn, true));

        assertThat(newLikesCount).isEqualTo(TrackFixtures.LIKE_COUNT + 1);
        databaseAssertions().assertLikesCount(targetUrn, TrackFixtures.LIKE_COUNT + 1);
    }

    @Test
    public void doesNotUpdatesUnknownLikesCountInSoundsWhenLiked() throws Exception {
        ApiTrack track = TrackFixtures.apiTrack();
        ApiTrackStats updatedStats = track.getStats().toBuilder().likesCount(Consts.NOT_SET).build();
        testFixtures().insertTrack(track.toBuilder().stats(updatedStats).build());
        targetUrn = track.getUrn();

        databaseAssertions().assertLikesCount(targetUrn, -1);

        final int newLikesCount = command.call(new UpdateLikeParams(targetUrn, true));

        assertThat(newLikesCount).isEqualTo(-1);
        databaseAssertions().assertLikesCount(targetUrn, -1);
    }

    @Test
    public void upsertReplacesLikedTrack() throws Exception {
        Urn trackUrn = testFixtures().insertLikedTrackPendingRemoval(new Date()).getUrn();

        command.call(new UpdateLikeParams(trackUrn, true));

        databaseAssertions().assertLikedTrackPendingAddition(trackUrn);
    }

    @Test
    public void updatesLikeStatusWhenUnlikingTrack() throws Exception {
        command.call(new UpdateLikeParams(targetUrn, false));

        databaseAssertions().assertLikedTrackPendingRemoval(targetUrn);
    }

    @Test
    public void updatesLikesCountInSoundsWhenUnliked() throws Exception {
        updateLikesCount();

        final int newLikesCount = command.call(new UpdateLikeParams(targetUrn, false));

        assertThat(newLikesCount).isEqualTo(0);
        databaseAssertions().assertLikesCount(targetUrn, 0);
    }

    private void updateLikesCount() {
        ContentValues cv = new ContentValues();
        cv.put(Tables.Sounds.LIKES_COUNT.name(), 1);
        assertThat(propeller().update(Tables.Sounds.TABLE, cv, filter()
                .whereEq(Tables.Sounds._ID, targetUrn.getNumericId())
                .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_TRACK)).success()).isTrue();
        databaseAssertions().assertLikesCount(targetUrn, 1);
    }
}
