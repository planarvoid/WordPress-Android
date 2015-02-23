package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;

import javax.inject.Inject;

class RemovePlaylistCommand extends Command<Urn, WriteResult, RemovePlaylistCommand> {

    private final PropellerDatabase database;

    @Inject
    RemovePlaylistCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public WriteResult call() throws Exception {
        final Where whereClause = new WhereBuilder()
                .whereEq(TableColumns.Sounds._ID, input.getNumericId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);

        return database.delete(Table.Sounds, whereClause);
    }
}
