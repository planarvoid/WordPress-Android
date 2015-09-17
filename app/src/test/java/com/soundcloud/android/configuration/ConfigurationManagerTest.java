package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigurationManagerTest extends AndroidUnitTest {

    @Mock private ConfigurationOperations configurationOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private DeviceManagementStorage deviceManagementStorage;

    private ConfigurationManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new ConfigurationManager(configurationOperations, offlineContentOperations, accountOperations, deviceManagementStorage);
    }

    @Test
    public void updateWithAuthorizedDeviceResponseSavesConfiguration() {
        Configuration configuration = new Configuration(Collections.<Feature>emptyList(),
                new UserPlan("free", Arrays.asList("mid_tier")), Collections.<Layer>emptyList(), new DeviceManagement(true, null));
        when(configurationOperations.update()).thenReturn(Observable.just(configuration));

        manager.update();

        verify(configurationOperations).saveConfiguration(configuration);
    }

    @Test
    public void updateWithUnauthorizedDeviceResponseLogsOutAndClearsContent() {
        Configuration configurationWithDeviceConflict = new Configuration(Collections.<Feature>emptyList(),
                new UserPlan("free", Arrays.asList("mid_tier")), Collections.<Layer>emptyList(), new DeviceManagement(false, null));

        when(configurationOperations.update()).thenReturn(Observable.just(configurationWithDeviceConflict));

        final PublishSubject<Void> logoutSubject = PublishSubject.create();
        final PublishSubject<List<Urn>> clearOfflineContentSubject = PublishSubject.create();
        when(accountOperations.logout()).thenReturn(logoutSubject);
        when(offlineContentOperations.clearOfflineContent()).thenReturn(clearOfflineContentSubject);

        manager.update();

        logoutSubject.onNext(null);
        assertThat(clearOfflineContentSubject.hasObservers()).isTrue();
        verify(configurationOperations, never()).saveConfiguration(any(Configuration.class));
    }

}