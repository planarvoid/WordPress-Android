package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.model.Urn;
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
public class LoadPostedPlaylistsCommandTest extends StorageIntegrationTest {

    private static final Date POSTED_DATE_1 = new Date(100000);
    private static final Date POSTED_DATE_2 = new Date(200000);
    private static final Date POSTED_DATE_3 = new Date(300000);

    private LoadPostedPlaylistsCommand command;
    private ApiUser user;
    private PropertySet playlist1;
    private PropertySet playlist2;

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        user = testFixtures().insertUser();

        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);

        command = new LoadPostedPlaylistsCommand(propeller());

        when(accountOperations.getLoggedInUserUrn()).thenReturn(user.getUrn());
    }

    @Test
    public void shouldLoadAllPlaylistPosts() throws Exception {
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).toEqual(Arrays.asList(playlist2, playlist1));
    }

    @Test
    public void shouldReturnTrackCountAsMaximumOfRemoteAndLocalCounts() throws Exception {
        expect(playlist1.get(PlaylistProperty.TRACK_COUNT)).toEqual(2);

        final Urn playlistUrn = playlist1.get(PlaylistProperty.URN);
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result.get(1).get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(result.get(1).get(PlaylistProperty.TRACK_COUNT)).toEqual(3);
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
        ApiPlaylist deletedPlaylist = createPlaylistAt(POSTED_DATE_3);
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).not.toContain(createPostPropertySet(deletedPlaylist));
        expect(result).toContain(playlist2, playlist1);
    }

    @Test
    public void shouldNotConfuseTracksForPlaylists() throws Exception {
        createTrackWithId(9999);
        createPlaylistCollectionWithId(9999, new Date());
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).toContainExactly(playlist2, playlist1);
    }

    private PropertySet createPlaylistPostAt(Date postedAt) {
        ApiPlaylist playlist = createPlaylistAt(postedAt);
        createPlaylistCollectionWithId(playlist.getUrn().getNumericId(), postedAt);
        return createPostPropertySet(playlist);
    }

    private PropertySet createPostPropertySet(ApiPlaylist playlist) {
        return playlist.toPropertySet().slice(
                PlaylistProperty.URN,
                PlaylistProperty.TITLE,
                PlaylistProperty.CREATOR_NAME,
                PlaylistProperty.TRACK_COUNT,
                PlaylistProperty.LIKES_COUNT,
                PlaylistProperty.CREATED_AT,
                PlaylistProperty.IS_PRIVATE
        );
    }

    private ApiPlaylist createPlaylistAt(Date creationDate) {
        return testFixtures().insertPlaylistWithCreationDate(user, creationDate);
    }

    private void createPlaylistCollectionWithId(long playlistId, Date postedAt) {
        testFixtures().insertPlaylistPost(playlistId, postedAt.getTime(), false);
    }

    private void createTrackWithId(long trackId) {
        ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setUser(user);
        apiTrack.setId(trackId);
        testFixtures().insertTrack(apiTrack);
        testFixtures().insertTrackPost(apiTrack.getId(), apiTrack.getCreatedAt().getTime(), false);
    }
}
