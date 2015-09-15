package com.soundcloud.android.associations;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;

class RepostStorage {

    private final PropellerDatabase propeller;
    private final DateProvider dateProvider;

    @Inject
    RepostStorage(PropellerDatabase propeller, CurrentDateProvider dateProvider) {
        this.propeller = propeller;
        this.dateProvider = dateProvider;
    }

    Command<Urn, InsertResult> addRepost() {
        return new DefaultWriteStorageCommand<Urn, InsertResult>(propeller) {
            @Override
            public InsertResult write(PropellerDatabase propeller, Urn urn) {
                final ContentValues values = new ContentValues();
                values.put(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);
                values.put(TableColumns.Posts.TARGET_TYPE, urn.isTrack()
                        ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST);
                values.put(TableColumns.Posts.TARGET_ID, urn.getNumericId());
                values.put(TableColumns.Posts.CREATED_AT, dateProvider.getDate().getTime());
                return propeller.insert(Table.Posts, values);
            }
        };
    }

    Command<Urn, ChangeResult> removeRepost() {
        return new DefaultWriteStorageCommand<Urn, ChangeResult>(propeller) {
            @Override
            public ChangeResult write(PropellerDatabase propeller, Urn urn) {
                final Where whereClause = filter()
                        .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                        .whereEq(TableColumns.Posts.TARGET_TYPE, urn.isTrack() ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);

                return propeller.delete(Table.Posts, whereClause);
            }
        };
    }
}
