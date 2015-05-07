package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.propeller.query.Filter.filter;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.PromotedTrackProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.observers.TestObserver;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamStorageTest extends StorageIntegrationTest {

    private static final long TIMESTAMP = 1000L;

    private SoundStreamStorage storage;

    @Mock private Observer<PropertySet> observer;

    @Before
    public void setup() {
        storage = new SoundStreamStorage(propellerRx(), propeller());
    }

    @Test
    public void loadingInitialStreamItemsIncludesPromotedTrack() {
        ApiTrack track = testFixtures().insertPromotedStreamTrack(TIMESTAMP);

        storage.initialStreamItems(50).subscribe(observer);

        final PropertySet promotedTrack = createTrackPropertySet(track)
                .put(PlayableProperty.IS_PRIVATE, false)
                .put(PromotedTrackProperty.AD_URN, "promoted:track:123")
                .put(PromotedTrackProperty.PROMOTER_URN, Optional.of(Urn.forUser(83)))
                .put(PromotedTrackProperty.PROMOTER_NAME, Optional.of("SoundCloud"));

        verify(observer).onNext(promotedTrack);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesTrackPosts() {
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        final PropertySet trackPost = createTrackPropertySet(track)
                .put(PlayableProperty.IS_PRIVATE, false);

        verify(observer).onNext(trackPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesTrackReposts() {
        final ApiUser reposter = testFixtures().insertUser();
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackRepost(track.getId(), TIMESTAMP, reposter.getId());
        storage.streamItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);
        final PropertySet trackRepost = createTrackPropertySet(track)
                .put(PlayableProperty.REPOSTER, reposter.getUsername())
                .put(PlayableProperty.IS_PRIVATE, false);

        verify(observer).onNext(trackRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsFiltersOutTrackWithSameTrackReposted() {
        final ApiUser reposter = testFixtures().insertUser();
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(track.getId(), TIMESTAMP);
        testFixtures().insertStreamTrackRepost(track.getId(), TIMESTAMP, reposter.getId());
        storage.streamItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);
        final PropertySet trackPost = createTrackPropertySet(track);
        final PropertySet trackRepost = createTrackPropertySet(track)
                .put(PlayableProperty.REPOSTER, reposter.getUsername())
                .put(PlayableProperty.IS_PRIVATE, false);

        verify(observer, never()).onNext(trackPost);
        verify(observer).onNext(trackRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistReposts() {
        final ApiUser reposter = testFixtures().insertUser();
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        testFixtures().insertStreamPlaylistRepost(playlist.getId(), TIMESTAMP, reposter.getId());
        storage.streamItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);
        PropertySet playlistRepost = createPlaylistPropertySet(playlist)
                .put(PlayableProperty.REPOSTER, reposter.getUsername())
                .put(PlayableProperty.IS_PRIVATE, false);

        verify(observer).onNext(playlistRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldIncludeLikesStateForPlaylist() {
        final ApiPlaylist playlist = testFixtures().insertLikedPlaylist(new Date());
        testFixtures().insertStreamPlaylistPost(playlist.getId(), TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        PropertySet playlistRepost = createPlaylistPropertySet(playlist)
                .put(PlayableProperty.IS_LIKED, true)
                .put(PlayableProperty.IS_PRIVATE, false);

        verify(observer).onNext(playlistRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsTakesIntoAccountTheGivenLimit() {
        final ApiTrack firstTrack = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(firstTrack.getId(), TIMESTAMP);
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<>();
        storage.streamItemsBefore(Long.MAX_VALUE, 1).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).toEqual(firstTrack.getUrn());
    }

    @Test
    public void streamItemsBeforeOnlyLoadsItemsOlderThanTheGivenTimestamp() {
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        final ApiTrack oldestTrack = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(oldestTrack.getId(), TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.streamItemsBefore(TIMESTAMP, 50).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).toEqual(oldestTrack.getUrn());
    }

    @Test
    public void loadStreamItemsSinceOnlyLoadsItemsNewerThanTheGivenTimestamp() {
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        final ApiTrack newest = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(newest.getId(), TIMESTAMP + 1);

        final List<PropertySet> actual = storage.loadStreamItemsSince(TIMESTAMP, 50);
        expect(actual.size()).toBe(1);
        expect(actual.get(0).get(PlayableProperty.URN)).toEqual(newest.getUrn());
    }

    @Test
    public void loadStreamItemsSinceDoesNotIncludePromotedTracks() {
        testFixtures().insertPromotedStreamTrack(TIMESTAMP);
        final ApiTrack newest = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(newest.getId(), TIMESTAMP + 1);

        storage.initialStreamItems(50).subscribe(observer);

        final List<PropertySet> actual = storage.loadStreamItemsSince(TIMESTAMP - 1, 50);
        expect(actual.size()).toBe(1);
    }

    @Test
    public void shouldExcludeOrphanedRecordsInActivityView() {
        final ApiTrack deletedTrack = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(deletedTrack.getId(), TIMESTAMP);
        testFixtures().insertStreamTrackPost(testFixtures().insertTrack().getId(), TIMESTAMP);
        propeller().delete(Table.Sounds, filter().whereEq(TableColumns.Sounds._ID, deletedTrack.getId()));

        TestObserver<PropertySet> observer = new TestObserver<>();
        storage.streamItemsBefore(Long.MAX_VALUE, 50).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).not.toEqual(deletedTrack.getUrn());
    }

    @Test
    public void trackUrnsLoadsUrnsOfAllTrackItemsInSoundStream() {
        final ApiTrack trackOne = testFixtures().insertTrack();
        testFixtures().insertStreamTrackPost(trackOne.getId(), TIMESTAMP);
        final ApiTrack trackTwo = testFixtures().insertTrack();
        testFixtures().insertStreamTrackRepost(trackTwo.getId(), TIMESTAMP - 1, testFixtures().insertUser().getId());
        testFixtures().insertStreamPlaylistPost(testFixtures().insertPlaylist().getId(), TIMESTAMP - 2);

        TestObserver<Urn> observer = new TestObserver<>();
        storage.trackUrns().subscribe(observer);
        expect(observer.getOnNextEvents()).toContainExactly(trackOne.getUrn(), trackTwo.getUrn());
    }

    private PropertySet createTrackPropertySet(final ApiTrack track) {
        return PropertySet.from(
                PlayableProperty.URN.bind(Urn.forTrack(track.getId())),
                PlayableProperty.TITLE.bind(track.getTitle()),
                PlayableProperty.DURATION.bind(track.getDuration()),
                PlayableProperty.CREATED_AT.bind(new Date(TIMESTAMP)),
                PlayableProperty.CREATOR_NAME.bind(track.getUser().getUsername()),
                TrackProperty.PLAY_COUNT.bind(track.getStats().getPlaybackCount()));
    }

    private PropertySet createPlaylistPropertySet(ApiPlaylist playlist) {
        return PropertySet.from(
                PlayableProperty.URN.bind(Urn.forPlaylist(playlist.getId())),
                PlayableProperty.TITLE.bind(playlist.getTitle()),
                PlayableProperty.DURATION.bind(playlist.getDuration()),
                PlayableProperty.CREATED_AT.bind(new Date(TIMESTAMP)),
                PlayableProperty.CREATOR_NAME.bind(playlist.getUser().getUsername()),
                PlayableProperty.LIKES_COUNT.bind(playlist.getStats().getLikesCount()),
                PlayableProperty.IS_LIKED.bind(false),
                PlaylistProperty.TRACK_COUNT.bind(playlist.getTrackCount()));
    }

}
