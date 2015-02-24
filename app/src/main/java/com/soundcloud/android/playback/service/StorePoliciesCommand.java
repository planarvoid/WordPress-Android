package com.soundcloud.android.playback.service;

import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.WhereBuilder;

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
                    step(propeller.update(Table.Sounds, buildPolicyContentValues(policy),
                            new WhereBuilder().whereEq(TableColumns.Sounds._ID, policy.getTrackUrn().getNumericId())));
                }
            }
        });
    }

    private ContentValues buildPolicyContentValues(PolicyInfo policyEntry) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Sounds._ID, policyEntry.getTrackUrn().getNumericId())
                .put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .put(TableColumns.Sounds.POLICY, policyEntry.getPolicy())
                .put(TableColumns.Sounds.MONETIZABLE, policyEntry.isMonetizable())
                .get();
    }

}
