package com.soundcloud.android.storage;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

public class DatabaseCleanupServiceTest extends StorageIntegrationTest {

    private DatabaseCleanupService service;

    @Mock private CleanupHelper cleanupHelper1;
    @Mock private CleanupHelper cleanupHelper2;
    @Mock private FeatureFlags featureFlags;
    private TestEventBusV2 eventBusV2 = new TestEventBusV2();

    @Before
    public void setUp() throws Exception {
        service = new DatabaseCleanupService(propeller(), eventBusV2, asList(cleanupHelper1, cleanupHelper2), featureFlags);
        when(featureFlags.isEnabled(Flag.DATABASE_CLEANUP_SERVICE)).thenReturn(true);
    }

    @Test
    public void cleansUpUsers() throws Exception {
        ApiUser user1 = testFixtures().insertUser();
        ApiUser user2 = testFixtures().insertUser();
        ApiUser user3 = testFixtures().insertUser();

        when(cleanupHelper1.usersToKeep()).thenReturn(singleton(user1.getUrn()));
        when(cleanupHelper1.tracksToKeep()).thenReturn(emptySet());
        when(cleanupHelper1.playlistsToKeep()).thenReturn(emptySet());

        when(cleanupHelper2.usersToKeep()).thenReturn(singleton(user2.getUrn()));
        when(cleanupHelper2.tracksToKeep()).thenReturn(emptySet());
        when(cleanupHelper2.playlistsToKeep()).thenReturn(emptySet());

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

        when(cleanupHelper1.usersToKeep()).thenReturn(singleton(user1.getUrn()));
        when(cleanupHelper1.tracksToKeep()).thenReturn(emptySet());
        when(cleanupHelper1.playlistsToKeep()).thenReturn(emptySet());

        when(cleanupHelper2.usersToKeep()).thenReturn(emptySet());
        when(cleanupHelper2.tracksToKeep()).thenReturn(singleton(apiTrack1.getUrn()));
        when(cleanupHelper2.playlistsToKeep()).thenReturn(emptySet());

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

        when(cleanupHelper1.playlistsToKeep()).thenReturn(singleton(playlistToKeep.getUrn()));
        when(cleanupHelper1.tracksToKeep()).thenReturn(emptySet());
        when(cleanupHelper1.usersToKeep()).thenReturn(emptySet());

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

    @Test
    public void sendsCleanupMetrics() throws Exception {

        ApiPlaylist playlistToKeep = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrack(playlistToKeep, 0);

        ApiPlaylist playlistToDelete = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrack(playlistToDelete, 0);

        when(cleanupHelper1.playlistsToKeep()).thenReturn(singleton(playlistToKeep.getUrn()));
        when(cleanupHelper1.tracksToKeep()).thenReturn(emptySet());
        when(cleanupHelper1.usersToKeep()).thenReturn(emptySet());

        service.onHandleIntent(new Intent());

        assertThat(eventBusV2.lastEventOn(EventQueue.TRACKING)).isEqualTo(StorageCleanupEvent.create(2, 1, 1));
    }
}
