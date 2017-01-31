package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchPlayRelatedTracksConfigTest {
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private SearchPlayRelatedTracksConfig config;

    @Before
    public void setUp() throws Exception {
        config = new SearchPlayRelatedTracksConfig(experimentOperations, featureFlags);
    }

    @Test
    public void enabledIfFlagEnabled() throws Exception {
        when(featureFlags.isEnabled(Flag.SEARCH_PLAY_RELATED_TRACKS)).thenReturn(true);
        whenVariant("nope");

        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    public void enabledIfExperimentEnabled() throws Exception {
        when(featureFlags.isEnabled(Flag.SEARCH_PLAY_RELATED_TRACKS)).thenReturn(false);
        whenVariant("related_tracks_on_search_results");

        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    public void disabledIfFlagAndExperimentDisabled() throws Exception {
        when(featureFlags.isEnabled(Flag.SEARCH_PLAY_RELATED_TRACKS)).thenReturn(false);
        whenVariant("nope");

        assertThat(config.isEnabled()).isFalse();
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(SearchPlayRelatedTracksConfig.CONFIGURATION)).thenReturn(variant);
    }
}
