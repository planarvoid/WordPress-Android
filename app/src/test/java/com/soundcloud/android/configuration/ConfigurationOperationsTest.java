package com.soundcloud.android.configuration;

import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.experiments.ExperimentOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
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

    private ConfigurationOperations operations;
    private TestEventBus eventBus;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        operations = new ConfigurationOperations(apiScheduler, experimentOperations, deviceHelper, eventBus);
        when(deviceHelper.getUDID()).thenReturn("device-id");
        when(experimentOperations.getActiveLayers()).thenReturn(new String[]{"android_listening", "ios"});
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.never());
    }

    @Test
    public void shouldNotLoadExperimentsIfDeviceIdIsNull() {
        when(deviceHelper.getUDID()).thenReturn(null);

        operations.update();

        verify(apiScheduler).mappedResponse(argThat(
                isMobileApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                        .withoutHeader(ApiRequest.HEADER_UDID)));
    }

    @Test
    public void shouldLoadExperiments() {
        operations.update();

        verify(apiScheduler).mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                .withQueryParam("experiment_layers", "android_listening", "ios")
                .withHeader(ApiRequest.HEADER_UDID, "device-id")));
    }

}