package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Filter.filter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observer;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class StreamStorageTest extends StorageIntegrationTest {

    private static final long TIMESTAMP = 1000L;
    public static final Date CREATED_AT = new Date(TIMESTAMP);

    @Mock private Observer<StreamEntity> observer;

    private StreamStorage storage;
    private TestSubscriber<StreamEntity> subscriber;

    @Before
    public void setup() {
        storage = new StreamStorage(propeller());
        subscriber = new TestSubscriber<>();
    }

    @Test
    public void loadingInitialStreamItemsIncludesPromotedTrack() {
        ApiUser promoter = ModelFixtures.create(ApiUser.class);
        ApiTrack track = testFixtures().insertPromotedStreamTrack(promoter, TIMESTAMP);

        storage.timelineItems(50).subscribe(observer);

        StreamEntity promotedStreamEntity = createPromotedDataItem(track, promoter, CREATED_AT);
        verify(observer).onNext(promotedStreamEntity);
        verify(observer).onCompleted();
    }

    @Test
    public void promotedTrackIsNotDeduplicatedWithSameTrack() {
        ApiUser promoter = ModelFixtures.create(ApiUser.class);
        ApiTrack track = testFixtures().insertPromotedStreamTrack(promoter, TIMESTAMP);
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);

        final StreamEntity normalTrack = createFromImageResource(CREATED_AT, track.getUrn(), track.getUser().getImageUrlTemplate());
        final StreamEntity promotedStreamEntity = createPromotedDataItem(track, promoter, CREATED_AT);

        storage.timelineItems(50).subscribe(subscriber);

        subscriber.assertValueCount(2);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(normalTrack);
        assertThat(subscriber.getOnNextEvents().get(1)).isEqualTo(promotedStreamEntity);
    }

    @Test
    public void loadingStreamItemsIncludesTrackPosts() {
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);
        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        final StreamEntity trackPost = createFromImageResource(CREATED_AT, track.getUrn(), track.getUser().getImageUrlTemplate());

        verify(observer).onNext(trackPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesTrackReposts() {
        final ApiUser reposter = insertReposter();
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackRepost(track.getId(), TIMESTAMP, reposter.getId());
        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        final StreamEntity trackRepost = createFromImageResourceWithReposter(track, reposter, CREATED_AT);

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

        final StreamEntity trackPost = createFromImageResource(CREATED_AT, track.getUrn(), track.getUser().getImageUrlTemplate());
        final StreamEntity trackRepost = createFromImageResourceWithReposter(track, reposter, new Date(repostedTimestamp));

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

        final StreamEntity trackRepostOld = createFromImageResourceWithReposter(track, reposter, new Date(repostedTimestampOld));
        final StreamEntity trackRepostNew = createFromImageResourceWithReposter(track, reposter, new Date(repostedTimestampNew));

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
        final StreamEntity trackPost = createFromImageResource(CREATED_AT, track.getUrn(), track.getUser().getImageUrlTemplate());

        verify(observer).onNext(trackPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistReposts() {
        final ApiUser reposter = insertReposter();
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        testFixtures().insertStreamPlaylistRepost(playlist.getId(), TIMESTAMP, reposter.getId());

        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        verify(observer).onNext(createFromImageResourceWithReposter(playlist, reposter, CREATED_AT));
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsTakesIntoAccountTheGivenLimit() {
        final ApiTrack firstTrack = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(firstTrack.getId(), TIMESTAMP);
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP - 1);

        TestSubscriber<StreamEntity> observer = new TestSubscriber<>();
        storage.timelineItemsBefore(Long.MAX_VALUE, 1).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        final StreamEntity streamEntity = observer.getOnNextEvents().get(0);
        assertThat(streamEntity.urn()).isEqualTo(firstTrack.getUrn());
    }

    @Test
    public void streamItemsBeforeOnlyLoadsItemsOlderThanTheGivenTimestamp() {
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        final ApiTrack oldestTrack = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(oldestTrack.getId(), TIMESTAMP - 1);

        TestSubscriber<StreamEntity> observer = new TestSubscriber<>();
        storage.timelineItemsBefore(TIMESTAMP, 50).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        final StreamEntity streamEntity = observer.getOnNextEvents().get(0);
        assertThat(streamEntity.urn()).isEqualTo(oldestTrack.getUrn());
    }

    @Test
    public void loadStreamItemsSinceOnlyLoadsItemsNewerThanTheGivenTimestamp() {
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        final ApiTrack newest = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(newest.getId(), TIMESTAMP + 1);

        final List<StreamEntity> actual = storage.timelineItemsSince(TIMESTAMP, 50);
        assertThat(actual).hasSize(1);
        final StreamEntity streamEntity = actual.get(0);
        assertThat(streamEntity.urn()).isEqualTo(newest.getUrn());
    }

    @Test
    public void loadStreamItemsSinceDoesNotIncludePromotedTracks() {
        testFixtures().insertPromotedStreamTrack(TIMESTAMP);
        final ApiTrack newest = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(newest.getId(), TIMESTAMP + 1);

        storage.timelineItems(50).subscribe(observer);

        final List<StreamEntity> actual = storage.timelineItemsSince(TIMESTAMP - 1, 50);
        assertThat(actual).hasSize(1);
    }

    @Test
    public void loadStreamItemsCountSinceOnlyCountsItemsNewerThanTheGivenTimestamp() {
        TestSubscriber<Integer> observer = new TestSubscriber<>();
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP + 1);

        storage.timelineItemCountSince(TIMESTAMP).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(1);
    }

    @Test
    public void shouldExcludeOrphanedRecordsInActivityView() {
        final ApiTrack deletedTrack = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(deletedTrack.getId(), TIMESTAMP);
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        propeller().delete(Tables.Sounds.TABLE, filter().whereEq(Tables.Sounds._ID, deletedTrack.getId()));

        TestSubscriber<StreamEntity> observer = new TestSubscriber<>();
        storage.timelineItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        final StreamEntity streamEntity = observer.getOnNextEvents().get(0);
        assertThat(streamEntity.urn()).isNotEqualTo(deletedTrack.getUrn());
    }

    @Test
    public void playbackItemsLoadsUrnsOfAllPlaybackItemsInSoundStream() {
        final ApiTrack trackOne = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(trackOne.getId(), TIMESTAMP);
        final ApiTrack trackTwo = testFixtures().insertTrack();
        final ApiUser reposter = testFixtures().insertUser();
        final TrackItem trackItem = ModelFixtures.trackItem(trackTwo).toBuilder().repostedProperties(RepostedProperties.create(reposter.getUsername(), reposter.getUrn())).build();
        testFixtures().insertStreamTrackRepost(trackTwo.getId(), TIMESTAMP - 1, reposter.getId());
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertStreamPlaylistPost(apiPlaylist.getId(), TIMESTAMP - 2);

        TestSubscriber<PlayableWithReposter> observer = new TestSubscriber<>();
        storage.playbackItems().subscribe(observer);
        assertThat(observer.getOnNextEvents()).containsExactly(
                PlayableWithReposter.from(trackOne.getUrn()),
                PlayableWithReposter.from(trackItem),
                PlayableWithReposter.from(apiPlaylist.getUrn()));
    }

    private StreamEntity createFromImageResource(Date createdAt, Urn urn, Optional<String> avatarUrlTemplate) {
        return builderFromImageResource(createdAt, Optional.absent(), urn, avatarUrlTemplate).build();
    }


    private StreamEntity.Builder builderFromImageResource(Date createdAt, Optional<RepostedProperties> repostedProperties, Urn urn, Optional<String> avatarUrlTemplate) {
        return StreamEntity.builder(urn, createdAt).avatarUrlTemplate(avatarUrlTemplate).repostedProperties(repostedProperties);
    }

    private StreamEntity createFromImageResourceWithReposter(ImageResource track, ApiUser reposter, Date createdAt) {
        return StreamEntity.builder(track.getUrn(), createdAt).avatarUrlTemplate(reposter.getImageUrlTemplate()).repostedProperties(RepostedProperties.create(reposter.getUsername(), reposter.getUrn())).build();
    }

    private StreamEntity createPromotedDataItem(ApiTrack track, ApiUser promoter, Date createdAt) {
        final StreamEntity.Builder builder = builderFromImageResource(createdAt, Optional.absent(), track.getUrn(), promoter.getImageUrlTemplate());
        builder.promotedProperties(Optional.of(promotedProperties(promoter)));
        return builder.build();
    }

    private PromotedProperties promotedProperties(ApiUser promoter) {
        return PromotedProperties.create("promoted:track:123",
                                         Arrays.asList("promoted1", "promoted2"),
                                         Arrays.asList("promoted3", "promoted4"),
                                         Arrays.asList("promoted5", "promoted6"),
                                         Arrays.asList("promoted7", "promoted8"),
                                         Optional.of(promoter.getUrn()),
                                         Optional.of(promoter.getUsername()));
    }

}
