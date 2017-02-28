package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.OtherPlaylistsByUserConfig.CONFIGURATION;
import static com.soundcloud.android.configuration.experiments.OtherPlaylistsByUserConfig.CONTROL;
import static com.soundcloud.android.properties.Flag.NEW_PLAYLIST_SCREEN;
import static com.soundcloud.android.properties.Flag.OTHER_PLAYLISTS_BY_CREATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class OtherPlaylistsByUserConfigTest extends AndroidUnitTest {
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private OtherPlaylistsByUserConfig config;

    @Before
    public void setUp() throws Exception {
        when(featureFlags.isEnabled(NEW_PLAYLIST_SCREEN)).thenReturn(true);
        when(featureFlags.isEnabled(OTHER_PLAYLISTS_BY_CREATOR)).thenReturn(true);
        whenVariant(CONTROL);

        config = new OtherPlaylistsByUserConfig(experimentOperations, featureFlags);
    }

    @Test
    public void enabledWhenNewPlaylistAndFlagAndExperimentAreAllEnabled() throws Exception {
        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    public void disabledWhenFeatureFlagDisabled() throws Exception {
        when(featureFlags.isEnabled(OTHER_PLAYLISTS_BY_CREATOR)).thenReturn(false);

        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    public void disabledWhenNewPlaylistScreenDisabled() throws Exception {
        when(featureFlags.isEnabled(NEW_PLAYLIST_SCREEN)).thenReturn(false);

        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    public void disabledWhenExperimentIsSetToHide() throws Exception {
        whenVariant(OtherPlaylistsByUserConfig.HIDE_OTHER_PLAYLISTS);

        assertThat(config.isEnabled()).isFalse();
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(CONFIGURATION)).thenReturn(variant);
    }
}
