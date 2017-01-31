package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.MiniplayerExperiment.PLAY_SESSION_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.MiniplayerStorage;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MiniplayerExperimentTest {

    @Mock private ExperimentOperations experimentOperations;
    @Mock private MiniplayerStorage miniplayerStorage;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    private MiniplayerExperiment miniplayerExperiment;


    @Before
    public void setUp() throws Exception {
        miniplayerExperiment = new MiniplayerExperiment(experimentOperations, miniplayerStorage, playSessionStateProvider);
    }

    @Test
    public void testNoVariant() throws Exception {
        setPreconditions(Strings.EMPTY);

        assertThat(miniplayerExperiment.canExpandPlayer()).isTrue();
    }

    @Test
    public void testControlVariant() throws Exception {
        setPreconditions(MiniplayerExperiment.VARIANT_CONTROL);

        assertThat(miniplayerExperiment.canExpandPlayer()).isTrue();
    }

    @Test
    public void testInverseVariant() throws Exception {
        setPreconditions(MiniplayerExperiment.VARIANT_INVERSE);

        assertThat(miniplayerExperiment.canExpandPlayer()).isFalse();
    }

    @Test
    public void testHybridVariantNeverCollapsed() throws Exception {
        when(miniplayerStorage.hasMinimizedPlayerManually()).thenReturn(false);
        when(playSessionStateProvider.getMillisSinceLastPlaySession()).thenReturn(PLAY_SESSION_LENGTH);
        setPreconditions(MiniplayerExperiment.VARIANT_HYBRID);

        assertThat(miniplayerExperiment.canExpandPlayer()).isTrue();
    }

    @Test
    public void testHybridVariantCollapsedWithinPlaySessionExpire() throws Exception {
        when(miniplayerStorage.hasMinimizedPlayerManually()).thenReturn(true);
        when(playSessionStateProvider.getMillisSinceLastPlaySession()).thenReturn(0L);
        setPreconditions(MiniplayerExperiment.VARIANT_HYBRID);

        assertThat(miniplayerExperiment.canExpandPlayer()).isFalse();
    }

    @Test
    public void testHybridVariantCollapsedAfterPlaySessionExpire() throws Exception {
        when(miniplayerStorage.hasMinimizedPlayerManually()).thenReturn(true);
        when(playSessionStateProvider.getMillisSinceLastPlaySession()).thenReturn(PLAY_SESSION_LENGTH);
        setPreconditions(MiniplayerExperiment.VARIANT_HYBRID);

        assertThat(miniplayerExperiment.canExpandPlayer()).isTrue();
    }

    private void setPreconditions(String variant) {
        when(experimentOperations.getExperimentVariant(MiniplayerExperiment.CONFIGURATION)).thenReturn(variant);
    }
}
