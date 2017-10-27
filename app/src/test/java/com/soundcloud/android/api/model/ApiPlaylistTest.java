package com.soundcloud.android.api.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

public class ApiPlaylistTest {

    @Test
    public void shouldConvertToPlaylistItem() {
        ApiPlaylist apiPlaylist = PlaylistFixtures.apiPlaylist();

        PlaylistItem playlistItem = PlaylistFixtures.playlistItem(apiPlaylist);

        assertThat(playlistItem.getUrn()).isEqualTo(apiPlaylist.getUrn());
        assertThat(playlistItem.title()).isEqualTo(apiPlaylist.getTitle());
        assertThat(playlistItem.getCreatedAt()).isEqualTo(apiPlaylist.getCreatedAt());
        assertThat(playlistItem.getDuration()).isEqualTo(apiPlaylist.getDuration());
        assertThat(playlistItem.permalinkUrl()).isEqualTo(apiPlaylist.getPermalinkUrl());
        assertThat(playlistItem.isPrivate()).isEqualTo(!apiPlaylist.isPublic());
        assertThat(playlistItem.trackCount()).isEqualTo(apiPlaylist.getTrackCount());
        assertThat(playlistItem.likesCount()).isEqualTo(apiPlaylist.getStats().getLikesCount());
        assertThat(playlistItem.repostsCount()).isEqualTo(apiPlaylist.getStats().getRepostsCount());
        assertThat(playlistItem.creatorName()).isEqualTo(apiPlaylist.getUsername());
        assertThat(playlistItem.creatorUrn()).isEqualTo(apiPlaylist.getUser().getUrn());
        assertThat(playlistItem.isAlbum()).isEqualTo(apiPlaylist.isAlbum());
        assertThat(playlistItem.setType()).isEqualTo(Optional.of(apiPlaylist.getSetType()));
        assertThat(playlistItem.getReleaseDate()).isEqualTo(apiPlaylist.getReleaseDate());
        assertThat(playlistItem.getTags()).isEqualTo(apiPlaylist.getTags());
        assertThat(playlistItem.genre().get()).isEqualTo(apiPlaylist.getGenre());
    }
}
