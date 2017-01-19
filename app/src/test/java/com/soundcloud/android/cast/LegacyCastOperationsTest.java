package com.soundcloud.android.cast;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.optional.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import android.content.res.Resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LegacyCastOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);
    private static final List<Urn> PLAY_QUEUE = newArrayList(TRACK1, TRACK2);
    private static final String IMAGE_URL = "http://image.url";

    private LegacyCastOperations castOperations;

    @Mock private VideoCastManager videoCastManager;
    @Mock private TrackRepository trackRepository;
    @Mock private PolicyOperations policyOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private Resources resources;

    private TestObserver<LocalPlayQueue> observer;

    @Before
    public void setUp() throws Exception {
        castOperations = new LegacyCastOperations(videoCastManager,
                                                  trackRepository,
                                                  policyOperations,
                                                  imageOperations,
                                                  resources,
                                                  Schedulers.immediate());
        observer = new TestObserver<>();
        when(imageOperations.getUrlForLargestImage(resources, TRACK1)).thenReturn(IMAGE_URL);
    }

    @Test
    public void loadsLocalPlayQueue() throws JSONException {
        Track currentTrack = createAndSetupPublicTrack(TRACK1);
        List<Urn> playQueueTracks = Arrays.asList(TRACK1, TRACK2);

        castOperations.loadLocalPlayQueue(TRACK1, playQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, playQueueTracks);
    }

    @Test
    public void loadsLocalPlayQueueWithoutMonetizableTracks() throws JSONException {
        createAndSetupPublicTrack(TRACK2);
        Track currentTrack = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        List<Urn> filteredPlayQueueTracks = Arrays.asList(TRACK1);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(
                filteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(TRACK1, unfilteredPlayQueueTracks)
                      .subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, filteredPlayQueueTracks);
    }

    @Test
    public void loadsLocalPlayQueueWithoutPrivateTracks() throws JSONException {
        createAndSetupPrivateTrack(TRACK2);
        Track currentTrack = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(
                unfilteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(TRACK1, unfilteredPlayQueueTracks)
                      .subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        List<Urn> expectedFilteredPlayQueueTracks = Arrays.asList(TRACK1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, expectedFilteredPlayQueueTracks);
    }

    @Test
    public void loadLocalPlayQueueWithoutMonetizableTracksCorrectsCurrentTrackWhenItIsFilteredOut() throws JSONException {
        createAndSetupPublicTrack(TRACK2);
        Track currentTrackBeforeFiltering = createAndSetupPublicTrack(TRACK3);
        Track currentTrackAfterFiltering = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2, TRACK3);
        List<Urn> filteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(
                filteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(currentTrackBeforeFiltering.urn(),
                                                                            unfilteredPlayQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrackAfterFiltering, filteredPlayQueueTracks);
    }

    @Test
    public void loadLocalPlayQueueWithoutPrivateTracksCorrectsCurrentTrackWhenItIsFilteredOut() throws JSONException {
        Track currentTrackBeforeFiltering = createAndSetupPrivateTrack(TRACK3);
        createAndSetupPublicTrack(TRACK2);
        Track currentTrackAfterFiltering = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2, TRACK3);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(
                unfilteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(currentTrackBeforeFiltering.urn(),
                                                                            unfilteredPlayQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        List<Urn> expectedFilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0),
                             currentTrackAfterFiltering,
                             expectedFilteredPlayQueueTracks);
    }

    @Test
    public void loadFilteredLocalPlayQueueEmitsEmptyLocalQueueWhenAllTracksAreFilteredOut() {
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1);
        List<Urn> filteredPlayQueueTracks = Collections.emptyList();
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(
                filteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(TRACK1, unfilteredPlayQueueTracks)
                      .subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isEmpty()).isTrue();
    }

    @Test
    public void loadRemotePlayQueueReturnsEmptyQueueOnNetworkError() throws TransientNetworkDisconnectionException, NoConnectionException {
        Mockito.doThrow(new TransientNetworkDisconnectionException())
               .when(videoCastManager)
               .getRemoteMediaInformation();

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        assertThat(remotePlayQueue).isEqualTo(RemotePlayQueue.create(Collections.emptyList(), Urn.NOT_SET));
    }

    @Test
    public void loadRemotePlayQueueReturnsEmptyQueueOnConnectionError() throws TransientNetworkDisconnectionException, NoConnectionException {
        Mockito.doThrow(new NoConnectionException()).when(videoCastManager).getRemoteMediaInformation();

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        assertThat(remotePlayQueue).isEqualTo(RemotePlayQueue.create(Collections.emptyList(), Urn.NOT_SET));
    }

    @Test
    public void loadsRemotePlayQueue() throws TransientNetworkDisconnectionException, NoConnectionException, JSONException {
        MediaInfo mediaInfo = createMediaInfo(PLAY_QUEUE, TRACK2);
        when(videoCastManager.getRemoteMediaInformation()).thenReturn(mediaInfo);

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        assertThat(remotePlayQueue).isEqualTo(RemotePlayQueue.create(PLAY_QUEUE, TRACK2));
    }

    private MediaInfo createMediaInfo(List<Urn> playQueue, Urn currentTrack) throws JSONException {
        JSONObject customData = new JSONObject()
                .put("play_queue", new JSONArray(Urns.toString(playQueue)));

        MediaMetadata metadata = new MediaMetadata();
        metadata.putString("urn", currentTrack.toString());

        return new MediaInfo.Builder("contentId123")
                .setCustomData(customData)
                .setMetadata(metadata)
                .setContentType("contentType123")
                .setStreamType(0)
                .build();
    }

    private Track createAndSetupPrivateTrack(Urn urn) {
        return createTrack(urn, true);
    }

    private Track createAndSetupPublicTrack(Urn urn) {
        return createTrack(urn, false);
    }

    private Track createTrack(Urn urn, boolean isPrivate) {
        Track track = ModelFixtures.trackBuilder().urn(urn).title("Title " + urn).creatorName(Optional.of("Creator " + urn)).isPrivate(isPrivate).build();
        when(trackRepository.track(urn)).thenReturn(Observable.just(track));
        return track;
    }

    private void expectLocalPlayQueue(LocalPlayQueue localPlayQueue,
                                      Track currentTrack,
                                      List<Urn> playQueueTracks) throws JSONException {
        assertThat(localPlayQueue.currentTrackUrn).isEqualTo(currentTrack.urn());
        assertThat(localPlayQueue.playQueueTrackUrns).isEqualTo(playQueueTracks);
        assertThat(localPlayQueue.playQueueTracksJSON.get("play_queue").toString())
                .isEqualTo(new JSONArray(Urns.toString(playQueueTracks)).toString());

        MediaInfo mediaInfo = localPlayQueue.mediaInfo;
        assertThat(mediaInfo.getContentType()).isEqualTo("audio/mpeg");
        assertThat(mediaInfo.getStreamType()).isEqualTo(MediaInfo.STREAM_TYPE_BUFFERED);

        MediaMetadata metadata = mediaInfo.getMetadata();
        assertThat(metadata.getMediaType()).isEqualTo(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        assertThat(metadata.getString(MediaMetadata.KEY_TITLE)).isEqualTo(currentTrack.title());
        assertThat(metadata.getString(MediaMetadata.KEY_ARTIST)).isEqualTo(currentTrack.creatorName().get());
        assertThat(metadata.getString("urn")).isEqualTo(currentTrack.urn().toString());
        assertThat(metadata.getImages().get(0).getUrl().toString()).isEqualTo(IMAGE_URL);
    }
}
