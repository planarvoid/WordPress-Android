package com.soundcloud.android.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
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
        final List<ApiTrack> input = Arrays.asList(likedTrack, track);

        final Map<Urn, Boolean> likedStatuses = command.call(Lists.transform(input, ApiTrack::getUrn));

        assertThat(likedStatuses).hasSize(2);
        assertThat(likedStatuses.get(likedTrack.getUrn())).isTrue();
        assertThat(likedStatuses.get(track.getUrn())).isFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForTracks() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        final List<ApiSyncable> input = Arrays.asList(likedTrack, this.track, playlist);

        final Map<Urn, Boolean> likedStatuses = command.call(Lists.transform(input, ApiSyncable::getUrn));

        assertThat(likedStatuses.get(likedTrack.getUrn())).isTrue();
        assertThat(likedStatuses.get(this.track.getUrn())).isFalse();
        assertThat(likedStatuses.containsKey(playlist.getUrn())).isFalse();
    }
}
