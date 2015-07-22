package com.soundcloud.android.sync.posts;

import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

class LoadLocalPlaylistsCommand extends LegacyCommand<Object, List<PropertySet>, LoadLocalPlaylistsCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLocalPlaylistsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<PropertySet> call() throws Exception {
        final QueryResult queryResult = database.query(Query.from(Table.Sounds.name())
                .select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.SHARING
                )
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereLt(TableColumns.Sounds._ID, 0));
        return queryResult.toList(new LocalPlaylistsMapper());
    }

    private static final class LocalPlaylistsMapper implements ResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader reader) {
            return PropertySet.from(
                    PlaylistProperty.URN.bind(Urn.forPlaylist(reader.getLong(TableColumns.Sounds._ID))),
                    PlaylistProperty.TITLE.bind(reader.getString(TableColumns.Sounds.TITLE)),
                    PlaylistProperty.IS_PRIVATE.bind(Sharing.PRIVATE.value().equals(
                            reader.getString(TableColumns.SoundView.SHARING)))
            );
        }
    }
}
