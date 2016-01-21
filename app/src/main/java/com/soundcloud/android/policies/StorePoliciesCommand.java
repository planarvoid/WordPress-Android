package com.soundcloud.android.policies;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
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
        return propeller.bulkInsert(Table.TrackPolicies, buildPolicyContentValues(input));
    }

    private List<ContentValues> buildPolicyContentValues(final Iterable<ApiPolicyInfo> input) {
        final List<ContentValues> cvs = new ArrayList<>();
        for (ApiPolicyInfo policyEntry : input) {
            final ContentValues cv = new ContentValues();
            cv.put(TableColumns.TrackPolicies.TRACK_ID, policyEntry.getUrn().getNumericId());
            cv.put(TableColumns.TrackPolicies.POLICY, policyEntry.getPolicy());
            cv.put(TableColumns.TrackPolicies.MONETIZABLE, policyEntry.isMonetizable());
            cv.put(TableColumns.TrackPolicies.SYNCABLE, policyEntry.isSyncable());
            cv.put(TableColumns.TrackPolicies.SNIPPED, policyEntry.isSnipped());
            cv.put(TableColumns.TrackPolicies.BLOCKED, policyEntry.isBlocked());
            cv.put(TableColumns.TrackPolicies.LAST_UPDATED, dateProvider.getCurrentTime());
            cv.put(TableColumns.TrackPolicies.MONETIZATION_MODEL, policyEntry.getMonetizationModel());
            cv.put(TableColumns.TrackPolicies.SUB_MID_TIER, policyEntry.isSubMidTier());
            cv.put(TableColumns.TrackPolicies.SUB_HIGH_TIER, policyEntry.isSubHighTier());
            cvs.add(cv);
        }
        return cvs;
    }
}
