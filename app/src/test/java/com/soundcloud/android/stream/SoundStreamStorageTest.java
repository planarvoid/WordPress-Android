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
import com.soundcloud.android.robolectric.StorageIntegrationTest;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
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

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamStorageTest extends StorageIntegrationTest {

    private static final long TIMESTAMP = 1000L;

    private SoundStreamStorage storage;

    @Mock
    private Observer<PropertySet> observer;

    @Before
    public void setup() {
        storage = new SoundStreamStorage(testScheduler());
    }

    @Test
    public void loadingStreamItemsIncludesTrackPosts() throws CreateModelException {
        final ApiTrack track = testHelper().insertTrack();
        testHelper().insertTrackPost(track, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        final PropertySet trackPost = createTrackPropertySet(track);

        verify(observer).onNext(trackPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesTrackReposts() throws CreateModelException {
        final ApiUser reposter = testHelper().insertUser();
        final ApiTrack track = testHelper().insertTrack();
        testHelper().insertTrackRepost(track, reposter, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        final PropertySet trackRepost = createTrackPropertySet(track)
                .put(PlayableProperty.REPOSTER, reposter.getUsername());

        verify(observer).onNext(trackRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistPosts() throws CreateModelException {
        final ApiPlaylist playlist = testHelper().insertPlaylist();
        testHelper().insertPlaylistPost(playlist, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        final PropertySet playlistPost = createPlaylistPropertySet(playlist);

        verify(observer).onNext(playlistPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistReposts() throws CreateModelException {
        final ApiUser reposter = testHelper().insertUser();
        final ApiPlaylist playlist = testHelper().insertPlaylist();
        testHelper().insertPlaylistRepost(playlist, reposter, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        PropertySet playlistRepost = createPlaylistPropertySet(playlist)
                .put(PlayableProperty.REPOSTER, reposter.getUsername());

        verify(observer).onNext(playlistRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldIncludeLikesStateForPlaylistAndUser() throws CreateModelException {
        final ApiPlaylist playlist = testHelper().insertPlaylist();
        testHelper().insertPlaylistPost(playlist, TIMESTAMP);
        final int currentUserId = 123;
        testHelper().insertPlaylistLike(playlist.getId(), currentUserId);
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
        testHelper().insertComment();

        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    // we'll eventually refactor the underlying schema, but for now we need to make sure to exclude stuff
    // like comments and affiliations from here
    @Test
    public void loadingStreamItemsDoesNotIncludeAffiliations() throws CreateModelException {
        testHelper().insertAffiliation();

        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void loadingStreamItemsDoesNotIncludeOwnContentRepostedByOtherPeople() throws CreateModelException {
        final ApiUser reposter = testHelper().insertUser();
        final ApiTrack track = testHelper().insertTrack();
        testHelper().insertTrackRepostOfOwnTrack(track, reposter, TIMESTAMP);

        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void loadingStreamItemsTakesIntoAccountTheGivenLimit() throws CreateModelException {
        final ApiTrack firstTrack = testHelper().insertTrack();
        testHelper().insertTrackPost(firstTrack, TIMESTAMP);
        testHelper().insertTrackPost(testHelper().insertTrack(), TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 1).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).toEqual(firstTrack.getUrn());
    }

    @Test
    public void loadingStreamItemsOnlyLoadsItemsOlderThanTheGivenTimestamp() throws CreateModelException {
        testHelper().insertTrackPost(testHelper().insertTrack(), TIMESTAMP);
        final ApiTrack oldestTrack = testHelper().insertTrack();
        testHelper().insertTrackPost(oldestTrack, TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.streamItemsBefore(TIMESTAMP, Urn.forUser(123), 50).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).toEqual(oldestTrack.getUrn());
    }

    @Test
    public void shouldExcludeOrphanedRecordsInActivityView() throws CreateModelException {
        final ApiTrack deletedTrack = testHelper().insertTrack();
        testHelper().insertTrackPost(deletedTrack, TIMESTAMP);
        testHelper().insertTrackPost(testHelper().insertTrack(), TIMESTAMP);
        propeller().delete(Table.SOUNDS.name, new WhereBuilder().whereEq(TableColumns.Sounds._ID, deletedTrack.getId()));

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).not.toEqual(deletedTrack.getUrn());
    }

    @Test
    public void trackUrnsLoadsUrnsOfAllTrackItemsInSoundStream() throws CreateModelException {
        final ApiTrack trackOne = testHelper().insertTrack();
        testHelper().insertTrackPost(trackOne, TIMESTAMP);
        final ApiTrack trackTwo = testHelper().insertTrack();
        testHelper().insertTrackRepost(trackTwo, testHelper().insertUser(), TIMESTAMP - 1);
        testHelper().insertPlaylistPost(testHelper().insertPlaylist(), TIMESTAMP - 2);

        TestObserver<TrackUrn> observer = new TestObserver<TrackUrn>();
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
