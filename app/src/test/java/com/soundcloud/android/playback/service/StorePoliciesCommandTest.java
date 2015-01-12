package com.soundcloud.android.playback.service;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class StorePoliciesCommandTest extends StorageIntegrationTest {

    private StorePoliciesCommand storePoliciesCommand;

    @Before
    public void setup() {
        storePoliciesCommand = new StorePoliciesCommand(propeller());
    }

    @Test
    public void shouldStoreListOfPolicies() throws Exception {
        List<PolicyInfo> policies = new ArrayList<>();
        policies.add(new PolicyInfo(Urn.forTrack(1L), true, "allowed"));
        policies.add(new PolicyInfo(Urn.forTrack(2L), false, "monetizable"));
        policies.add(new PolicyInfo(Urn.forTrack(3L), true, "something"));

        storePoliciesCommand.with(policies).call();

        expectPolicyInserted(Urn.forTrack(1L), true, "allowed");
        expectPolicyInserted(Urn.forTrack(2L), false, "monetizable");
        expectPolicyInserted(Urn.forTrack(3L), true, "something");
    }

    @Test
    public void storingListOfPoliciesEmitsTransactionResult() throws Exception {
        List<PolicyInfo> policies = new ArrayList<PolicyInfo>();
        policies.add(new PolicyInfo(Urn.forTrack(1L), true, "allowed"));
        policies.add(new PolicyInfo(Urn.forTrack(2L), false, "monetizable"));

        WriteResult result = storePoliciesCommand.with(policies).call();
        assertThat(result.success(), is(true));
    }

    private void expectPolicyInserted(Urn trackUrn, boolean monetizable, String policy) {
        assertThat(select(from(Table.SoundView.name())
                        .whereEq(TableColumns.SoundView._ID, trackUrn.getNumericId())
                        .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .whereEq(TableColumns.SoundView.MONETIZABLE, monetizable)
                        .whereEq(TableColumns.SoundView.POLICY, policy)
        ), counts(1));
    }

}