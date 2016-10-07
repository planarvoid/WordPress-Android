package com.soundcloud.android.likes;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.NewPlaylistMapper;
import com.soundcloud.android.playlists.PlaylistAssociation;
import com.soundcloud.android.playlists.PlaylistAssociationMapperFactory;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import javax.inject.Provider;
import java.util.Arrays;
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
        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE, Strings.EMPTY).subscribe(testListSubscriber);

        final List<PlaylistAssociation> playlistAssociations = newArrayList(
                expectedLikedPlaylistFor(playlist2.toPropertySet(), LIKED_DATE_2),
                expectedLikedPlaylistFor(playlist1.toPropertySet(), LIKED_DATE_1));

        assertThat(testListSubscriber.getOnNextEvents()).containsExactly(playlistAssociations);
    }

    @Test
    public void loadLikedPlaylistsAdhereToLimit() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(1, Long.MAX_VALUE, Strings.EMPTY).subscribe(testListSubscriber);

        final List<PlaylistAssociation> playlistAssociations = Arrays.asList(
                expectedLikedPlaylistFor(playlist2.toPropertySet(), LIKED_DATE_2));

        testListSubscriber.assertValue(playlistAssociations);
    }

    @Test
    public void loadLikedPlaylistsAdhereToTimestamp() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(1, LIKED_DATE_2.getTime(), Strings.EMPTY).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist1.toPropertySet(), LIKED_DATE_1));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void shouldAdhereToPlaylistTitleFilter() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(1, Long.MAX_VALUE, playlist1.getTitle()).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist1.toPropertySet(), LIKED_DATE_1));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void shouldAdhereToPlaylistTitleWithQuoteFilter() throws Exception {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setTitle("Jan's Playlist");
        testFixtures().insertUser(playlist.getUser());
        testFixtures().insertLikedPlaylist(LIKED_DATE_1, playlist);

        playlistLikesStorage.loadLikedPlaylists(1, Long.MAX_VALUE, playlist.getTitle()).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist.toPropertySet(), LIKED_DATE_1));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void shouldAdhereToPlaylistUserFilter() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(1, Long.MAX_VALUE, playlist1.getUsername()).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist1.toPropertySet(), LIKED_DATE_1));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void shouldAdhereToPlaylistTrackTitleFilter() throws Exception {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1.getUrn(), 0);

        playlistLikesStorage.loadLikedPlaylists(1, Long.MAX_VALUE, apiTrack.getTitle()).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist1.toPropertySet(), LIKED_DATE_1));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void shouldAdhereToPlaylistTrackTitleFilterWithSingleResult() throws Exception {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1.getUrn(), 0);
        final ApiTrack apiTrack2 = testFixtures().insertTrackWithTitle(apiTrack.getTitle());
        testFixtures().insertPlaylistTrack(playlist1.getUrn(), apiTrack2.getUrn(), 1);

        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE, apiTrack.getTitle()).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist1.toPropertySet(), LIKED_DATE_1));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void shouldAdhereToPlaylistTrackUserFilter() throws Exception {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1.getUrn(), 0);

        playlistLikesStorage.loadLikedPlaylists(1, Long.MAX_VALUE, apiTrack.getUserName()).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist1.toPropertySet(), LIKED_DATE_1));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void loadsRequestedDownloadStateForPlaylistMarkedForOffline() {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1, 0);
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist1);
        testFixtures().insertTrackPendingDownload(apiTrack.getUrn(), LIKED_DATE_1.getTime());

        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE, Strings.EMPTY).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = newArrayList(
                expectedLikedPlaylistFor(playlist2.toPropertySet(), LIKED_DATE_2),
                expectedLikedPlaylistWithOfflineState(playlist1, LIKED_DATE_1, OfflineState.REQUESTED));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void loadsDownloadedStateForPlaylistMarkedForOffline() {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1, 0);
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist1);
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), LIKED_DATE_1.getTime(), LIKED_DATE_1.getTime());

        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE, Strings.EMPTY).subscribe(testListSubscriber);

        final List<PlaylistAssociation> propertySets = newArrayList(
                expectedLikedPlaylistFor(playlist2.toPropertySet(), LIKED_DATE_2),
                expectedLikedPlaylistWithOfflineState(playlist1, LIKED_DATE_1, OfflineState.DOWNLOADED));

        testListSubscriber.assertValue(propertySets);
    }

    static PlaylistAssociation expectedLikedPlaylistFor(PropertySet playlist, Date likedAt) {
        return PlaylistAssociation.create(PlaylistItem.from(PropertySet.from(
                PlaylistProperty.URN.bind(playlist.get(PlaylistProperty.URN)),
                PlaylistProperty.TITLE.bind(playlist.get(PlaylistProperty.TITLE)),
                EntityProperty.IMAGE_URL_TEMPLATE.bind(playlist.get(EntityProperty.IMAGE_URL_TEMPLATE)),
                PlaylistProperty.CREATOR_URN.bind(playlist.get(PlaylistProperty.CREATOR_URN)),
                PlaylistProperty.CREATOR_NAME.bind(playlist.get(PlaylistProperty.CREATOR_NAME)),
                PlaylistProperty.TRACK_COUNT.bind(playlist.get(PlaylistProperty.TRACK_COUNT)),
                PlaylistProperty.LIKES_COUNT.bind(playlist.get(PlaylistProperty.LIKES_COUNT)),
                PlaylistProperty.IS_PRIVATE.bind(playlist.get(PlaylistProperty.IS_PRIVATE)),
                PlaylistProperty.IS_USER_LIKE.bind(true),
                OfflineProperty.IS_MARKED_FOR_OFFLINE.bind(false))), likedAt);
    }

    private PlaylistAssociation expectedLikedPlaylistWithOfflineState(ApiPlaylist playlist, Date likedAt, OfflineState state) {
        final PlaylistAssociation playlistAssociation = expectedLikedPlaylistFor(playlist.toPropertySet(), likedAt);
        playlistAssociation.getPlaylistItem().getSource()
                .put(OfflineProperty.IS_MARKED_FOR_OFFLINE, true)
                .put(OfflineProperty.OFFLINE_STATE, state);
        return playlistAssociation;
    }
}
