package com.soundcloud.android.playlists;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.PlaylistUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

public class LoadPlaylistPendingRemovalCommand extends Command<Void, List<Urn>> {

    private final PropellerDatabase database;

    @Inject
    LoadPlaylistPendingRemovalCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call(Void input) {
        return database.query(Query.from(Table.Sounds.name())
                .select(TableColumns.SoundView._ID)
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNotNull(TableColumns.Sounds.REMOVED_AT)).toList(new PlaylistUrnMapper());
    }

}
