package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.common.collect.Lists;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.propeller.PropertySet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestObserver;

import android.content.res.Resources;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CastOperationsTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);
    private static final List<Urn> PLAY_QUEUE = Lists.newArrayList(TRACK1, TRACK2);
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
                resources);
        observer = new TestObserver<>();
        when(imageOperations.getUrlForLargestImage(resources, TRACK1)).thenReturn(IMAGE_URL);
    }

    @Test
    public void loadsLocalPlayQueue() throws JSONException {
        PropertySet currentTrack = createTrack(TRACK1);
        List<Urn> playQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(trackRepository.track(TRACK1)).thenReturn(Observable.just(currentTrack));

        castOperations.loadLocalPlayQueue(TRACK1, playQueueTracks).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, playQueueTracks);
    }

    @Test
    public void loadsLocalPlayQueueWithoutMonetizableTracks() throws JSONException {
        PropertySet currentTrack = createTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        List<Urn> filteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(trackRepository.track(TRACK1)).thenReturn(Observable.just(currentTrack));
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(filteredPlayQueueTracks));

        castOperations.loadLocalPlayQueueWithoutMonetizableTracks(TRACK1, unfilteredPlayQueueTracks).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrack, filteredPlayQueueTracks);
    }

    @Test
    public void loadLocalPlayQueueWithoutMonetizableTracksCorrectsCurrentTrackWhenItIsFilteredOut() throws JSONException {
        PropertySet currentTrackBeforeFiltering = createTrack(TRACK3);
        PropertySet currentTrackAfterFiltering = createTrack(TRACK1);
        List<Urn> unfilteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2, TRACK3);
        List<Urn> filteredPlayQueueTracks = Arrays.asList(TRACK1, TRACK2);
        when(policyOperations.filterMonetizableTracks(unfilteredPlayQueueTracks)).thenReturn(Observable.just(filteredPlayQueueTracks));
        when(trackRepository.track(TRACK1)).thenReturn(Observable.just(currentTrackAfterFiltering));

        castOperations.loadLocalPlayQueueWithoutMonetizableTracks(currentTrackBeforeFiltering.get(TrackProperty.URN), unfilteredPlayQueueTracks).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expectLocalPlayQueue(observer.getOnNextEvents().get(0), currentTrackAfterFiltering, filteredPlayQueueTracks);
    }

    @Test
    public void loadRemotePlayQueueReturnsEmptyQueueOnNetworkError() throws TransientNetworkDisconnectionException, NoConnectionException {
        Mockito.doThrow(new TransientNetworkDisconnectionException()).when(videoCastManager).getRemoteMediaInformation();

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        expect(remotePlayQueue.getTrackList()).toBeEmpty();
        expect(remotePlayQueue.getCurrentTrackUrn()).toEqual(Urn.NOT_SET);
    }

    @Test
    public void loadRemotePlayQueueReturnsEmptyQueueOnConnectionError() throws TransientNetworkDisconnectionException, NoConnectionException {
        Mockito.doThrow(new NoConnectionException()).when(videoCastManager).getRemoteMediaInformation();

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        expect(remotePlayQueue.getTrackList()).toBeEmpty();
        expect(remotePlayQueue.getCurrentTrackUrn()).toEqual(Urn.NOT_SET);
    }

    @Test
    public void loadsRemotePlayQueue() throws TransientNetworkDisconnectionException, NoConnectionException, JSONException {
        MediaInfo mediaInfo = createMediaInfo(PLAY_QUEUE, TRACK2);
        when(videoCastManager.getRemoteMediaInformation()).thenReturn(mediaInfo);

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        expect(remotePlayQueue.getTrackList()).toEqual(PLAY_QUEUE);
        expect(remotePlayQueue.getCurrentTrackUrn()).toEqual(TRACK2);
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

    public static PropertySet createTrack(Urn urn) {
        return PropertySet.from(
                TrackProperty.URN.bind(urn),
                TrackProperty.TITLE.bind("Title " + urn),
                TrackProperty.CREATOR_NAME.bind("Creator " + urn));
    }

    private void expectLocalPlayQueue(LocalPlayQueue localPlayQueue,
                                      PropertySet currentTrack,
                                      List<Urn> playQueueTracks) throws JSONException {
        expect(localPlayQueue.currentTrackUrn).toEqual(currentTrack.get(TrackProperty.URN));
        expect(localPlayQueue.playQueueTrackUrns).toEqual(playQueueTracks);
        expect(localPlayQueue.playQueueTracksJSON.get("play_queue").toString())
                .toEqual(new JSONArray(CollectionUtils.urnsToStrings(playQueueTracks)).toString());

        MediaInfo mediaInfo = localPlayQueue.mediaInfo;
        expect(mediaInfo.getContentType()).toEqual("audio/mpeg");
        expect(mediaInfo.getStreamType()).toEqual(MediaInfo.STREAM_TYPE_BUFFERED);

        MediaMetadata metadata = mediaInfo.getMetadata();
        expect(metadata.getMediaType()).toEqual(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        expect(metadata.getString(MediaMetadata.KEY_TITLE)).toEqual(currentTrack.get(TrackProperty.TITLE));
        expect(metadata.getString(MediaMetadata.KEY_ARTIST)).toEqual(currentTrack.get(TrackProperty.CREATOR_NAME));
        expect(metadata.getString("urn")).toEqual(currentTrack.get(TrackProperty.URN).toString());
        expect(metadata.getImages().get(0).getUrl().toString()).toEqual(IMAGE_URL);
    }
}