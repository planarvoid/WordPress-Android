package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.storage.TableColumns.PlaylistTracks;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.Posts;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;
import android.util.Pair;

import javax.inject.Inject;


class ReplacePlaylistPostCommand
        extends LegacyCommand<Pair<Urn, ApiPlaylist>, WriteResult, ReplacePlaylistPostCommand> {

    private final PropellerDatabase propeller;

    @Inject
    ReplacePlaylistPostCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public WriteResult call() throws Exception {
        final Urn localPlaylistUrn = input.first;
        final ApiPlaylist newPlaylist = input.second;
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                insertNewPlaylistAndUser(propeller);
                updatePlaylistTrackEntries(propeller);
                removePlaylist(propeller);
                updatePostsTable(propeller);
                updateLikesTable(propeller);
                updateOfflinePlaylist(propeller);

            }

            private void updateOfflinePlaylist(PropellerDatabase propeller) {
                final ContentValues offlinePlaylistValues = new ContentValues();
                offlinePlaylistValues.put(OfflineContent._ID.name(), newPlaylist.getUrn().getNumericId());
                offlinePlaylistValues.put(OfflineContent._TYPE.name(), OfflineContent.TYPE_PLAYLIST);

                final ChangeResult changeResult = step(
                        propeller.delete(OfflineContent.TABLE,
                                         filter().whereEq(OfflineContent._ID, localPlaylistUrn.getNumericId())
                                                 .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST)));

                if (changeResult.getNumRowsAffected() > 0) {
                    step(propeller.insert(OfflineContent.TABLE, offlinePlaylistValues));
                }
            }

            private void insertNewPlaylistAndUser(PropellerDatabase propeller) {
                step(propeller.upsert(Tables.Users.TABLE, StoreUsersCommand.buildUserContentValues(newPlaylist.getUser())));
                step(propeller.insert(Tables.Sounds.TABLE, StorePlaylistsCommand.buildPlaylistContentValues(newPlaylist)));
            }

            private void updateLikesTable(PropellerDatabase propeller) {
                final ContentValues playlistLikesValues = new ContentValues();
                playlistLikesValues.put(Tables.Likes._ID.name(), newPlaylist.getId());
                step(propeller.update(Tables.Likes.TABLE, playlistLikesValues, filter()
                        .whereEq(Tables.Likes._ID, localPlaylistUrn.getNumericId())
                        .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_PLAYLIST)));
            }

            private void updatePostsTable(PropellerDatabase propeller) {
                final ContentValuesBuilder playlistPostValues = ContentValuesBuilder.values(2);
                playlistPostValues.put(Posts.TARGET_ID, newPlaylist.getId());
                playlistPostValues.put(Posts.CREATED_AT, newPlaylist.getCreatedAt().getTime());
                step(propeller.update(Posts.TABLE, playlistPostValues.get(), filter()
                        .whereEq(Posts.TARGET_ID, localPlaylistUrn.getNumericId())
                        .whereEq(Posts.TARGET_TYPE, Tables.Sounds.TYPE_PLAYLIST)));
            }

            private void removePlaylist(PropellerDatabase propeller) {
                step(propeller.delete(Tables.Sounds.TABLE, filter()
                        .whereEq(Tables.Sounds._ID, localPlaylistUrn.getNumericId())
                        .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_PLAYLIST)));
            }

            private void updatePlaylistTrackEntries(PropellerDatabase propeller) {
                final ContentValues playlistTracksValues = new ContentValues();
                playlistTracksValues.put(PlaylistTracks.PLAYLIST_ID, newPlaylist.getId());
                playlistTracksValues.putNull(PlaylistTracks.ADDED_AT);
                step(propeller.update(Table.PlaylistTracks, playlistTracksValues,
                                      filter().whereEq(PlaylistTracks.PLAYLIST_ID, localPlaylistUrn.getNumericId())));
            }
        });
    }
}
