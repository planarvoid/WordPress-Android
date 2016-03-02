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
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
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

    final TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() {
        user = testFixtures().insertUser();

        storage = new PostsStorage(propellerRx(), accountOperations);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(user.getUrn());
    }

    @Test
    public void shouldLoadAllPostsForPlayback() {
        post1 = createPlaylistPostAt(POSTED_DATE_1);
        post2 = createTrackPostAt(POSTED_DATE_2);
        post3 = createTrackRepostAt(POSTED_DATE_3);
        post4 = createPlaylistRepostAt(POSTED_DATE_4);

        storage.loadPostsForPlayback().subscribe(subscriber);

        subscriber.assertValue(
                Arrays.asList(
                        post4.slice(PlaylistProperty.URN).put(PostProperty.REPOSTER_URN, user.getUrn()),
                        post3.slice(TrackProperty.URN).put(PostProperty.REPOSTER_URN, user.getUrn()),
                        post2.slice(TrackProperty.URN),
                        post1.slice(TrackProperty.URN))
        );
    }

    @Test
    public void shouldLoadAllPosts() {
        post1 = createPlaylistPostAt(POSTED_DATE_1);
        post2 = createTrackPostAt(POSTED_DATE_2);
        post3 = createTrackRepostAt(POSTED_DATE_3);
        post4 = createPlaylistRepostAt(POSTED_DATE_4);

        storage.loadPosts(10, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(post4, post3, post2, post1));
    }

    @Test
    public void shouldReturnTrackCountAsMaximumOfRemoteAndLocalCounts() {
        post1 = createPlaylistPostAt(POSTED_DATE_1);
        post2 = createPlaylistRepostAt(POSTED_DATE_2);
        assertThat(post1.get(PlaylistProperty.TRACK_COUNT)).isEqualTo(2);

        final Urn playlistUrn = post1.get(PlaylistProperty.URN);
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        storage.loadPosts(10, Long.MAX_VALUE).subscribe(subscriber);

        final List<PropertySet> result = subscriber.getOnNextEvents().get(0);
        assertThat(result.get(1).get(PlaylistProperty.URN)).isEqualTo(playlistUrn);
        assertThat(result.get(1).get(PlaylistProperty.TRACK_COUNT)).isEqualTo(3);
    }

    @Test
    public void shouldAdhereToLimit() {
        post1 = createTrackPostAt(POSTED_DATE_1);
        post2 = createPlaylistPostAt(POSTED_DATE_2);
        storage.loadPosts(1, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(post2));
    }

    @Test
    public void shouldAdhereToPostedTime() throws Exception {
        post1 = createTrackPostAt(POSTED_DATE_2);
        post2 = createPlaylistPostAt(POSTED_DATE_1);

        // 2 old items, reposted after the above tracks
        post3 = createTrackRepostAt(POSTED_DATE_4, POSTED_DATE_1);
        post4 = createPlaylistRepostAt(POSTED_DATE_4, POSTED_DATE_1);

        storage.loadPosts(2, POSTED_DATE_3.getTime()).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(post1, post2));
    }

    @Test
    public void shouldIncludeLikeStatusInPlaylistPosts() throws Exception {
        post1 = createPlaylistPostAt(POSTED_DATE_1).put(PlayableProperty.IS_USER_LIKE, true);
        post2 = createPlaylistRepostAt(POSTED_DATE_2).put(PlayableProperty.IS_USER_LIKE, true);

        testFixtures().insertLike(new ApiLike(post1.get(PlaylistProperty.URN), new Date()));
        testFixtures().insertLike(new ApiLike(post2.get(PlaylistProperty.URN), new Date()));

        storage.loadPosts(2, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(post2, post1));
    }

    @Test
    public void shouldNotIncludePlaylistsNotPresentInThePostsTable() throws Exception {
        post1 = createTrackPostAt(POSTED_DATE_1);
        post2 = createTrackRepostAt(POSTED_DATE_2);
        createTrackAt(POSTED_DATE_3);

        storage.loadPosts(10, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(post2, post1));
    }

    @Test
    public void shouldLoadLastPublicPostedTrackWithDatePostedAndPermalink() throws Exception {
        post1 = createTrackPostForLastPostedAt(POSTED_DATE_2);
        createTrackPostForLastPostedAt(POSTED_DATE_1);
        TestSubscriber<Optional<PropertySet>> subscriber = new TestSubscriber<>();

        storage.loadLastPublicPostedTrack().subscribe(subscriber);

        subscriber.assertValue(Optional.of(post1));
    }

    @Test
    public void shouldLoadLastPublicPostedTrackExcludingPrivateTracks() throws Exception {
        createPrivateTrackPostForLastPostedAt(POSTED_DATE_2);
        post2 = createTrackPostForLastPostedAt(POSTED_DATE_1);
        TestSubscriber<Optional<PropertySet>> subscriber = new TestSubscriber<>();

        storage.loadLastPublicPostedTrack().subscribe(subscriber);

        subscriber.assertValue(Optional.of(post2));
    }

    @Test
    public void shouldLoadLastPublicPostedAsAbsentWhenUserHasNoPosts() throws Exception {
        TestSubscriber<Optional<PropertySet>> subscriber = new TestSubscriber<>();

        storage.loadLastPublicPostedTrack().subscribe(subscriber);

        subscriber.assertValue(Optional.<PropertySet>absent());
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
        return createTrackPostPropertySet(track).put(TrackProperty.IS_USER_REPOST, true)
                .put(PostProperty.REPOSTER, user.getUsername());
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
                .put(TrackProperty.IS_USER_REPOST, true)
                .put(PostProperty.REPOSTER, user.getUsername());
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
                .put(PlayableProperty.IS_USER_LIKE, false)
                .put(PlayableProperty.IS_USER_REPOST, false);
    }

    private PropertySet createTrackPostPropertySet(ApiTrack track) {
        return track.toPropertySet().slice(
                TrackProperty.URN,
                TrackProperty.TITLE,
                TrackProperty.CREATOR_NAME,
                TrackProperty.LIKES_COUNT,
                TrackProperty.PLAY_COUNT,
                TrackProperty.SNIPPET_DURATION,
                TrackProperty.FULL_DURATION,
                TrackProperty.IS_PRIVATE,
                TrackProperty.BLOCKED,
                TrackProperty.SNIPPED,
                TrackProperty.SUB_MID_TIER,
                TrackProperty.SUB_HIGH_TIER
        ).put(PostProperty.CREATED_AT, track.getCreatedAt())
                .put(PlayableProperty.IS_USER_LIKE, false)
                .put(PlayableProperty.IS_USER_REPOST, false);
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

    private PropertySet createTrackPostForLastPostedAt(Date postedAt) {
        ApiTrack track = createTrackAt(postedAt);
        createTrackPostWithId(track.getUrn().getNumericId(), postedAt);
        return createTrackPostForLastPostedPropertySet(track);
    }

    private PropertySet createTrackPostForLastPostedPropertySet(ApiTrack track) {
        return track.toPropertySet().slice(
                TrackProperty.URN,
                TrackProperty.PERMALINK_URL
        ).put(PostProperty.CREATED_AT, track.getCreatedAt());
    }

    private ApiTrack createPrivateTrackAt(Date creationDate) {
        return testFixtures().insertPrivateTrackWithCreationDate(user, creationDate);
    }

    private PropertySet createPrivateTrackPostForLastPostedAt(Date postedAt) {
        ApiTrack track = createPrivateTrackAt(postedAt);
        createTrackPostWithId(track.getUrn().getNumericId(), postedAt);
        return createTrackPostForLastPostedPropertySet(track);
    }

}
