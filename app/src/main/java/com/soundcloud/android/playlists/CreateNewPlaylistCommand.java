package com.soundcloud.android.playlists;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;

class CreateNewPlaylistCommand extends Command<CreateNewPlaylistCommand.Params, TxnResult, CreateNewPlaylistCommand> {

    private final PropellerDatabase propeller;
    private final AccountOperations accountOperations;

    public CreateNewPlaylistCommand(PropellerDatabase propeller, AccountOperations accountOperations) {
        this.propeller = propeller;
        this.accountOperations = accountOperations;
    }

    @Override
    public TxnResult call() throws Exception {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                final long createdAt = System.currentTimeMillis();
                final long localId = -createdAt;
                step(propeller.insert(Table.Sounds, getContentValuesForPlaylistsTable(localId, createdAt)));
                step(propeller.insert(Table.Posts, getContentValuesForPostsTable(localId, createdAt)));
                step(propeller.insert(Table.PlaylistTracks, getContentValuesForPlaylistTrack(localId, input.firstTrackUrn)));
            }
        });
    }

    private ContentValues getContentValuesForPlaylistTrack(long playlistId, Urn firstTrackUrn) {
        return ContentValuesBuilder.values()
                .put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistId)
                .put(TableColumns.PlaylistTracks.TRACK_ID, firstTrackUrn.getNumericId())
                .put(TableColumns.PlaylistTracks.POSITION, 0)
                .get();
    }

    private ContentValues getContentValuesForPlaylistsTable(long localId, long createdAt) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Sounds._ID, localId)
                .put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .put(TableColumns.Sounds.TITLE, input.title)
                .put(TableColumns.Sounds.SHARING, input.isPrivate ? Sharing.PRIVATE.value() : Sharing.PUBLIC.value())
                .put(TableColumns.Sounds.CREATED_AT, createdAt)
                .put(TableColumns.Sounds.USER_ID, accountOperations.getLoggedInUserUrn().getNumericId())
                .get();
    }

    private ContentValues getContentValuesForPostsTable(long localId, long createdAt) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Posts.TARGET_ID, localId)
                .put(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .put(TableColumns.Posts.CREATED_AT, createdAt)
                .put(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_POST)
                .get();
    }

    static final class Params {
        private final String title;
        private final boolean isPrivate;
        private final Urn firstTrackUrn;

        Params(String title, boolean isPrivate, Urn firstTrackUrn) {
            this.title = title;
            this.isPrivate = isPrivate;
            this.firstTrackUrn = firstTrackUrn;
        }
    }
}
