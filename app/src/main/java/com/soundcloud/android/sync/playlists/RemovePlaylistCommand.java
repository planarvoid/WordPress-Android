package com.soundcloud.android.sync.playlists;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;

class RemovePlaylistCommand extends LegacyCommand<Urn, WriteResult, RemovePlaylistCommand> {

    private final PropellerDatabase database;

    @Inject
    RemovePlaylistCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public WriteResult call() throws Exception {
        final Where whereClause = filter()
                .whereEq(TableColumns.Sounds._ID, input.getNumericId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);

        return database.delete(Table.Sounds, whereClause);
    }
}
