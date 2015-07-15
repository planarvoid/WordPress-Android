package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class DeleteOfflineTrackCommandTest extends StorageIntegrationTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private SecureFileStorage fileStorage;
    private DeleteOfflineTrackCommand command;

    @Before
    public void setUp() {
        command = new DeleteOfflineTrackCommand(fileStorage, propeller());
    }

    @Test
    public void deleteTrackFromTheFileSystem() throws Exception {
        command.call(Arrays.asList(TRACK_URN));

        verify(fileStorage).deleteTrack(TRACK_URN);
    }

    @Test
    public void deleteTrackFromTheDatabase() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_URN, 100L);
        when(fileStorage.deleteTrack(TRACK_URN)).thenReturn(true);

        final Collection<Urn> deleted = command.call(Collections.singletonList(TRACK_URN));

        databaseAssertions().assertNotDownloaded(TRACK_URN);
        assertThat(deleted).containsExactly(TRACK_URN);
    }

    @Test
    public void doesNotDeleteTrackFromTheDataBaseWhenFailedToDeleteFromTheFileSystem() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_URN, 100L);
        when(fileStorage.deleteTrack(TRACK_URN)).thenReturn(false);

        final Collection<Urn> deleted = command.call(Collections.singletonList(TRACK_URN));

        databaseAssertions().assertDownloadPendingRemoval(TRACK_URN);
        assertThat(deleted).isEmpty();
    }

}