package com.soundcloud.android.api.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiPlaylistTest {

    @Test
    public void shouldConvertToPropertySet() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);

        PropertySet propertySet = playlist.toPropertySet();

        expect(propertySet.get(PlaylistProperty.URN)).toEqual(playlist.getUrn());
        expect(propertySet.get(PlaylistProperty.TITLE)).toEqual(playlist.getTitle());
        expect(propertySet.get(PlaylistProperty.CREATED_AT)).toEqual(playlist.getCreatedAt());
        expect(propertySet.get(PlaylistProperty.DURATION)).toEqual(playlist.getDuration());
        expect(propertySet.get(PlaylistProperty.PERMALINK_URL)).toEqual(playlist.getPermalinkUrl());
        expect(propertySet.get(PlaylistProperty.IS_PRIVATE)).toEqual(!playlist.isPublic());
        expect(propertySet.get(PlaylistProperty.TRACK_COUNT)).toEqual(playlist.getTrackCount());
        expect(propertySet.get(PlaylistProperty.LIKES_COUNT)).toEqual(playlist.getStats().getLikesCount());
        expect(propertySet.get(PlaylistProperty.REPOSTS_COUNT)).toEqual(playlist.getStats().getRepostsCount());
        expect(propertySet.get(PlaylistProperty.CREATOR_NAME)).toEqual(playlist.getUsername());
        expect(propertySet.get(PlaylistProperty.CREATOR_URN)).toEqual(playlist.getUser().getUrn());
    }
}