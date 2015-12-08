package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;

import javax.inject.Inject;

public class RemovePlaylistCommand extends DefaultWriteStorageCommand<Urn, ChangeResult> {

    @Inject
    RemovePlaylistCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected ChangeResult write(PropellerDatabase propeller, Urn playlist) {
        return propeller.delete(Table.Sounds, filter()
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Sounds._ID, playlist.getNumericId()));
    }
}
