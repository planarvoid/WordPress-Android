package com.soundcloud.android.policies;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.schema.BulkInsertValues;
import com.soundcloud.propeller.schema.Column;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class StorePoliciesCommand extends DefaultWriteStorageCommand<Iterable<ApiPolicyInfo>, TxnResult> {

    private final DateProvider dateProvider;

    @Inject
    public StorePoliciesCommand(PropellerDatabase database, CurrentDateProvider dateProvider) {
        super(database);
        this.dateProvider = dateProvider;
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, final Iterable<ApiPolicyInfo> input) {
        return propeller.bulkInsert(Tables.TrackPolicies.TABLE, buildPolicyBulkValues(input));
    }

    private List<Column> getColumns() {
        return Arrays.asList(
        Tables.TrackPolicies.TRACK_ID,
        Tables.TrackPolicies.POLICY,
        Tables.TrackPolicies.MONETIZABLE,
        Tables.TrackPolicies.SYNCABLE,
        Tables.TrackPolicies.SNIPPED,
        Tables.TrackPolicies.BLOCKED,
        Tables.TrackPolicies.LAST_UPDATED,
        Tables.TrackPolicies.MONETIZATION_MODEL,
        Tables.TrackPolicies.SUB_MID_TIER,
        Tables.TrackPolicies.SUB_HIGH_TIER
        );
    }

    private BulkInsertValues buildPolicyBulkValues(final Iterable<ApiPolicyInfo> input) {
        BulkInsertValues.Builder builder = new BulkInsertValues.Builder(getColumns());
        for (ApiPolicyInfo policyEntry : input) {
            Log.d(UpdatePoliciesCommand.TAG, "Writing policy: " + policyEntry);
            builder.addRow(
                    Arrays.asList(
                            policyEntry.getUrn().getNumericId(),
                            policyEntry.getPolicy(),
                            policyEntry.isMonetizable(),
                            policyEntry.isSyncable(),
                            policyEntry.isSnipped(),
                            policyEntry.isBlocked(),
                            dateProvider.getCurrentTime(),
                            policyEntry.getMonetizationModel(),
                            policyEntry.isSubMidTier(),
                            policyEntry.isSubHighTier()
                    )
            );
        }
        return builder.build();
    }
}
