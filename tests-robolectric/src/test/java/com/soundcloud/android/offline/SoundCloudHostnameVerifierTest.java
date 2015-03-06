package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundCloudHostnameVerifierTest {

    @Mock SSLSession session;
    @Mock X509Certificate certificate;

    private SoundCloudHostnameVerifier soundCloudHostnameVerifier;

    @Before
    public void setUp() throws Exception {
        Certificate[] certs = {certificate};
        soundCloudHostnameVerifier = new SoundCloudHostnameVerifier();
        when(session.getPeerCertificates()).thenReturn(certs);
        when(certificate.getSubjectAlternativeNames()).thenReturn(getAlternativeNames());
    }

    @Test
    public void verifyValidHostname() {
        expect(soundCloudHostnameVerifier.verify("soundcloud.com", session)).toBeTrue();
        expect(soundCloudHostnameVerifier.verify("myapi.soundcloud.com", session)).toBeTrue();
        expect(soundCloudHostnameVerifier.verify("vader.sndcdn.com", session)).toBeTrue();
        expect(soundCloudHostnameVerifier.verify("tony.stark.hs.llnwd.net", session)).toBeTrue();
        expect(soundCloudHostnameVerifier.verify("tony-asd.hs.llnwd.net", session)).toBeTrue();
        expect(soundCloudHostnameVerifier.verify("sndcdn.com", session)).toBeTrue();
    }

    @Test
    public void verifyNotValidHostname() {
        expect(soundCloudHostnameVerifier.verify("blabla", session)).toBeFalse();
        expect(soundCloudHostnameVerifier.verify("myapi.soundcloud.com.ar", session)).toBeFalse();
        expect(soundCloudHostnameVerifier.verify("eoiruwerllnwd.net", session)).toBeFalse();
    }

    private Collection<List<?>> getAlternativeNames() {
        List<List<?>> result = new ArrayList<>();
        List<Object> entry = new ArrayList<>();
        entry.add(0, SoundCloudHostnameVerifier.ALT_TYPE_DNS);
        entry.add(1, "*.soundcloud.com");
        result.add(entry);
        return result;
    }
}