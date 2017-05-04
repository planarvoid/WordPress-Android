package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
    public void enabledControl() {
        mockVariant("control");

        assertThat(experiment.isControlPlusTooltip()).isFalse();
        assertThat(experiment.isSaveInCopy()).isFalse();
        assertThat(experiment.isSaveInCopyPlusTooltip()).isFalse();
    }

    @Test
    public void enabledControlPlusTooltip() {
        mockVariant("control_plus_tooltip");

        assertThat(experiment.isControlPlusTooltip()).isTrue();
    }

    @Test
    public void enabledSaveInCopy() {
        mockVariant("save_in_copy");

        assertThat(experiment.isSaveInCopy()).isTrue();
    }

    @Test
    public void enabledSaveInCopyPlusTooltip() {
        mockVariant("save_in_copy_plus_tooltip");

        assertThat(experiment.isSaveInCopyPlusTooltip()).isTrue();
    }

    private void mockVariant(String variant) {
        when(experimentOperations.getExperimentVariant(ChangeLikeToSaveExperiment.CONFIGURATION)).thenReturn(variant);
    }
}
