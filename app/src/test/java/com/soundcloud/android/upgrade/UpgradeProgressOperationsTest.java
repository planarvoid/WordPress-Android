package com.soundcloud.android.upgrade;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.upgrade.UpgradeProgressOperations.UpgradeResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeProgressOperationsTest {

    private UpgradeProgressOperations upgradeProgressOperations;

    @Mock private ConfigurationOperations configurationOperations;
    @Mock private PolicyOperations policyOperations;
    private TestSubscriber<UpgradeResult> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        upgradeProgressOperations = new UpgradeProgressOperations(configurationOperations, policyOperations);
    }

    @Test
    public void shouldEmitResultIfBothConfigurationAndPolicyFetchSucceed() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.updatedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        upgradeProgressOperations.awaitAccountUpgrade().subscribe(subscriber);

        subscriber.assertValueCount(1);
        UpgradeResult upgradeResult = subscriber.getOnNextEvents().get(0);
        assertThat(upgradeResult.hasFailures()).isFalse();
        assertThat(upgradeResult.configurationReceived).isTrue();
        assertThat(upgradeResult.policiesUpdated).isTrue();
    }

    @Test
    public void shouldEmitResultIfConfigurationFetchFailsAndPolicyFetchSucceeds() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.<Configuration>error(new Exception()));
        when(policyOperations.updatedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        upgradeProgressOperations.awaitAccountUpgrade().subscribe(subscriber);

        subscriber.assertValueCount(1);
        UpgradeResult upgradeResult = subscriber.getOnNextEvents().get(0);
        assertThat(upgradeResult.hasFailures()).isTrue();
        assertThat(upgradeResult.configurationReceived).isFalse();
        assertThat(upgradeResult.policiesUpdated).isTrue();
    }

    @Test
    public void shouldEmitResultIfConfigurationFetchSucceedsAndPolicyFetchFails() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.updatedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>error(new Exception()));

        upgradeProgressOperations.awaitAccountUpgrade().subscribe(subscriber);

        subscriber.assertValueCount(1);
        UpgradeResult upgradeResult = subscriber.getOnNextEvents().get(0);
        assertThat(upgradeResult.hasFailures()).isTrue();
        assertThat(upgradeResult.configurationReceived).isTrue();
        assertThat(upgradeResult.policiesUpdated).isFalse();
    }
}
