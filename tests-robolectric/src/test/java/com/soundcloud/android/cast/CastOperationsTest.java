package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CastOperationsTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final Urn URN2 = Urn.forTrack(456L);
    private static final List<Urn> PLAY_QUEUE = Lists.newArrayList(URN, URN2);

    private CastOperations castOperations;

    @Mock private VideoCastManager videoCastManager;


    @Before
    public void setUp() throws Exception {
        castOperations = new CastOperations(videoCastManager);
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
        MediaInfo mediaInfo = createMediaInfo(PLAY_QUEUE, URN2);
        when(videoCastManager.getRemoteMediaInformation()).thenReturn(mediaInfo);

        RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();

        expect(remotePlayQueue.getTrackList()).toEqual(PLAY_QUEUE);
        expect(remotePlayQueue.getCurrentTrackUrn()).toEqual(URN2);
    }

    private MediaInfo createMediaInfo(List<Urn> playQueue, Urn currentTrack) throws JSONException {
        JSONObject customData = new JSONObject()
                .put(CastPlayer.KEY_PLAY_QUEUE, new JSONArray(CollectionUtils.urnsToStrings(playQueue)));

        MediaMetadata metadata = new MediaMetadata();
        metadata.putString(CastPlayer.KEY_URN, currentTrack.toString());

        return new MediaInfo.Builder("contentId123")
                .setCustomData(customData)
                .setMetadata(metadata)
                .setContentType("contentType123")
                .setStreamType(0)
                .build();
    }
}