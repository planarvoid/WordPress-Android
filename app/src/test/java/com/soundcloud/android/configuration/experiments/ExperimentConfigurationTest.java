package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Collections;

public class ExperimentConfigurationTest {

    @Test
    public void experimentFromPatternReturnsTrueWhenPatternMatches() {
        final Layer layer = new Layer("layer", 1, "experiment-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromPattern("layer", "experiment-.*");

        assertThat(configuration.matches(layer)).isTrue();
    }

    @Test
    public void experimentFromPatternReturnsFalseWhenPatternDoesNotMatch() {
        final Layer layer = new Layer("layer", 1, "something-else-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromPattern("layer", "experiment-.*");

        assertThat(configuration.matches(layer)).isFalse();
    }

    @Test
    public void experimentFromNameReturnsTrueWhenNameAreIdentical() {
        final Layer layer = new Layer("layer", 1, "experiment-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer",
                                                                                       "experiment-1",
                                                                                       Collections.emptyList());

        assertThat(configuration.matches(layer)).isTrue();
    }

    @Test
    public void experimentFromNameReturnsFalseWhenNameIsDifferent() {
        final Layer layer = new Layer("layer", 1, "experiment-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer",
                                                                                       "experiment-.*",
                                                                                       Collections.emptyList());

        assertThat(configuration.matches(layer)).isFalse();
    }

    @Test
    public void experimentReturnsFalseWhenLayersAreDifferent() {
        final Layer layer = new Layer("another-layer", 1, "experiment-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer",
                                                                                       "experiment-1",
                                                                                       Collections.emptyList());

        assertThat(configuration.matches(layer)).isFalse();
    }
}
