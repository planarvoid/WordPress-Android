package com.soundcloud.android.gcm;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
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
    @Mock private FeatureFlags featureFlags;
    @Mock private PlaySessionController playSessionController;

    @Before
    public void setUp() throws Exception {
        handler = new GcmMessageHandler(resources(), featureFlags, decryptor, playSessionController);
        when(featureFlags.isEnabled(Flag.KILL_CONCURRENT_STREAMING)).thenReturn(true);
    }

    @Test
    public void stopsPlaybackWhenStopMessageReceived() throws UnsupportedEncodingException, EncryptionException {
        final Intent intent = new Intent("com.google.android.c2dm.intent.RECEIVE");
        intent.putExtra("from", resources().getString(R.string.google_api_key));
        intent.putExtra("data", ENCRYPTED_DATA);

        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"stop\"}");

        handler.handleMessage(intent);

        verify(playSessionController).pause();
    }

    @Test
    public void doesNotstopPlaybackForOtherActionType() throws UnsupportedEncodingException, EncryptionException {
        final Intent intent = new Intent("com.google.android.c2dm.intent.RECEIVE");
        intent.putExtra("from", resources().getString(R.string.google_api_key));
        intent.putExtra("data", ENCRYPTED_DATA);

        when(decryptor.decrypt(ENCRYPTED_DATA)).thenReturn("{\"action\":\"blah\"}");

        handler.handleMessage(intent);

        verify(playSessionController, never()).pause();
    }
}
