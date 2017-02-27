package com.soundcloud.android.configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

public class ConfigurationManagerTest extends AndroidUnitTest {

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
    @Mock private ForceUpdateHandler forceUpdateHandler;

    private ConfigurationManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new ConfigurationManager(configurationOperations, accountOperations,
                                           deviceManagementStorage, forceUpdateHandler);
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

        final PublishSubject<Void> logoutSubject = PublishSubject.create();
        when(accountOperations.logout()).thenReturn(logoutSubject);

        manager.forceConfigurationUpdate();

        logoutSubject.onNext(null);
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

        final PublishSubject<Void> logoutSubject = PublishSubject.create();
        when(accountOperations.logout()).thenReturn(logoutSubject);

        manager.requestConfigurationUpdate();

        logoutSubject.onNext(null);
        verify(configurationOperations, never()).saveConfiguration(any(Configuration.class));
    }

    @Test
    public void requestedUnnecessaryUpdateIsNoOp() {
        when(configurationOperations.fetchIfNecessary()).thenReturn(Observable.<Configuration>empty());

        manager.requestConfigurationUpdate();

        verify(configurationOperations, never()).saveConfiguration(any(Configuration.class));
        verifyZeroInteractions(accountOperations);
        verifyZeroInteractions(deviceManagementStorage);
    }

}
