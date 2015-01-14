package com.soundcloud.android.playback.service;

import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

class StorePoliciesCommand extends StoreCommand<Iterable<PolicyInfo>> {

    @Inject
    public StorePoliciesCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (PolicyInfo policy : input) {
                    step(propeller.upsert(Table.Sounds, buildPolicyContentValues(policy)));
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
