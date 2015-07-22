package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

        Map<Urn, PropertySet> likedStatuses = command.call(input);

        expect(likedStatuses.size()).toEqual(2);
        expect(likedStatuses.get(apiPlaylist1.getUrn()).get(PlaylistProperty.IS_LIKED)).toBeTrue();
        expect(likedStatuses.get(apiPlaylist2.getUrn()).get(PlaylistProperty.IS_LIKED)).toBeFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForPlaylists() throws Exception {
        final ApiPlaylist likedPlaylist = testFixtures().insertLikedPlaylist(new Date());
        final ApiPlaylist unlikedPlaylist = testFixtures().insertPlaylist();
        final ApiTrack track = testFixtures().insertTrack();

        List<PropertySet> input = Arrays.asList(
                likedPlaylist.toPropertySet(), unlikedPlaylist.toPropertySet(), track.toPropertySet());


        Map<Urn, PropertySet> likedStatuses = command.call(input);

        expect(likedStatuses.get(likedPlaylist.getUrn()).get(PlaylistProperty.IS_LIKED)).toBeTrue();
        expect(likedStatuses.get(unlikedPlaylist.getUrn()).get(PlaylistProperty.IS_LIKED)).toBeFalse();
        expect(likedStatuses.containsKey(track.getUrn())).toBeFalse();
    }

}
