package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.StaticDiscoverContentExperiment.VARIANT_CONTROL;
import static com.soundcloud.android.configuration.experiments.StaticDiscoverContentExperiment.VARIANT_STATIC_DISCOVER_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StaticDiscoverContentExperimentTest {

    @Mock private ExperimentOperations experimentOperations;

    private StaticDiscoverContentExperiment staticDiscoverContentExperiment;

    @Before
    public void setUp() throws Exception {
        staticDiscoverContentExperiment = new StaticDiscoverContentExperiment(experimentOperations);
    }

    @Test
    public void isEnabled() throws Exception {
        mockVariant(VARIANT_STATIC_DISCOVER_CONTENT);
        assertThat(staticDiscoverContentExperiment.isEnabled()).isTrue();

        mockVariant(VARIANT_CONTROL);
        assertThat(staticDiscoverContentExperiment.isEnabled()).isFalse();
    }

    private void mockVariant(String variant) {
        when(experimentOperations.getExperimentVariant(StaticDiscoverContentExperiment.CONFIGURATION)).thenReturn(variant);
    }

}
