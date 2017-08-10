package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistAndAlbumsPreviewsExperimentTest {
    @Mock private ExperimentOperations experimentOperations;

    private PlaylistAndAlbumsPreviewsExperiment config;

    @Before
    public void setUp() throws Exception {
        config = new PlaylistAndAlbumsPreviewsExperiment(experimentOperations);
    }

    @Test
    public void enabledIfExperimentEnabled() throws Exception {
        whenVariant("albums_bucket");

        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    public void disabledIfExperimentDisabled() throws Exception {
        whenVariant("nope");

        assertThat(config.isEnabled()).isFalse();
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(PlaylistAndAlbumsPreviewsExperiment.CONFIGURATION)).thenReturn(variant);
    }
}
