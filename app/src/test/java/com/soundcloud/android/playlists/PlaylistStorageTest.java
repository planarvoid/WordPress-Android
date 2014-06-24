package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.TrackUrn;
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
        PlaylistSummary playlistSummary = helper.insertPlaylist();
        List<TrackSummary> trackSummaryList = Lists.newArrayListWithCapacity(3);
        for (int i = 0; i < 3; i++) {
            trackSummaryList.add(helper.insertPlaylistTrack(playlistSummary, i));
        }

        TestObserver<TrackUrn> observer = new TestObserver<TrackUrn>();
        storage.trackUrns(playlistSummary.getUrn()).subscribe(observer);
        expect(observer.getOnNextEvents()).toContainExactly(
                trackSummaryList.get(0).getUrn(), trackSummaryList.get(1).getUrn(), trackSummaryList.get(2).getUrn()
        );
    }

}