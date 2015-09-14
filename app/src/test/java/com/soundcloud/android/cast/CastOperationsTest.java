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
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.java.collections.PropertySet;
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

public class CastOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);
    private static final List<Urn> PLAY_QUEUE = newArrayList(TRACK1, TRACK2);
    private static final String IMAGE_URL = "http://image.url";

    private CastOperations castOperations;

    @Mock private VideoCastManager videoCastManager;
    @Mock private TrackRepository trackRepository;
    @Mock private PolicyOperations policyOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private Resources resources;

    private TestObserver<LocalPlayQueue> observer;

    @Before
    public void setUp() throws Exception {
        castOperations = new CastOperations(videoCastManager,
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
        PropertySet currentTrack = createAndSetupPublicTrack(TRACK1);
        List<Urn> playQueueTracks = Arrays.asList(TRACK1, TRACK2);

        castOperations.loadLocalPlayQueue(TRACK1, playQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, playQueueTracks);
    }

    @Test
    public void loadsLocalPlayQueueWithoutMonetizableTracks() throws JSONException {
        createAndSetupPublicTrack(TRACK2);
        PropertySet currentTrack = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        List<Urn> filteredPlayQueueTracks = Arrays.asList(TRACK1);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(filteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(TRACK1, unfilteredPlayQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, filteredPlayQueueTracks);
    }

    @Test
    public void loadsLocalPlayQueueWithoutPrivateTracks() throws JSONException {
        createAndSetupPrivateTrack(TRACK2);
        PropertySet currentTrack = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(unfilteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(TRACK1, unfilteredPlayQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        List<Urn> expectedFilteredPlayQueueTracks = Arrays.asList(TRACK1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, expectedFilteredPlayQueueTracks);
    }

    @Test
    public void loadLocalPlayQueueWithoutMonetizableTracksCorrectsCurrentTrackWhenItIsFilteredOut() throws JSONException {
        createAndSetupPublicTrack(TRACK2);
        PropertySet currentTrackBeforeFiltering = createAndSetupPublicTrack(TRACK3);
        PropertySet currentTrackAfterFiltering = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2, TRACK3);
        List<Urn> filteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(filteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(currentTrackBeforeFiltering.get(TrackProperty.URN), unfilteredPlayQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrackAfterFiltering, filteredPlayQueueTracks);
    }

    @Test
    public void loadLocalPlayQueueWithoutPrivateTracksCorrectsCurrentTrackWhenItIsFilteredOut() throws JSONException {
        PropertySet currentTrackBeforeFiltering = createAndSetupPrivateTrack(TRACK3);
        createAndSetupPublicTrack(TRACK2);
        PropertySet currentTrackAfterFiltering = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2, TRACK3);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(unfilteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(currentTrackBeforeFiltering.get(TrackProperty.URN), unfilteredPlayQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        List<Urn> expectedFilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrackAfterFiltering, expectedFilteredPlayQueueTracks);
    }

    @Test
    public void loadFilteredLocalPlayQueueEmitsEmptyLocalQueueWhenAllTracksAreFilteredOut() {
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1);
        List<Urn> filteredPlayQueueTracks = Collections.emptyList();
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(filteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(TRACK1, unfilteredPlayQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isEmpty()).isTrue();
    }

    @Test
    public void loadRemotePlayQueueReturnsEmptyQueueOnNetworkError() throws TransientNetworkDisconnectionException, NoConnectionException {
        Mockito.doThrow(new TransientNetworkDisconnectionException()).when(videoCastManager).getRemoteMediaInformation();

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        assertThat(remotePlayQueue.getTrackList()).isEmpty();
        assertThat(remotePlayQueue.getCurrentTrackUrn()).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void loadRemotePlayQueueReturnsEmptyQueueOnConnectionError() throws TransientNetworkDisconnectionException, NoConnectionException {
        Mockito.doThrow(new NoConnectionException()).when(videoCastManager).getRemoteMediaInformation();

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        assertThat(remotePlayQueue.getTrackList()).isEmpty();
        assertThat(remotePlayQueue.getCurrentTrackUrn()).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void loadsRemotePlayQueue() throws TransientNetworkDisconnectionException, NoConnectionException, JSONException {
        MediaInfo mediaInfo = createMediaInfo(PLAY_QUEUE, TRACK2);
        when(videoCastManager.getRemoteMediaInformation()).thenReturn(mediaInfo);

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        assertThat(remotePlayQueue.getTrackList()).isEqualTo(PLAY_QUEUE);
        assertThat(remotePlayQueue.getCurrentTrackUrn()).isEqualTo(TRACK2);
    }

    private MediaInfo createMediaInfo(List<Urn> playQueue, Urn currentTrack) throws JSONException {
        JSONObject customData = new JSONObject()
                .put("play_queue", new JSONArray(CollectionUtils.urnsToStrings(playQueue)));

        MediaMetadata metadata = new MediaMetadata();
        metadata.putString("urn", currentTrack.toString());

        return new MediaInfo.Builder("contentId123")
                .setCustomData(customData)
                .setMetadata(metadata)
                .setContentType("contentType123")
                .setStreamType(0)
                .build();
    }

    private PropertySet createAndSetupPrivateTrack(Urn urn) {
        PropertySet track = createAndSetupPublicTrack(urn);
        track.put(TrackProperty.IS_PRIVATE, true);
        return track;
    }

    private PropertySet createAndSetupPublicTrack(Urn urn) {
        PropertySet track = PropertySet.from(
                TrackProperty.URN.bind(urn),
                TrackProperty.TITLE.bind("Title " + urn),
                TrackProperty.CREATOR_NAME.bind("Creator " + urn),
                TrackProperty.IS_PRIVATE.bind(false));
        when(trackRepository.track(urn)).thenReturn(Observable.just(track));
        return track;
    }

    private void expectLocalPlayQueue(LocalPlayQueue localPlayQueue,
                                      PropertySet currentTrack,
                                      List<Urn> playQueueTracks) throws JSONException {
        assertThat(localPlayQueue.currentTrackUrn).isEqualTo(currentTrack.get(TrackProperty.URN));
        assertThat(localPlayQueue.playQueueTrackUrns).isEqualTo(playQueueTracks);
        assertThat(localPlayQueue.playQueueTracksJSON.get("play_queue").toString())
                .isEqualTo(new JSONArray(CollectionUtils.urnsToStrings(playQueueTracks)).toString());

        MediaInfo mediaInfo = localPlayQueue.mediaInfo;
        assertThat(mediaInfo.getContentType()).isEqualTo("audio/mpeg");
        assertThat(mediaInfo.getStreamType()).isEqualTo(MediaInfo.STREAM_TYPE_BUFFERED);

        MediaMetadata metadata = mediaInfo.getMetadata();
        assertThat(metadata.getMediaType()).isEqualTo(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        assertThat(metadata.getString(MediaMetadata.KEY_TITLE)).isEqualTo(currentTrack.get(TrackProperty.TITLE));
        assertThat(metadata.getString(MediaMetadata.KEY_ARTIST)).isEqualTo(currentTrack.get(TrackProperty.CREATOR_NAME));
        assertThat(metadata.getString("urn")).isEqualTo(currentTrack.get(TrackProperty.URN).toString());
        assertThat(metadata.getImages().get(0).getUrl().toString()).isEqualTo(IMAGE_URL);
    }
}
