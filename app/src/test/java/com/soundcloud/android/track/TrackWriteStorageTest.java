package com.soundcloud.android.track;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.UserSummary;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.StorageIntegrationTest;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.Query;
import com.soundcloud.propeller.TxnResult;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackWriteStorageTest extends StorageIntegrationTest {

    private TrackWriteStorage storage;

    @Before
    public void setup() {
        storage = new TrackWriteStorage(testScheduler());
    }

    @Test
    public void shouldStoreTrackMetadataFromApiMobileTrack() throws CreateModelException {
        TestObserver<TxnResult> observer = new TestObserver<TxnResult>();
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);

        storage.storeTrackAsync(track).subscribe(observer);

        expectTrackInserted(track);
    }

    @Test
    public void shouldStoreUserMetadataFromApiMobileTrack() throws CreateModelException {
        TestObserver<TxnResult> observer = new TestObserver<TxnResult>();
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);

        storage.storeTrackAsync(track).subscribe(observer);

        expectUserInserted(track.getUser());
    }

    @Test
    public void storingApiMobileTrackEmitsTransactionResult() throws CreateModelException {
        TestObserver<TxnResult> observer = new TestObserver<TxnResult>();
        TrackSummary track = TestHelper.getModelFactory().createModel(TrackSummary.class);

        storage.storeTrackAsync(track).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnCompletedEvents()).toNumber(1);
        TxnResult result = observer.getOnNextEvents().get(0);
        expect(result.success()).toBeTrue();
        expect(((ChangeResult) result.getResults().get(0)).getNumRowsAffected()).toEqual(1);
    }

    @Test
    public void shouldStoreTrackMetadataFromListOfApiMobileTracks() throws CreateModelException {
        TestObserver<TxnResult> observer = new TestObserver<TxnResult>();
        final List<TrackSummary> tracks = Arrays.asList(
                TestHelper.getModelFactory().createModel(TrackSummary.class),
                TestHelper.getModelFactory().createModel(TrackSummary.class));

        storage.storeTracksAsync(tracks).subscribe(observer);

        expectTrackInserted(tracks.get(0));
        expectTrackInserted(tracks.get(1));
    }

    @Test
    public void shouldStoreUserMetadataFromListOfApiMobileTracks() throws CreateModelException {
        TestObserver<TxnResult> observer = new TestObserver<TxnResult>();
        final List<TrackSummary> tracks = Arrays.asList(
                TestHelper.getModelFactory().createModel(TrackSummary.class),
                TestHelper.getModelFactory().createModel(TrackSummary.class));

        storage.storeTracksAsync(tracks).subscribe(observer);

        expectUserInserted(tracks.get(0).getUser());
        expectUserInserted(tracks.get(1).getUser());
    }

    @Test
    public void storingListOfApiMobileTracksEmitsTransactionResult() throws CreateModelException {
        TestObserver<TxnResult> observer = new TestObserver<TxnResult>();
        final List<TrackSummary> tracks = Arrays.asList(
                TestHelper.getModelFactory().createModel(TrackSummary.class),
                TestHelper.getModelFactory().createModel(TrackSummary.class));

        storage.storeTracksAsync(tracks).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnCompletedEvents()).toNumber(1);
        TxnResult result = observer.getOnNextEvents().get(0);
        expect(result.success()).toBeTrue();
        expect(result.getResults()).toNumber(4);
    }

    private void expectTrackInserted(TrackSummary track) {
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
                        .whereEq(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount())
                        .whereEq(TableColumns.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount())
                        .whereEq(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount())
                        .whereEq(TableColumns.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount())
        )).toBeTrue();
    }

    private void expectUserInserted(UserSummary user) {
        expect(exists(Query.from(Table.SOUND_VIEW.name)
                        .whereEq(TableColumns.SoundView.USER_ID, user.getId())
                        .whereEq(TableColumns.SoundView.USERNAME, user.getUsername())
        )).toBeTrue();
    }
}