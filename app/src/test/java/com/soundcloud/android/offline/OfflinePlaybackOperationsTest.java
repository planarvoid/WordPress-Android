package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class OfflinePlaybackOperationsTest extends AndroidUnitTest {

    @Mock private FeatureOperations featureOperations;

    private OfflinePlaybackOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new OfflinePlaybackOperations(featureOperations);
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

    private PropertySet downloadedTrack() {
        return PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(OfflineState.DOWNLOADED));
    }

    private PropertySet removedTrack() {
        return PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(OfflineState.NOT_OFFLINE));
    }

    private PropertySet notDownloadedTrack() {
        return PropertySet.create();
    }

}
