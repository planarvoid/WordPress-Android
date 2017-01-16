package com.soundcloud.android.playlists;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;

public class NewPlaylistMapper extends RxResultMapper<Playlist> {

    @Inject
    public NewPlaylistMapper() {
    }

    @Override
    public Playlist map(CursorReader cursorReader) {
        final Playlist.Builder builder = Playlist.builder()
                                                 .urn(Urn.forPlaylist(cursorReader.getLong(Tables.PlaylistView.ID.name())))
                                                 .title(cursorReader.getString(Tables.PlaylistView.TITLE.name()))
                                                 .duration(cursorReader.getLong(Tables.PlaylistView.DURATION.name()))
                                                 .repostCount(cursorReader.getInt(Tables.PlaylistView.REPOSTS_COUNT.name()))
                                                 .createdAt(cursorReader.getDateFromTimestamp(Tables.PlaylistView.CREATED_AT.name()))
                                                 .isAlbum(cursorReader.getBoolean(Tables.PlaylistView.IS_ALBUM.name()))
                                                 .imageUrlTemplate(Optional.fromNullable(cursorReader.getString(Tables.PlaylistView.ARTWORK_URL.name())))
                                                 .creatorUrn(Urn.forUser(cursorReader.getLong(Tables.PlaylistView.USER_ID.name())))
                                                 .creatorName(cursorReader.getString(Tables.PlaylistView.USERNAME.name()))
                                                 .trackCount(readTrackCount(cursorReader))
                                                 .likesCount(cursorReader.getInt(Tables.PlaylistView.LIKES_COUNT.name()))
                                                 .genre(cursorReader.getString(Tables.PlaylistView.GENRE.name()))
                                                 .setType(cursorReader.getString(Tables.PlaylistView.SET_TYPE.name()))
                                                 .releaseDate(cursorReader.getString(Tables.PlaylistView.RELEASE_DATE.name()))
                                                 .isPrivate(readIsPrivate(cursorReader))
                                                 .isLikedByCurrentUser(cursorReader.getBoolean(Tables.PlaylistView.IS_USER_LIKE.name()))
                                                 .isRepostedByCurrentUser(cursorReader.getBoolean(Tables.PlaylistView.IS_USER_REPOST.name()));

        builder.permalinkUrl(Optional.fromNullable(cursorReader.getString(Tables.PlaylistView.PERMALINK_URL.name())));

        final boolean isMarkedForOffline = cursorReader.getBoolean(Tables.PlaylistView.IS_MARKED_FOR_OFFLINE.name());
        builder.isMarkedForOffline(isMarkedForOffline);
        if (isMarkedForOffline) {
            builder.offlineState(OfflineState.getOfflineState(
                    cursorReader.getBoolean(Tables.PlaylistView.HAS_PENDING_DOWNLOAD_REQUEST.name()),
                    cursorReader.getBoolean(Tables.PlaylistView.HAS_DOWNLOADED_TRACKS.name()),
                    cursorReader.getBoolean(Tables.PlaylistView.HAS_UNAVAILABLE_TRACKS.name())));
        }
        return builder.build();
    }

    private boolean readIsPrivate(CursorReader cursorReader) {
        return Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(Tables.PlaylistView.SHARING.name()));
    }

    static int readTrackCount(CursorReader cursorReader) {
        return Math.max(cursorReader.getInt(Tables.PlaylistView.LOCAL_TRACK_COUNT.name()),
                        cursorReader.getInt(Tables.PlaylistView.TRACK_COUNT.name()));
    }

}
