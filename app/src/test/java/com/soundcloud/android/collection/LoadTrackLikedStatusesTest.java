package com.soundcloud.android.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.collection.LoadTrackLikedStatuses;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LoadTrackLikedStatusesTest extends StorageIntegrationTest {
    private LoadTrackLikedStatuses command;
    private ApiTrack likedTrack;
    private ApiTrack track;

    @Before
    public void setUp() throws Exception {
        likedTrack = testFixtures().insertLikedTrack(new Date());
        track = testFixtures().insertTrack();

        command = new LoadTrackLikedStatuses(propeller());
    }

    @Test
    public void shouldReturnTrackLikeStatuses() {
        final List<PropertySet> input = Arrays.asList(likedTrack.toPropertySet(), track.toPropertySet());

        final Map<Urn, PropertySet> likedStatuses = command.call(input);

        assertThat(likedStatuses).hasSize(2);
        assertThat(likedStatuses.get(likedTrack.getUrn()).get(PlayableProperty.IS_USER_LIKE)).isTrue();
        assertThat(likedStatuses.get(track.getUrn()).get(PlayableProperty.IS_USER_LIKE)).isFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForTracks() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        final List<PropertySet> input = Arrays.asList(likedTrack.toPropertySet(),
                                                      this.track.toPropertySet(), playlist.toPropertySet());

        final Map<Urn, PropertySet> likedStatuses = command.call(input);

        assertThat(likedStatuses.get(likedTrack.getUrn()).get(PlaylistProperty.IS_USER_LIKE)).isTrue();
        assertThat(likedStatuses.get(this.track.getUrn()).get(PlaylistProperty.IS_USER_LIKE)).isFalse();
        assertThat(likedStatuses.containsKey(playlist.getUrn())).isFalse();
    }
}
