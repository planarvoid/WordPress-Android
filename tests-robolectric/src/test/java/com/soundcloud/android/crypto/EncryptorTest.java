package com.soundcloud.android.crypto;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.ScTextUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

@RunWith(SoundCloudTestRunner.class)
public class EncryptorTest {

    @Mock private MessageDigest digest;
    @Mock private InputStream inputStream;
    @Mock private OutputStream outputStream;
    @Mock private CipherWrapper cipher;

    private DeviceSecret secret;
    private Encryptor encryptor;

    @Before
    public void setUp() {
        encryptor = new Encryptor(cipher);
        secret = new DeviceSecret("name", null, null);
    }

    @Test
    public void hashTrackUrnUsesMessageDigest() throws EncryptionException {
        final Urn urn = Urn.forTrack(123L);
        final byte[] testBytes = "test".getBytes();
        when(digest.digest(urn.toEncodedString().getBytes(Charsets.UTF_8))).thenReturn(testBytes);

        final String result = encryptor.hash(urn, digest);

        expect(result).toEqual(ScTextUtils.hexString(testBytes));
    }

    @Test(expected = EncryptionInterruptedException.class)
    public void cancelThrowsEncryptionInterrupted() throws IOException, EncryptionException {
        encryptor.tryToCancelRequest();
        encryptor.encrypt(inputStream, outputStream, secret);
    }

    @Test
    public void cancelStopsEncryption() throws IOException, EncryptionException {
        encryptor.tryToCancelRequest();
        try {
            encryptor.encrypt(inputStream, outputStream, secret);
        } catch (Exception ignored) {
        }

        verify(cipher, never()).update(any(byte[].class), anyInt(), anyInt(), any(byte[].class));
        verify(cipher, never()).doFinal(any(byte[].class), anyInt());
    }
}