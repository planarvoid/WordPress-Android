package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ChangeLikeToSaveExperimentStringHelperTest extends AndroidUnitTest {

    @Mock private ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    private ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Before
    public void setUp() {
        changeLikeToSaveExperimentStringHelper = new ChangeLikeToSaveExperimentStringHelper(changeLikeToSaveExperiment, context());
    }

    @Test
    public void shouldGetCorrectStringResourcesWhenExperimentEnabled() {
        when(changeLikeToSaveExperiment.isEnabled()).thenReturn(true);

        assertThat(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.LIKE)).isEqualTo(R.string.btn_add_to_collection);
        assertThat(changeLikeToSaveExperimentStringHelper.getString(ExperimentString.LIKE)).isEqualTo("Add to collection");
    }

    @Test
    public void shouldGetCorrectStringResourcesWhenExperimentDisabled() {
        when(changeLikeToSaveExperiment.isEnabled()).thenReturn(false);

        assertThat(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.LIKE)).isEqualTo(R.string.btn_like);
        assertThat(changeLikeToSaveExperimentStringHelper.getString(ExperimentString.LIKE)).isEqualTo("Like");
    }
}
