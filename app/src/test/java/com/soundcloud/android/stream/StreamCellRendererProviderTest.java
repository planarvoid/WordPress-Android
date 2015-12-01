package com.soundcloud.android.stream;

import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.experiments.StreamDesignExperiment;
import com.soundcloud.android.profile.PostedPlaylistItemRenderer;
import com.soundcloud.android.profile.PostedTrackItemRenderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamCellRendererProviderTest {

    @Mock StreamDesignExperiment designExperiment;
    @Mock PostedTrackItemRenderer trackItemRenderer;
    @Mock PostedPlaylistItemRenderer playlistItemRenderer;
    @Mock StreamTrackItemRenderer trackCardRenderer;
    @Mock StreamPlaylistItemRenderer playlistCardRenderer;

    @Test
    public void returnsCardRenderersWhenCardDesignIsEnabled() {
        when(designExperiment.isCardDesign()).thenReturn(true);

        StreamCellRendererProvider provider =
                new StreamCellRendererProvider(designExperiment, lazyOf(trackItemRenderer), lazyOf(playlistItemRenderer),
                        lazyOf(trackCardRenderer), lazyOf(playlistCardRenderer));

        assertThat(provider.getTrackItemRenderer()).isEqualTo(trackCardRenderer);
        assertThat(provider.getPlaylistItemRenderer()).isEqualTo(playlistCardRenderer);
    }

    @Test
    public void returnsListRenderersWhenCardDesignIsDisabled() {
        when(designExperiment.isCardDesign()).thenReturn(false);

        StreamCellRendererProvider provider =
                new StreamCellRendererProvider(designExperiment, lazyOf(trackItemRenderer), lazyOf(playlistItemRenderer),
                        lazyOf(trackCardRenderer), lazyOf(playlistCardRenderer));

        assertThat(provider.getTrackItemRenderer()).isEqualTo(trackItemRenderer);
        assertThat(provider.getPlaylistItemRenderer()).isEqualTo(playlistItemRenderer);
    }

}
