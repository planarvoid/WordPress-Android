package com.soundcloud.android.configuration;

import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import java.util.HashMap;

@RunWith(SoundCloudTestRunner.class)
public class ConfigurationOperationsTest {

    @Mock private ApiScheduler apiScheduler;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private FeatureFlags featureFlags;

    private ConfigurationOperations operations;
    private Configuration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = ModelFixtures.create(Configuration.class);
        operations = new ConfigurationOperations(apiScheduler, experimentOperations, featureOperations, featureFlags);

        when(experimentOperations.loadAssignment()).thenReturn(Observable.just(Assignment.empty()));
        when(experimentOperations.getActiveLayers()).thenReturn(new String[]{"android_listening", "ios"});
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.just(configuration));
        when(featureFlags.isEnabled(Flag.CONFIGURATION_FEATURES)).thenReturn(true);
    }

    @Test
    public void loadsExperimentsOnUpdate() {
        operations.update();

        verify(apiScheduler).mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                .withQueryParam("experiment_layers", "android_listening", "ios")));
    }

    @Test
    public void updatesFeatures() {
        final HashMap<String, Boolean> featuresAsAMap = new HashMap<>();
        featuresAsAMap.put("feature_disabled", false);
        featuresAsAMap.put("feature_enabled", true);

        operations.update();

        verify(featureOperations).update(eq(featuresAsAMap));
    }

    @Test
    public void updatesExperiments() {
        operations.update();

        verify(experimentOperations).update(configuration.assignment);
    }

}