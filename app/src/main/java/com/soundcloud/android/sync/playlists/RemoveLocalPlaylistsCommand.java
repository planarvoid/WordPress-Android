package com.soundcloud.android.sync.playlists;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;

public class RemoveLocalPlaylistsCommand extends DefaultWriteStorageCommand<Void, WriteResult> {

    @Inject
    RemoveLocalPlaylistsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Void input) {
        return propeller.delete(Tables.Sounds.TABLE, filter()
                .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_PLAYLIST)
                .whereLt(Tables.Sounds._ID, 0));
    }
}
