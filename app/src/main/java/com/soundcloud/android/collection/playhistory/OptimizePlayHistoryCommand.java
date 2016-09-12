package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.storage.Tables.PlayHistory.TABLE;
import static com.soundcloud.android.storage.Tables.PlayHistory.TIMESTAMP;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;

public class OptimizePlayHistoryCommand extends DefaultWriteStorageCommand<Integer, ChangeResult> {

    @Inject
    OptimizePlayHistoryCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected ChangeResult write(PropellerDatabase propeller, Integer offset) {
        QueryResult queryResult = propeller.query(Query.from(TABLE)
                                                       .select(TIMESTAMP)
                                                       .order(TIMESTAMP, DESC)
                                                       .limit(1, offset));

        if (queryResult.getResultCount() > 0) {
            Long last = queryResult.first(scalar(Long.class));
            queryResult.release();
            return propeller.delete(TABLE, filter().whereLe(TIMESTAMP, last));
        } else {
            queryResult.release();
            return new ChangeResult(0);
        }
    }
}
