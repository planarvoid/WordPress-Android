package com.soundcloud.android.playlists;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.likes.ChronologicalQueryParams;
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
        return Query.from(Table.SoundView.name())
                .select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.TRACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.SHARING,
                        TableColumns.SoundView.CREATED_AT)
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.SoundView.USER_ID, accountOperations.getLoggedInUserUrn().getNumericId())
                .whereLt(TableColumns.SoundView.CREATED_AT, input.getTimestamp())
                .order(TableColumns.SoundView.CREATED_AT, Query.ORDER_DESC);
    }
}
