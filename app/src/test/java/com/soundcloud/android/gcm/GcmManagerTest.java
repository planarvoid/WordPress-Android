package com.soundcloud.android.gcm;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.common.ConnectionResult;
import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class GcmManagerTest {

    private GcmManager gcmManager;

    @Mock private GcmStorage gcmStorage;
    @Mock private GooglePlayServicesWrapper googlePlayServices;
    @Mock private ServiceInitiator serviceInitiator;
    @Mock private AppCompatActivity activity;
    @Mock private ApplicationProperties applicationProperties;

    @Before
    public void setUp() throws Exception {
        gcmManager = new GcmManager(applicationProperties, gcmStorage, googlePlayServices, serviceInitiator);
        activity = new AppCompatActivity();
    }

    @Test
    public void checksForPlayServicesWhenBundleIsNull() {
        when(googlePlayServices.isPlayServicesAvailable(activity)).thenReturn(ConnectionResult.SUCCESS);

        gcmManager.onCreate(activity, null);

        verify(googlePlayServices).isPlayServicesAvailable(activity);
    }

    @Test
    public void showsErrorDialogWhenPlayServicesAvailableReturnsRecoverableErrorWhenBundleIsNull() {
        when(applicationProperties.isGooglePlusEnabled()).thenReturn(true);
        when(googlePlayServices.isPlayServicesAvailable(activity)).thenReturn(123);
        when(googlePlayServices.isUserRecoverableError(123)).thenReturn(true);

        gcmManager.onCreate(activity, null);

        verify(googlePlayServices).showUnrecoverableErrorDialog(activity, 123);
    }

    @Test
    public void doesNotShowErrorDialogForNonPlayStoreReleases() {
        when(applicationProperties.isGooglePlusEnabled()).thenReturn(false);
        when(googlePlayServices.isPlayServicesAvailable(activity)).thenReturn(123);
        when(googlePlayServices.isUserRecoverableError(123)).thenReturn(true);

        gcmManager.onCreate(activity, null);

        verify(googlePlayServices, never()).showUnrecoverableErrorDialog(activity, 123);
    }

    @Test
    public void startsRegistrationServiceWithNoToken() {
        when(googlePlayServices.isPlayServicesAvailable(activity)).thenReturn(ConnectionResult.SUCCESS);

        gcmManager.onCreate(activity, null);

        verify(serviceInitiator).startGcmService(activity);

    }

    @Test
    public void doesNotStartRegistrationServiceWithToken() {
        when(googlePlayServices.isPlayServicesAvailable(activity)).thenReturn(ConnectionResult.SUCCESS);
        when(gcmStorage.hasToken()).thenReturn(true);

        gcmManager.onCreate(activity, null);

        verify(serviceInitiator, never()).startGcmService(any(Context.class));

    }
}
