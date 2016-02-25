package com.soundcloud.android.offline;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.PlaylistUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

public class LoadOfflinePlaylistsCommand extends Command<Void, List<Urn>> {
    private final PropellerDatabase propeller;

    @Inject
    LoadOfflinePlaylistsCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public List<Urn> call(Void ignored) {
        return propeller.query(offlinePlaylists()).toList(new PlaylistUrnMapper());
    }

    private static Query offlinePlaylists() {
        return Query
                .from(Tables.OfflineContent.TABLE)
                .where(filter().whereEq(Tables.OfflineContent._TYPE, Tables.OfflineContent.TYPE_PLAYLIST));
    }

}
