package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.TxnResult;
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
        final ApiTrack track1 = testFixtures().insertTrack();
        final ApiTrack track2 = testFixtures().insertTrack();
        final ApiTrack track3 = testFixtures().insertTrack();

        List<PolicyInfo> policies = new ArrayList<>();
        policies.add(new PolicyInfo(track1.getUrn(), true, "allowed"));
        policies.add(new PolicyInfo(track2.getUrn(), false, "monetizable"));
        policies.add(new PolicyInfo(track3.getUrn(), true, "something"));

        final WriteResult result = storePoliciesCommand.with(policies).call();

        expect(result.success()).toBeTrue();
        expectPolicyInserted(track1.getUrn(), true, "allowed");
        expectPolicyInserted(track2.getUrn(), false, "monetizable");
        expectPolicyInserted(track3.getUrn(), true, "something");
    }

    @Test // this is to avoid the problem where we created broken track entries with just an ID and policy
    public void shouldNotCreateNewRecordIfTrackMetadataIsMissing() throws PropellerWriteException {
        final ApiTrack existingTrack = testFixtures().insertTrack();
        List<PolicyInfo> policies = new ArrayList<PolicyInfo>();
        policies.add(new PolicyInfo(existingTrack.getUrn(), true, "allowed"));
        // this one should be ignored, since we're missing the track for it in the DB
        policies.add(new PolicyInfo(Urn.forTrack(1234L), false, "monetizable"));

        TxnResult result = (TxnResult) storePoliciesCommand.with(policies).call();

        expect(result.getResults()).toNumber(2);
        expect(((ChangeResult) result.getResults().get(0)).getNumRowsAffected()).toBe(1);
        expect(((ChangeResult) result.getResults().get(1)).getNumRowsAffected()).toBe(0);
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