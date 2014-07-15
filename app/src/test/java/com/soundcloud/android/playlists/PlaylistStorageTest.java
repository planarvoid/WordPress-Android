package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.robolectric.DatabaseHelper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
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

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistStorageTest {


    private PlaylistStorage storage;
    private SQLiteDatabase sqliteDatabase = new DatabaseManager(Robolectric.application).getWritableDatabase();
    private PropellerDatabase database = new PropellerDatabase(sqliteDatabase);
    private DatabaseHelper helper = new DatabaseHelper(sqliteDatabase);

    @Mock
    private Observer<PropertySet> observer;

    @Before
    public void setUp() throws Exception {
        storage = new PlaylistStorage(database, Schedulers.immediate());

    }

    @Test
    public void trackUrnsLoadsUrnsOfAllTrackItemsInAGivenPlaylist() throws CreateModelException {
        ApiPlaylist apiPlaylist = helper.insertPlaylist();
        List<ApiTrack> apiTrackList = Lists.newArrayListWithCapacity(3);
        for (int i = 0; i < 3; i++) {
            apiTrackList.add(helper.insertPlaylistTrack(apiPlaylist, i));
        }

        TestObserver<TrackUrn> observer = new TestObserver<TrackUrn>();
        storage.trackUrns(apiPlaylist.getUrn()).subscribe(observer);
        expect(observer.getOnNextEvents()).toContainExactly(
                apiTrackList.get(0).getUrn(), apiTrackList.get(1).getUrn(), apiTrackList.get(2).getUrn()
        );
    }

}