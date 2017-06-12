package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.groupie.ExperimentConfiguration;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentOperationsTest {

    private static final String LAYER_NAME = "layer";
    private static final String ANOTHER_LAYER_NAME = "another-layer";
    private static final String EXPERIMENT_1 = "experiment-1";
    private static final String EXPERIMENT_2 = "experiment-2";
    private static final String DIFFERENT_EXPERIMENT = "something-else-1";
    private static final String VARIANT_0 = "variant-0";
    private static final String EXPERIMENT_PATTERN = "experiment-.*";
    private static final int VARIANT_ID_1 = 1;
    private static final String SERIALIZED_VARIANT_ID = "1";
    private static final int VARIANT_ID_2 = 2;
    private static final String MULTIPLE_SERIALIZED_VARIANT_IDS = "1,2";
    private ExperimentOperations operations;

    @Mock private ExperimentStorage experimentStorage;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private Assignment assignment;

    @Before
    public void setUp() throws Exception {
        when(experimentStorage.readAssignment()).thenReturn(assignment);
        when(applicationProperties.isDevelopmentMode()).thenReturn(false);
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
        final Layer layer = new Layer(LAYER_NAME, 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(layer.getLayerName(), layer.getExperimentName(), Collections.emptyList());
        when(experimentStorage.readAssignment()).thenReturn(assignment);

        operations.loadAssignment();
        assertThat(operations.findLayer(configuration).get()).isEqualTo(layer);
    }

    @Test
    public void shouldFindUpdatedLayer() {
        final Layer layer = new Layer(LAYER_NAME, 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(layer.getLayerName(), layer.getExperimentName(), Collections.emptyList());

        operations.update(assignment);

        assertThat(operations.findLayer(configuration).get()).isEqualTo(layer);
    }

    @Test
    public void shouldReturnAbsentWhenNotAssigned() {
        final String layerName = LAYER_NAME;
        final Assignment assignment = createAssignment(layerName);
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(layerName, "unknown", Collections.emptyList());

        when(experimentStorage.readAssignment()).thenReturn(assignment);

        operations.loadAssignment();
        assertThat(operations.findLayer(configuration).isPresent()).isFalse();
    }

    @Test
    public void experimentFromPatternReturnsTrueWhenPatternMatches() {
        final Layer layer = new Layer(LAYER_NAME, 1, EXPERIMENT_1, VARIANT_ID_1, VARIANT_0);
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromPattern(LAYER_NAME, EXPERIMENT_PATTERN);

        assertThat(ExperimentOperations.matches(configuration, layer)).isTrue();
    }

    @Test
    public void experimentFromPatternReturnsFalseWhenPatternDoesNotMatch() {
        final Layer layer = new Layer(LAYER_NAME, 1, DIFFERENT_EXPERIMENT, VARIANT_ID_1, VARIANT_0);
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromPattern(LAYER_NAME, EXPERIMENT_PATTERN);

        assertThat(ExperimentOperations.matches(configuration, layer)).isFalse();
    }

    @Test
    public void experimentFromNameReturnsTrueWhenNameAreIdentical() {
        final Layer layer = new Layer(LAYER_NAME, 1, EXPERIMENT_1, VARIANT_ID_1, VARIANT_0);
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(LAYER_NAME, EXPERIMENT_1, Collections.emptyList());

        assertThat(ExperimentOperations.matches(configuration, layer)).isTrue();
    }

    @Test
    public void experimentFromNameReturnsFalseWhenNameIsDifferent() {
        final Layer layer = new Layer(LAYER_NAME, 1, EXPERIMENT_1, VARIANT_ID_1, VARIANT_0);
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(LAYER_NAME, EXPERIMENT_PATTERN, Collections.emptyList());

        assertThat(ExperimentOperations.matches(configuration, layer)).isFalse();
    }

    @Test
    public void experimentReturnsFalseWhenLayersAreDifferent() {
        final Layer layer = new Layer(ANOTHER_LAYER_NAME, 1, EXPERIMENT_1, VARIANT_ID_1, VARIANT_0);
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(LAYER_NAME, EXPERIMENT_1, Collections.emptyList());

        assertThat(ExperimentOperations.matches(configuration, layer)).isFalse();
    }

    @Test
    public void getActiveVariantsEmpty() throws Exception {
        when(assignment.getLayers()).thenReturn(Collections.emptyList());

        final Optional<String> activeVariants = operations.getSerializedActiveVariants();

        assertThat(activeVariants.isPresent()).isFalse();
    }

    @Test
    public void getActiveVariantsSingle() throws Exception {
        final Layer layer = new Layer(LAYER_NAME, 1, EXPERIMENT_1, VARIANT_ID_1, VARIANT_0);

        when(assignment.getLayers()).thenReturn(Lists.newArrayList(layer));

        final Optional<String> activeVariants = operations.getSerializedActiveVariants();

        assertThat(activeVariants.get()).isEqualTo(SERIALIZED_VARIANT_ID);
    }

    @Test
    public void getActiveVariantsMany() throws Exception {
        final Layer layer1 = new Layer(LAYER_NAME, 1, EXPERIMENT_1, VARIANT_ID_1, VARIANT_0);
        final Layer layer2 = new Layer(LAYER_NAME, 2, EXPERIMENT_2, VARIANT_ID_2, VARIANT_0);

        when(assignment.getLayers()).thenReturn(Lists.newArrayList(layer1, layer2));

        final Optional<String> activeVariants = operations.getSerializedActiveVariants();

        assertThat(activeVariants.get()).isEqualTo(MULTIPLE_SERIALIZED_VARIANT_IDS);
    }

    @Test
    public void noActiveVariantsWhenDeveloperMode() throws Exception {
        final Layer layer1 = new Layer(LAYER_NAME, 1, EXPERIMENT_1, VARIANT_ID_1, VARIANT_0);
        final Layer layer2 = new Layer(LAYER_NAME, 2, EXPERIMENT_2, VARIANT_ID_2, VARIANT_0);

        when(assignment.getLayers()).thenReturn(Lists.newArrayList(layer1, layer2));
        when(applicationProperties.isDevelopmentMode()).thenReturn(true);

        final Optional<String> activeVariants = operations.getSerializedActiveVariants();

        assertThat(activeVariants).isEqualTo(Optional.absent());
    }

    private Assignment createAssignment(String layerName) {
        final Layer layer = new Layer(layerName, 1, "experiment", 1, "variant");
        return new Assignment(Collections.singletonList(layer));
    }

}
