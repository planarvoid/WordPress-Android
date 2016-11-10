package com.soundcloud.android.gcm;

import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.common.ConnectionResult;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

public class GcmManagerTest extends AndroidUnitTest {

    private GcmManager gcmManager;

    @Mock private GcmStorage gcmStorage;
    @Mock private GooglePlayServicesWrapper googlePlayServices;
    @Mock private AppCompatActivity activity;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private FeatureFlags featureFlags;

    @Captor ArgumentCaptor<Intent> intentCaptor;

    @Before
    public void setUp() throws Exception {
        gcmManager = new GcmManager(applicationProperties, gcmStorage, googlePlayServices);
    }

    @Test
    public void checksForPlayServicesWhenBundleIsNull() {
        when(googlePlayServices.getPlayServicesAvailableStatus(activity)).thenReturn(ConnectionResult.SUCCESS);

        gcmManager.onCreate(activity, null);

        verify(googlePlayServices).getPlayServicesAvailableStatus(activity);
    }

    @Test
    public void showsErrorDialogWhenPlayServicesAvailableReturnsRecoverableErrorWhenBundleIsNull() {
        when(applicationProperties.isGooglePlusEnabled()).thenReturn(true);
        when(googlePlayServices.getPlayServicesAvailableStatus(activity)).thenReturn(123);
        when(googlePlayServices.isUserRecoverableError(123)).thenReturn(true);

        gcmManager.onCreate(activity, null);

        verify(googlePlayServices).showUnrecoverableErrorDialog(activity, 123);
    }

    @Test
    public void doesNotShowErrorDialogForNonPlayStoreReleases() {
        when(applicationProperties.isGooglePlusEnabled()).thenReturn(false);
        when(googlePlayServices.getPlayServicesAvailableStatus(activity)).thenReturn(123);
        when(googlePlayServices.isUserRecoverableError(123)).thenReturn(true);

        gcmManager.onCreate(activity, null);

        verify(googlePlayServices, never()).showUnrecoverableErrorDialog(activity, 123);
    }

    @Test
    public void startsRegistrationServiceWhenShouldRegister() {
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(googlePlayServices.getPlayServicesAvailableStatus(activity)).thenReturn(ConnectionResult.SUCCESS);

        gcmManager.onCreate(activity, null);

        verify(activity).startService(intentCaptor.capture());
        assertThat(intentCaptor.getValue()).startsService(GcmRegistrationService.class);
    }

    @Test
    public void doesNotStartRegistrationIfShouldNotRegister() {
        when(googlePlayServices.getPlayServicesAvailableStatus(activity)).thenReturn(ConnectionResult.SUCCESS);

        gcmManager.onCreate(activity, null);

        verify(activity,never()).startService(any(Intent.class));

    }
}
