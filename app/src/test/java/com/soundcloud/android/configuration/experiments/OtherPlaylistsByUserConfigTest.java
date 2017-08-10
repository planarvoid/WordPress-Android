package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.OtherPlaylistsByUserConfig.CONFIGURATION;
import static com.soundcloud.android.configuration.experiments.OtherPlaylistsByUserConfig.CONTROL;
import static com.soundcloud.android.properties.Flag.OTHER_PLAYLISTS_BY_CREATOR;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OtherPlaylistsByUserConfigTest {
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private OtherPlaylistsByUserConfig config;

    @Before
    public void setUp() throws Exception {
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
    public void disabledWhenExperimentIsSetToHide() throws Exception {
        whenVariant(OtherPlaylistsByUserConfig.HIDE_OTHER_PLAYLISTS);

        assertThat(config.isEnabled()).isFalse();
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(CONFIGURATION)).thenReturn(variant);
    }
}
