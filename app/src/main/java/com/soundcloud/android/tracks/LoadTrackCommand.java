package com.soundcloud.android.tracks;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.commands.SingleResourceQueryCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;

import javax.inject.Inject;

public class LoadTrackCommand extends SingleResourceQueryCommand<Urn> {

    @Inject
    LoadTrackCommand(DatabaseScheduler databaseScheduler) {
        super(databaseScheduler, new TrackItemMapper());
    }

    @Override
    protected Query buildQuery(Urn input) {
        return Query.from(Table.SoundView.name())
                .select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.USER_ID,
                        TableColumns.SoundView.DURATION,
                        TableColumns.SoundView.PLAYBACK_COUNT,
                        TableColumns.SoundView.COMMENT_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.REPOSTS_COUNT,
                        TableColumns.SoundView.WAVEFORM_URL,
                        TableColumns.SoundView.STREAM_URL,
                        TableColumns.SoundView.MONETIZABLE,
                        TableColumns.SoundView.POLICY,
                        TableColumns.SoundView.PERMALINK_URL,
                        TableColumns.SoundView.SHARING,
                        TableColumns.SoundView.CREATED_AT,
                        TableColumns.SoundView.OFFLINE_DOWNLOADED_AT,
                        TableColumns.SoundView.OFFLINE_REMOVED_AT,
                        exists(likeQuery()).as(TableColumns.SoundView.USER_LIKE),
                        exists(repostQuery()).as(TableColumns.SoundView.USER_REPOST)
                )
                .whereEq(TableColumns.SoundView._ID, input.getNumericId())
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK);
    }

    private Query likeQuery() {
        return Query.from(Table.Likes.name(), Table.Sounds.name())
                .joinOn(Table.SoundView + "." + TableColumns.SoundView._ID, Table.Likes.name() + "." + TableColumns.Likes._ID)
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereNull(TableColumns.Likes.REMOVED_AT);
    }

    private Query repostQuery() {
        return Query.from(Table.CollectionItems.name(), Table.Sounds.name())
                .joinOn(Table.SoundView + "." + TableColumns.SoundView._ID, TableColumns.CollectionItems.ITEM_ID)
                .joinOn(TableColumns.SoundView._TYPE, TableColumns.CollectionItems.RESOURCE_TYPE)
                .whereEq(TableColumns.CollectionItems.COLLECTION_TYPE, REPOST)
                .whereEq(TableColumns.CollectionItems.RESOURCE_TYPE, TableColumns.Sounds.TYPE_TRACK);
    }
}
