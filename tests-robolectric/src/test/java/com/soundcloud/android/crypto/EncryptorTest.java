package com.soundcloud.android.crypto;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.ScTextUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

@RunWith(SoundCloudTestRunner.class)
public class EncryptorTest {

    @Mock private MessageDigest digest;
    @Mock private InputStream inputStream;
    @Mock private OutputStream outputStream;

    private Encryptor encryptor = new Encryptor();

    @Test
    public void hashTrackUrnUsesMessageDigest() throws EncryptionException {
        final Urn urn = Urn.forTrack(123L);
        final byte[] testBytes = "test".getBytes();
        when(digest.digest(urn.toEncodedString().getBytes(Charsets.UTF_8))).thenReturn(testBytes);

        final String result = encryptor.hash(urn, digest);

        expect(result).toEqual(ScTextUtils.hexString(testBytes));
    }
}
