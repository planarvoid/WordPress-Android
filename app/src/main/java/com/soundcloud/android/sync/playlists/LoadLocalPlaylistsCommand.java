package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

class LoadLocalPlaylistsCommand extends LegacyCommand<Object, List<PlaylistItem>, LoadLocalPlaylistsCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLocalPlaylistsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<PlaylistItem> call() throws Exception {
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

    private static final class LocalPlaylistsMapper implements ResultMapper<PlaylistItem> {
        @Override
        public PlaylistItem map(CursorReader reader) {
            return PlaylistItem.from(PropertySet.from(
                    PlaylistProperty.URN.bind(Urn.forPlaylist(reader.getLong(Tables.Sounds._ID))),
                    PlaylistProperty.TITLE.bind(reader.getString(Tables.Sounds.TITLE)),
                    PlaylistProperty.IS_PRIVATE.bind(Sharing.PRIVATE.value().equals(
                            reader.getString(Tables.Sounds.SHARING)))
            ));
        }
    }
}
