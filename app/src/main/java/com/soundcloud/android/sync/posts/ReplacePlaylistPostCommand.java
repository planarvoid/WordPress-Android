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
import com.soundcloud.android.storage.TableColumns.Likes;
import com.soundcloud.android.storage.Tables.OfflineContent;
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
                step(propeller.upsert(Table.Users, StoreUsersCommand.buildUserContentValues(newPlaylist.getUser())));
                step(propeller.insert(Table.Sounds, StorePlaylistsCommand.buildPlaylistContentValues(newPlaylist)));
            }

            private void updateLikesTable(PropellerDatabase propeller) {
                final ContentValues playlistLikesValues = new ContentValues();
                playlistLikesValues.put(Likes._ID, newPlaylist.getId());
                step(propeller.update(Table.Likes, playlistLikesValues, filter()
                        .whereEq(Likes._ID, localPlaylistUrn.getNumericId())
                        .whereEq(Likes._TYPE, Sounds.TYPE_PLAYLIST)));
            }

            private void updatePostsTable(PropellerDatabase propeller) {
                final ContentValues playlistPostValues = new ContentValues();
                playlistPostValues.put(Posts.TARGET_ID, newPlaylist.getId());
                step(propeller.update(Table.Posts, playlistPostValues, filter()
                        .whereEq(Posts.TARGET_ID, localPlaylistUrn.getNumericId())
                        .whereEq(Posts.TARGET_TYPE, Sounds.TYPE_PLAYLIST)));
            }

            private void removePlaylist(PropellerDatabase propeller) {
                step(propeller.delete(Table.Sounds, filter()
                        .whereEq(Sounds._ID, localPlaylistUrn.getNumericId())
                        .whereEq(Sounds._TYPE, Sounds.TYPE_PLAYLIST)));
            }

            private void updatePlaylistTrackEntries(PropellerDatabase propeller) {
                final ContentValues playlistTracksValues = new ContentValues();
                playlistTracksValues.put(PlaylistTracks.PLAYLIST_ID, newPlaylist.getId());
                step(propeller.update(Table.PlaylistTracks, playlistTracksValues,
                        filter().whereEq(PlaylistTracks.PLAYLIST_ID, localPlaylistUrn.getNumericId())));
            }
        });
    }
}
