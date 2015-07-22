package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
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
    public void shouldConvertToPropertySet() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);

        PropertySet propertySet = playlist.toPropertySet();

        assertThat(propertySet.get(PlaylistProperty.URN)).isEqualTo(playlist.getUrn());
        assertThat(propertySet.get(PlaylistProperty.TITLE)).isEqualTo(playlist.getTitle());
        assertThat(propertySet.get(PlaylistProperty.CREATED_AT)).isEqualTo(playlist.getCreatedAt());
        assertThat(propertySet.get(PlaylistProperty.DURATION)).isEqualTo(playlist.getDuration());
        assertThat(propertySet.get(PlaylistProperty.PERMALINK_URL)).isEqualTo(playlist.getPermalinkUrl());
        assertThat(propertySet.get(PlaylistProperty.IS_PRIVATE)).isEqualTo(!playlist.isPublic());
        assertThat(propertySet.get(PlaylistProperty.TRACK_COUNT)).isEqualTo(playlist.getTrackCount());
        assertThat(propertySet.get(PlaylistProperty.LIKES_COUNT)).isEqualTo(playlist.getStats().getLikesCount());
        assertThat(propertySet.get(PlaylistProperty.REPOSTS_COUNT)).isEqualTo(playlist.getStats().getRepostsCount());
        assertThat(propertySet.get(PlaylistProperty.CREATOR_NAME)).isEqualTo(playlist.getUsername());
        assertThat(propertySet.get(PlaylistProperty.CREATOR_URN)).isEqualTo(playlist.getUser().getUrn());
    }
}