package com.soundcloud.android.policies;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DailyUpdateServiceTest extends AndroidUnitTest {

    @Mock PolicyOperations policyOperations;
    @Mock PolicySettingsStorage policySettingsStorage;
    @Mock ConfigurationManager configurationManager;
    @Mock AdIdHelper adIdHelper;
    @Mock CurrentDateProvider dateProvider;

    private DailyUpdateService dailyUpdateService;
    private List<Urn> tracks = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(124L));
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        dailyUpdateService = new DailyUpdateService(policyOperations, policySettingsStorage,
                                                    configurationManager, adIdHelper, eventBus);
    }

    @Test
    public void publishesPolicyUpdateEventAfterSuccessfulPolicyUpdate() {
        when(policyOperations.updateTrackPolicies()).thenReturn(tracks);

        dailyUpdateService.onHandleIntent(startIntent());

        assertThat(eventBus.eventsOn(EventQueue.POLICY_UPDATES)).hasSize(1);
        assertThat(eventBus.eventsOn(EventQueue.POLICY_UPDATES).get(0).getTracks()).containsAll(tracks);
    }

    @Test
    public void doesNotSendEventsWhenPolicyUpdateFailed() {
        when(policyOperations.updateTrackPolicies()).thenReturn(Collections.emptyList());

        dailyUpdateService.onHandleIntent(startIntent());

        eventBus.verifyNoEventsOn(EventQueue.POLICY_UPDATES);
    }

    @Test
    public void updatesConfiguration() {
        dailyUpdateService.onHandleIntent(startIntent());

        verify(configurationManager).forceConfigurationUpdate();
    }

    @Test
    public void updatesAdId() {
        dailyUpdateService.onHandleIntent(startIntent());

        verify(adIdHelper).init();
    }

    private static Intent startIntent() {
        final Intent intent = new Intent(context(), DailyUpdateService.class);
        intent.setAction(DailyUpdateService.ACTION_START);
        return intent;
    }

}
