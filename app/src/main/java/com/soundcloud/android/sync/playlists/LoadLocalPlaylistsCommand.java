package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

class LoadLocalPlaylistsCommand extends LegacyCommand<Object, List<LocalPlaylistChange>, LoadLocalPlaylistsCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLocalPlaylistsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<LocalPlaylistChange> call() throws Exception {
        final QueryResult queryResult = database.query(Query.from(Tables.Sounds.TABLE)
                                                            .select(
                                                                    Tables.Sounds._ID,
                                                                    Tables.Sounds.TITLE,
                                                                    Tables.Sounds.SHARING
                                                            )
                                                            .whereEq(Tables.Sounds._TYPE,
                                                                     Tables.Sounds.TYPE_PLAYLIST)
                                                            .whereLt(Tables.Sounds._ID, 0));
        return queryResult.toList(new LocalPlaylistsMapper());
    }

    private static final class LocalPlaylistsMapper implements ResultMapper<LocalPlaylistChange> {
        @Override
        public LocalPlaylistChange map(CursorReader reader) {
            return LocalPlaylistChange.create(Urn.forPlaylist(reader.getLong(Tables.Sounds._ID)),
                                              reader.getString(Tables.Sounds.TITLE),
                                              Sharing.PRIVATE.value().equals(reader.getString(Tables.Sounds.SHARING)));
        }
    }

}
