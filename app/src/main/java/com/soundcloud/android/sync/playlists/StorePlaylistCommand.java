package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;

class StorePlaylistCommand extends StoreCommand<ApiPlaylist> {

    @Inject
    public StorePlaylistCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.upsert(Table.Users, StoreUsersCommand.buildUserContentValues(input.getUser())));
                step(propeller.upsert(Table.Sounds, StorePlaylistsCommand.buildPlaylistContentValues(input)));
            }
        });
    }
}
