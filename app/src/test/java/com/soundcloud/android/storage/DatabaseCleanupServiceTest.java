package com.soundcloud.android.storage;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.storage.DatabaseCleanupService.CleanupHelper;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

public class DatabaseCleanupServiceTest extends StorageIntegrationTest {

    private DatabaseCleanupService service;

    @Mock private CleanupHelper cleanupHelper1;
    @Mock private CleanupHelper cleanupHelper2;

    @Before
    public void setUp() throws Exception {
        service = new DatabaseCleanupService(propeller(), asList(cleanupHelper1, cleanupHelper2));
    }

    @Test
    public void cleansUpUsers() throws Exception {
        ApiUser user1 = testFixtures().insertUser();
        ApiUser user2 = testFixtures().insertUser();
        ApiUser user3 = testFixtures().insertUser();

        when(cleanupHelper1.getUsersToKeep()).thenReturn(singleton(user1.getUrn()));
        when(cleanupHelper1.getTracksToKeep()).thenReturn(emptySet());
        when(cleanupHelper1.getPlaylistsToKeep()).thenReturn(emptySet());

        when(cleanupHelper2.getUsersToKeep()).thenReturn(singleton(user2.getUrn()));
        when(cleanupHelper2.getTracksToKeep()).thenReturn(emptySet());
        when(cleanupHelper2.getPlaylistsToKeep()).thenReturn(emptySet());

        service.onHandleIntent(new Intent());

        databaseAssertions().assertUserInserted(user1);
        databaseAssertions().assertUserInserted(user2);
        databaseAssertions().assertUserNotStored(user3.getUrn());
    }

    @Test
    public void cleansUpTracksAndUsers() throws Exception {
        ApiUser user1 = testFixtures().insertUser();
        ApiUser user2 = testFixtures().insertUser();

        ApiTrack apiTrack1 = testFixtures().insertTrack();
        ApiTrack apiTrack2 = testFixtures().insertTrack();

        when(cleanupHelper1.getUsersToKeep()).thenReturn(singleton(user1.getUrn()));
        when(cleanupHelper1.getTracksToKeep()).thenReturn(emptySet());
        when(cleanupHelper1.getPlaylistsToKeep()).thenReturn(emptySet());

        when(cleanupHelper2.getUsersToKeep()).thenReturn(emptySet());
        when(cleanupHelper2.getTracksToKeep()).thenReturn(singleton(apiTrack1.getUrn()));
        when(cleanupHelper2.getPlaylistsToKeep()).thenReturn(emptySet());

        service.onHandleIntent(new Intent());

        databaseAssertions().assertUserInserted(user1);
        databaseAssertions().assertUserNotStored(user2.getUrn());

        databaseAssertions().assertTrackInserted(apiTrack1);
        databaseAssertions().assertUserInserted(apiTrack1.getUser());

        databaseAssertions().assertTrackNotInserted(apiTrack2.getUrn());
        databaseAssertions().assertUserNotStored(apiTrack2.getUser().getUrn());
    }

    @Test
    public void cleansUpPlaylists() throws Exception {

        ApiPlaylist playlistToKeep = testFixtures().insertPlaylist();
        ApiTrack playlistToKeepTrack = testFixtures().insertPlaylistTrack(playlistToKeep, 0);

        ApiPlaylist playlistToDelete = testFixtures().insertPlaylist();
        ApiTrack playlistToDeleteTrack = testFixtures().insertPlaylistTrack(playlistToDelete, 0);

        when(cleanupHelper1.getPlaylistsToKeep()).thenReturn(singleton(playlistToKeep.getUrn()));
        when(cleanupHelper1.getTracksToKeep()).thenReturn(emptySet());
        when(cleanupHelper1.getUsersToKeep()).thenReturn(emptySet());

        service.onHandleIntent(new Intent());

        databaseAssertions().assertPlaylistInserted(playlistToKeep);
        databaseAssertions().assertTrackInserted(playlistToKeepTrack);
        databaseAssertions().assertUserInserted(playlistToKeepTrack.getUser());
        databaseAssertions().assertUserInserted(playlistToKeep.getUser());

        databaseAssertions().assertPlaylistNotStored(playlistToDelete.getUrn());
        databaseAssertions().assertTrackNotInserted(playlistToDeleteTrack.getUrn());
        databaseAssertions().assertUserNotStored(playlistToDelete.getUser().getUrn());
        databaseAssertions().assertUserNotStored(playlistToDeleteTrack.getUser().getUrn());
    }
}
