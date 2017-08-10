package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.groupie.ExperimentConfiguration;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TopResultsBackendExperimentsTest {
    @Mock private ExperimentOperations experimentOperations;

    private TopResultsBackendExperiments topResultsBackendExperiments;

    @Before
    public void setUp() throws Exception {
        topResultsBackendExperiments = new TopResultsBackendExperiments(experimentOperations);
    }

    @Test
    public void hideTopResultsInControl1Variant() throws Exception {
        for (ExperimentConfiguration experimentConfiguration : allExperiments()) {
            enableVariantInExperiment(TopResultsBackendExperiments.VARIANT_CONTROL_1, experimentConfiguration);

            assertThat(topResultsBackendExperiments.topResultsEnabled()).isFalse();
        }
    }

    @Test
    public void hideTopResultsInControl2Variant() throws Exception {
        for (ExperimentConfiguration experimentConfiguration : allExperiments()) {
            enableVariantInExperiment(TopResultsBackendExperiments.VARIANT_CONTROL_2, experimentConfiguration);

            assertThat(topResultsBackendExperiments.topResultsEnabled()).isFalse();
        }
    }

    @Test
    public void hideTopResultsInTopResultsMixedListVariant() throws Exception {
        for (ExperimentConfiguration experimentConfiguration : allExperiments()) {
            enableVariantInExperiment(TopResultsBackendExperiments.VARIANT_TOP_RESULT_MIXED_LIST, experimentConfiguration);

            assertThat(topResultsBackendExperiments.topResultsEnabled()).isFalse();
        }
    }

    @Test
    public void showTopResultsInTopResultVariant() throws Exception {
        for (ExperimentConfiguration experimentConfiguration : allExperiments()) {
            enableVariantInExperiment(TopResultsBackendExperiments.VARIANT_TOP_RESULT, experimentConfiguration);

            assertThat(topResultsBackendExperiments.topResultsEnabled()).isTrue();
        }
    }

    @Test
    public void showTopResultsInFixedBucketsVariant() throws Exception {
        for (ExperimentConfiguration experimentConfiguration : allExperiments()) {
            enableVariantInExperiment(TopResultsBackendExperiments.VARIANT_FIXED_BUCKETS, experimentConfiguration);

            assertThat(topResultsBackendExperiments.topResultsEnabled()).isTrue();
        }
    }

    @Test
    public void showTopResultsInVariableBucketsVariant() throws Exception {
        for (ExperimentConfiguration experimentConfiguration : allExperiments()) {
            enableVariantInExperiment(TopResultsBackendExperiments.VARIANT_VARIABLE_BUCKETS, experimentConfiguration);

            assertThat(topResultsBackendExperiments.topResultsEnabled()).isTrue();
        }
    }

    private List<ExperimentConfiguration> allExperiments() {
        return Lists.newArrayList(TopResultsBackendExperiments.OrderOfBucketsExperiment.CONFIGURATION,
                                  TopResultsBackendExperiments.MoreResultsExperiment.CONFIGURATION,
                                  TopResultsBackendExperiments.CombinedImprovementsExperiment.CONFIGURATION);
    }

    private void enableVariantInExperiment(String variant, ExperimentConfiguration experiment) {
        when(experimentOperations.getOptionalExperimentVariant(any(ExperimentConfiguration.class))).thenReturn(Optional.absent());
        when(experimentOperations.getOptionalExperimentVariant(experiment)).thenReturn(Optional.of(variant));
    }
}
