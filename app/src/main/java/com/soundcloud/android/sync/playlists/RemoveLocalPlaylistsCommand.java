package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.WhereBuilder;

import javax.inject.Inject;

public class RemoveLocalPlaylistsCommand extends WriteStorageCommand<Void, WriteResult> {

    @Inject
    RemoveLocalPlaylistsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Void input) {
        return propeller.delete(Table.Sounds, new WhereBuilder()
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereLt(TableColumns.Sounds._ID, 0));
    }
}
