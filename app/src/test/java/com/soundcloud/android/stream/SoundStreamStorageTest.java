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
        final long timestamp = System.currentTimeMillis();
        helper.insertTrackPost(track, timestamp);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123)).subscribe(observer);

        final PropertySet trackPost = PropertySet.from(
                StreamItemProperty.SOUND_URN.bind("soundcloud:sounds:" + track.getId()),
                StreamItemProperty.SOUND_TITLE.bind(track.getTitle()),
                StreamItemProperty.CREATED_AT.bind(new Date(timestamp)),
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
        final long timestamp = System.currentTimeMillis();
        helper.insertTrackRepost(track, reposter, timestamp);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123)).subscribe(observer);

        final PropertySet trackRepost = PropertySet.from(
                StreamItemProperty.SOUND_URN.bind("soundcloud:sounds:" + track.getId()),
                StreamItemProperty.SOUND_TITLE.bind(track.getTitle()),
                StreamItemProperty.CREATED_AT.bind(new Date(timestamp)),
                StreamItemProperty.POSTER.bind(reposter.getUsername()),
                StreamItemProperty.REPOST.bind(true));

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnCompletedEvents()).toNumber(1);
        observer.assertReceivedOnNext(Arrays.asList(trackRepost));
    }

    @Test
    public void loadingStreamItemsIncludesPlaylistPosts() throws CreateModelException {
        final PlaylistSummary playlist = helper.insertPlaylist();
        final long timestamp = System.currentTimeMillis();
        helper.insertPlaylistPost(playlist, timestamp);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123)).subscribe(observer);

        final PropertySet playlistPost = PropertySet.from(
                StreamItemProperty.SOUND_URN.bind("soundcloud:playlists:" + playlist.getId()),
                StreamItemProperty.SOUND_TITLE.bind(playlist.getTitle()),
                StreamItemProperty.CREATED_AT.bind(new Date(timestamp)),
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
        final long timestamp = System.currentTimeMillis();
        helper.insertPlaylistRepost(playlist, reposter, timestamp);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123)).subscribe(observer);

        final PropertySet playlistRepost = PropertySet.from(
                StreamItemProperty.SOUND_URN.bind("soundcloud:playlists:" + playlist.getId()),
                StreamItemProperty.SOUND_TITLE.bind(playlist.getTitle()),
                StreamItemProperty.CREATED_AT.bind(new Date(timestamp)),
                StreamItemProperty.POSTER.bind(reposter.getUsername()),
                StreamItemProperty.REPOST.bind(true));

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnCompletedEvents()).toNumber(1);
        observer.assertReceivedOnNext(Arrays.asList(playlistRepost));
    }
}
