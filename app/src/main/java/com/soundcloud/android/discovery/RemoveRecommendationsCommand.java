package com.soundcloud.android.discovery;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;

public class RemoveRecommendationsCommand extends DefaultWriteStorageCommand<Void, WriteResult> {

    @Inject
    RemoveRecommendationsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Void input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(Tables.Recommendations.TABLE));
                step(propeller.delete(Tables.RecommendationSeeds.TABLE));
            }
        });
    }
}
