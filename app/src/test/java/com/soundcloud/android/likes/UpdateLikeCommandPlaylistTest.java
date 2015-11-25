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

public class UpdateLikeCommandPlaylistTest extends StorageIntegrationTest {

    private UpdateLikeCommand command;
    private Urn targetUrn;

    @Before
    public void setUp() throws Exception {
        command = new UpdateLikeCommand(propeller());
        targetUrn = testFixtures().insertPlaylist().getUrn();
    }

    @Test
    public void updatesLikeStatusWhenLikingTrack() throws Exception {
        command.call(new UpdateLikeParams(targetUrn, true));

        databaseAssertions().assertLikedPlaylistPendingAddition(targetUrn);
    }

    @Test
    public void updatesLikesCountInSoundsWhenLiked() throws Exception {
        databaseAssertions().assertLikesCount(targetUrn, 10);

        final int newLikesCount = command.call(new UpdateLikeParams(targetUrn, true));

        assertThat(newLikesCount).isEqualTo(11);
        databaseAssertions().assertLikesCount(targetUrn, 11);
    }

    @Test
    public void upsertReplacesLikedPlaylist() throws Exception {
        Urn playlistUrn = testFixtures().insertLikedPlaylistPendingRemoval(new Date()).getUrn();

        command.call(new UpdateLikeParams(playlistUrn, true));

        databaseAssertions().assertLikedPlaylistPendingAddition(playlistUrn);
    }

    @Test
    public void updatesLikeStatusWhenUnlikingPlaylist() throws Exception {
        command.call(new UpdateLikeParams(targetUrn, false));

        databaseAssertions().assertLikedPlaylistPendingRemoval(targetUrn);
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
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)).success()).isTrue();
        databaseAssertions().assertLikesCount(targetUrn, 1);
    }

}
