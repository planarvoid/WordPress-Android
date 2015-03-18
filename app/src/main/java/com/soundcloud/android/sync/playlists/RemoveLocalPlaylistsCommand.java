package com.soundcloud.android.sync.playlists;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.WhereBuilder;

import javax.inject.Inject;
import javax.inject.Provider;

public class RemoveLocalPlaylistsCommand extends DefaultWriteStorageCommand<Void, WriteResult> {

    @Inject
    RemoveLocalPlaylistsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @VisibleForTesting
    RemoveLocalPlaylistsCommand(PropellerDatabase propeller, Provider<Thread> currentThreadProvider) {
        super(propeller, currentThreadProvider);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Void input) {
        return propeller.delete(Table.Sounds, new WhereBuilder()
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereLt(TableColumns.Sounds._ID, 0));
    }
}
