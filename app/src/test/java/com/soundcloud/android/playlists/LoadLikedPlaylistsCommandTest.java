package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadLikedPlaylistsCommandTest extends StorageIntegrationTest {

    private LoadLikedPlaylistsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedPlaylistsCommand(propeller());
    }

    @Test
    public void shouldLoadTrackLikesFromObservable() throws Exception {
        PropertySet playlist1 = testFixtures().insertLikedPlaylist(new Date(100)).toPropertySet();
        PropertySet playlist2 = testFixtures().insertLikedPlaylist(new Date(200)).toPropertySet();

        List<PropertySet> result = command.call();

        expect(result).toEqual(Lists.newArrayList(expectedLikedTrackFor(playlist2), expectedLikedTrackFor(playlist1)));
    }

    private PropertySet expectedLikedTrackFor(PropertySet playlist) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlist.get(PlaylistProperty.URN)),
                PlaylistProperty.TITLE.bind(playlist.get(PlaylistProperty.TITLE)),
                PlaylistProperty.CREATOR_NAME.bind(playlist.get(PlaylistProperty.CREATOR_NAME)),
                PlaylistProperty.TRACK_COUNT.bind(playlist.get(PlaylistProperty.TRACK_COUNT)),
                PlaylistProperty.LIKES_COUNT.bind(playlist.get(PlaylistProperty.LIKES_COUNT)),
                PlaylistProperty.IS_PRIVATE.bind(playlist.get(PlaylistProperty.IS_PRIVATE)));
    }

}