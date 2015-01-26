package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.playlists.PlaylistProperty;
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

    private static final Date LIKED_DATE_1 = new Date(100);
    private static final Date LIKED_DATE_2 = new Date(200);

    private LoadLikedPlaylistsCommand command;
    private PropertySet playlist1;
    private PropertySet playlist2;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedPlaylistsCommand(propeller());

        playlist1 = testFixtures().insertLikedPlaylist(LIKED_DATE_1).toPropertySet();
        playlist2 = testFixtures().insertLikedPlaylist(LIKED_DATE_2).toPropertySet();
    }

    @Test
    public void shouldLoadAllPlaylistLikes() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).toEqual(Lists.newArrayList(expectedLikedPlaylistFor(playlist2, LIKED_DATE_2), expectedLikedPlaylistFor(playlist1, LIKED_DATE_1)));
    }

    @Test
    public void shouldAdhereToLimit() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(1, Long.MAX_VALUE)).call();

        expect(result).toEqual(Lists.<PropertySet>newArrayList(expectedLikedPlaylistFor(playlist2, LIKED_DATE_2)));
    }

    @Test
    public void shouldAdhereToTimestamp() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(2, LIKED_DATE_2.getTime())).call();

        expect(result).toEqual(Lists.<PropertySet>newArrayList(expectedLikedPlaylistFor(playlist1, LIKED_DATE_1)));
    }

    private PropertySet expectedLikedPlaylistFor(PropertySet playlist, Date likedAt) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlist.get(PlaylistProperty.URN)),
                PlaylistProperty.TITLE.bind(playlist.get(PlaylistProperty.TITLE)),
                PlaylistProperty.CREATOR_NAME.bind(playlist.get(PlaylistProperty.CREATOR_NAME)),
                PlaylistProperty.TRACK_COUNT.bind(playlist.get(PlaylistProperty.TRACK_COUNT)),
                PlaylistProperty.LIKES_COUNT.bind(playlist.get(PlaylistProperty.LIKES_COUNT)),
                LikeProperty.CREATED_AT.bind((likedAt)),
                PlaylistProperty.IS_PRIVATE.bind(playlist.get(PlaylistProperty.IS_PRIVATE)));
    }
}