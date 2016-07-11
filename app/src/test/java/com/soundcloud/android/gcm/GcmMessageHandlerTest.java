package com.soundcloud.android.gcm;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ConcurrentPlaybackOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

import java.io.UnsupportedEncodingException;

public class GcmMessageHandlerTest extends AndroidUnitTest {

    private static final String ENCRYPTED_DATA = "encryptedData";
    private GcmMessageHandler handler;

    @Mock private GcmDecryptor decryptor;
    @Mock private ConcurrentPlaybackOperations concurrentPlaybackOperations;
    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        handler = new GcmMessageHandler(resources(), decryptor, concurrentPlaybackOperations, accountOperations);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(123L));
    }

    @Test
    public void stopsPlaybackWhenStopMessageReceivedAndPlaying() throws UnsupportedEncodingException, EncryptionException {
        final Intent intent = new Intent("com.google.android.c2dm.intent.RECEIVE");
        intent.putExtra("from", resources().getString(R.string.google_api_key));
        intent.putExtra("data", ENCRYPTED_DATA);

        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"user_id\":123}");

        handler.handleMessage(intent);

        verify(concurrentPlaybackOperations).pauseIfPlaying();
    }

    @Test
    public void doesNotStopPlaybackWhenWhenMessageForADifferentUser() throws UnsupportedEncodingException, EncryptionException {
        final Intent intent = new Intent("com.google.android.c2dm.intent.RECEIVE");
        intent.putExtra("from", resources().getString(R.string.google_api_key));
        intent.putExtra("data", ENCRYPTED_DATA);

        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"user_id\":234}");

        handler.handleMessage(intent);

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
    }

    @Test
    public void stopsPlaybackWInStealthMode() throws UnsupportedEncodingException, EncryptionException {
        final Intent intent = new Intent("com.google.android.c2dm.intent.RECEIVE");
        intent.putExtra("from", resources().getString(R.string.google_api_key));
        intent.putExtra("data", ENCRYPTED_DATA);

        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\", \"stealth\":true, \"user_id\":123}");

        handler.handleMessage(intent);

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
    }

    @Test
    public void doesNotStopPlaybackForOtherActionType() throws UnsupportedEncodingException, EncryptionException {
        final Intent intent = new Intent("com.google.android.c2dm.intent.RECEIVE");
        intent.putExtra("from", resources().getString(R.string.google_api_key));
        intent.putExtra("data", ENCRYPTED_DATA);

        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"blah\"}, \"user_id\":123}");

        handler.handleMessage(intent);

        verify(concurrentPlaybackOperations, never()).pauseIfPlaying();
    }
}
