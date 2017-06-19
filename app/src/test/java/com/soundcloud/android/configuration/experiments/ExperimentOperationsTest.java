package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.matches;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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

    @Mock private ExperimentStorage experimentStorage;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private Assignment assignment;

    private ExperimentOperations operations;

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
        final Layer layer = new Layer("layerName", 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(layer.getLayerName(), layer.getExperimentName(), Collections.emptyList());
        when(experimentStorage.readAssignment()).thenReturn(assignment);

        operations.loadAssignment();
        assertThat(operations.findLayer(configuration).get()).isEqualTo(layer);
    }

    @Test
    public void shouldFindUpdatedLayer() {
        final Layer layer = new Layer("layerName", 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(layer.getLayerName(), layer.getExperimentName(), Collections.emptyList());

        operations.update(assignment);

        assertThat(operations.findLayer(configuration).get()).isEqualTo(layer);
    }

    @Test
    public void shouldReturnAbsentWhenNotAssigned() {
        final String layerName = "layerName";
        final Assignment assignment = createAssignment(layerName);
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName(layerName, "unknown", Collections.emptyList());

        when(experimentStorage.readAssignment()).thenReturn(assignment);

        operations.loadAssignment();
        assertThat(operations.findLayer(configuration).isPresent()).isFalse();
    }

    @Test
    public void matchesConfigAndLayerWhenLayerNameAndExperimentIdOtherwiseExperimentNameMatch() {
        assertThat(matches(configuration("layer", "expName", absent()), layer("layer", "expName", absent()))).isTrue();
        assertThat(matches(configuration("layer", "expName", of(1234)), layer("layer", "expName", of(1234)))).isTrue();
        assertThat(matches(configuration("layer", "expName", of(1234)), layer("layer", "-------", of(1234)))).isTrue();
        assertThat(matches(configuration("layer", "expName", absent()), layer("-----", "expName", absent()))).isFalse();
        assertThat(matches(configuration("layer", "expName", absent()), layer("layer", "-------", absent()))).isFalse();
        assertThat(matches(configuration("layer", "expName", of(1234)), layer("layer", "expName", of(-999)))).isFalse();
    }

    @Test
    public void getSerializedActiveVariants() throws Exception {
        final String expectedVariantIds = "123,456";
        when(assignment.commaSeparatedVariantIds()).thenReturn(of(expectedVariantIds));

        final Optional<String> activeVariants = operations.getSerializedActiveVariants();

        assertThat(activeVariants.get()).isEqualTo(expectedVariantIds);
    }

    @Test
    public void noSerializedActiveVariantsWhenDeveloperMode() throws Exception {
        when(applicationProperties.isDevelopmentMode()).thenReturn(true);

        final Optional<String> activeVariants = operations.getSerializedActiveVariants();

        assertThat(activeVariants).isEqualTo(absent());
        verifyZeroInteractions(assignment);
    }

    private Assignment createAssignment(String layerName) {
        final Layer layer = new Layer(layerName, 1, "experiment", 1, "variant");
        return new Assignment(Collections.singletonList(layer));
    }

    private ExperimentConfiguration configuration(String layerName, String experimentName, Optional<Integer> experimentId) {
        if (experimentId.isPresent()) {
            return ExperimentConfiguration.fromNamesAndIds(layerName, experimentName, experimentId.get(), Collections.emptyList());
        } else {
            return ExperimentConfiguration.fromName(layerName, experimentName, Collections.emptyList());
        }
    }

    private Layer layer(String layerName, String experimentName, Optional<Integer> experimentId) {
        return new Layer(layerName, experimentId.or(-1), experimentName, -1, "");
    }
}
