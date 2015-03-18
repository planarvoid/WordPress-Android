package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.WhereBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
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
        final WriteResult result = command.call(new UpdateLikeCommand.UpdateLikeParams(targetUrn, true));
        expect(result.success()).toBeTrue();

        databaseAssertions().assertLikedPlaylistPendingAddition(targetUrn);
    }

    @Test
    public void updatesLikesCountInSoundsWhenLiked() throws Exception {
        databaseAssertions().assertLikesCount(targetUrn, 10);

        final WriteResult result = command.call(new UpdateLikeCommand.UpdateLikeParams(targetUrn, true));
        expect(result.success()).toBeTrue();

        expect((Integer) command.getUpdatedLikesCount()).toBe(11);
        databaseAssertions().assertLikesCount(targetUrn, 11);
    }

    @Test
    public void upsertReplacesLikedPlaylist() throws Exception {
        Urn playlistUrn = testFixtures().insertLikedPlaylistPendingRemoval(new Date()).getUrn();

        final WriteResult result = command.call(new UpdateLikeCommand.UpdateLikeParams(playlistUrn, true));
        expect(result.success()).toBeTrue();

        databaseAssertions().assertLikedPlaylistPendingAddition(playlistUrn);
    }

    @Test
    public void updatesLikeStatusWhenUnlikingPlaylist() throws Exception {
        final WriteResult result = command.call(new UpdateLikeCommand.UpdateLikeParams(targetUrn, false));
        expect(result.success()).toBeTrue();

        databaseAssertions().assertLikedPlaylistPendingRemoval(targetUrn);
    }

    @Test
    public void updatesLikesCountInSoundsWhenUnliked() throws Exception {
        updateLikesCount();

        final WriteResult result = command.call(new UpdateLikeCommand.UpdateLikeParams(targetUrn, false));
        expect(result.success()).toBeTrue();

        expect(command.getUpdatedLikesCount()).toBe(0);
        databaseAssertions().assertLikesCount(targetUrn, 0);
    }

    private void updateLikesCount() {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds.LIKES_COUNT, 1);
        expect(propeller().update(Table.Sounds, cv, new WhereBuilder()
                .whereEq(TableColumns.Sounds._ID, targetUrn.getNumericId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)).success()).toBeTrue();
        databaseAssertions().assertLikesCount(targetUrn, 1);
    }

}