package com.soundcloud.android.configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import io.reactivex.subjects.CompletableSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerTest {

    private static final Configuration AUTHORIZED_DEVICE_CONFIG = Configuration.builder()
                                                                               .deviceManagement(new DeviceManagement(
                                                                                       true,
                                                                                       false))
                                                                               .build();

    private static final Configuration UNAUTHORIZED_DEVICE_CONFIG = Configuration.builder()
                                                                                 .deviceManagement(new DeviceManagement(
                                                                                         false,
                                                                                         true))
                                                                                 .build();

    @Mock private ConfigurationOperations configurationOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private DeviceManagementStorage deviceManagementStorage;

    private ConfigurationManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new ConfigurationManager(configurationOperations, accountOperations,
                                           deviceManagementStorage);
    }

    @Test
    public void forceUpdateWithAuthorizedDeviceResponseSavesConfiguration() {
        when(configurationOperations.fetch()).thenReturn(Observable.just(AUTHORIZED_DEVICE_CONFIG));

        manager.forceConfigurationUpdate();

        verify(configurationOperations).saveConfiguration(AUTHORIZED_DEVICE_CONFIG);
    }

    @Test
    public void forceUpdateWithUnauthorizedDeviceResponseLogsOutAndClearsContent() {
        when(configurationOperations.fetch()).thenReturn(Observable.just(UNAUTHORIZED_DEVICE_CONFIG));

        final CompletableSubject logoutSubject = CompletableSubject.create();
        when(accountOperations.logout()).thenReturn(logoutSubject);

        manager.forceConfigurationUpdate();

        logoutSubject.onComplete();
        verify(configurationOperations, never()).saveConfiguration(any(Configuration.class));
    }

    @Test
    public void requestedUpdateWithAuthorizedDeviceResponseSavesConfiguration() {
        when(configurationOperations.fetchIfNecessary()).thenReturn(Observable.just(AUTHORIZED_DEVICE_CONFIG));

        manager.requestConfigurationUpdate();

        verify(configurationOperations).saveConfiguration(AUTHORIZED_DEVICE_CONFIG);
    }

    @Test
    public void requestedUpdateWithUnauthorizedDeviceResponseLogsOutAndClearsContent() {
        when(configurationOperations.fetchIfNecessary()).thenReturn(Observable.just(UNAUTHORIZED_DEVICE_CONFIG));

        final CompletableSubject logoutSubject = CompletableSubject.create();
        when(accountOperations.logout()).thenReturn(logoutSubject);

        manager.requestConfigurationUpdate();

        logoutSubject.onComplete();
        verify(configurationOperations, never()).saveConfiguration(any(Configuration.class));
    }

    @Test
    public void requestedUnnecessaryUpdateIsNoOp() {
        when(configurationOperations.fetchIfNecessary()).thenReturn(Observable.empty());

        manager.requestConfigurationUpdate();

        verify(configurationOperations, never()).saveConfiguration(any(Configuration.class));
        verifyZeroInteractions(accountOperations);
        verifyZeroInteractions(deviceManagementStorage);
    }

}
