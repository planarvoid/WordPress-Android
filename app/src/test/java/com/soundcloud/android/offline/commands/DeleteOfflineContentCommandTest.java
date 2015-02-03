package com.soundcloud.android.offline.commands;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

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

@RunWith(SoundCloudTestRunner.class)
public class DeleteOfflineContentCommandTest extends StorageIntegrationTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private SecureFileStorage fileStorage;
    private DeleteOfflineContentCommand command;

    @Before
    public void setUp() {
        command = new DeleteOfflineContentCommand(fileStorage, propeller());
    }

    @Test
    public void deleteRemovedTrack() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_URN, 100L);

        command.with(Arrays.asList(TRACK_URN)).call();

        assertDownloadPendingRemovalIsAbsent(TRACK_URN, counts(0));
    }

    public void assertDownloadPendingRemovalIsAbsent(Urn trackUrn, RowCountMatcher count) {
        assertThat(select(from(Table.TrackDownloads.name())
                .whereEq(TableColumns.TrackDownloads._ID, trackUrn.getNumericId())
                .whereNotNull(TableColumns.TrackDownloads.REMOVED_AT)), count);
    }
}