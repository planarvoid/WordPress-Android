package com.soundcloud.android.cast;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.PropertySet;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CastOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);
    private static final String IMAGE_URL = "http://image.url";

    private DefaultCastOperations castOperations;

    @Mock private TrackRepository trackRepository;
    @Mock private PolicyOperations policyOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private ImageOperations imageOperations;

    private TestObserver<LocalPlayQueue> observer;

    @Before
    public void setUp() throws Exception {
        castOperations = new DefaultCastOperations(trackRepository,
                                                   policyOperations,
                                                   playQueueManager,
                                                   new CastProtocol(imageOperations, resources()),
                                                   Schedulers.immediate());
        observer = new TestObserver<>();
        when(imageOperations.getUrlForLargestImage(resources(), TRACK1)).thenReturn(IMAGE_URL);
    }

    @Test
    public void loadsLocalPlayQueue() throws JSONException {
        TrackItem currentTrack = createAndSetupPublicTrack(TRACK1);
        List<Urn> playQueueTracks = Arrays.asList(TRACK1, TRACK2);

        castOperations.loadLocalPlayQueue(TRACK1, playQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, playQueueTracks);
    }

    @Test
    public void loadsLocalPlayQueueWithoutMonetizableTracks() throws JSONException {
        createAndSetupPublicTrack(TRACK2);
        TrackItem currentTrack = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        List<Urn> filteredPlayQueueTracks = singletonList(TRACK1);
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
        TrackItem currentTrack = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(
                unfilteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(TRACK1, unfilteredPlayQueueTracks)
                      .subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        List<Urn> expectedFilteredPlayQueueTracks = singletonList(TRACK1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, expectedFilteredPlayQueueTracks);
    }

    @Test
    public void loadLocalPlayQueueWithoutMonetizableTracksCorrectsCurrentTrackWhenItIsFilteredOut() throws JSONException {
        createAndSetupPublicTrack(TRACK2);
        TrackItem currentTrackBeforeFiltering = createAndSetupPublicTrack(TRACK3);
        TrackItem currentTrackAfterFiltering = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2, TRACK3);
        List<Urn> filteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(
                filteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(currentTrackBeforeFiltering.getUrn(), unfilteredPlayQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrackAfterFiltering, filteredPlayQueueTracks);
    }

    @Test
    public void loadLocalPlayQueueWithoutPrivateTracksCorrectsCurrentTrackWhenItIsFilteredOut() throws JSONException {
        TrackItem currentTrackBeforeFiltering = createAndSetupPrivateTrack(TRACK3);
        createAndSetupPublicTrack(TRACK2);
        TrackItem currentTrackAfterFiltering = createAndSetupPublicTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2, TRACK3);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(
                unfilteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(currentTrackBeforeFiltering.getUrn(), unfilteredPlayQueueTracks).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        List<Urn> expectedFilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0),
                             currentTrackAfterFiltering,
                             expectedFilteredPlayQueueTracks);
    }

    @Test
    public void loadFilteredLocalPlayQueueEmitsEmptyLocalQueueWhenAllTracksAreFilteredOut() {
        List<Urn> unfilteredPlayQueueTracks = singletonList(TRACK1);
        List<Urn> filteredPlayQueueTracks = Collections.emptyList();
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(
                filteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(TRACK1, unfilteredPlayQueueTracks)
                      .subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isEmpty()).isTrue();
    }

    private TrackItem createAndSetupPrivateTrack(Urn urn) {
        return createAndSetupPublicTrack(urn, true);
    }

    private TrackItem createAndSetupPublicTrack(Urn urn) {
        return createAndSetupPublicTrack(urn, false);
    }

    private TrackItem createAndSetupPublicTrack(Urn urn, boolean isPrivate) {
        TrackItem track = TrackItem.from(PropertySet.from(
                TrackProperty.URN.bind(urn),
                TrackProperty.TITLE.bind("Title " + urn),
                TrackProperty.CREATOR_NAME.bind("Creator " + urn),
                TrackProperty.IS_PRIVATE.bind(isPrivate)));
        when(trackRepository.track(urn)).thenReturn(Observable.just(track));
        return track;
    }

    private void expectLocalPlayQueue(LocalPlayQueue localPlayQueue,
                                      TrackItem currentTrack,
                                      List<Urn> playQueueTracks) throws JSONException {
        assertThat(localPlayQueue.currentTrackUrn).isEqualTo(currentTrack.getUrn());
        assertThat(localPlayQueue.playQueueTrackUrns).isEqualTo(playQueueTracks);
        assertThat(localPlayQueue.playQueueTracksJSON.get("play_queue").toString())
                .isEqualTo(new JSONArray(Urns.toString(playQueueTracks)).toString());

        MediaInfo mediaInfo = localPlayQueue.mediaInfo;
        assertThat(mediaInfo.getContentType()).isEqualTo("audio/mp3");
        assertThat(mediaInfo.getStreamType()).isEqualTo(MediaInfo.STREAM_TYPE_BUFFERED);

        MediaMetadata metadata = mediaInfo.getMetadata();
        assertThat(metadata.getMediaType()).isEqualTo(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        assertThat(metadata.getString(MediaMetadata.KEY_TITLE)).isEqualTo(currentTrack.getTitle());
        assertThat(metadata.getString(MediaMetadata.KEY_ARTIST)).isEqualTo(currentTrack.getCreatorName());
        assertThat(metadata.getString("urn")).isEqualTo(currentTrack.getUrn().toString());
        assertThat(metadata.getImages().get(0).getUrl().toString()).isEqualTo(IMAGE_URL);
    }
}
