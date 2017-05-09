package com.soundcloud.android.collection.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;

import android.support.annotation.PluralsRes;

@AutoValue
public abstract class PlaylistCollectionHeaderItem extends PlaylistCollectionItem {
    enum Kind {
        PLAYLISTS_AND_ALBUMS(R.plurals.collections_playlists_and_albums_header_plural), PLAYLISTS(R.plurals.collections_playlists_header_plural), ALBUMS(R.plurals.collections_albums_header_plural);

        private int headerRes;

        Kind(@PluralsRes int headerRes) {
            this.headerRes = headerRes;
        }

        @PluralsRes
        public int headerResource() {
            return headerRes;
        }
    }

    public static PlaylistCollectionHeaderItem create(int playlistCount) {
        return create(Kind.PLAYLISTS_AND_ALBUMS, playlistCount);
    }

    public static PlaylistCollectionHeaderItem createForPlaylists(int playlistCount) {
        return create(Kind.PLAYLISTS, playlistCount);
    }

    public static PlaylistCollectionHeaderItem createForAlbums(int playlistCount) {
        return create(Kind.ALBUMS, playlistCount);
    }

    private static PlaylistCollectionHeaderItem create(Kind kind, int playlistCount) {
        return new AutoValue_PlaylistCollectionHeaderItem(PlaylistCollectionItem.TYPE_HEADER, kind, playlistCount);
    }

    abstract Kind kind();

    abstract int getPlaylistCount();

}
