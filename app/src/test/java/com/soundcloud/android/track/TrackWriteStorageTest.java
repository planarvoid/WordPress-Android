package com.soundcloud.android.track;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.StorageIntegrationTest;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.Query;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

@RunWith(SoundCloudTestRunner.class)
public class TrackWriteStorageTest extends StorageIntegrationTest {

    private TrackWriteStorage storage;

    @Before
    public void setup() {
        storage = new TrackWriteStorage(testScheduler());
    }

    @Test
    public void shouldStoreTrackMetadataFromApiMobileTrack() throws CreateModelException {
        TestObserver<InsertResult> observer = new TestObserver<InsertResult>();
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);

        storage.storeTrack(track).subscribe(observer);

        expect(exists(Query.from(Table.SOUNDS.name)
                        .whereEq(TableColumns.Sounds._ID, track.getId())
                        .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .whereEq(TableColumns.Sounds.TITLE, track.getTitle())
                        .whereEq(TableColumns.Sounds.DURATION, track.getDuration())
                        .whereEq(TableColumns.Sounds.WAVEFORM_URL, track.getWaveformUrl())
                        .whereEq(TableColumns.Sounds.STREAM_URL, track.getStreamUrl())
                        .whereEq(TableColumns.Sounds.PERMALINK_URL, track.getPermalinkUrl())
                        .whereEq(TableColumns.Sounds.CREATED_AT, track.getCreatedAt().getTime())
                        .whereEq(TableColumns.Sounds.GENRE, track.getGenre())
                        .whereEq(TableColumns.Sounds.SHARING, track.getSharing().value())
                        .whereEq(TableColumns.Sounds.USER_ID, track.getUser().getId())
                        .whereEq(TableColumns.Sounds.COMMENTABLE, track.isCommentable())
                        .whereEq(TableColumns.Sounds.MONETIZABLE, track.isMonetizable())
        )).toBeTrue();
    }

    @Test
    public void shouldStoreTrackStatsFromApiMobileTrack() throws CreateModelException {
        TestObserver<InsertResult> observer = new TestObserver<InsertResult>();
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);

        storage.storeTrack(track).subscribe(observer);

        expect(exists(Query.from(Table.SOUNDS.name)
                        .whereEq(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount())
                        .whereEq(TableColumns.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount())
                        .whereEq(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount())
                        .whereEq(TableColumns.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount())
        )).toBeTrue();
    }

    @Test
    public void shouldStoreUserMetadataFromApiMobileTrack() throws CreateModelException {
        TestObserver<InsertResult> observer = new TestObserver<InsertResult>();
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);

        storage.storeTrack(track).subscribe(observer);

        expect(exists(Query.from(Table.SOUND_VIEW.name)
                        .whereEq(TableColumns.SoundView.USER_ID, track.getUser().getId())
                        .whereEq(TableColumns.SoundView.USERNAME, track.getUser().getUsername())
        )).toBeTrue();
    }

    @Test
    public void storingApiMobileTrackEmitsInsertResult() throws CreateModelException {
        TestObserver<InsertResult> observer = new TestObserver<InsertResult>();
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);

        storage.storeTrack(track).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnCompletedEvents()).toNumber(1);
        InsertResult result = observer.getOnNextEvents().get(0);
        expect(result.success()).toBeTrue();
    }
}