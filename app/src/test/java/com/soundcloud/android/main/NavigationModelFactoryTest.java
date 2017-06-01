package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.discovery.DiscoveryConfiguration;
import com.soundcloud.android.discovery.DiscoveryNavigationTarget;
import com.soundcloud.android.olddiscovery.DefaultHomeScreenConfiguration;
import com.soundcloud.android.olddiscovery.OldDiscoveryNavigationTarget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NavigationModelFactoryTest {

    @Mock DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;
    @Mock DiscoveryConfiguration discoveryConfiguration;
    @Mock ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    private NavigationModelFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new NavigationModelFactory(defaultHomeScreenConfiguration, discoveryConfiguration, changeLikeToSaveExperiment);
    }

    @Test
    public void testWhenDiscoveryIsHomeScreenAndDiscoverBackendIsEnabled() throws Exception {
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(true);
        when(discoveryConfiguration.navigationTarget()).thenReturn(new DiscoveryNavigationTarget());

        NavigationModel navigationModel = factory.build();
        assertThat(navigationModel.getItem(0).getScreen()).isEqualTo(Screen.DISCOVER);
        assertThat(navigationModel.getItem(1).getScreen()).isEqualTo(Screen.STREAM);
    }

    @Test
    public void testWhenDiscoveryIsHomeScreenAndDiscoverBackendIsNotEnabled() throws Exception {
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(true);
        when(discoveryConfiguration.navigationTarget()).thenReturn(new OldDiscoveryNavigationTarget());

        NavigationModel navigationModel = factory.build();
        assertThat(navigationModel.getItem(0).getScreen()).isEqualTo(Screen.SEARCH_MAIN);
        assertThat(navigationModel.getItem(1).getScreen()).isEqualTo(Screen.STREAM);
    }

    @Test
    public void testWhenStreamIsHomeScreenAndDiscoveryBackendIsEnabled() throws Exception {
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(false);
        when(discoveryConfiguration.navigationTarget()).thenReturn(new DiscoveryNavigationTarget());

        NavigationModel navigationModel = factory.build();
        assertThat(navigationModel.getItem(0).getScreen()).isEqualTo(Screen.STREAM);
        assertThat(navigationModel.getItem(1).getScreen()).isEqualTo(Screen.DISCOVER);
    }

    @Test
    public void testWhenStreamIsHomeScreenAndDiscoveryBackendIsDisabled() throws Exception {
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(false);
        when(discoveryConfiguration.navigationTarget()).thenReturn(new OldDiscoveryNavigationTarget());

        NavigationModel navigationModel = factory.build();
        assertThat(navigationModel.getItem(0).getScreen()).isEqualTo(Screen.STREAM);
        assertThat(navigationModel.getItem(1).getScreen()).isEqualTo(Screen.SEARCH_MAIN);
    }
}
