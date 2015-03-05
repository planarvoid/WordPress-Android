package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

class LoadPlaylistCommand extends Command<Urn, PropertySet, LoadPlaylistCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadPlaylistCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public PropertySet call() throws Exception {
        final List<PropertySet> queryResult = database.query(buildQuery(input)).toList(new PlaylistItemMapper());
        return queryResult.isEmpty() ? PropertySet.create() : queryResult.get(0);
    }

    private Query buildQuery(Urn input) {
        return Query.from(Table.SoundView.name())
                .select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.USER_ID,
                        TableColumns.SoundView.DURATION,
                        TableColumns.SoundView.TRACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.REPOSTS_COUNT,
                        TableColumns.SoundView.PERMALINK_URL,
                        TableColumns.SoundView.SHARING,
                        TableColumns.SoundView.CREATED_AT,
                        exists(likeQuery()).as(TableColumns.SoundView.USER_LIKE),
                        exists(repostQuery()).as(TableColumns.SoundView.USER_REPOST),
                        exists(isMarkedForOfflineQuery()).as(PlaylistItemMapper.IS_MARKED_FOR_OFFLINE)
                )
                .whereEq(TableColumns.SoundView._ID, input.getNumericId())
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private Query likeQuery() {
        return Query.from(Table.Likes.name(), Table.Sounds.name())
                .joinOn(Table.SoundView + "." + TableColumns.SoundView._ID, Table.Likes.name() + "." + TableColumns.Likes._ID)
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNull(TableColumns.Likes.REMOVED_AT);
    }

    private Query repostQuery() {
        return Query.from(Table.CollectionItems.name(), Table.Sounds.name())
                .joinOn(Table.SoundView + "." + TableColumns.SoundView._ID, TableColumns.CollectionItems.ITEM_ID)
                .joinOn(TableColumns.SoundView._TYPE, TableColumns.CollectionItems.RESOURCE_TYPE)
                .whereEq(TableColumns.CollectionItems.COLLECTION_TYPE, REPOST)
                .whereEq(TableColumns.CollectionItems.RESOURCE_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private Query isMarkedForOfflineQuery() {
        return Query.from(Table.OfflineContent.name(), Table.Sounds.name())
                .joinOn(Table.SoundView + "." + TableColumns.SoundView._ID, Table.OfflineContent.name() + "." + TableColumns.Likes._ID)
                .whereEq(Table.OfflineContent + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
    }

}