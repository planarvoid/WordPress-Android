package com.soundcloud.android.policies;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PolicyUpdateServiceTest extends AndroidUnitTest {

    @Mock PolicyOperations policyOperations;
    @Mock PolicySettingsStorage policySettingsStorage;
    @Mock DateProvider dateProvider;

    private PolicyUpdateService policyUpdateService;
    private List<Urn> tracks = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(124L));
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        policyUpdateService = new PolicyUpdateService(policyOperations, policySettingsStorage, dateProvider, eventBus);
    }

    @Test
    public void publishesPolicyUpdateEventAfterSuccessfulPolicyUpdate() {
        when(policyOperations.updateTrackPolicies()).thenReturn(tracks);

        policyUpdateService.onHandleIntent(null);

        assertThat(eventBus.eventsOn(EventQueue.POLICY_UPDATES)).hasSize(1);
        assertThat(eventBus.eventsOn(EventQueue.POLICY_UPDATES).get(0).getTracks()).containsAll(tracks);
    }

    @Test
    public void storesLastPolicyUpdateTimeAfterSucessfulPolicyUpdate() {
        when(dateProvider.getCurrentTime()).thenReturn(1000L);
        when(policyOperations.updateTrackPolicies()).thenReturn(tracks);

        policyUpdateService.onHandleIntent(null);

        verify(policySettingsStorage).setPolicyUpdateTime(dateProvider.getCurrentTime());
    }

    @Test
    public void doesNotStoreNotSendEventsWhenPolicyUpdateFailed() {
        when(policyOperations.updateTrackPolicies()).thenReturn(Collections.<Urn>emptyList());

        policyUpdateService.onHandleIntent(null);

        verify(policySettingsStorage, never()).setPolicyUpdateTime(anyLong());
        eventBus.verifyNoEventsOn(EventQueue.POLICY_UPDATES);
    }

}