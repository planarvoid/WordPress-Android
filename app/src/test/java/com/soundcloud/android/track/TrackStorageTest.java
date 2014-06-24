package com.soundcloud.android.track;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.Urn;
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
import rx.schedulers.Schedulers;

import android.database.sqlite.SQLiteDatabase;

@RunWith(SoundCloudTestRunner.class)
public class TrackStorageTest {

    private TrackStorage storage;

    @Mock
    private Observer<PropertySet> observer;

    private SQLiteDatabase sqliteDatabase = new DatabaseManager(Robolectric.application).getWritableDatabase();
    private PropellerDatabase database = new PropellerDatabase(sqliteDatabase);
    private DatabaseHelper helper = new DatabaseHelper(sqliteDatabase);

    @Before
    public void setup() {
        storage = new TrackStorage(database, Schedulers.immediate());
    }

    @Test
    public void trackByUrnEmitsInsertedTrack() throws CreateModelException {

        final TrackSummary track = helper.insertTrack();
        storage.track(track.getUrn(), Urn.forUser(123)).subscribe(observer);
        final PropertySet trackPropertySet = createTrackPropertySet(track);

        verify(observer).onNext(trackPropertySet);
        verify(observer).onCompleted();
    }

    private PropertySet createTrackPropertySet(final TrackSummary track) throws CreateModelException {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(track.getId())),
                PlayableProperty.TITLE.bind(track.getTitle()),
                PlayableProperty.DURATION.bind(track.getDuration()),
                PlayableProperty.CREATOR_NAME.bind(track.getUser().getUsername()),
                TrackProperty.PLAY_COUNT.bind(track.getStats().getPlaybackCount()),
                PlayableProperty.LIKES_COUNT.bind(track.getStats().getLikesCount()));
    }
}
