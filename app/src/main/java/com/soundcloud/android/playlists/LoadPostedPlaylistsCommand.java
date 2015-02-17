package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.ColumnFunctions.field;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;

public class LoadPostedPlaylistsCommand extends PagedQueryCommand<ChronologicalQueryParams> {

    private final AccountOperations accountOperations;

    @Inject
    LoadPostedPlaylistsCommand(PropellerDatabase database, AccountOperations accountOperations) {
        super(database, new PostedPlaylistMapper());
        this.accountOperations = accountOperations;
    }

    @Override
    protected Query buildQuery(ChronologicalQueryParams input) {
        return Query.from(Table.SoundView.name(), Table.CollectionItems.name())
                .select(
                        field(Table.SoundView + "." + TableColumns.SoundView._ID).as(TableColumns.SoundView._ID),
                        field(Table.SoundView + "." + TableColumns.SoundView.TITLE).as(TableColumns.SoundView.TITLE),
                        field(Table.SoundView + "." + TableColumns.SoundView.USERNAME).as(TableColumns.SoundView.USERNAME),
                        field(Table.SoundView + "." + TableColumns.SoundView.TRACK_COUNT).as(TableColumns.SoundView.TRACK_COUNT),
                        field(Table.SoundView + "." + TableColumns.SoundView.LIKES_COUNT).as(TableColumns.SoundView.LIKES_COUNT),
                        field(Table.SoundView + "." + TableColumns.SoundView.SHARING).as(TableColumns.SoundView.SHARING),
                        field(Table.SoundView + "." + TableColumns.SoundView.CREATED_AT).as(TableColumns.SoundView.CREATED_AT))
                .joinOn(Table.CollectionItems + "." + TableColumns.CollectionItems.ITEM_ID, Table.SoundView + "." + TableColumns.SoundView._ID)
                .joinOn(Table.CollectionItems + "." + TableColumns.CollectionItems.COLLECTION_TYPE, Integer.toString(CollectionStorage.CollectionItemTypes.PLAYLIST))
                .joinOn(Table.CollectionItems + "." + TableColumns.CollectionItems.USER_ID, Table.SoundView + "." + TableColumns.SoundView.USER_ID)
                .whereEq(Table.SoundView + "." + TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(Table.SoundView + "." + TableColumns.SoundView.USER_ID, accountOperations.getLoggedInUserUrn().getNumericId())
                .whereLt(Table.SoundView + "." + TableColumns.SoundView.CREATED_AT, input.getTimestamp())
                .order(TableColumns.SoundView.CREATED_AT, Query.ORDER_DESC);
    }
}
