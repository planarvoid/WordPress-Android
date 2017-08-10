package com.soundcloud.android.stations;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class RecommendedStationsSyncerTest {

    @Mock WriteStationsRecommendationsCommand command;
    @Mock StationsApi api;
    @Mock StationsStorage storage;

    private RecommendedStationsSyncer syncer;

    @Before
    public void setUp() {
        syncer = new RecommendedStationsSyncer(api, command);
    }

    @Test
    public void shouldStoreRecommendationsAndUpdateLastSync() throws Exception {
        final Urn station = Urn.forTrackStation(1L);
        final ModelCollection<ApiStationMetadata> metadata = StationFixtures.createStationsCollection(Collections.singletonList(
                station));

        when(api.fetchStationRecommendations()).thenReturn(metadata);
        when(command.call(metadata)).thenReturn(true);

        assertThat(syncer.call()).isTrue();

        verify(command).call(eq(metadata));
    }

    @Test(expected = Exception.class)
    public void shouldNoOpWhenAnErrorOccurredWhenRetrievingRemoteContent() throws Exception {
        when(api.fetchStationRecommendations()).thenThrow(new RuntimeException("Test exception."));

        syncer.call();

        verifyNoMoreInteractions(command);
    }
}
