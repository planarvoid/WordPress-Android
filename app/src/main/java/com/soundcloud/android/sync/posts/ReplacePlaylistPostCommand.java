package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.TableColumns.PlaylistTracks;
import static com.soundcloud.android.storage.TableColumns.Posts;
import static com.soundcloud.android.storage.TableColumns.Sounds;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;
import android.util.Pair;

import javax.inject.Inject;


class ReplacePlaylistPostCommand extends LegacyCommand<Pair<Urn, ApiPlaylist>, WriteResult, ReplacePlaylistPostCommand> {

    private final PropellerDatabase propeller;

    @Inject
    public ReplacePlaylistPostCommand(PropellerDatabase propeller) {
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
                final ContentValues playlistTracksValues = new ContentValues();
                playlistTracksValues.put(PlaylistTracks.PLAYLIST_ID, newPlaylist.getId());
                step(propeller.update(Table.PlaylistTracks, playlistTracksValues,
                        filter().whereEq(PlaylistTracks.PLAYLIST_ID, localPlaylistUrn.getNumericId())));

                // remove the old playlist entry
                step(propeller.delete(Table.Sounds, filter()
                        .whereEq(Sounds._ID, localPlaylistUrn.getNumericId())
                        .whereEq(Sounds._TYPE, Sounds.TYPE_PLAYLIST)));

                // make sure the posted playlist appears under the new ID in the Posts table
                final ContentValues playlistPostValues = new ContentValues();
                playlistPostValues.put(Posts.TARGET_ID, newPlaylist.getId());
                step(propeller.update(Table.Posts, playlistPostValues, filter()
                        .whereEq(Posts.TARGET_ID, localPlaylistUrn.getNumericId())
                        .whereEq(Posts.TARGET_TYPE, Sounds.TYPE_PLAYLIST)));


                // update offline playlist if exists
                final ContentValues offlinePlaylistValues = new ContentValues();
                offlinePlaylistValues.put(TableColumns.OfflineContent._ID, newPlaylist.getUrn().getNumericId());
                offlinePlaylistValues.put(TableColumns.OfflineContent._TYPE, TableColumns.OfflineContent.TYPE_PLAYLIST);

                final ChangeResult changeResult = step(propeller.delete(Table.OfflineContent,
                        filter().whereEq(TableColumns.OfflineContent._ID, localPlaylistUrn.getNumericId())
                                .whereEq(TableColumns.OfflineContent._TYPE, TableColumns.OfflineContent.TYPE_PLAYLIST)));

                if (changeResult.getNumRowsAffected() > 0) {
                    step(propeller.insert(Table.OfflineContent, offlinePlaylistValues));
                }
            }
        });
    }
}
