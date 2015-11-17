package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.UpdateLikeCommand.UpdateLikeParams;
import static com.soundcloud.propeller.query.Filter.filter;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
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
        databaseAssertions().assertLikesCount(targetUrn, 34);

        final int newLikesCount = command.call(new UpdateLikeParams(targetUrn, true));

        assertThat(newLikesCount).isEqualTo(35);
        databaseAssertions().assertLikesCount(targetUrn, 35);
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
        cv.put(TableColumns.Sounds.LIKES_COUNT, 1);
        assertThat(propeller().update(Table.Sounds, cv, filter()
                .whereEq(TableColumns.Sounds._ID, targetUrn.getNumericId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)).success()).isTrue();
        databaseAssertions().assertLikesCount(targetUrn, 1);
    }
}
