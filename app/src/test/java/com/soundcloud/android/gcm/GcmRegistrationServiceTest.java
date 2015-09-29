package com.soundcloud.android.gcm;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

public class GcmRegistrationServiceTest extends AndroidUnitTest {

    private GcmRegistrationService service;

    @Mock private GcmStorage gcmStorage;
    @Mock private InstanceIdWrapper instanceId;
    @Mock private FeatureFlags featureFlags;
    @Mock private AppboyWrapper appboyWrapper;

    @Before
    public void setUp() throws Exception {
        service = new GcmRegistrationService(gcmStorage, instanceId, featureFlags, InjectionSupport.providerOf(appboyWrapper));
    }

    @Test
    public void storesSuccessfullyFetchedToken() throws IOException {
        when(instanceId.getToken(service, resources().getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE))
                .thenReturn("token");

        service.onHandleIntent(null);

        verify(gcmStorage).storeToken("token");
    }

    @Test
    public void sendsTokenToAppboyOnSuccessfullyFetchedToken() throws IOException {
        when(featureFlags.isEnabled(Flag.APPBOY)).thenReturn(true);
        when(instanceId.getToken(service, resources().getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE))
                .thenReturn("token");

        service.onHandleIntent(null);

        verify(appboyWrapper).handleRegistration("token");
    }

    @Test
    public void clearsTokenOnUnsuccessfullyFetchedToken() throws IOException {
        when(instanceId.getToken(service, resources().getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE))
                .thenThrow(new IOException());

        service.onHandleIntent(null);

        verify(gcmStorage).clearToken();
    }
}
