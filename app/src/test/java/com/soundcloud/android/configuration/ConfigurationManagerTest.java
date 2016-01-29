package com.soundcloud.android.configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;

public class ConfigurationManagerTest extends AndroidUnitTest {

    private static final Configuration AUTHORIZED_DEVICE_CONFIG = new Configuration(Collections.<Feature>emptyList(),
            new UserPlan("free", Arrays.asList("high_tier")), Collections.<Layer>emptyList(), new DeviceManagement(true, false, null));

    private static final Configuration UNAUTHORIZED_DEVICE_CONFIG = new Configuration(Collections.<Feature>emptyList(),
            new UserPlan("free", Arrays.asList("high_tier")), Collections.<Layer>emptyList(), new DeviceManagement(false, true, null));

    @Mock private ConfigurationOperations configurationOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private DeviceManagementStorage deviceManagementStorage;

    private ConfigurationManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new ConfigurationManager(configurationOperations, accountOperations, deviceManagementStorage);
    }

    @Test
    public void updateWithAuthorizedDeviceResponseSavesConfiguration() {
        when(configurationOperations.update()).thenReturn(Observable.just(AUTHORIZED_DEVICE_CONFIG));

        manager.update();

        verify(configurationOperations).saveConfiguration(AUTHORIZED_DEVICE_CONFIG);
    }

    @Test
    public void updateWithUnauthorizedDeviceResponseLogsOutAndClearsContent() {
        when(configurationOperations.update()).thenReturn(Observable.just(UNAUTHORIZED_DEVICE_CONFIG));

        final PublishSubject<Void> logoutSubject = PublishSubject.create();
        when(accountOperations.logout()).thenReturn(logoutSubject);

        manager.update();

        logoutSubject.onNext(null);
        verify(configurationOperations, never()).saveConfiguration(any(Configuration.class));
    }

}
