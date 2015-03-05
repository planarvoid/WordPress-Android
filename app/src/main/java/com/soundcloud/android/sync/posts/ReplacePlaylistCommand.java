package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.TableColumns.PlaylistTracks;
import static com.soundcloud.android.storage.TableColumns.Sounds;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.WhereBuilder;

import android.content.ContentValues;
import android.util.Pair;

import javax.inject.Inject;


class ReplacePlaylistCommand extends Command<Pair<Urn, ApiPlaylist>, WriteResult, ReplacePlaylistCommand> {

    private final PropellerDatabase propeller;

    @Inject
    public ReplacePlaylistCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public WriteResult call() throws Exception {
        final Urn localPlaylistUrn = input.first;
        final ApiPlaylist newPlaylist = input.second;
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                // insert the new playlist and user
                step(propeller.upsert(Table.Users, StoreUsersCommand.buildUserContentValues(newPlaylist.getUser())));
                step(propeller.insert(Table.Sounds, StorePlaylistsCommand.buildPlaylistContentValues(newPlaylist)));

                // update the playlist tracks entries
                final ContentValues updatedPlaylistId = new ContentValues();
                updatedPlaylistId.put(PlaylistTracks.PLAYLIST_ID, newPlaylist.getId());
                step(propeller.update(Table.PlaylistTracks, updatedPlaylistId, new WhereBuilder()
                        .whereEq(PlaylistTracks.PLAYLIST_ID, localPlaylistUrn.getNumericId())));

                // remove the old playlist entry
                step(propeller.delete(Table.Sounds, new WhereBuilder()
                        .whereEq(Sounds._ID, localPlaylistUrn.getNumericId())
                        .whereEq(Sounds._TYPE, Sounds.TYPE_PLAYLIST)));
            }
        });
    }
}
