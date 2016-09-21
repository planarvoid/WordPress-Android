package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Filter.filter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observer;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SoundStreamStorageTest extends StorageIntegrationTest {

    private static final long TIMESTAMP = 1000L;

    @Mock private Observer<PropertySet> observer;

    private SoundStreamStorage storage;

    @Before
    public void setup() {
        storage = new SoundStreamStorage(propeller());
    }

    @Test
    public void loadingInitialStreamItemsIncludesPromotedTrack() {
        ApiUser promoter = ModelFixtures.create(ApiUser.class);
        ApiTrack track = testFixtures().insertPromotedStreamTrack(promoter, TIMESTAMP);

        storage.timelineItems(50).subscribe(observer);

        final PropertySet promotedTrack = createPromotedTrackPropertySet(track, promoter);

        verify(observer).onNext(promotedTrack);
        verify(observer).onCompleted();
    }

    @Test
    public void promotedTrackIsNotDeduplicatedWithSameTrack() {
        ApiUser promoter = ModelFixtures.create(ApiUser.class);
        ApiTrack track = testFixtures().insertPromotedStreamTrack(promoter, TIMESTAMP);
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);

        final PropertySet normalTrackProperties = createTrackPropertySet(track);
        final PropertySet promotedTrackProperties = createPromotedTrackPropertySet(track, promoter);

        storage.timelineItems(50).subscribe(observer);

        verify(observer).onNext(promotedTrackProperties);
        verify(observer).onNext(normalTrackProperties);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesTrackPosts() {
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);
        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        final PropertySet trackPost = createTrackPropertySet(track);

        verify(observer).onNext(trackPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesTrackReposts() {
        final ApiUser reposter = insertReposter();
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackRepost(track.getId(), TIMESTAMP, reposter.getId());
        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        final PropertySet trackRepost = createRepostedTrack(track, reposter);

        verify(observer).onNext(trackRepost);
        verify(observer).onCompleted();
    }

    private ApiUser insertReposter() {
        final ApiUser reposter = ModelFixtures.create(ApiUser.class);
        reposter.setAvatarUrlTemplate("reposter-avatar"); // to distinguish from creator avatar
        testFixtures().insertUser(reposter);
        return reposter;
    }

    @Test
    public void loadingStreamItemsFiltersOutTrackWithSameTrackReposted() {
        final ApiUser reposter = insertReposter();
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);

        final long repostedTimestamp = TIMESTAMP + 100; // must be later, as query is based on timestamp
        testFixtures().insertStreamTrackRepost(track.getId(), repostedTimestamp, reposter.getId());

        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        final PropertySet trackPost = createTrackPropertySet(track);
        final PropertySet trackRepost = createRepostedTrack(track, reposter, new Date(repostedTimestamp));

        verify(observer, never()).onNext(trackPost);
        verify(observer).onNext(trackRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsOnlyIncludesNewestRepost() {
        final ApiUser reposter = insertReposter();
        final ApiTrack track = testFixtures().insertTrack();

        final long repostedTimestampOld = TIMESTAMP;
        final long repostedTimestampNew = TIMESTAMP + 10000;

        testFixtures().insertStreamTrackRepost(track.getId(), repostedTimestampOld, reposter.getId());
        testFixtures().insertStreamTrackRepost(track.getId(), repostedTimestampNew, reposter.getId());

        final PropertySet trackRepostOld = createRepostedTrack(track, reposter, new Date(repostedTimestampOld));
        final PropertySet trackRepostNew = createRepostedTrack(track, reposter, new Date(repostedTimestampNew));

        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        verify(observer, never()).onNext(trackRepostOld);
        verify(observer).onNext(trackRepostNew);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsFiltersOutDuplicateTrack() {
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);

        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);
        final PropertySet trackPost = createTrackPropertySet(track);

        verify(observer).onNext(trackPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistReposts() {
        final ApiUser reposter = insertReposter();
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        testFixtures().insertStreamPlaylistRepost(playlist.getId(), TIMESTAMP, reposter.getId());

        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        verify(observer).onNext(createRepostedPlaylist(playlist, reposter));
        verify(observer).onCompleted();
    }

    @Test
    public void shouldIncludeLikesStateForPlaylist() {
        final ApiPlaylist playlist = testFixtures().insertLikedPlaylist(new Date());
        testFixtures().insertStreamPlaylistPost(playlist.getId(), TIMESTAMP);
        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        PropertySet playlistRepost = createPlaylistPropertySet(playlist)
                .put(PlayableProperty.IS_USER_LIKE, true);

        verify(observer).onNext(playlistRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldIncludeRepostStateForPlaylist() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistPost(playlist.getId(), TIMESTAMP, true);
        testFixtures().insertStreamPlaylistPost(playlist.getId(), TIMESTAMP);
        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        PropertySet playlistRepost = createPlaylistPropertySet(playlist)
                .put(PlayableProperty.IS_USER_REPOST, true);

        verify(observer).onNext(playlistRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsTakesIntoAccountTheGivenLimit() {
        final ApiTrack firstTrack = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(firstTrack.getId(), TIMESTAMP);
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<>();
        storage.timelineItemsBefore(Long.MAX_VALUE, 1).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).isEqualTo(firstTrack.getUrn());
    }

    @Test
    public void streamItemsBeforeOnlyLoadsItemsOlderThanTheGivenTimestamp() {
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        final ApiTrack oldestTrack = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(oldestTrack.getId(), TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<>();
        storage.timelineItemsBefore(TIMESTAMP, 50).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).isEqualTo(oldestTrack.getUrn());
    }

    @Test
    public void loadStreamItemsSinceOnlyLoadsItemsNewerThanTheGivenTimestamp() {
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        final ApiTrack newest = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(newest.getId(), TIMESTAMP + 1);

        final List<PropertySet> actual = storage.timelineItemsSince(TIMESTAMP, 50);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).get(PlayableProperty.URN)).isEqualTo(newest.getUrn());
    }

    @Test
    public void loadStreamItemsSinceDoesNotIncludePromotedTracks() {
        testFixtures().insertPromotedStreamTrack(TIMESTAMP);
        final ApiTrack newest = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(newest.getId(), TIMESTAMP + 1);

        storage.timelineItems(50).subscribe(observer);

        final List<PropertySet> actual = storage.timelineItemsSince(TIMESTAMP - 1, 50);
        assertThat(actual).hasSize(1);
    }

    @Test
    public void loadStreamItemsCountSinceOnlyCountsItemsNewerThanTheGivenTimestamp() {
        TestObserver<Integer> observer = new TestObserver<>();
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP + 1);

        storage.timelineItemCountSince(TIMESTAMP).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(1);
    }

    @Test
    public void loadingStreamItemsIncludesTierInformation() {
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);
        testFixtures().insertPolicyHighTierMonetizable(track.getUrn());

        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        final PropertySet highTierTrackPost = createTrackPropertySet(track).put(TrackProperty.SUB_HIGH_TIER, true);

        verify(observer).onNext(highTierTrackPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemIncludesSnippedFlag() {
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setSnipped(true);
        testFixtures().insertTrack(track);
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);

        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        final PropertySet snippedTrack = createTrackPropertySet(track);

        verify(observer).onNext(snippedTrack);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldExcludeOrphanedRecordsInActivityView() {
        final ApiTrack deletedTrack = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(deletedTrack.getId(), TIMESTAMP);
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        propeller().delete(Table.Sounds, filter().whereEq(TableColumns.Sounds._ID, deletedTrack.getId()));

        TestObserver<PropertySet> observer = new TestObserver<>();
        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).isNotEqualTo(deletedTrack.getUrn());
    }

    @Test
    public void playbackItemsLoadsUrnsOfAllPlaybackItemsInSoundStream() {
        final ApiTrack trackOne = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(trackOne.getId(), TIMESTAMP);
        final ApiTrack trackTwo = testFixtures().insertTrack();
        final ApiUser reposter = testFixtures().insertUser();
        testFixtures().insertStreamTrackRepost(trackTwo.getId(), TIMESTAMP - 1, reposter.getId());
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertStreamPlaylistPost(apiPlaylist.getId(), TIMESTAMP - 2);

        TestObserver<PropertySet> observer = new TestObserver<>();
        storage.playbackItems().subscribe(observer);
        assertThat(observer.getOnNextEvents()).containsExactly(
                trackOne.getUrn().toPropertySet(),
                trackTwo.getUrn().toPropertySet().put(PostProperty.REPOSTER_URN, reposter.getUrn()),
                apiPlaylist.getUrn().toPropertySet());
    }

    private PropertySet createTrackPropertySet(final ApiTrack track) {
        return createTrackPropertySet(track, new Date(TIMESTAMP));
    }

    private PropertySet createRepostedTrack(ApiTrack track, ApiUser reposter, Date repostedAt) {
        return createTrackPropertySet(track, repostedAt)
                .put(PostProperty.REPOSTER, reposter.getUsername())
                .put(PostProperty.REPOSTER_URN, reposter.getUrn())
                .put(SoundStreamProperty.AVATAR_URL_TEMPLATE, reposter.getImageUrlTemplate());
    }

    private PropertySet createRepostedTrack(ApiTrack track, ApiUser reposter) {
        return createRepostedTrack(track, reposter, new Date(TIMESTAMP));
    }

    private PropertySet createTrackPropertySet(final ApiTrack track, final Date createdAt) {
        return PropertySet.from(
                SoundStreamProperty.AVATAR_URL_TEMPLATE.bind(track.getUser().getImageUrlTemplate()),
                PlayableProperty.CREATED_AT.bind(createdAt),
                PlayableProperty.URN.bind(Urn.forTrack(track.getId())),
                PlayableProperty.TITLE.bind(track.getTitle()),
                EntityProperty.IMAGE_URL_TEMPLATE.bind(track.getImageUrlTemplate()),
                TrackProperty.SNIPPET_DURATION.bind(track.getSnippetDuration()),
                TrackProperty.FULL_DURATION.bind(track.getFullDuration()),
                PlayableProperty.CREATOR_NAME.bind(track.getUser().getUsername()),
                PlayableProperty.CREATOR_URN.bind(track.getUser().getUrn()),
                PlayableProperty.IS_USER_LIKE.bind(false),
                PlayableProperty.LIKES_COUNT.bind(track.getLikesCount()),
                PlayableProperty.IS_USER_REPOST.bind(false),
                PlayableProperty.REPOSTS_COUNT.bind(track.getRepostsCount()),
                PlayableProperty.IS_PRIVATE.bind(false),
                TrackProperty.PLAY_COUNT.bind(track.getStats().getPlaybackCount()),
                TrackProperty.SUB_HIGH_TIER.bind(track.isSubHighTier().get()),
                TrackProperty.SNIPPED.bind(track.isSnipped()));
    }

    private PropertySet createRepostedPlaylist(ApiPlaylist playlist, ApiUser reposter) {
        return createPlaylistPropertySet(playlist)
                .put(PostProperty.REPOSTER, reposter.getUsername())
                .put(PostProperty.REPOSTER_URN, reposter.getUrn())
                .put(SoundStreamProperty.AVATAR_URL_TEMPLATE, reposter.getImageUrlTemplate());
    }

    private PropertySet createPlaylistPropertySet(ApiPlaylist playlist) {
        return PropertySet.from(
                SoundStreamProperty.AVATAR_URL_TEMPLATE.bind(playlist.getUser().getImageUrlTemplate()),
                PlayableProperty.CREATED_AT.bind(new Date(TIMESTAMP)),
                PlayableProperty.URN.bind(Urn.forPlaylist(playlist.getId())),
                PlayableProperty.TITLE.bind(playlist.getTitle()),
                EntityProperty.IMAGE_URL_TEMPLATE.bind(playlist.getImageUrlTemplate()),
                PlaylistProperty.PLAYLIST_DURATION.bind(playlist.getDuration()),
                PlayableProperty.CREATOR_NAME.bind(playlist.getUser().getUsername()),
                PlayableProperty.CREATOR_URN.bind(playlist.getUser().getUrn()),
                PlayableProperty.IS_USER_LIKE.bind(false),
                PlayableProperty.LIKES_COUNT.bind(playlist.getStats().getLikesCount()),
                PlayableProperty.IS_USER_REPOST.bind(false),
                PlayableProperty.REPOSTS_COUNT.bind(playlist.getStats().getRepostsCount()),
                PlayableProperty.IS_USER_LIKE.bind(false),
                PlayableProperty.IS_PRIVATE.bind(false),
                PlaylistProperty.IS_ALBUM.bind(false),
                PlaylistProperty.SET_TYPE.bind(""),
                PlaylistProperty.TRACK_COUNT.bind(playlist.getTrackCount()));
    }

    private PropertySet createPromotedTrackPropertySet(ApiTrack track, ApiUser promoter) {
        return createTrackPropertySet(track)
                .put(SoundStreamProperty.AVATAR_URL_TEMPLATE, promoter.getImageUrlTemplate())
                .put(PromotedItemProperty.AD_URN, "promoted:track:123")
                .put(PromotedItemProperty.PROMOTER_URN, Optional.of(promoter.getUrn()))
                .put(PromotedItemProperty.PROMOTER_NAME, Optional.of(promoter.getUsername()))
                .put(PromotedItemProperty.TRACK_CLICKED_URLS, Arrays.asList("promoted1", "promoted2"))
                .put(PromotedItemProperty.TRACK_IMPRESSION_URLS, Arrays.asList("promoted3", "promoted4"))
                .put(PromotedItemProperty.TRACK_PLAYED_URLS, Arrays.asList("promoted5", "promoted6"))
                .put(PromotedItemProperty.PROMOTER_CLICKED_URLS, Arrays.asList("promoted7", "promoted8"));
    }

}