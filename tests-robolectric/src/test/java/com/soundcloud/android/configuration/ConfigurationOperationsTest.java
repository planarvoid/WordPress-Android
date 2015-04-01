package com.soundcloud.android.configuration;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.net.HttpHeaders;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

@RunWith(SoundCloudTestRunner.class)
public class ConfigurationOperationsTest {

    @Mock private ApiClientRx apiClientRx;
    @Mock private ApiClient apiClient;
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private AccountOperations accountOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private DeviceManagementStorage deviceManagementStorage;

    private ConfigurationOperations operations;
    private Configuration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = ModelFixtures.create(Configuration.class);
        operations = new ConfigurationOperations(InjectionSupport.lazyOf(apiClientRx), InjectionSupport.lazyOf(apiClient),
                experimentOperations, featureOperations, accountOperations, offlineContentOperations,
                deviceManagementStorage, featureFlags, Schedulers.immediate());

        when(experimentOperations.loadAssignment()).thenReturn(Observable.just(Assignment.empty()));
        when(experimentOperations.getActiveLayers()).thenReturn(new String[]{"android_listening", "ios"});
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.just(configuration));
        when(featureFlags.isEnabled(Flag.OFFLINE_SYNC)).thenReturn(true);
    }

    @Test
    public void loadsExperimentsOnUpdate() {
        operations.update();

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                .withQueryParam("experiment_layers", "android_listening", "ios")));
    }

    @Test
    public void registerDeviceGetsConfigurationAndReturnsDeviceManagement() throws ApiRequestException, IOException, ApiMapperException {
        Token token = new Token("accessToken","refreshToken");
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                .withQueryParam("experiment_layers", "android_listening", "ios")
                .withHeader(HttpHeaders.AUTHORIZATION, "OAuth accessToken")))).thenReturn(configuration);

        expect(operations.registerDevice(token)).toBe(configuration.deviceManagement);
    }

    @Test
    public void registerDeviceStoresConfiguration() throws Exception {
        Token token = new Token("accessToken","refreshToken");
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CONFIGURATION.path())
                .withQueryParam("experiment_layers", "android_listening", "ios")
                .withHeader(HttpHeaders.AUTHORIZATION, "OAuth accessToken")))).thenReturn(configuration);

        operations.registerDevice(token);

        verify(featureOperations).update(eq(getFeaturesAsMap()));
        verify(experimentOperations).update(configuration.assignment);
    }

    @Test
    public void forceRegisterReturnsResultOfUnregisterPost() throws Exception {
        Token token = new Token("accessToken","refreshToken");
        String deviceId = "device-id";

        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.CONFIGURATION.path())
                .withHeader(HttpHeaders.AUTHORIZATION, "OAuth accessToken")
                .withContent(Collections.singletonMap("conflicting_device", Collections.singletonMap("device_id", deviceId)))))).thenReturn(configuration);

        expect(operations.forceRegisterDevice(token, deviceId)).toBe(configuration.deviceManagement);
    }

    @Test
    public void updateStoresConfiguration() {
        final Configuration authorizedConfiguration = getAuthorizedConfiguration();
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.just(authorizedConfiguration));
        operations.update();

        verify(featureOperations).update(eq(getFeaturesAsMap()));
        verify(experimentOperations).update(authorizedConfiguration.assignment);
    }

    private Configuration getAuthorizedConfiguration() {
        return new Configuration(ConfigurationBlueprint.createFeatures(), ConfigurationBlueprint.createLayers(),
                new DeviceManagement(true, null));
    }

    @Test
    public void updateWithUnauthorizedDeviceResponseLogsOutAndClearsContent() throws Exception {
        Configuration configurationWithDeviceConflict = new Configuration(Collections.<Feature>emptyList(), Collections.<Layer>emptyList(),
                new DeviceManagement(false, null));
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.just(configurationWithDeviceConflict));

        final PublishSubject<Void> logoutSubject = PublishSubject.create();
        final PublishSubject<WriteResult> clearOfflineContentSubject = PublishSubject.create();
        when(accountOperations.logout()).thenReturn(logoutSubject);
        when(offlineContentOperations.clearOfflineContent()).thenReturn(clearOfflineContentSubject);

        operations.update();

        logoutSubject.onNext(null);
        expect(clearOfflineContentSubject.hasObservers()).toBeTrue();
    }

    private HashMap<String, Boolean> getFeaturesAsMap() {
        final HashMap<String, Boolean> featuresAsAMap = new HashMap<>();
        featuresAsAMap.put("feature_disabled", false);
        featuresAsAMap.put("feature_enabled", true);
        return featuresAsAMap;
    }

}