package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NavigationModelFactoryTest {

    @Mock PlaylistDiscoveryConfig playlistDiscoveryConfig;
    @Mock FeatureOperations featureOperations;

    private NavigationModelFactory factory;

    @Before
    public void setUp() throws Exception {
        when(playlistDiscoveryConfig.isEnabled()).thenReturn(false);
        when(featureOperations.isNewHomeEnabled()).thenReturn(false);

        factory = new NavigationModelFactory(playlistDiscoveryConfig, featureOperations);
    }

    @Test
    public void enabledByConfig() throws Exception {
        when(playlistDiscoveryConfig.isEnabled()).thenReturn(true);

        NavigationModel navigationModel = factory.build();
        assertThat(navigationModel.getItem(0).getScreen()).isEqualTo(Screen.SEARCH_MAIN);
        assertThat(navigationModel.getItem(1).getScreen()).isEqualTo(Screen.STREAM);
    }

    @Test
    public void enabledByFeatureOperaions() throws Exception {
        when(featureOperations.isNewHomeEnabled()).thenReturn(true);

        NavigationModel navigationModel = factory.build();
        assertThat(navigationModel.getItem(0).getScreen()).isEqualTo(Screen.SEARCH_MAIN);
        assertThat(navigationModel.getItem(1).getScreen()).isEqualTo(Screen.STREAM);
    }

    @Test
    public void disabledByDefault() throws Exception {
        NavigationModel navigationModel = factory.build();
        assertThat(navigationModel.getItem(0).getScreen()).isEqualTo(Screen.STREAM);
        assertThat(navigationModel.getItem(1).getScreen()).isEqualTo(Screen.SEARCH_MAIN);
    }
}
