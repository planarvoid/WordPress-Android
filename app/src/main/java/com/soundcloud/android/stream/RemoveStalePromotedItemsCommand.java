package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Filter.filter;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RemoveStalePromotedItemsCommand extends WriteStorageCommand<Void, WriteResult, List<Long>> {

    @VisibleForTesting
    static final long STALE_TIME_MILLIS = TimeUnit.MINUTES.toMillis(50);

    private final DateProvider dateProvider;
    private List<Long> removeItems = Collections.emptyList();

    @Inject
    protected RemoveStalePromotedItemsCommand(PropellerDatabase propeller, DateProvider dateProvider) {
        super(propeller);
        this.dateProvider = dateProvider;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Void input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase db) {
                final long staleItemCutoff = dateProvider.getCurrentTime() - STALE_TIME_MILLIS;
                final Where whereClause = filter().whereLt(TableColumns.PromotedTracks.CREATED_AT, staleItemCutoff);
                removeItems = db.query(Query.from(Table.PromotedTracks.name())
                        .select(TableColumns.PromotedTracks._ID)
                        .where(whereClause))
                        .toList(RxResultMapper.scalar(Long.class));
                if (!removeItems.isEmpty()){
                    for (Long id : removeItems) {
                        step(db.delete(Table.SoundStream, filter().whereEq(TableColumns.SoundStream.PROMOTED_ID, id)));
                        step(db.delete(Table.PromotedTracks, filter().whereEq(TableColumns.PromotedTracks._ID, id)));
                    }
                }
            }
        });
    }

    @Override
    protected List<Long> transform(WriteResult result) {
        return removeItems;
    }
}
