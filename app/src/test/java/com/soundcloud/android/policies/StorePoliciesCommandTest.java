package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

public class StorePoliciesCommandTest extends StorageIntegrationTest {

    @Mock private CurrentDateProvider dateProvider;

    private StorePoliciesCommand storePoliciesCommand;

    @Before
    public void setup() {
        storePoliciesCommand = new StorePoliciesCommand(propeller(), dateProvider);
    }

    @Test
    public void shouldStoreListOfPolicies() {
        final ApiTrack track1 = testFixtures().insertTrack();
        final ApiTrack track2 = testFixtures().insertTrack();
        final ApiTrack track3 = testFixtures().insertTrack();

        List<ApiPolicyInfo> policies = new ArrayList<>();
        policies.add(ModelFixtures.apiPolicyInfo(Urn.forTrack(123L)));
        policies.add(ModelFixtures.apiPolicyInfo(Urn.forTrack(234L)));

        final WriteResult result = storePoliciesCommand.call(policies);

        assertThat(result.success()).isTrue();

        for (ApiPolicyInfo info : policies) {
            databaseAssertions().assertPolicyInserted(info);
        }
    }
}