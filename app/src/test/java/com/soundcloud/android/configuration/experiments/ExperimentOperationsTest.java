package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.groupie.ExperimentConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentOperationsTest {

    private ExperimentOperations operations;

    @Mock private ExperimentStorage experimentStorage;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private Assignment assignment;

    @Before
    public void setUp() throws Exception {
        when(experimentStorage.readAssignment()).thenReturn(assignment);
        operations = new ExperimentOperations(experimentStorage, applicationProperties);
    }

    @Test
    public void shouldLoadAssignmentWhenCreated() {
        assertThat(operations.getAssignment()).isSameAs(assignment);
    }

    @Test
    public void shouldGetEmptyAssignmentIfNoAssignmentIsStored() {
        when(experimentStorage.readAssignment()).thenReturn(Assignment.empty());

        operations.loadAssignment();
        Assignment assignment = operations.getAssignment();

        assertThat(assignment.isEmpty()).isTrue();
    }

    @Test
    public void shouldGetAssignmentIfAssignmentIsStored() {
        Assignment reloadedAssignment = ModelFixtures.create(Assignment.class);
        when(experimentStorage.readAssignment()).thenReturn(reloadedAssignment);

        operations.loadAssignment();

        assertThat(operations.getAssignment()).isSameAs(reloadedAssignment);
    }

    @Test
    public void updateShouldSaveAssignment() {
        Assignment assignment = ModelFixtures.create(Assignment.class);

        operations.update(assignment);

        verify(experimentStorage).storeAssignment(eq(assignment));
    }

    @Test
    public void shouldReturnUpdatedAssignmentAfterUpdate() {
        Assignment assignment = ModelFixtures.create(Assignment.class);

        operations.update(assignment);

        assertThat(operations.getAssignment()).isSameAs(assignment);
    }

    @Test
    public void shouldReturnLayerWhenAssigned() {
        final Layer layer = new Layer("layer", 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(layer.getLayerName(), layer.getExperimentName(), Collections.emptyList());
        when(experimentStorage.readAssignment()).thenReturn(assignment);

        operations.loadAssignment();
        assertThat(operations.findLayer(configuration).get()).isEqualTo(layer);
    }

    @Test
    public void shouldFindUpdatedLayer() {
        final Layer layer = new Layer("layer", 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(layer.getLayerName(), layer.getExperimentName(), Collections.emptyList());

        operations.update(assignment);

        assertThat(operations.findLayer(configuration).get()).isEqualTo(layer);
    }

    @Test
    public void shouldReturnAbsentWhenNotAssigned() {
        final String layerName = "layer";
        final Assignment assignment = createAssignment(layerName);
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(layerName, "unknown", Collections.emptyList());

        when(experimentStorage.readAssignment()).thenReturn(assignment);

        operations.loadAssignment();
        assertThat(operations.findLayer(configuration).isPresent()).isFalse();
    }

    @Test
    public void experimentFromPatternReturnsTrueWhenPatternMatches() {
        final Layer layer = new Layer("layer", 1, "experiment-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromPattern("layer", "experiment-.*");

        assertThat(ExperimentOperations.matches(configuration, layer)).isTrue();
    }

    @Test
    public void experimentFromPatternReturnsFalseWhenPatternDoesNotMatch() {
        final Layer layer = new Layer("layer", 1, "something-else-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromPattern("layer", "experiment-.*");

        assertThat(ExperimentOperations.matches(configuration, layer)).isFalse();
    }

    @Test
    public void experimentFromNameReturnsTrueWhenNameAreIdentical() {
        final Layer layer = new Layer("layer", 1, "experiment-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer", "experiment-1", Collections.emptyList());

        assertThat(ExperimentOperations.matches(configuration, layer)).isTrue();
    }

    @Test
    public void experimentFromNameReturnsFalseWhenNameIsDifferent() {
        final Layer layer = new Layer("layer", 1, "experiment-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer", "experiment-.*", Collections.emptyList());

        assertThat(ExperimentOperations.matches(configuration, layer)).isFalse();
    }

    @Test
    public void experimentReturnsFalseWhenLayersAreDifferent() {
        final Layer layer = new Layer("another-layer", 1, "experiment-1", 0, "variant-0");
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer", "experiment-1", Collections.emptyList());

        assertThat(ExperimentOperations.matches(configuration, layer)).isFalse();
    }

    private Assignment createAssignment(String layerName) {
        final Layer layer = new Layer(layerName, 1, "experiment", 1, "variant");
        return new Assignment(Collections.singletonList(layer));
    }

}
