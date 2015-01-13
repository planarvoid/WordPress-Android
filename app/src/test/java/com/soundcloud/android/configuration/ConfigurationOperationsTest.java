package com.soundcloud.android.configuration;

import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.DeviceHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class ConfigurationOperationsTest {

    @Mock private ApiScheduler apiScheduler;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private DeviceHelper deviceHelper;
    @Mock private FeatureOperations featureOperations;
    @Mock private FeatureFlags featureFlags;

    private ConfigurationOperations operations;
    private Configuration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = ModelFixtures.create(Configuration.class);
        operations = new ConfigurationOperations(apiScheduler, experimentOperations, featureOperations, deviceHelper, new TestEventBus(), featureFlags);

        when(deviceHelper.getUDID()).thenReturn("device-id");
        when(experimentOperations.loadAssignment()).thenReturn(Observable.just(Assignment.empty()));
        when(experimentOperations.getActiveLayers()).thenReturn(new String[]{"android_listening", "ios"});
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.just(configuration));
        when(featureFlags.isEnabled(Feature.CONFIGURATION_FEATURES)).thenReturn(true);
    }

    @Test
    public void doesNotLoadExperimentsIfDeviceIdIsNull() {
        when(deviceHelper.getUDID()).thenReturn(null);

        operations.update();

        verify(apiScheduler).mappedResponse(argThat(
                isMobileApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                        .withoutHeader(ApiRequest.HEADER_UDID)));
    }

    @Test
    public void loadsExperimentsOnUpdate() {
        operations.update();

        verify(apiScheduler).mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                .withQueryParam("experiment_layers", "android_listening", "ios")
                .withHeader(ApiRequest.HEADER_UDID, "device-id")));
    }

    @Test
    public void updatesFeatures() {
        operations.update();

        verify(featureOperations).update(configuration.features);
    }

    @Test
    public void updatesExperiments() {
        operations.update();

        verify(experimentOperations).update(configuration.assignment);
    }

}