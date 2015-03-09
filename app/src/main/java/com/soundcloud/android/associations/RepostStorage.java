package com.soundcloud.android.associations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;

class RepostStorage {

    private final DatabaseScheduler scheduler;
    private final DateProvider dateProvider;

    @Inject
    RepostStorage(DatabaseScheduler scheduler, DateProvider dateProvider) {
        this.scheduler = scheduler;
        this.dateProvider = dateProvider;
    }

    Observable<InsertResult> addRepost(final Urn urn) {
        final ContentValues values = new ContentValues();
        values.put(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);
        values.put(TableColumns.Posts.TARGET_TYPE, urn.isTrack()
                ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST);
        values.put(TableColumns.Posts.TARGET_ID, urn.getNumericId());
        values.put(TableColumns.Posts.CREATED_AT, dateProvider.getCurrentDate().getTime());

        return scheduler.scheduleInsert(Table.Posts, values);
    }

    Observable<ChangeResult> removeRepost(final Urn urn) {
        final Where whereClause = new WhereBuilder()
                .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                .whereEq(TableColumns.Posts.TARGET_TYPE, urn.isTrack() ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);

        return scheduler.scheduleDelete(Table.Posts, whereClause);
    }

}
