package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LegacyLoadPostedPlaylistsCommandTest extends StorageIntegrationTest {

    private static final Date POSTED_DATE_1 = new Date(100000);
    private static final Date POSTED_DATE_2 = new Date(200000);
    private static final Date POSTED_DATE_3 = new Date(300000);

    private LegacyLoadPostedPlaylistsCommand command;
    private ApiUser user;
    private PropertySet playlist1;
    private PropertySet playlist2;

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        user = testFixtures().insertUser();
        playlist1 = createPlaylistWithCollectionAt(POSTED_DATE_1);
        playlist2 = createPlaylistWithCollectionAt(POSTED_DATE_2);

        command = new LegacyLoadPostedPlaylistsCommand(propeller(), accountOperations);

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

    @Test
    public void shouldNotIncludePlaylistsNotPresentInTheCollectionItemsTable() throws Exception {
        PropertySet deletedPlaylist = createPlaylistAt(POSTED_DATE_3);
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).not.toContain(deletedPlaylist);
        expect(result).toContain(playlist2, playlist1);
    }

    @Test
    public void shouldNotConfuseTracksForPlaylists() throws Exception {
        createTrackWithId(9999);
        createPlaylistCollectionWithId(9999);
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).toContainExactly(playlist2, playlist1);
    }

    private PropertySet createPlaylistAt(Date postedAt) {
        PropertySet playlist = testFixtures().insertPlaylistWithCreationDate(user, postedAt).toPropertySet();

        return PropertySet.from(
                PlaylistProperty.URN.bind(playlist.get(PlaylistProperty.URN)),
                PlaylistProperty.TITLE.bind(playlist.get(PlaylistProperty.TITLE)),
                PlaylistProperty.CREATOR_NAME.bind(playlist.get(PlaylistProperty.CREATOR_NAME)),
                PlaylistProperty.TRACK_COUNT.bind(playlist.get(PlaylistProperty.TRACK_COUNT)),
                PlaylistProperty.LIKES_COUNT.bind(playlist.get(PlaylistProperty.LIKES_COUNT)),
                PlaylistProperty.CREATED_AT.bind(playlist.get(PlaylistProperty.CREATED_AT)),
                PlaylistProperty.IS_PRIVATE.bind(playlist.get(PlaylistProperty.IS_PRIVATE)));
    }

    private PropertySet createPlaylistWithCollectionAt(Date postedAt) {
        PropertySet playlist = createPlaylistAt(postedAt);
        createPlaylistCollectionWithId(playlist.get(PlaylistProperty.URN).getNumericId());
        return playlist;
    }

    private void createPlaylistCollectionWithId(long playlistId) {
        testFixtures().insertPlaylistCollection(playlistId, user.getId());
    }

    private void createTrackWithId(long trackId) {
        ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setUser(user);
        apiTrack.setId(trackId);
        testFixtures().insertTrack(apiTrack);
    }
}
