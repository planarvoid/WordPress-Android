package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class RecommendedStationsSyncerTest {

    @Mock SyncStateStorage syncStateStorage;
    @Mock WriteStationsRecommendationsCommand command;
    @Mock StationsApi api;
    @Mock StationsStorage storage;

    private RecommendedStationsSyncer syncer;

    @Before
    public void setUp() {
        syncer = new RecommendedStationsSyncer(syncStateStorage, api, command);
    }

    @Test
    public void shouldStoreRecommendationsAndUpdateLastSync() throws Exception {
        final Urn station = Urn.forTrackStation(1L);
        final ModelCollection<ApiStationMetadata> metadata = StationFixtures.createStationsCollection(Collections.singletonList(station));

        when(api.fetchStationRecommendations()).thenReturn(metadata);
        when(command.call(metadata)).thenReturn(true);

        assertThat(syncer.call()).isTrue();

        verify(command).call(eq(metadata));
        verify(syncStateStorage).synced(Syncable.RECOMMENDED_STATIONS);
    }

    @Test(expected = Exception.class)
    public void shouldNoOpWhenAnErrorOccurredWhenRetrievingRemoteContent() throws Exception {
        when(api.fetchStationRecommendations()).thenThrow(new RuntimeException("Test exception."));

        syncer.call();

        verifyNoMoreInteractions(command);
        verifyNoMoreInteractions(syncStateStorage);
    }
}
