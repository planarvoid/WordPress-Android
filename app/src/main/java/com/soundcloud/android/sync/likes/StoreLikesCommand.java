package com.soundcloud.android.sync.likes;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.schema.BulkInsertValues;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class StoreLikesCommand extends DefaultWriteStorageCommand<Collection<LikeRecord>, TxnResult> {

    @Inject
    StoreLikesCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, Collection<LikeRecord> input) {
        BulkInsertValues.Builder builder = new BulkInsertValues.Builder(
                Arrays.asList(
                        Tables.Likes._ID,
                        Tables.Likes._TYPE,
                        Tables.Likes.CREATED_AT
                )
        );
        for (LikeRecord like : input) {
            builder.addRow(buildContentValuesForLike(like));
        }
        return propeller.bulkInsert(Tables.Likes.TABLE, builder.build());
    }

    private List<Object> buildContentValuesForLike(LikeRecord like) {
        final Urn targetUrn = like.getTargetUrn();
        return Arrays.<Object>asList(
                targetUrn.getNumericId(),
                targetUrn.isTrack()
                ? Tables.Sounds.TYPE_TRACK
                : Tables.Sounds.TYPE_PLAYLIST,
                like.getCreatedAt().getTime()
        );

    }

}
