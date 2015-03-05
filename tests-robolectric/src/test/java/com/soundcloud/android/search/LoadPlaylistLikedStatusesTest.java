package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadPlaylistLikedStatusesTest extends StorageIntegrationTest {

    private LoadPlaylistLikedStatuses command;

    @Before
    public void setUp() throws Exception {
        command = new LoadPlaylistLikedStatuses(propeller());
    }

    @Test
    public void shouldReturnPlaylistLikeStatuses() throws Exception {
        ApiPlaylist apiPlaylist1 = testFixtures().insertLikedPlaylist(new Date());
        ApiPlaylist apiPlaylist2 = testFixtures().insertPlaylist();
        List<PropertySet> input = Arrays.asList(apiPlaylist1.toPropertySet(), apiPlaylist2.toPropertySet());

        final List<PropertySet> likedStatuses = command.with(input).call();

        expect(likedStatuses).toNumber(2);
        expect(likedStatuses.get(0).get(PlayableProperty.URN)).toEqual(apiPlaylist1.getUrn());
        expect(likedStatuses.get(0).get(PlayableProperty.IS_LIKED)).toEqual(true);
        expect(likedStatuses.get(1).get(PlayableProperty.URN)).toEqual(apiPlaylist2.getUrn());
        expect(likedStatuses.get(1).get(PlayableProperty.IS_LIKED)).toEqual(false);
    }

    @Test
    public void shouldOnlyReturnLikedStatusForPlaylists() throws Exception {
        final ApiPlaylist likedPlaylist = testFixtures().insertLikedPlaylist(new Date());
        final ApiPlaylist unlikedPlaylist = testFixtures().insertPlaylist();
        final ApiTrack track = testFixtures().insertTrack();

        List<PropertySet> input = Arrays.asList(
                likedPlaylist.toPropertySet(), unlikedPlaylist.toPropertySet(), track.toPropertySet());
        List<PropertySet> likedStatuses = command.with(input).call();

        expect(likedStatuses).toContainExactlyInAnyOrder(
                PropertySet.from(PlaylistProperty.URN.bind(likedPlaylist.getUrn()), PlaylistProperty.IS_LIKED.bind(true)),
                PropertySet.from(PlaylistProperty.URN.bind(unlikedPlaylist.getUrn()), PlaylistProperty.IS_LIKED.bind(false))
        );
    }

}
