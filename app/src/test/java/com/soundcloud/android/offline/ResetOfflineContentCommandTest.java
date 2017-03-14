package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.List;

public class ResetOfflineContentCommandTest extends StorageIntegrationTest {

    private ResetOfflineContentCommand command;

    @Mock SecureFileStorage secureFileStorage;
    @Mock OfflineSettingsStorage offlineSettingsStorage;
    @Mock TrackOfflineStateProvider trackOfflineStateProvider;
    @Mock CurrentDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        when(dateProvider.getCurrentTime()).thenReturn(789L);

        command = new ResetOfflineContentCommand(propeller(),
                                                 secureFileStorage,
                                                 offlineSettingsStorage,
                                                 trackOfflineStateProvider,
                                                 dateProvider);
    }

    @Test
    public void refreshesTrackDownloads() {
        Urn track = Urn.forTrack(123L);
        testFixtures().insertCompletedTrackDownload(track, 123L, 456L);

        List<Urn> resetEntities = command.call(null);

        databaseAssertions().assertDownloadRequestsInserted(singletonList(track));
        assertThat(resetEntities).containsExactly(track);
    }

    @Test
    public void processSuccessfulTxn() {
        command.call(OfflineContentLocation.SD_CARD);

        InOrder inOrder = inOrder(trackOfflineStateProvider, secureFileStorage, offlineSettingsStorage);
        inOrder.verify(trackOfflineStateProvider).clear();
        inOrder.verify(secureFileStorage).deleteAllTracks();
        inOrder.verify(offlineSettingsStorage).setOfflineContentLocation(OfflineContentLocation.SD_CARD);
        inOrder.verify(secureFileStorage).updateOfflineDir();
    }
}
