package com.soundcloud.android.offline;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class OfflinePlaybackOperationsTest {

    @Mock private FeatureOperations featureOperations;
    @Mock private TrackDownloadsStorage trackDownloadsStorage;

    private OfflinePlaybackOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new OfflinePlaybackOperations(featureOperations, trackDownloadsStorage);
    }

    @Test
    public void shouldPlayOfflineWhenFeatureEnabledAndTrackDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        assertThat(operations.shouldPlayOffline(downloadedTrack())).isTrue();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureEnabledAndTrackRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        assertThat(operations.shouldPlayOffline(removedTrack())).isFalse();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureEnabledAndTrackNotDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        assertThat(operations.shouldPlayOffline(notDownloadedTrack())).isFalse();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        assertThat(operations.shouldPlayOffline(downloadedTrack())).isFalse();
    }

    @Test
    public void shouldFindOnlyOfflineTracksInTrackDownloads() {
        List<Urn> tracks = Collections.singletonList(Urn.forTrack(12));

        operations.findOfflineAvailableTracks(tracks);

        verify(trackDownloadsStorage).onlyOfflineTracks(tracks);
    }

    private TrackItem downloadedTrack() {
        return PlayableFixtures.downloadedTrack();
    }

    private TrackItem removedTrack() {
        return PlayableFixtures.removedTrack();
    }

    private TrackItem notDownloadedTrack() {
        return ModelFixtures.trackItem();
    }
}
