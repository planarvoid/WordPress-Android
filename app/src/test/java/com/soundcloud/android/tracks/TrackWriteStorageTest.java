package com.soundcloud.android.tracks;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackWriteStorageTest extends StorageIntegrationTest {

    private TrackWriteStorage storage;
    private TestObserver<TxnResult> observer;
    private ApiTrack track;

    @Before
    public void setup() {
        storage = new TrackWriteStorage(propeller(), Schedulers.immediate());
        observer = new TestObserver<>();
        track = ModelFixtures.create(ApiTrack.class);
    }

    @Test
    public void shouldStoreTrackMetadataFromApiMobileTrack() {
        storage.storeTrackAsync(track).subscribe(observer);
        expectTrackInserted(track);
    }

    @Test
    public void shouldStoreUserMetadataFromApiMobileTrack() {
        storage.storeTrackAsync(track).subscribe(observer);
        expectUserInserted(track.getUser());
    }

    @Test
    public void storingApiMobileTrackEmitsTransactionResult() {
        storage.storeTrackAsync(track).subscribe(observer);

        assertThat(observer.getOnNextEvents().size(), is(1));
        assertThat(observer.getOnCompletedEvents().size(), is(1));

        TxnResult result = observer.getOnNextEvents().get(0);
        assertThat(result.success(), is(true));
        assertThat(((ChangeResult) result.getResults().get(0)).getNumRowsAffected(), is(1));
    }

    @Test
    public void shouldStoreTrackMetadataFromListOfApiMobileTracks() {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);

        storage.storeTracksAsync(tracks).subscribe(observer);

        expectTrackInserted(tracks.get(0));
        expectTrackInserted(tracks.get(1));
    }

    @Test
    public void shouldStoreUserMetadataFromListOfApiMobileTracks() {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);

        storage.storeTracksAsync(tracks).subscribe(observer);

        expectUserInserted(tracks.get(0).getUser());
        expectUserInserted(tracks.get(1).getUser());
    }

    @Test
    public void shouldSyncStoreTrackMetadataFromListOfApiMobileTrack() {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);

        storage.storeTracks(tracks);

        expectTrackInserted(tracks.get(0));
        expectTrackInserted(tracks.get(1));
    }

    @Test
    public void shouldSyncStoreUserMetadataFromListOfApiMobileTracks() {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);

        storage.storeTracks(tracks);

        expectUserInserted(tracks.get(0).getUser());
        expectUserInserted(tracks.get(1).getUser());
    }

    @Test
    public void shouldStoreListOfPolicies() {
        List<PolicyInfo> policies = new ArrayList<>();
        policies.add(new PolicyInfo(Urn.forTrack(1L), true, "allowed"));
        policies.add(new PolicyInfo(Urn.forTrack(2L), false, "monetizable"));
        policies.add(new PolicyInfo(Urn.forTrack(3L), true, "something"));


        storage.storePoliciesAsync(policies).subscribe(observer);

        expectPolicyInserted(Urn.forTrack(1L), true, "allowed");
        expectPolicyInserted(Urn.forTrack(2L), false, "monetizable");
        expectPolicyInserted(Urn.forTrack(3L), true, "something");
    }

    @Test
    public void storingListOfApiMobileTracksEmitsTransactionResult() {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);

        storage.storeTracksAsync(tracks).subscribe(observer);

        assertThat(observer.getOnNextEvents().size(), is(1));
        assertThat(observer.getOnCompletedEvents().size(), is(1));

        TxnResult result = observer.getOnNextEvents().get(0);
        assertThat(result.success(), is(true));
        assertThat(result.getResults().size(), is(4));
    }

    @Test
    public void storingListOfPoliciesEmitsTransactionResult() {
        List<PolicyInfo> policies = new ArrayList<PolicyInfo>();
        policies.add(new PolicyInfo(Urn.forTrack(1L), true, "allowed"));
        policies.add(new PolicyInfo(Urn.forTrack(2L), false, "monetizable"));

        storage.storePoliciesAsync(policies).subscribe(observer);

        assertThat(observer.getOnNextEvents().size(), is(1));
        assertThat(observer.getOnCompletedEvents().size(), is(1));

        TxnResult result = observer.getOnNextEvents().get(0);
        assertThat(result.success(), is(true));
        assertThat(result.getResults().size(), is(2));
    }

    private void expectTrackInserted(ApiTrack track) {
        assertThat(select(from(Table.SOUNDS.name)
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
        ), counts(1));
    }

    private void expectUserInserted(ApiUser user) {
        assertThat(select(from(Table.SOUND_VIEW.name)
                        .whereEq(TableColumns.SoundView.USER_ID, user.getId())
                        .whereEq(TableColumns.SoundView.USERNAME, user.getUsername())
        ), counts(1));
    }

    private void expectPolicyInserted(Urn trackUrn, boolean monetizable, String policy) {
        assertThat(select(from(Table.SOUND_VIEW.name)
                        .whereEq(TableColumns.SoundView._ID, trackUrn.getNumericId())
                        .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .whereEq(TableColumns.SoundView.MONETIZABLE, monetizable)
                        .whereEq(TableColumns.SoundView.POLICY, policy)
        ), counts(1));
    }
}