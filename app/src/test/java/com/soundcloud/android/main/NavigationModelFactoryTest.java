package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.DefaultHomeScreenConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NavigationModelFactoryTest {

    @Mock DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;

    private NavigationModelFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new NavigationModelFactory(defaultHomeScreenConfiguration);
    }

    @Test
    public void testWhenDiscoveryIsHomeScreen() throws Exception {
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(true);

        NavigationModel navigationModel = factory.build();
        assertThat(navigationModel.getItem(0).getScreen()).isEqualTo(Screen.SEARCH_MAIN);
        assertThat(navigationModel.getItem(1).getScreen()).isEqualTo(Screen.STREAM);
    }

    @Test
    public void testWhenStreamIsHomeScreen() throws Exception {
        when(defaultHomeScreenConfiguration.isDiscoveryHome()).thenReturn(false);

        NavigationModel navigationModel = factory.build();
        assertThat(navigationModel.getItem(0).getScreen()).isEqualTo(Screen.STREAM);
        assertThat(navigationModel.getItem(1).getScreen()).isEqualTo(Screen.SEARCH_MAIN);
    }
}
