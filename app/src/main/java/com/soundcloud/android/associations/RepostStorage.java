package com.soundcloud.android.associations;

import com.soundcloud.android.commands.CommandNG;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;

import android.content.ContentValues;

import javax.inject.Inject;

class RepostStorage {

    private final PropellerDatabase propeller;
    private final DateProvider dateProvider;

    @Inject
    RepostStorage(PropellerDatabase propeller, DateProvider dateProvider) {
        this.propeller = propeller;
        this.dateProvider = dateProvider;
    }

    CommandNG<Urn, InsertResult> addRepost() {
        return new WriteStorageCommand<Urn, InsertResult>(propeller) {
            @Override
            public InsertResult write(PropellerDatabase propeller, Urn urn) {
                final ContentValues values = new ContentValues();
                values.put(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);
                values.put(TableColumns.Posts.TARGET_TYPE, urn.isTrack()
                        ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST);
                values.put(TableColumns.Posts.TARGET_ID, urn.getNumericId());
                values.put(TableColumns.Posts.CREATED_AT, dateProvider.getCurrentDate().getTime());
                return propeller.insert(Table.Posts, values);
            }
        };
    }

    CommandNG<Urn, ChangeResult> removeRepost() {
        return new WriteStorageCommand<Urn, ChangeResult>(propeller) {
            @Override
            public ChangeResult write(PropellerDatabase propeller, Urn urn) {
                final Where whereClause = new WhereBuilder()
                        .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                        .whereEq(TableColumns.Posts.TARGET_TYPE, urn.isTrack() ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);

                return propeller.delete(Table.Posts, whereClause);
            }
        };
    }
}
