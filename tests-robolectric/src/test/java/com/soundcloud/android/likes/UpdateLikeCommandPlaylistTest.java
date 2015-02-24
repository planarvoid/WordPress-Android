package com.soundcloud.android.likes;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class UpdateLikeCommandPlaylistTest extends StorageIntegrationTest {

    private UpdateLikeCommand command;
    private PropertySet playlist;

    @Before
    public void setUp() throws Exception {
        command = new UpdateLikeCommand(propeller());
    }

    @Test
    public void upsertLikedPlaylist() throws Exception {
        setUpPlaylistForLiking();

        command.with(playlist).call();

        databaseAssertions().assertLikedPlaylistPendingAddition(playlist);
    }

    @Test
    public void upsertUnlikedPlaylist() throws Exception {
        setUpPlaylistForUnLiking();

        command.with(playlist).call();

        databaseAssertions().assertLikedPlaylistPendingRemoval(playlist);
    }

    private void setUpPlaylistForLiking() {
        final Date created = new Date();
        playlist = testFixtures().insertPlaylist()
                .toPropertySet()
                .put(LikeProperty.CREATED_AT, created)
                .put(LikeProperty.ADDED_AT, created)
                .put(PlayableProperty.IS_LIKED, true);
    }

    private void setUpPlaylistForUnLiking() {
        final Date created = new Date();
        playlist = testFixtures().insertPlaylist()
                .toPropertySet()
                .put(LikeProperty.CREATED_AT, created)
                .put(LikeProperty.REMOVED_AT, created)
                .put(PlayableProperty.IS_LIKED, false);
    }

}