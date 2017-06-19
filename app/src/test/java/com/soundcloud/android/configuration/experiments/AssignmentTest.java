package com.soundcloud.android.configuration.experiments;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class AssignmentTest {
    private static final Layer FIRST_LAYER = new Layer("firstLayerName", 123, "firstExperimentName", 456, "firstVariantName");
    private static final Layer SECOND_LAYER = new Layer("secondLayerName", 789, "secondExperimentName", 101, "secondVariantName");

    @Test
    public void exposesMultipleCommaSeparatedVariantIds() {
        assertThat(new Assignment(Arrays.asList(FIRST_LAYER, SECOND_LAYER)).commaSeparatedVariantIds()).isEqualTo(Optional.of("456,101"));
    }

    @Test
    public void exposesSingleCommaSeparatedVariantIds() {
        assertThat(new Assignment(singletonList(FIRST_LAYER)).commaSeparatedVariantIds()).isEqualTo(Optional.of("456"));
    }

    @Test
    public void exposesAbsentCommaSeparatedVariantIds() {
        assertThat(new Assignment(emptyList()).commaSeparatedVariantIds()).isEqualTo(Optional.absent());
    }
}
