package com.soundcloud.android.offline;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class OfflinePlaybackOperationsTest {

    private static final Urn TRACK = Urn.forTrack(1);
    @Mock private FeatureOperations featureOperations;
    @Mock private TrackDownloadsStorage trackDownloadsStorage;
    @Mock private TrackOfflineStateProvider trackOfflineStateProvider;

    private OfflinePlaybackOperations operations;


    @Before
    public void setUp() throws Exception {
        operations = new OfflinePlaybackOperations(featureOperations, trackDownloadsStorage, trackOfflineStateProvider);
    }

    @Test
    public void shouldPlayOfflineWhenFeatureEnabledAndTrackDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(trackOfflineStateProvider.getOfflineState(TRACK)).thenReturn(OfflineState.DOWNLOADED);

        assertThat(operations.shouldPlayOffline(TRACK)).isTrue();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureEnabledAndTrackNotDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(trackOfflineStateProvider.getOfflineState(TRACK)).thenReturn(OfflineState.NOT_OFFLINE);

        assertThat(operations.shouldPlayOffline(TRACK)).isFalse();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        assertThat(operations.shouldPlayOffline(TRACK)).isFalse();
    }

    @Test
    public void shouldFindOnlyOfflineTracksInTrackDownloads() {
        List<Urn> tracks = Collections.singletonList(Urn.forTrack(12));

        operations.findOfflineAvailableTracks(tracks);

        verify(trackDownloadsStorage).onlyOfflineTracks(tracks);
    }
}
