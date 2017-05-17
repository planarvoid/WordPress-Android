package com.soundcloud.android.view.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ChangeLikeToSaveExperimentMenuHelperTest extends AndroidUnitTest {

    @Mock ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    private ChangeLikeToSaveExperimentMenuHelper changeLikeToSaveExperimentMenuHelper;

    @Before
    public void setUp() {
        changeLikeToSaveExperimentMenuHelper = new ChangeLikeToSaveExperimentMenuHelper(context(), changeLikeToSaveExperiment);
    }

    @Test
    public void getTitleBasedOnExperimentVariant() {
        assertThat(changeLikeToSaveExperimentMenuHelper.getTitleForLikeAction(false)).isEqualTo("Like");
        assertThat(changeLikeToSaveExperimentMenuHelper.getTitleForLikeAction(true)).isEqualTo("Unlike");

        when(changeLikeToSaveExperiment.isEnabled()).thenReturn(true);
        assertThat(changeLikeToSaveExperimentMenuHelper.getTitleForLikeAction(false)).isEqualTo("Add to collection");
        assertThat(changeLikeToSaveExperimentMenuHelper.getTitleForLikeAction(true)).isEqualTo("Remove from collection");
    }
}
