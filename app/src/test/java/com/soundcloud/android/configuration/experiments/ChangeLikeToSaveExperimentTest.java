package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.CollectionNavigationTarget;
import com.soundcloud.android.collection.SaveCollectionNavigationTarget;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ChangeLikeToSaveExperimentTest extends AndroidUnitTest {

    @Mock private ExperimentOperations experimentOperations;

    private ChangeLikeToSaveExperiment experiment;

    @Before
    public void setUp() {
        experiment = new ChangeLikeToSaveExperiment(experimentOperations);
    }

    @Test
    public void navigationTarget() {
        mockVariant("control");
        assertThat(experiment.navigationTarget()).isInstanceOf(CollectionNavigationTarget.class);

        mockVariant("control_plus_tooltip");
        assertThat(experiment.navigationTarget()).isInstanceOf(CollectionNavigationTarget.class);

        mockVariant("save_in_copy");
        assertThat(experiment.navigationTarget()).isInstanceOf(SaveCollectionNavigationTarget.class);

        mockVariant("save_in_copy_plus_tooltip");
        assertThat(experiment.navigationTarget()).isInstanceOf(SaveCollectionNavigationTarget.class);
    }

    @Test
    public void enabledControl() {
        mockVariant("control");
        assertThat(experiment.isEnabled()).isFalse();

        mockVariant("control_plus_tooltip");
        assertThat(experiment.isEnabled()).isFalse();

        mockVariant("save_in_copy");
        assertThat(experiment.isEnabled()).isTrue();

        mockVariant("save_in_copy_plus_tooltip");
        assertThat(experiment.isEnabled()).isTrue();
    }

    private void mockVariant(String variant) {
        when(experimentOperations.getExperimentVariant(ChangeLikeToSaveExperiment.CONFIGURATION)).thenReturn(variant);
    }
}
