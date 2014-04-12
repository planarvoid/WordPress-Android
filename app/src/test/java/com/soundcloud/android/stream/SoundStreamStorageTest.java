package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.DatabaseHelper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.storage.provider.DBHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.observers.TestObserver;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamStorageTest {

    private SoundStreamStorage storage;

    @Mock
    private Observer<PropertySet> observer;

    private SQLiteDatabase database = new DBHelper(Robolectric.application).getWritableDatabase();
    private DatabaseHelper helper = new DatabaseHelper(database);

    @Before
    public void setup() {
        storage = new SoundStreamStorage(database);
    }

    @Test
    public void shouldLoadStreamItemsFromLocalStorage() throws CreateModelException {
        final TrackSummary track = helper.insertTrack();
        final long now = System.currentTimeMillis();
        helper.insertTrackPost(track, now);
        helper.insertTrackRepost(track, now - 1);
        final PlaylistSummary playlist = helper.insertPlaylist();
        helper.insertPlaylistPost(playlist, now - 2);
        helper.insertPlaylistRepost(playlist, now - 3);

        TestObserver<PropertySet> observer = new TestObserver<PropertySet>();
        storage.loadStreamItemsAsync(Urn.forUser(123)).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(4);

        List<PropertySet> expectedProps = Lists.newArrayList(
                PropertySet.from(
                        StreamItemProperty.SOUND_URN.bind("soundcloud:sounds:" + track.getId()),
                        StreamItemProperty.SOUND_TITLE.bind(track.getTitle()),
                        StreamItemProperty.REPOSTED.bind(false)),
                PropertySet.from(
                        StreamItemProperty.SOUND_URN.bind("soundcloud:sounds:" + track.getId()),
                        StreamItemProperty.SOUND_TITLE.bind(track.getTitle()),
                        StreamItemProperty.REPOSTED.bind(true)),
                PropertySet.from(
                        StreamItemProperty.SOUND_URN.bind("soundcloud:playlists:" + playlist.getId()),
                        StreamItemProperty.SOUND_TITLE.bind(playlist.getTitle()),
                        StreamItemProperty.REPOSTED.bind(false)),
                PropertySet.from(
                        StreamItemProperty.SOUND_URN.bind("soundcloud:playlists:" + playlist.getId()),
                        StreamItemProperty.SOUND_TITLE.bind(playlist.getTitle()),
                        StreamItemProperty.REPOSTED.bind(true))
        );
        observer.assertReceivedOnNext(expectedProps);
    }
}
