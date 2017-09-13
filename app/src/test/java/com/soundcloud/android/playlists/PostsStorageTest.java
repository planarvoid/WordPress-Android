package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.Activities.SOUND_ID;
import static com.soundcloud.android.storage.TableColumns.Activities.SOUND_TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.sync.activities.ApiPlaylistRepostActivity;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.test.assertions.QueryAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class PostsStorageTest extends StorageIntegrationTest {

    private static final Date POSTED_DATE_1 = new Date(100000);
    private static final Date POSTED_DATE_2 = new Date(200000);
    private static final Date POSTED_DATE_3 = new Date(300000);

    private PostsStorage storage;

    @Mock private RemovePlaylistCommand removePlaylistCommand;

    @Before
    public void setUp() throws Exception {
        storage = new PostsStorage(propellerRxV2(),
                                   new TestDateProvider(),
                                   removePlaylistCommand);
    }

    @Test
    public void shouldLoadAllPlaylistPosts() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).test()
               .assertValue(Arrays.asList(playlist2.getAssociation(), playlist1.getAssociation()));
    }

    @Test
    public void shouldAdhereToLimit() throws Exception {
        createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);

        storage.loadPostedPlaylists(1, Long.MAX_VALUE).test()
               .assertValue(Collections.singletonList(playlist2.getAssociation()));
    }

    @Test
    public void shouldAdhereToPostedTime() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistAssociation(POSTED_DATE_1, POSTED_DATE_1);
        createPlaylistAssociation(POSTED_DATE_3, POSTED_DATE_1);

        storage.loadPostedPlaylists(2, POSTED_DATE_2.getTime()).test()
               .assertValue(Collections.singletonList(playlist1.getAssociation()));
    }

    @Test
    public void shouldNotIncludePlaylistsNotPresentInTheCollectionItemsTable() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        createPlaylistAt(POSTED_DATE_3); // deleted

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).test()
               .assertValue(Arrays.asList(playlist2.getAssociation(), playlist1.getAssociation()));
    }

    @Test
    public void shouldNotConfuseTracksForPlaylists() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);

        createTrackPostWithId(playlist2.getPlaylist().urn().getNumericId());

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).test()
               .assertValue(Arrays.asList(playlist2.getAssociation(), playlist1.getAssociation()));
    }

    @Test
    public void removeAssociatedActivitiesWhenMarkingPlaylistPendingRemovals() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        final ApiPlaylistRepostActivity apiActivityItem = ModelFixtures.apiPlaylistRepostActivity(playlist);
        testFixtures().insertPlaylistRepostActivity(apiActivityItem);

        storage.markPendingRemoval(playlist.getUrn()).subscribe();

        final Query query = from(Table.Activities)
                .whereEq(SOUND_ID, playlist.getId())
                .whereEq(SOUND_TYPE, TYPE_PLAYLIST);

        QueryAssertions.assertThat(select(query)).isEmpty();
    }

    @Test
    public void removedSoundStreamEntryAssociatedWithRemovedPlaylist() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        testFixtures().insertStreamPlaylistPost(playlist.getId(), 123L);

        storage.markPendingRemoval(playlist.getUrn()).subscribe();

        final Query query = from(Table.SoundStream)
                .whereEq(TableColumns.SoundStream.SOUND_ID, playlist.getId())
                .whereEq(TableColumns.SoundStream.SOUND_TYPE, TYPE_PLAYLIST);

        QueryAssertions.assertThat(select(query)).isEmpty();
    }

    @Test
    public void removedPostFromRemovedPlaylist() {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);


        storage.markPendingRemoval(playlist1.getTargetUrn()).subscribe();

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).test()
               .assertValue(Arrays.asList(playlist2.getAssociation()));
    }

    private PlaylistAssociation createPlaylistPostAt(Date playlistPostedAt) {
        return createPlaylistAssociation(playlistPostedAt, playlistPostedAt);
    }

    private PlaylistAssociation createPlaylistAssociation(Date postedAt, Date playlistCreatedAt) {
        final ApiPlaylist apiPlaylist = createPlaylistAt(playlistCreatedAt);
        final Playlist.Builder playlist = ModelFixtures.playlistBuilder(apiPlaylist)
                                                       .isLikedByCurrentUser(false)
                                                       .isRepostedByCurrentUser(false)
                                                       .isMarkedForOffline(false);

        insertPlaylistPost(apiPlaylist.getUrn().getNumericId(), postedAt);
        return createPlaylistAssociation(playlist, postedAt);
    }

    private PlaylistAssociation createPlaylistAssociation(Playlist.Builder builder, Date createdAt) {
        final Playlist playlist = builder.isLikedByCurrentUser(false).build();
        return PlaylistAssociation.create(playlist, new Association(playlist.urn(), createdAt));
    }

    private ApiPlaylist createPlaylistAt(Date creationDate) {
        return testFixtures().insertPlaylistWithCreatedAt(creationDate);
    }

    private void insertPlaylistPost(long playlistId, Date postedAt) {
        testFixtures().insertPlaylistPost(playlistId, postedAt.getTime(), false);
    }

    private void createTrackPostWithId(long trackId) {
        ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setUrn(Urn.forTrack(trackId));
        testFixtures().insertTrack(apiTrack);
        testFixtures().insertTrackPost(apiTrack.getId(), apiTrack.getCreatedAt().getTime(), false);
    }
}
