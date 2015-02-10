package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadPostedPlaylistsCommandTest extends StorageIntegrationTest {

    private static final Date POSTED_DATE_1 = new Date(100000);
    private static final Date POSTED_DATE_2 = new Date(200000);

    private LoadPostedPlaylistsCommand command;
    private ApiUser user;
    private PropertySet playlist1;
    private PropertySet playlist2;

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        user = testFixtures().insertUser();
        playlist1 = createPostedPlaylistAt(POSTED_DATE_1);
        playlist2 = createPostedPlaylistAt(POSTED_DATE_2);

        command = new LoadPostedPlaylistsCommand(propeller(), accountOperations);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(user.getUrn());
    }

    @Test
    public void shouldLoadAllPlaylistPosts() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).toEqual(Arrays.asList(playlist2, playlist1));
    }

    @Test
    public void shouldAdhereToLimit() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(1, Long.MAX_VALUE)).call();

        expect(result).toEqual(Arrays.asList(playlist2));
    }

    @Test
    public void shouldAdhereToTimestamp() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(2, POSTED_DATE_2.getTime())).call();

        expect(result).toEqual(Arrays.asList(playlist1));
    }

    private PropertySet createPostedPlaylistAt(Date postedAt) {
        PropertySet playlist = testFixtures().insertPostedPlaylist(user, postedAt).toPropertySet();

        return PropertySet.from(
                PlaylistProperty.URN.bind(playlist.get(PlaylistProperty.URN)),
                PlaylistProperty.TITLE.bind(playlist.get(PlaylistProperty.TITLE)),
                PlaylistProperty.CREATOR_NAME.bind(playlist.get(PlaylistProperty.CREATOR_NAME)),
                PlaylistProperty.TRACK_COUNT.bind(playlist.get(PlaylistProperty.TRACK_COUNT)),
                PlaylistProperty.LIKES_COUNT.bind(playlist.get(PlaylistProperty.LIKES_COUNT)),
                PlaylistProperty.CREATED_AT.bind(playlist.get(PlaylistProperty.CREATED_AT)),
                PlaylistProperty.IS_PRIVATE.bind(playlist.get(PlaylistProperty.IS_PRIVATE)));
    }
}
