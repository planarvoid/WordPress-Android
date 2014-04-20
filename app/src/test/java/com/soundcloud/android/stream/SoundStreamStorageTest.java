package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserSummary;
import com.soundcloud.android.robolectric.DatabaseHelper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.PropertySet;
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

import java.util.Arrays;
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

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123), TIMESTAMP, 50, 0).subscribe(observer);

        final PropertySet trackPost = PropertySet.from(
                StreamItemProperty.SOUND_URN.bind(Urn.forTrack(track.getId())),
                StreamItemProperty.SOUND_TITLE.bind(track.getTitle()),
                StreamItemProperty.CREATED_AT.bind(new Date(TIMESTAMP)),
                StreamItemProperty.POSTER.bind(track.getUser().getUsername()),
                StreamItemProperty.REPOST.bind(false));

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnCompletedEvents()).toNumber(1);
        observer.assertReceivedOnNext(Arrays.asList(trackPost));
    }

    @Test
    public void loadingStreamItemsIncludesTrackReposts() throws CreateModelException {
        final UserSummary reposter = helper.insertUser();
        final TrackSummary track = helper.insertTrack();
        helper.insertTrackRepost(track, reposter, TIMESTAMP);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123), TIMESTAMP, 50, 0).subscribe(observer);

        final PropertySet trackRepost = PropertySet.from(
                StreamItemProperty.SOUND_URN.bind(Urn.forTrack(track.getId())),
                StreamItemProperty.SOUND_TITLE.bind(track.getTitle()),
                StreamItemProperty.CREATED_AT.bind(new Date(TIMESTAMP)),
                StreamItemProperty.POSTER.bind(reposter.getUsername()),
                StreamItemProperty.REPOST.bind(true));

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnCompletedEvents()).toNumber(1);
        observer.assertReceivedOnNext(Arrays.asList(trackRepost));
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistPosts() throws CreateModelException {
        final PlaylistSummary playlist = helper.insertPlaylist();
        helper.insertPlaylistPost(playlist, TIMESTAMP);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123), TIMESTAMP, 50, 0).subscribe(observer);

        final PropertySet playlistPost = PropertySet.from(
                StreamItemProperty.SOUND_URN.bind(Urn.forPlaylist(playlist.getId())),
                StreamItemProperty.SOUND_TITLE.bind(playlist.getTitle()),
                StreamItemProperty.CREATED_AT.bind(new Date(TIMESTAMP)),
                StreamItemProperty.POSTER.bind(playlist.getUser().getUsername()),
                StreamItemProperty.REPOST.bind(false));

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnCompletedEvents()).toNumber(1);
        observer.assertReceivedOnNext(Arrays.asList(playlistPost));
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistReposts() throws CreateModelException {
        final UserSummary reposter = helper.insertUser();
        final PlaylistSummary playlist = helper.insertPlaylist();
        helper.insertPlaylistRepost(playlist, reposter, TIMESTAMP);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123), TIMESTAMP, 50, 0).subscribe(observer);

        final PropertySet playlistRepost = PropertySet.from(
                StreamItemProperty.SOUND_URN.bind(Urn.forPlaylist(playlist.getId())),
                StreamItemProperty.SOUND_TITLE.bind(playlist.getTitle()),
                StreamItemProperty.CREATED_AT.bind(new Date(TIMESTAMP)),
                StreamItemProperty.POSTER.bind(reposter.getUsername()),
                StreamItemProperty.REPOST.bind(true));

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnCompletedEvents()).toNumber(1);
        observer.assertReceivedOnNext(Arrays.asList(playlistRepost));
    }

    @Test
    public void shouldLoadFirstPageViaLimitAndOffset() throws CreateModelException {
        final TrackSummary postedTrack = helper.insertTrack();
        helper.insertTrackPost(postedTrack, TIMESTAMP);
        final TrackSummary repostedTrack = helper.insertTrack();
        helper.insertTrackRepost(repostedTrack, helper.insertUser(), TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123), TIMESTAMP, 1, 0).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(StreamItemProperty.SOUND_URN)).toEqual(postedTrack.getUrn());
    }

    @Test
    public void shouldLoadNextPageViaLimitAndOffset() throws CreateModelException {
        final TrackSummary postedTrack = helper.insertTrack();
        helper.insertTrackPost(postedTrack, TIMESTAMP);
        final TrackSummary repostedTrack = helper.insertTrack();
        helper.insertTrackRepost(repostedTrack, helper.insertUser(), TIMESTAMP - 1);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123), TIMESTAMP, 1, 1).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(StreamItemProperty.SOUND_URN)).toEqual(repostedTrack.getUrn());
    }

    @Test
    public void shouldOnlyReturnRecordsFromOrBeforeTheGivenTimestamp() throws CreateModelException {
        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        helper.insertTrackPost(helper.insertTrack(), TIMESTAMP + 1);
        final TrackSummary expectedTrack = helper.insertTrack();
        helper.insertTrackPost(expectedTrack, TIMESTAMP);

        storage.loadStreamItemsAsync(Urn.forUser(123), TIMESTAMP, 2, 0).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).get(StreamItemProperty.SOUND_URN)).toEqual(expectedTrack.getUrn());
    }

}
