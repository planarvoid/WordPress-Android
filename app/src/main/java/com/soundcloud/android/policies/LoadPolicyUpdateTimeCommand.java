package com.soundcloud.android.policies;

import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;

class LoadPolicyUpdateTimeCommand extends Command<Void, Long> {

    private final PropellerDatabase propeller;

    @Inject
    LoadPolicyUpdateTimeCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Long call(Void input) {
        final Query query = Query.from(Tables.TrackPolicies.TABLE)
                                 .select(Tables.TrackPolicies.LAST_UPDATED)
                                 .order(Tables.TrackPolicies.LAST_UPDATED, Query.Order.DESC)
                                 .limit(1);

        return propeller.query(query).firstOrDefault(Long.class, (long) Consts.NOT_SET);
    }
}
