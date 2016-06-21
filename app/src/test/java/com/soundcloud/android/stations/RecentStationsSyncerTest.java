package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.WriteRecentStationsCollectionsCommand.SyncCollectionsMetadata;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class RecentStationsSyncerTest {

    private RecentStationsSyncer syncer;
    private CurrentDateProvider dateProvider;
    @Mock private WriteRecentStationsCollectionsCommand command;
    @Mock private StationsApi api;
    @Mock private StationsStorage storage;
    @Captor private ArgumentCaptor<SyncCollectionsMetadata> captor;

    @Before
    public void setUp() {
        dateProvider = new TestDateProvider(System.currentTimeMillis());
        syncer = new RecentStationsSyncer(api, command, dateProvider, storage);
    }

    @Test
    public void shouldAcceptAllRemoteContentAndKeepLocalContentWhenUpdatedDuringSyncing() throws Exception {
        final Urn station = Urn.forTrackStation(1L);
        final ApiStationsCollections remoteContent = StationFixtures.collections(
                Collections.singletonList(station)
        );
        final SyncCollectionsMetadata metadata = new SyncCollectionsMetadata(dateProvider.getCurrentTime(), remoteContent);
        when(api.syncStationsCollections(anyList())).thenReturn(remoteContent);
        when(command.call(metadata)).thenReturn(true);

        assertThat(syncer.call()).isTrue();

        verify(command).call(eq(metadata));
    }

    @Test
    public void shouldNotSetSyncedStateWhenWriteFailed() throws Exception {
        final Urn station = Urn.forTrackStation(1L);
        final ApiStationsCollections remoteContent = StationFixtures.collections(
                Collections.singletonList(station)
        );
        final SyncCollectionsMetadata metadata = new SyncCollectionsMetadata(dateProvider.getCurrentTime(), remoteContent);
        when(api.syncStationsCollections(anyList())).thenReturn(remoteContent);
        when(command.call(metadata)).thenReturn(false);

        assertThat(syncer.call()).isFalse();
    }

    @Test(expected = Exception.class)
    public void shouldNoOpWhenAnErrorOccurredWhenRetrievingRemoteContent() throws Exception {
        when(api.syncStationsCollections(anyList())).thenThrow(new RuntimeException("Test exception."));

        syncer.call();

        verifyNoMoreInteractions(command);
    }
}
