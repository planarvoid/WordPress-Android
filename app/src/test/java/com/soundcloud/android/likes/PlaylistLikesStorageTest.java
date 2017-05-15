package com.soundcloud.android.likes;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.NewPlaylistMapper;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistAssociation;
import com.soundcloud.android.playlists.PlaylistAssociationMapperFactory;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import javax.inject.Provider;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PlaylistLikesStorageTest extends StorageIntegrationTest {

    private static final Date LIKED_DATE_1 = new Date(100);
    private static final Date LIKED_DATE_2 = new Date(200);

    private PlaylistLikesStorage playlistLikesStorage;

    private ApiPlaylist playlist1;
    private ApiPlaylist playlist2;

    private TestSubscriber<List<PlaylistAssociation>> testListSubscriber;

    @Before
    public void setUp() throws Exception {

        testListSubscriber = new TestSubscriber<>();
        final Provider<NewPlaylistMapper> mapperProvider = providerOf(new NewPlaylistMapper());
        playlistLikesStorage = new PlaylistLikesStorage(propellerRx(), new PlaylistAssociationMapperFactory(mapperProvider));

        playlist1 = testFixtures().insertLikedPlaylist(LIKED_DATE_1);
        playlist2 = testFixtures().insertLikedPlaylist(LIKED_DATE_2);
    }

    @Test
    public void loadAllLikedPlaylists() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE).subscribe(testListSubscriber);

        final List<PlaylistAssociation> playlistAssociations = newArrayList(
                expectedLikedPlaylistFor(playlist2, LIKED_DATE_2),
                expectedLikedPlaylistFor(playlist1, LIKED_DATE_1));

        assertThat(testListSubscriber.getOnNextEvents()).containsExactly(playlistAssociations);
    }

    @Test
    public void loadLikedPlaylistsAdhereToLimit() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(1, Long.MAX_VALUE).subscribe(testListSubscriber);

        final List<PlaylistAssociation> playlistAssociations = Collections.singletonList(expectedLikedPlaylistFor(playlist2, LIKED_DATE_2));

        testListSubscriber.assertValue(playlistAssociations);
    }

    @Test
    public void loadLikedPlaylistsAdhereToTimestamp() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(1, LIKED_DATE_2.getTime()).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = Collections.singletonList(expectedLikedPlaylistFor(playlist1, LIKED_DATE_1));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void loadsRequestedDownloadStateForPlaylistMarkedForOffline() {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1, 0);
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist1);
        testFixtures().insertTrackPendingDownload(apiTrack.getUrn(), LIKED_DATE_1.getTime());

        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = newArrayList(
                expectedLikedPlaylistFor(playlist2, LIKED_DATE_2),
                expectedLikedPlaylistWithOfflineState(playlist1, LIKED_DATE_1, OfflineState.REQUESTED));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void loadsDownloadedStateForPlaylistMarkedForOffline() {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1, 0);
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist1);
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), LIKED_DATE_1.getTime(), LIKED_DATE_1.getTime());

        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = newArrayList(
                expectedLikedPlaylistFor(playlist2, LIKED_DATE_2),
                expectedLikedPlaylistWithOfflineState(playlist1, LIKED_DATE_1, OfflineState.DOWNLOADED));

        testListSubscriber.assertValue(propertySets);
    }

    private static PlaylistAssociation expectedLikedPlaylistFor(ApiPlaylist apiPlaylist, Date likedAt) {
        final Playlist playlist = ModelFixtures.playlistBuilder(apiPlaylist)
                                               .isLikedByCurrentUser(true)
                                               .isRepostedByCurrentUser(false)
                                               .isMarkedForOffline(false)
                                               .build();
        return PlaylistAssociation.create(playlist, likedAt);
    }

    private PlaylistAssociation expectedLikedPlaylistWithOfflineState(ApiPlaylist apiPlaylist, Date likedAt, OfflineState state) {
        final Playlist playlist = ModelFixtures.playlistBuilder(apiPlaylist)
                                               .isLikedByCurrentUser(true)
                                               .isRepostedByCurrentUser(false)
                                               .isMarkedForOffline(true)
                                               .offlineState(state)
                                               .build();
        return PlaylistAssociation.create(playlist, likedAt);
    }
}
