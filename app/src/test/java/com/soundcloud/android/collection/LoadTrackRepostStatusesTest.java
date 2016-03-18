package com.soundcloud.android.collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.collection.LoadTrackRepostStatuses;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LoadTrackRepostStatusesTest extends StorageIntegrationTest {

    private LoadTrackRepostStatuses command;

    @Before
    public void setUp() throws Exception {
        command = new LoadTrackRepostStatuses(propeller());
    }

    @Test
    public void shouldReturnTrackRepostStatuses() {
        final ApiTrack repostedTrack = insertRepostedTrack();
        final ApiTrack track = testFixtures().insertTrack();

        List<PropertySet> input = Arrays.asList(repostedTrack.toPropertySet(), track.toPropertySet());

        Map<Urn, PropertySet> repostStatuses = command.call(input);

        assertThat(repostStatuses).hasSize(2);
        assertThat(repostStatuses.get(repostedTrack.getUrn()).get(PlayableProperty.IS_USER_REPOST)).isTrue();
        assertThat(repostStatuses.get(track.getUrn()).get(PlayableProperty.IS_USER_REPOST)).isFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForTracks() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        final List<PropertySet> input = singletonList(playlist.toPropertySet());

        Map<Urn, PropertySet> repostStatuses = command.call(input);

        assertThat(repostStatuses.containsKey(playlist.getUrn())).isFalse();
    }

    private ApiTrack insertRepostedTrack() {
        final ApiTrack repostedTrack = testFixtures().insertTrack();
        testFixtures().insertTrackRepost(repostedTrack.getId(), new Date().getTime());
        return repostedTrack;
    }

}
