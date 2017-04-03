package com.soundcloud.android.cast.api;

import static java.util.Collections.emptyList;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Collections;

public class CastProtocolTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final String TOKEN = "fakeToken";
    private CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN, emptyList());

    @Mock private CastJsonHandler castJsonHandler;
    @Mock private AccountOperations accountOperations;
    @Mock private Token token;
    @Mock private CastSession castSession;
    @Mock private CastProtocol.Listener listener;
    @Mock private RemoteMediaClient remoteMediaClient;
    @Captor private ArgumentCaptor<String> jsonCaptor;
    @Captor private ArgumentCaptor<CastMessage> castMessageCaptor;
    @Captor private ArgumentCaptor<MediaInfo> mediaInfoCaptor;
    @Captor private ArgumentCaptor<CastCredentials> credentialsCaptor;

    private CastProtocol castProtocol;

    @Before
    public void setUp() throws IOException {
        when(token.getAccessToken()).thenReturn(TOKEN);
        when(token.valid()).thenReturn(true);
        when(castSession.getRemoteMediaClient()).thenReturn(remoteMediaClient);
        when(castSession.isConnected()).thenReturn(true);
        when(accountOperations.getSoundCloudToken()).thenReturn(token);

        castProtocol = new CastProtocol(castJsonHandler, accountOperations);
        castProtocol.registerCastSession(castSession);
    }

    @Test
    public void updateQueueMessageIsSentWithAttachedCredentials() throws ApiMapperException {
        castProtocol.sendUpdateQueue(castPlayQueue);

        verify(castJsonHandler).toString(castMessageCaptor.capture());
        assertThat(castMessageCaptor.getValue().payload().credentials().get().getAuthorization()).contains(TOKEN);
    }

    @Test
    public void remoteMediaClientListenerIsSetInRemoteMediaClient() {
        castProtocol.setListener(listener);

        verify(remoteMediaClient).addListener(eq(castProtocol));
    }

    @Test
    public void remoteMediaClientListenerIsRemovedFromRemoteMediaClient() {
        castProtocol.removeListener(listener);

        verify(remoteMediaClient).removeListener(castProtocol);
    }

    @Test
    public void progressListenerIsSetInRemoteMediaClient() {
        castProtocol.setListener(listener);

        verify(remoteMediaClient).addProgressListener(eq(listener), anyLong());
    }

    @Test
    public void progressListenerIsRemovedFromRemoteMediaClient() {
        castProtocol.removeListener(listener);

        verify(remoteMediaClient).removeProgressListener(listener);
    }

    @Test
    public void buildCorrectMediaInfoMetadataWhenSendingLoadMessage() {
        String contentId = "fakeId";
        long position = 123L;
        JSONObject customData = new JSONObject();
        when(castJsonHandler.toJson(any(CastPlayQueue.class))).thenReturn(customData);

        castProtocol.sendLoad(contentId, true, position, castPlayQueue);

        verify(remoteMediaClient).load(mediaInfoCaptor.capture(), eq(true), eq(position), eq(customData));
        MediaInfo mediaInfo = mediaInfoCaptor.getValue();
        assertThat(mediaInfo.getContentId()).isEqualTo(contentId);
        assertThat(mediaInfo.getContentType()).isEqualTo(CastProtocol.MIME_TYPE_AUDIO_MPEG);
        assertThat(mediaInfo.getStreamType()).isEqualTo(MediaInfo.STREAM_TYPE_BUFFERED);
    }

    @Test
    public void forwardUpdateStatusCallbackToListener() {
        castProtocol.setListener(listener);

        castProtocol.onStatusUpdated();

        verify(listener).onStatusUpdated();
    }

    @Test
    public void handleEmptyIdleSessionScenarioByForwardingTheCallToListener() {
        when(remoteMediaClient.getMediaInfo()).thenReturn(null);
        when(remoteMediaClient.getPlayerState()).thenReturn(MediaStatus.PLAYER_STATE_IDLE);
        castProtocol.setListener(listener);

        castProtocol.onMetadataUpdated();

        verify(listener).onRemoteEmptyStateFetched();
    }

    @Test
    public void doNotForwardEmprtyIdleStateToListenerIfStateDidNotChange() {
        when(remoteMediaClient.getMediaInfo()).thenReturn(null);
        when(remoteMediaClient.getPlayerState()).thenReturn(MediaStatus.PLAYER_STATE_IDLE);
        castProtocol.setListener(listener);

        castProtocol.onMetadataUpdated();
        castProtocol.onMetadataUpdated();
        castProtocol.onMetadataUpdated();

        verify(listener, times(1)).onRemoteEmptyStateFetched();
    }

    @Test
    public void handleMulticastScenarioByForwardingTheCurrentlyRemoteQueueToListener() {
        CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN, Collections.singletonList(TRACK_URN));
        mockRemoteState(MediaStatus.PLAYER_STATE_PLAYING, castPlayQueue);
        castProtocol.setListener(listener);

        castProtocol.onMetadataUpdated();

        verify(listener).onQueueReceived(castPlayQueue);
    }

    @Test
    public void doNotForwardQueueToListenerIfStateDidNotChange() {
        CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN, Collections.singletonList(TRACK_URN));
        mockRemoteState(MediaStatus.PLAYER_STATE_PLAYING, castPlayQueue);
        castProtocol.setListener(listener);

        castProtocol.onMetadataUpdated();
        castProtocol.onMetadataUpdated();
        castProtocol.onMetadataUpdated();

        verify(listener, times(1)).onQueueReceived(castPlayQueue);
    }

    @Test
    public void invalidateCachedStateAfterSessionIsUnregistered() {
        CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN, Collections.singletonList(TRACK_URN));
        mockRemoteState(MediaStatus.PLAYER_STATE_PLAYING, castPlayQueue);
        castProtocol.setListener(listener);
        castProtocol.onMetadataUpdated();

        castProtocol.unregisterCastSession();

        castProtocol.registerCastSession(castSession);
        castProtocol.onMetadataUpdated();

        verify(listener, times(2)).onQueueReceived(castPlayQueue);
    }

    @Test
    public void ignoreIdleStatesAfterLoadMessageWasSent() {
        String contentId = "fakeId";
        long position = 123L;
        JSONObject customData = new JSONObject();
        when(castJsonHandler.toJson(castPlayQueue)).thenReturn(customData);
        when(remoteMediaClient.getMediaInfo()).thenReturn(null);
        when(remoteMediaClient.getPlayerState()).thenReturn(MediaStatus.PLAYER_STATE_IDLE);
        castProtocol.setListener(listener);

        castProtocol.sendLoad(contentId, true, position, castPlayQueue);

        castProtocol.onStatusUpdated();
        castProtocol.onMetadataUpdated();

        verifyZeroInteractions(listener);
    }

    private void mockRemoteState(int playerState, CastPlayQueue queue) {
        try {
            when(castJsonHandler.parseCastPlayQueue(any(JSONObject.class))).thenReturn(queue);
            when(remoteMediaClient.getPlayerState()).thenReturn(playerState);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(CastJsonHandler.KEY_QUEUE_STATUS, new JSONObject());
            MediaInfo mediaInfo = new MediaInfo.Builder("fake").setContentType("audio/mpeg").setStreamType(MediaInfo.STREAM_TYPE_BUFFERED).setCustomData(jsonObject).build();
            when(remoteMediaClient.getMediaInfo()).thenReturn(mediaInfo);
        } catch (Exception e) {
            fail();
        }
    }
}
