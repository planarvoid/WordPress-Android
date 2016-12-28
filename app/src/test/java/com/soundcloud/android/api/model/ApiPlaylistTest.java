package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

public class ApiPlaylistTest extends AndroidUnitTest {

    @Test
    public void shouldDefineEqualityBasedOnUrn() {
        ApiPlaylist playlist1 = ModelFixtures.create(ApiPlaylist.class);
        ApiPlaylist playlist2 = ModelFixtures.create(ApiPlaylist.class);
        playlist2.setUrn(playlist1.getUrn());

        assertThat(playlist1).isEqualTo(playlist2);
    }

    @Test
    public void shouldDefineHashCodeBasedOnUrn() {
        ApiPlaylist playlist1 = ModelFixtures.create(ApiPlaylist.class);
        ApiPlaylist playlist2 = ModelFixtures.create(ApiPlaylist.class);
        playlist2.setUrn(playlist1.getUrn());

        assertThat(playlist1.hashCode()).isEqualTo(playlist2.hashCode());
    }

    @Test
    public void shouldConvertToPlaylistItem() {
        ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

        PlaylistItem playlistItem = PlaylistItem.from(apiPlaylist);

        assertThat(playlistItem.getUrn()).isEqualTo(apiPlaylist.getUrn());
        assertThat(playlistItem.getTitle()).isEqualTo(apiPlaylist.getTitle());
        assertThat(playlistItem.getCreatedAt()).isEqualTo(apiPlaylist.getCreatedAt());
        assertThat(playlistItem.getDuration()).isEqualTo(apiPlaylist.getDuration());
        assertThat(playlistItem.getPermalinkUrl()).isEqualTo(apiPlaylist.getPermalinkUrl());
        assertThat(playlistItem.isPrivate()).isEqualTo(!apiPlaylist.isPublic());
        assertThat(playlistItem.getTrackCount()).isEqualTo(apiPlaylist.getTrackCount());
        assertThat(playlistItem.getLikesCount()).isEqualTo(apiPlaylist.getStats().getLikesCount());
        assertThat(playlistItem.getRepostCount()).isEqualTo(apiPlaylist.getStats().getRepostsCount());
        assertThat(playlistItem.getCreatorName()).isEqualTo(apiPlaylist.getUsername());
        assertThat(playlistItem.getCreatorUrn()).isEqualTo(apiPlaylist.getUser().getUrn());
        assertThat(playlistItem.isAlbum()).isEqualTo(apiPlaylist.isAlbum());
        assertThat(playlistItem.getSetType()).isEqualTo(apiPlaylist.getSetType());
        assertThat(playlistItem.getReleaseDate()).isEqualTo(apiPlaylist.getReleaseDate());
        assertThat(playlistItem.getTags()).isEqualTo(apiPlaylist.getTags());
        assertThat(playlistItem.getGenre()).isEqualTo(apiPlaylist.getGenre());
    }
}
