package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PlaylistProperty;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserSummary;
import com.soundcloud.android.robolectric.DatabaseHelper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.DatabaseManager;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import android.database.sqlite.SQLiteDatabase;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamStorageTest {

    private static final long TIMESTAMP = 1000L;

    private SoundStreamStorage storage;

    @Mock
    private Observer<PropertySet> observer;

    private SQLiteDatabase database = new DatabaseManager(Robolectric.application).getWritableDatabase();
    private DatabaseHelper helper = new DatabaseHelper(database);

    @Before
    public void setup() {
        storage = new SoundStreamStorage(database, Schedulers.immediate());
    }

    @Test
    public void loadingStreamItemsIncludesTrackPosts() throws CreateModelException {
        final TrackSummary track = helper.insertTrack();
        helper.insertTrackPost(track, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        final PropertySet trackPost = createTrackPropertySet(track);

        verify(observer).onNext(trackPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesTrackReposts() throws CreateModelException {
        final UserSummary reposter = helper.insertUser();
        final TrackSummary track = helper.insertTrack();
        helper.insertTrackRepost(track, reposter, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        final PropertySet trackRepost = createTrackPropertySet(track)
                .put(PlayableProperty.REPOSTER, reposter.getUsername());

        verify(observer).onNext(trackRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistPosts() throws CreateModelException {
        final PlaylistSummary playlist = helper.insertPlaylist();
        helper.insertPlaylistPost(playlist, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        final PropertySet playlistPost = createPlaylistPropertySet(playlist);

        verify(observer).onNext(playlistPost);
        verify(observer).onCompleted();
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistReposts() throws CreateModelException {
        final UserSummary reposter = helper.insertUser();
        final PlaylistSummary playlist = helper.insertPlaylist();
        helper.insertPlaylistRepost(playlist, reposter, TIMESTAMP);
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);
        PropertySet playlistRepost = createPlaylistPropertySet(playlist)
                .put(PlayableProperty.REPOSTER, reposter.getUsername());

        verify(observer).onNext(playlistRepost);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldIncludeLikesStateForPlaylistAndUser() throws CreateModelException {
        final PlaylistSummary playlist = helper.insertPlaylist();
        helper.insertPlaylistPost(playlist, TIMESTAMP);
        final int currentUserId = 123;
        helper.insertPlaylistLike(playlist.getId(), currentUserId);
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
        helper.insertComment();

        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    // we'll eventually refactor the underlying schema, but for now we need to make sure to exclude stuff
    // like comments and affiliations from here
    @Test
    public void loadingStreamItemsDoesNotIncludeAffiliations() throws CreateModelException {
        helper.insertAffiliation();

        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void loadingStreamItemsDoesNotIncludeOwnContentRepostedByOtherPeople() throws CreateModelException {
        final UserSummary reposter = helper.insertUser();
        final TrackSummary track = helper.insertTrack();
        helper.insertTrackRepostOfOwnTrack(track, reposter, TIMESTAMP);

        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 50).subscribe(observer);

        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void loadingStreamItemsTakesIntoAccountTheGivenLimit() throws CreateModelException {
        final TrackSummary firstTrack = helper.insertTrack();
        helper.insertTrackPost(firstTrack, TIMESTAMP);
        helper.insertTrackPost(helper.insertTrack(), TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.streamItemsBefore(Long.MAX_VALUE, Urn.forUser(123), 1).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).toEqual(firstTrack.getUrn());
    }

    @Test
    public void loadingStreamItemsOnlyLoadsItemsOlderThanTheGivenTimestamp() throws CreateModelException {
        helper.insertTrackPost(helper.insertTrack(), TIMESTAMP);
        final TrackSummary oldestTrack = helper.insertTrack();
        helper.insertTrackPost(oldestTrack, TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.streamItemsBefore(TIMESTAMP, Urn.forUser(123), 50).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(PlayableProperty.URN)).toEqual(oldestTrack.getUrn());
    }

    @Test
    public void trackUrnsLoadsUrnsOfAllTrackItemsInSoundStream() throws CreateModelException {
        final TrackSummary trackOne = helper.insertTrack();
        helper.insertTrackPost(trackOne, TIMESTAMP);
        final TrackSummary trackTwo = helper.insertTrack();
        helper.insertTrackRepost(trackTwo, helper.insertUser(), TIMESTAMP - 1);
        helper.insertPlaylistPost(helper.insertPlaylist(), TIMESTAMP - 2);

        TestObserver<TrackUrn> observer = new TestObserver<TrackUrn>();
        storage.trackUrns().subscribe(observer);
        expect(observer.getOnNextEvents()).toContainExactly(trackOne.getUrn(), trackTwo.getUrn());
    }

    private PropertySet createTrackPropertySet(final TrackSummary track) throws CreateModelException {
        return PropertySet.from(
                PlayableProperty.URN.bind(Urn.forTrack(track.getId())),
                PlayableProperty.TITLE.bind(track.getTitle()),
                PlayableProperty.DURATION.bind(track.getDuration()),
                PlayableProperty.CREATED_AT.bind(new Date(TIMESTAMP)),
                PlayableProperty.CREATOR.bind(track.getUser().getUsername()),
                TrackProperty.PLAY_COUNT.bind(track.getStats().getPlaybackCount()));
    }

    private PropertySet createPlaylistPropertySet(PlaylistSummary playlist) {
        return PropertySet.from(
                PlayableProperty.URN.bind(Urn.forPlaylist(playlist.getId())),
                PlayableProperty.TITLE.bind(playlist.getTitle()),
                PlayableProperty.DURATION.bind(playlist.getDuration()),
                PlayableProperty.CREATED_AT.bind(new Date(TIMESTAMP)),
                PlayableProperty.CREATOR.bind(playlist.getUser().getUsername()),
                PlayableProperty.LIKES_COUNT.bind(playlist.getStats().getLikesCount()),
                PlayableProperty.IS_LIKED.bind(false),
                PlaylistProperty.TRACK_COUNT.bind(playlist.getTrackCount()));
    }

}
