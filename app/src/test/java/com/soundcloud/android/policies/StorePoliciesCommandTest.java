package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class StorePoliciesCommandTest extends StorageIntegrationTest {

    private StorePoliciesCommand storePoliciesCommand;

    @Before
    public void setup() {
        storePoliciesCommand = new StorePoliciesCommand(propeller());
    }

    @Test
    public void shouldStoreListOfPolicies() throws Exception {
        final ApiTrack track1 = testFixtures().insertTrack();
        final ApiTrack track2 = testFixtures().insertTrack();
        final ApiTrack track3 = testFixtures().insertTrack();

        List<PolicyInfo> policies = new ArrayList<>();
        policies.add(new PolicyInfo(track1.getUrn(), true, "allowed", true));
        policies.add(new PolicyInfo(track2.getUrn(), false, "monetizable", true));
        policies.add(new PolicyInfo(track3.getUrn(), true, "something", false));

        final WriteResult result = storePoliciesCommand.with(policies).call();

        assertThat(result.success()).isTrue();
        for (PolicyInfo info : policies) {
            databaseAssertions().assetPolicyInserted(
                    info.getTrackUrn(),
                    info.isMonetizable(),
                    info.getPolicy(),
                    info.isSyncable());
        }
    }
}