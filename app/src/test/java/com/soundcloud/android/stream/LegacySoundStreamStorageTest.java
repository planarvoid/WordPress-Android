package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.WhereBuilder;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.observers.TestObserver;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LegacySoundStreamStorageTest extends StorageIntegrationTest {

    private static final long TIMESTAMP = 1000L;

    private LegacySoundStreamStorage storage;

    @Mock private Observer<PropertySet> observer;

    @Before
    public void setup() {
        storage = new LegacySoundStreamStorage(testScheduler(), propeller());
    }

    @Test
    public void loadingStreamItemsIncludesTrackPosts() throws CreateModelException {
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertLegacyTrackPost(track, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        final PropertySet trackPost = createTrackPropertySet(track);

        verify(observer).onNext(trackPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesTrackReposts() throws CreateModelException {
        final ApiUser reposter = testFixtures().insertUser();
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertLegacyTrackRepost(track, reposter, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        final PropertySet trackRepost = createTrackPropertySet(track)
                .put(PlayableProperty.REPOSTER, reposter.getUsername());

        verify(observer).onNext(trackRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistPosts() throws CreateModelException {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        testFixtures().insertLegacyPlaylistPost(playlist, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        final PropertySet playlistPost = createPlaylistPropertySet(playlist);

        verify(observer).onNext(playlistPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistReposts() throws CreateModelException {
        final ApiUser reposter = testFixtures().insertUser();
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        testFixtures().insertLegacyPlaylistRepost(playlist, reposter, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        PropertySet playlistRepost = createPlaylistPropertySet(playlist)
                .put(PlayableProperty.REPOSTER, reposter.getUsername());

        verify(observer).onNext(playlistRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldIncludeLikesStateForPlaylistAndUser() throws CreateModelException {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        testFixtures().insertLegacyPlaylistPost(playlist, TIMESTAMP);
        final int currentUserId = 123;
        testFixtures().insertLegacyPlaylistLike(playlist.getId(), currentUserId);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(currentUserId), 50).subscribe(observer);

        PropertySet playlistRepost = createPlaylistPropertySet(playlist)
                .put(PlayableProperty.IS_LIKED, true);

        verify(observer).onNext(playlistRepost);
        verify(observer).onCompleted();
    }

    // we'll eventually refactor the underlying schema, but for now we need to make sure to exclude stuff
    // like comments and affiliations from here
    @Test
    public void loadingStreamItemsDoesNotIncludeComments() throws CreateModelException {
        testFixtures().insertComment();

        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    // we'll eventually refactor the underlying schema, but for now we need to make sure to exclude stuff
    // like comments and affiliations from here
    @Test
    public void loadingStreamItemsDoesNotIncludeAffiliations() throws CreateModelException {
        testFixtures().insertAffiliation();

        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void loadingStreamItemsDoesNotIncludeOwnContentRepostedByOtherPeople() throws CreateModelException {
        final ApiUser reposter = testFixtures().insertUser();
        final ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertTrackRepostOfOwnTrack(track, reposter, TIMESTAMP);

        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void loadingStreamItemsTakesIntoAccountTheGivenLimit() throws CreateModelException {
        final ApiTrack firstTrack = testFixtures().insertTrack();
        testFixtures().insertLegacyTrackPost(firstTrack, TIMESTAMP);
        testFixtures().insertLegacyTrackPost(testFixtures().insertTrack(), TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 1).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).toEqual(firstTrack.getUrn());
    }

    @Test
    public void loadingStreamItemsOnlyLoadsItemsOlderThanTheGivenTimestamp() throws CreateModelException {
        testFixtures().insertLegacyTrackPost(testFixtures().insertTrack(), TIMESTAMP);
        final ApiTrack oldestTrack = testFixtures().insertTrack();
        testFixtures().insertLegacyTrackPost(oldestTrack, TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.streamItemsBefore(TIMESTAMP, Urn.forUser(123), 50).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).toEqual(oldestTrack.getUrn());
    }

    @Test
    public void loadStreamItemsSinceOnlyLoadsItemsNewerThanTheGivenTimestamp() throws CreateModelException {
        testFixtures().insertLegacyTrackPost(testFixtures().insertTrack(), TIMESTAMP);
        final ApiTrack newest = testFixtures().insertTrack();
        testFixtures().insertLegacyTrackPost(newest, TIMESTAMP + 1);

        final List<PropertySet> actual = storage.loadStreamItemsSince(TIMESTAMP, Urn.forUser(123), 50);
        expect(actual.size()).toBe(1);
        expect(actual.get(0).get(PlayableProperty.URN)).toEqual(newest.getUrn());
    }

    @Test
    public void shouldExcludeOrphanedRecordsInActivityView() throws CreateModelException {
        final ApiTrack deletedTrack = testFixtures().insertTrack();
        testFixtures().insertLegacyTrackPost(deletedTrack, TIMESTAMP);
        testFixtures().insertLegacyTrackPost(testFixtures().insertTrack(), TIMESTAMP);
        propeller().delete(Table.Sounds, new WhereBuilder().whereEq(TableColumns.Sounds._ID, deletedTrack.getId()));

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).not.toEqual(deletedTrack.getUrn());
    }

    @Test
    public void trackUrnsLoadsUrnsOfAllTrackItemsInSoundStream() throws CreateModelException {
        final ApiTrack trackOne = testFixtures().insertTrack();
        testFixtures().insertLegacyTrackPost(trackOne, TIMESTAMP);
        final ApiTrack trackTwo = testFixtures().insertTrack();
        testFixtures().insertLegacyTrackRepost(trackTwo, testFixtures().insertUser(), TIMESTAMP - 1);
        testFixtures().insertLegacyPlaylistPost(testFixtures().insertPlaylist(), TIMESTAMP - 2);

        TestObserver<Urn> observer = new TestObserver<Urn>();
        storage.trackUrns().subscribe(observer);
        expect(observer.getOnNextEvents()).toContainExactly(trackOne.getUrn(), trackTwo.getUrn());
    }

    private PropertySet createTrackPropertySet(final ApiTrack track) throws CreateModelException {
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
