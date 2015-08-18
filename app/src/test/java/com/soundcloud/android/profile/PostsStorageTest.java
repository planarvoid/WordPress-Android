package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PostsStorageTest extends StorageIntegrationTest {

    private static final Date POSTED_DATE_1 = new Date(100000);
    private static final Date POSTED_DATE_2 = new Date(200000);
    private static final Date POSTED_DATE_3 = new Date(300000);
    private static final Date POSTED_DATE_4 = new Date(400000);

    private PostsStorage storage;
    private ApiUser user;
    private PropertySet post1;
    private PropertySet post2;
    private PropertySet post3;
    private PropertySet post4;

    private TestObserver<List<PropertySet>> observer = new TestObserver<>();

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        user = testFixtures().insertUser();

        storage = new PostsStorage(propellerRx(), accountOperations);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(user.getUrn());
    }

    @Test
    public void shouldLoadAllTrackPostsForPlayback() throws Exception {
        post1 = createPlaylistPostAt(POSTED_DATE_1);
        post2 = createTrackPostAt(POSTED_DATE_2);
        post3 = createTrackRepostAt(POSTED_DATE_3);
        post4 = createPlaylistRepostAt(POSTED_DATE_4);

        storage.loadPostsForPlayback().subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(
                Arrays.asList(
                        post3.slice(TrackProperty.URN).put(TrackProperty.REPOSTER_URN, user.getUrn()),
                        post2.slice(TrackProperty.URN))
        );
    }

    @Test
    public void shouldLoadAllPosts() throws Exception {
        post1 = createPlaylistPostAt(POSTED_DATE_1);
        post2 = createTrackPostAt(POSTED_DATE_2);
        post3 = createTrackRepostAt(POSTED_DATE_3);
        post4 = createPlaylistRepostAt(POSTED_DATE_4);

        storage.loadPosts(10, Long.MAX_VALUE).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(Arrays.asList(post4, post3, post2, post1));
    }

    @Test
    public void shouldReturnTrackCountAsMaximumOfRemoteAndLocalCounts() throws Exception {
        post1 = createPlaylistPostAt(POSTED_DATE_1);
        post2 = createPlaylistRepostAt(POSTED_DATE_2);
        assertThat(post1.get(PlaylistProperty.TRACK_COUNT)).isEqualTo(2);

        final Urn playlistUrn = post1.get(PlaylistProperty.URN);
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        storage.loadPosts(10, Long.MAX_VALUE).subscribe(observer);

        final List<PropertySet> result = observer.getOnNextEvents().get(0);
        assertThat(result.get(1).get(PlaylistProperty.URN)).isEqualTo(playlistUrn);
        assertThat(result.get(1).get(PlaylistProperty.TRACK_COUNT)).isEqualTo(3);
    }

    @Test
    public void shouldAdhereToLimit() throws Exception {
        post1 = createTrackPostAt(POSTED_DATE_1);
        post2 = createPlaylistPostAt(POSTED_DATE_2);
        storage.loadPosts(1, Long.MAX_VALUE).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(Arrays.asList(post2));
    }

    @Test
    public void shouldAdhereToPostedTime() throws Exception {
        post1 = createTrackPostAt(POSTED_DATE_2);
        post2 = createPlaylistPostAt(POSTED_DATE_2);

        // 2 old items, reposted after the above tracks
        post3 = createTrackRepostAt(POSTED_DATE_4, POSTED_DATE_1);
        post4 = createPlaylistRepostAt(POSTED_DATE_4, POSTED_DATE_1);

        storage.loadPosts(2, POSTED_DATE_3.getTime()).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(Arrays.asList(post1, post2));
    }

    @Test
    public void shouldIncludeLikeStatusInPlaylistPosts() throws Exception {
        post1 = createPlaylistPostAt(POSTED_DATE_1).put(PlayableProperty.IS_LIKED, true);
        post2 = createPlaylistRepostAt(POSTED_DATE_2).put(PlayableProperty.IS_LIKED, true);

        testFixtures().insertLike(new ApiLike(post1.get(PlaylistProperty.URN), new Date()));
        testFixtures().insertLike(new ApiLike(post2.get(PlaylistProperty.URN), new Date()));

        storage.loadPosts(2, Long.MAX_VALUE).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(Arrays.asList(post2, post1));
    }

    @Test
    public void shouldNotIncludePlaylistsNotPresentInThePostsTable() throws Exception {
        post1 = createTrackPostAt(POSTED_DATE_1);
        post2 = createTrackRepostAt(POSTED_DATE_2);
        createTrackAt(POSTED_DATE_3);

        storage.loadPosts(10, Long.MAX_VALUE).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(Arrays.asList(post2, post1));
    }

    private PropertySet createTrackPostAt(Date postedAt) {
        ApiTrack track = createTrackAt(postedAt);
        createTrackPostWithId(track.getUrn().getNumericId(), postedAt);
        return createTrackPostPropertySet(track);
    }

    private PropertySet createTrackRepostAt(Date postedAt) {
        return createTrackRepostAt(postedAt, postedAt);
    }

    private PropertySet createTrackRepostAt(Date postedAt, Date createdAt) {
        ApiTrack track = createTrackAt(createdAt);
        createTrackRepostWithId(track.getUrn().getNumericId(), postedAt);
        return createTrackPostPropertySet(track).put(TrackProperty.IS_REPOSTED, true)
                .put(TrackProperty.REPOSTER, user.getUsername());
    }

    private PropertySet createPlaylistPostAt(Date postedAt) {
        ApiPlaylist playlist = createPlaylistAt(postedAt);
        createPlaylistPostWithId(playlist.getUrn().getNumericId(), postedAt);
        return createPlaylistPostPropertySet(playlist);
    }

    private PropertySet createPlaylistRepostAt(Date postedAt) {
        return createPlaylistRepostAt(postedAt, postedAt);
    }

    private PropertySet createPlaylistRepostAt(Date postedAt, Date createdAt) {
        ApiPlaylist playlist = createPlaylistAt(createdAt);
        createPlaylistRepostWithId(playlist.getUrn().getNumericId(), postedAt);
        return createPlaylistPostPropertySet(playlist)
                .put(TrackProperty.IS_REPOSTED, true)
                .put(TrackProperty.REPOSTER, user.getUsername());
    }

    private PropertySet createPlaylistPostPropertySet(ApiPlaylist playlist) {
        return playlist.toPropertySet().slice(
                PlaylistProperty.URN,
                PlaylistProperty.TITLE,
                PlaylistProperty.CREATOR_NAME,
                PlaylistProperty.TRACK_COUNT,
                PlaylistProperty.LIKES_COUNT,
                PlaylistProperty.IS_PRIVATE
        ).put(PostProperty.CREATED_AT, playlist.getCreatedAt())
                .put(PlayableProperty.IS_LIKED, false)
                .put(PlayableProperty.IS_REPOSTED, false);
    }

    private PropertySet createTrackPostPropertySet(ApiTrack track) {
        return track.toPropertySet().slice(
                TrackProperty.URN,
                TrackProperty.TITLE,
                TrackProperty.CREATOR_NAME,
                TrackProperty.LIKES_COUNT,
                TrackProperty.DURATION,
                TrackProperty.IS_PRIVATE
        ).put(PostProperty.CREATED_AT, track.getCreatedAt())
                .put(PlayableProperty.IS_LIKED, false)
                .put(PlayableProperty.IS_REPOSTED, false);
    }

    private ApiPlaylist createPlaylistAt(Date creationDate) {
        return testFixtures().insertPlaylistWithCreationDate(user, creationDate);
    }

    private ApiTrack createTrackAt(Date creationDate) {
        return testFixtures().insertTrackWithCreationDate(user, creationDate);
    }

    private void createPlaylistPostWithId(long playlistId, Date postedAt) {
        testFixtures().insertPlaylistPost(playlistId, postedAt.getTime(), false);
    }

    private void createTrackPostWithId(long trackId, Date postedAt) {
        testFixtures().insertTrackPost(trackId, postedAt.getTime(), false);
    }

    private void createTrackRepostWithId(long trackId, Date postedAt) {
        testFixtures().insertTrackRepost(trackId, postedAt.getTime());
    }

    private void createPlaylistRepostWithId(long trackId, Date postedAt) {
        testFixtures().insertPlaylistRepost(trackId, postedAt.getTime());
    }
}
