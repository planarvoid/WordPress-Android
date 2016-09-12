package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentOperationsTest {

    private ExperimentOperations operations;

    @Mock private ExperimentStorage experimentStorage;
    @Mock private ActiveExperiments activeExperiments;
    @Mock private Assignment assignment;

    @Before
    public void setUp() throws Exception {
        when(experimentStorage.readAssignment()).thenReturn(assignment);
        operations = new ExperimentOperations(experimentStorage, activeExperiments);
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
    public void shouldGetAssignmentIfAssigmentIsStored() {
        Assignment reloadedAsignment = ModelFixtures.create(Assignment.class);
        when(experimentStorage.readAssignment()).thenReturn(reloadedAsignment);

        operations.loadAssignment();

        assertThat(operations.getAssignment()).isSameAs(reloadedAsignment);
    }

    @Test
    public void updateShouldSaveAssignment() {
        Assignment assignment = ModelFixtures.create(Assignment.class);

        operations.update(assignment);

        verify(experimentStorage).storeAssignment(eq(assignment));
    }

    @Test
    public void shouldReturnLayerWhenAssigned() {
        final Layer layer = new Layer("layer", 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer",
                                                                                       "experiment",
                                                                                       Collections.<String>emptyList());
        when(experimentStorage.readAssignment()).thenReturn(assignment);

        operations.loadAssignment();
        assertThat(operations.findLayer(configuration).get()).isEqualTo(layer);
    }

    @Test
    public void shouldReturnAbsentWhenNotAssigned() {
        final Layer layer = new Layer("layer", 1, "experiment", 1, "variant");
        final Assignment assignment = new Assignment(Collections.singletonList(layer));
        final ExperimentConfiguration configuration = ExperimentConfiguration.fromName("layer",
                                                                                       "unknown",
                                                                                       Collections.<String>emptyList());
        when(experimentStorage.readAssignment()).thenReturn(assignment);

        operations.loadAssignment();
        assertThat(operations.findLayer(configuration).isPresent()).isFalse();
    }

}
