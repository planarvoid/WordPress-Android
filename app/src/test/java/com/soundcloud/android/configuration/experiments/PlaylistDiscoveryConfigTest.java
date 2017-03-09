package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistDiscoveryConfigTest {

    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;
    private PlaylistDiscoveryConfig playlistDiscoveryConfig;

    @Before
    public void setUp() throws Exception {
        playlistDiscoveryConfig = new PlaylistDiscoveryConfig(experimentOperations, featureFlags);
    }

    @Test
    public void testDisabledAndControlVariant() throws Exception {
        setPreconditions(false, PlaylistDiscoveryConfig.VARIANT_CONTROL);

        assertThat(playlistDiscoveryConfig.isEnabled()).isFalse();
    }

    @Test
    public void testDisabledAndVariantPlaylistsFirst() throws Exception {
        setPreconditions(false, PlaylistDiscoveryConfig.VARIANT_PLAYLIST_DISCOVERY_FIRST);

        assertThat(playlistDiscoveryConfig.isEnabled()).isTrue();
    }

    @Test
    public void testDisabledAndVariantStationsFirst() throws Exception {
        setPreconditions(false, PlaylistDiscoveryConfig.VARIANT_SUGGESTED_STATIONS_FIRST);

        assertThat(playlistDiscoveryConfig.isEnabled()).isTrue();
    }

    @Test
    public void testEnabledAndControlVariant() throws Exception {
        setPreconditions(true, PlaylistDiscoveryConfig.VARIANT_CONTROL);

        assertThat(playlistDiscoveryConfig.isEnabled()).isTrue();
    }

    @Test
    public void testEnabledAndVariantPlaylistsFirst() throws Exception {
        setPreconditions(true, PlaylistDiscoveryConfig.VARIANT_PLAYLIST_DISCOVERY_FIRST);

        assertThat(playlistDiscoveryConfig.isEnabled()).isTrue();
    }

    @Test
    public void testEnabledAndVariantStationsFirst() throws Exception {
        setPreconditions(true, PlaylistDiscoveryConfig.VARIANT_SUGGESTED_STATIONS_FIRST);

        assertThat(playlistDiscoveryConfig.isEnabled()).isTrue();
    }

    @Test
    public void testsControlGroupPlaylistsNotFirst() throws Exception {
        setPreconditions(false, PlaylistDiscoveryConfig.VARIANT_CONTROL);

        assertThat(playlistDiscoveryConfig.isPlaylistDiscoveryFirst()).isFalse();
    }

    @Test
    public void testsWrongVariantPlaylistsNotFirst() throws Exception {
        setPreconditions(false, PlaylistDiscoveryConfig.VARIANT_SUGGESTED_STATIONS_FIRST);

        assertThat(playlistDiscoveryConfig.isPlaylistDiscoveryFirst()).isFalse();
    }

    @Test
    public void testPlaylistsFirst() throws Exception {
        setPreconditions(false, PlaylistDiscoveryConfig.VARIANT_PLAYLIST_DISCOVERY_FIRST);

        assertThat(playlistDiscoveryConfig.isPlaylistDiscoveryFirst()).isTrue();
    }

    private void setPreconditions(boolean featureFlag, String variant) {
        when(featureFlags.isEnabled(Flag.NEW_HOME)).thenReturn(featureFlag);
        when(experimentOperations.getExperimentVariant(PlaylistDiscoveryConfig.CONFIGURATION)).thenReturn(variant);
    }
}
