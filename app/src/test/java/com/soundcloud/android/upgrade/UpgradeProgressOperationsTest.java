package com.soundcloud.android.upgrade;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
    private TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        upgradeProgressOperations = new UpgradeProgressOperations(configurationOperations, policyOperations);
    }

    @Test
    public void shouldEmitIfBothConfigurationAndPolicyFetchSucceed() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        upgradeProgressOperations.awaitAccountUpgrade().subscribe(subscriber);

        subscriber.assertValue(singletonList(Urn.forTrack(123)));
    }
}
