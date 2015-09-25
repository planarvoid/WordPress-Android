package com.soundcloud.android.stations;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.WriteStationsCollectionsCommand.SyncCollectionsMetadata;
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
public class StationsSyncerTest {

    private StationsSyncer syncer;
    private CurrentDateProvider dateProvider;
    @Mock private WriteStationsCollectionsCommand command;
    @Mock private StationsApi api;
    @Mock private StationsStorage storage;
    @Captor private ArgumentCaptor<SyncCollectionsMetadata> captor;

    @Before
    public void setUp() {
        dateProvider = new TestDateProvider(System.currentTimeMillis());
        syncer = new StationsSyncer(api, command, dateProvider, storage);
    }

    @Test
    public void shouldAcceptAllRemoteContentAndKeepLocalContentWhenUpdatedDuringSyncing() throws Exception {
        final Urn station = Urn.forTrackStation(1L);
        final ApiStationsCollections remoteContent = StationFixtures.collections(
                Collections.singletonList(station),
                Collections.<Urn>emptyList(),
                Collections.<Urn>emptyList(),
                Collections.<Urn>emptyList(),
                Collections.<Urn>emptyList()
        );

        when(api.syncStationsCollections(anyList())).thenReturn(remoteContent);

        syncer.call();

        final SyncCollectionsMetadata metadata = new SyncCollectionsMetadata(dateProvider.getCurrentTime(), remoteContent);
        verify(command).call(eq(metadata));
    }

    @Test(expected = Exception.class)
    public void shouldNoOpWhenAnErrorOccurredWhenRetrievingRemoteContent() throws Exception {
        when(api.syncStationsCollections(anyList())).thenThrow(new RuntimeException("Test exception."));

        syncer.call();

        verifyNoMoreInteractions(command);
    }
}