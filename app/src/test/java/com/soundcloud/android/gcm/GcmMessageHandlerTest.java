package com.soundcloud.android.gcm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.messaging.RemoteMessage;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ConcurrentPlaybackOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class GcmMessageHandlerTest extends AndroidUnitTest {

    private static final String ENCRYPTED_DATA = "encryptedData";
    private GcmMessageHandler handler;

    @Mock private GcmDecryptor decryptor;
    @Mock private ConcurrentPlaybackOperations concurrentPlaybackOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private GcmStorage gcmStorage;
    @Mock private GcmMessageHandler.Listener listener;

    @Before
    public void setUp() throws Exception {
        handler = new GcmMessageHandler(resources(),
                                        decryptor,
                                        concurrentPlaybackOperations,
                                        accountOperations,
                                        gcmStorage);
        handler.setListener(listener);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(123L));
    }

    @Test
    public void stopsPlaybackWhenStopMessageReceivedAndPlaying() throws UnsupportedEncodingException, EncryptionException {
        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"user_id\":123}");

        handler.handleMessage(getRemoteMessage());

        verify(concurrentPlaybackOperations).pauseIfPlaying();
    }

    @Test
    public void doesNotStopPlaybackWhenWhenMessageForADifferentUser() throws UnsupportedEncodingException, EncryptionException {
        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"user_id\":234}");

        handler.handleMessage(getRemoteMessage());

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
    }

    @Test
    public void stopsPlaybackWInStealthMode() throws UnsupportedEncodingException, EncryptionException {
        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"stealth\":true, \"user_id\":123}");

        handler.handleMessage(getRemoteMessage());

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
    }

    @Test
    public void doesNotStopPlaybackForOtherActionType() throws UnsupportedEncodingException, EncryptionException {
        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"blah\"}, \"user_id\":123}");

        handler.handleMessage(getRemoteMessage());

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
    }

    @Test
    public void doesNotStopPlaybackForMyToken() throws UnsupportedEncodingException, EncryptionException {
        when(gcmStorage.getToken()).thenReturn("token");
        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"user_id\":123, \"token\":\"token\"}");

        handler.handleMessage(getRemoteMessage());

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
    }

    @Test
    public void ignoresAllMessagesWithoutPayload() throws UnsupportedEncodingException, EncryptionException {
        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"user_id\":123}");
        RemoteMessage remoteMessage = getRemoteMessage();
        remoteMessage.getData().remove(GcmMessageHandler.EXTRA_DATA);
        handler.handleMessage(remoteMessage);

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
        verify(listener, never()).onRemoteMessage(any(), any());
    }

    @Test
    public void ignoresAppboyMessagesWithoutPayload() throws UnsupportedEncodingException, EncryptionException {
        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"user_id\":123}");
        RemoteMessage remoteMessage = getRemoteMessage();
        remoteMessage.getData().remove(GcmMessageHandler.EXTRA_DATA);
        remoteMessage.getData().put(GcmMessageHandler.APPBOY_KEY, GcmMessageHandler.APPBOY_VALUE);
        handler.handleMessage(remoteMessage);

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
        verify(listener, never()).onRemoteMessage(any(), any());
    }

    @Test
    public void ignoresAppboyMessagesWithPayload() throws UnsupportedEncodingException, EncryptionException {
        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"user_id\":123}");
        RemoteMessage remoteMessage = getRemoteMessage();
        remoteMessage.getData().put(GcmMessageHandler.APPBOY_KEY, GcmMessageHandler.APPBOY_VALUE);
        handler.handleMessage(remoteMessage);

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
        verify(listener, never()).onRemoteMessage(any(), any());
    }

    @Test
    public void handlesNonAppboyMessages() throws UnsupportedEncodingException, EncryptionException {
        String payload = "{\"action\":\"stop\", \"user_id\":123}";
        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn(payload);
        RemoteMessage remoteMessage = getRemoteMessage();
        handler.handleMessage(remoteMessage);

        verify(concurrentPlaybackOperations).pauseIfPlaying();
        verify(listener).onRemoteMessage(eq(remoteMessage), eq(payload));
    }

    private RemoteMessage getRemoteMessage() {
        final Map<String, String> data = new HashMap<>();
        data.put(GcmMessageHandler.EXTRA_DATA, ENCRYPTED_DATA);
        data.put("from", resources().getString(R.string.gcm_defaultSenderId));
        return new RemoteMessage.Builder("to").setData(data).build();
    }
}
