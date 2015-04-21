package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.test.matchers.RowCountMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;

@RunWith(SoundCloudTestRunner.class)
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

        final Collection<Urn> deleted = command.call(Arrays.asList(TRACK_URN));

        assertDownloadPendingRemovalCount(TRACK_URN, counts(0));
        expect(deleted).toContainExactly(TRACK_URN);
    }

    @Test
    public void doesNotDeleteTrackFromTheDataBaseWhenFailedToDeleteFromTheFileSystem() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_URN, 100L);
        when(fileStorage.deleteTrack(TRACK_URN)).thenReturn(false);

        final Collection<Urn> deleted = command.call(Arrays.asList(TRACK_URN));

        assertDownloadPendingRemovalCount(TRACK_URN, counts(1));
        expect(deleted).toBeEmpty();
    }

    public void assertDownloadPendingRemovalCount(Urn trackUrn, RowCountMatcher count) {
        assertThat(select(from(Table.TrackDownloads.name())
                .whereEq(TableColumns.TrackDownloads._ID, trackUrn.getNumericId())
                .whereNotNull(TableColumns.TrackDownloads.REMOVED_AT)), count);
    }

    public void assertDownloadedTrackCount(Urn trackUrn, RowCountMatcher count) {
        assertThat(select(from(Table.TrackDownloads.name())
                .whereEq(TableColumns.TrackDownloads._ID, trackUrn.getNumericId())
                .whereNull(TableColumns.TrackDownloads.REMOVED_AT)), count);
    }
}