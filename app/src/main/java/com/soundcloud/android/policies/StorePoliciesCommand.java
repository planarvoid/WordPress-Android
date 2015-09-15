package com.soundcloud.android.policies;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;

import javax.inject.Inject;

class StorePoliciesCommand extends DefaultWriteStorageCommand<Iterable<ApiPolicyInfo>, TxnResult> {

    private final DateProvider dateProvider;

    @Inject
    public StorePoliciesCommand(PropellerDatabase database, CurrentDateProvider dateProvider) {
        super(database);
        this.dateProvider = dateProvider;
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, final Iterable<ApiPolicyInfo> input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (ApiPolicyInfo policy : input) {
                    step(propeller.upsert(Table.TrackPolicies, buildPolicyContentValues(policy)));
                }
            }
        });
    }

    private ContentValues buildPolicyContentValues(ApiPolicyInfo policyEntry) {
        return ContentValuesBuilder.values()
                .put(TableColumns.TrackPolicies.TRACK_ID, policyEntry.getUrn().getNumericId())
                .put(TableColumns.TrackPolicies.POLICY, policyEntry.getPolicy())
                .put(TableColumns.TrackPolicies.MONETIZABLE, policyEntry.isMonetizable())
                .put(TableColumns.TrackPolicies.SYNCABLE, policyEntry.isSyncable())
                .put(TableColumns.TrackPolicies.LAST_UPDATED, dateProvider.getTime())
                .put(TableColumns.TrackPolicies.MONETIZATION_MODEL, policyEntry.getMonetizationModel())
                .put(TableColumns.TrackPolicies.SUB_MID_TIER, policyEntry.isSubMidTier())
                .put(TableColumns.TrackPolicies.SUB_HIGH_TIER, policyEntry.isSubHighTier())
                .get();
    }
}
