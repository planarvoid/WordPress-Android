package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;

import java.util.Collections;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentOperationsTest {

    private ExperimentOperations operations;

    @Mock private ExperimentStorage experimentStorage;
    @Mock private ActiveExperiments activeExperiments;

    @Before
    public void setUp() throws Exception {
        operations = new ExperimentOperations(experimentStorage, activeExperiments);
    }

    @Test
    public void shouldGetEmptyAssignmentsIfHasNotInitialised() {
        Assignment assignment = operations.getAssignment();

        assertThat(assignment.isEmpty()).isTrue();
    }

    @Test
    public void shouldGetEmptyAssignmentIfNoAssignmentIsStored() {
        when(experimentStorage.readAssignment()).thenReturn(Observable.<Assignment>empty());

        operations.loadAssignment().subscribe();
        Assignment assignment = operations.getAssignment();

        assertThat(assignment.isEmpty()).isTrue();
    }

    @Test
    public void shouldGetAssignmentIfAssigmentIsStored() {
        Assignment assignment = ModelFixtures.create(Assignment.class);
        when(experimentStorage.readAssignment()).thenReturn(Observable.just(assignment));

        operations.loadAssignment().subscribe();
        Assignment loadedAssignment = operations.getAssignment();

        assertThat(loadedAssignment.getLayers()).isEqualTo(assignment.getLayers());
    }

    @Test
    public void updateShouldSaveAssignment() {
        Assignment assignment = ModelFixtures.create(Assignment.class);
        when(experimentStorage.readAssignment()).thenReturn(Observable.<Assignment>empty());

        operations.update(assignment);

        verify(experimentStorage).storeAssignment(eq(assignment));
    }

    @Test
    public void shouldGenerateTrackingParametersMapForActiveExperiments() {
        Assignment assignment = ModelFixtures.create(Assignment.class);
        when(experimentStorage.readAssignment()).thenReturn(Observable.just(assignment));
        when(activeExperiments.isActive(any(Layer.class))).thenReturn(true);

        operations.loadAssignment().subscribe();

        Map<String, Integer> params = operations.getTrackingParams();

        assertThat(params.containsKey("exp_android-ui")).isTrue();
        assertThat(params.get("exp_android-ui")).isEqualTo(3);

        assertThat(params.containsKey("exp_android-listen")).isTrue();
        assertThat(params.get("exp_android-listen")).isEqualTo(9);
    }

    @Test
    public void shouldNotGenerateTrackingParametersForExperimentsThatAreNotRunning() {
        final Layer layer = new Layer("android-ui", 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));

        when(experimentStorage.readAssignment()).thenReturn(Observable.just(assignment));
        when(activeExperiments.isActive(layer)).thenReturn(true);

        operations.loadAssignment().subscribe();

        Map<String, Integer> params = operations.getTrackingParams();

        assertThat(params.containsKey("exp_android-ui")).isTrue();
        assertThat(params.containsKey("exp_android-listen")).isFalse();
    }

    @Test
    public void shouldReturnLayerWhenAssigned() {
        final Layer layer = new Layer("layer", 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer",
                                                                                       "experiment",
                                                                                       Collections.<String>emptyList());
        when(experimentStorage.readAssignment()).thenReturn(Observable.just(assignment));

        operations.loadAssignment().subscribe();
        assertThat(operations.findLayer(configuration).get()).isEqualTo(layer);
    }

    @Test
    public void shouldReturnAbsentWhenNotAssigned() {
        final Layer layer = new Layer("layer", 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer",
                                                                                       "unknown",
                                                                                       Collections.<String>emptyList());
        when(experimentStorage.readAssignment()).thenReturn(Observable.just(assignment));

        operations.loadAssignment().subscribe();
        assertThat(operations.findLayer(configuration).isPresent()).isFalse();
    }

}
