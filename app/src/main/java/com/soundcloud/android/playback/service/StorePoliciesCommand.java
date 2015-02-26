package com.soundcloud.android.playback.service;

import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;

import javax.inject.Inject;

class StorePoliciesCommand extends StoreCommand<Iterable<PolicyInfo>> {

    @Inject
    public StorePoliciesCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected TxnResult store() {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (PolicyInfo policy : input) {
                    step(propeller.upsert(Table.TrackPolicies, buildPolicyContentValues(policy)));
                }
            }
        });
    }

    private ContentValues buildPolicyContentValues(PolicyInfo policyEntry) {
        return ContentValuesBuilder.values()
                .put(TableColumns.TrackPolicies.TRACK_ID, policyEntry.getTrackUrn().getNumericId())
                .put(TableColumns.TrackPolicies.POLICY, policyEntry.getPolicy())
                .put(TableColumns.TrackPolicies.MONETIZABLE, policyEntry.isMonetizable())
                .put(TableColumns.TrackPolicies.SYNCABLE, policyEntry.isSyncable())
                .put(TableColumns.TrackPolicies.LAST_UPDATED, System.currentTimeMillis())
                .get();
    }

}
