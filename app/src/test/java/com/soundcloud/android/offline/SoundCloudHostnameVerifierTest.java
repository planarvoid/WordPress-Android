package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SoundCloudHostnameVerifierTest extends AndroidUnitTest {

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
        assertThat(soundCloudHostnameVerifier.verify("soundcloud.com", session)).isTrue();
        assertThat(soundCloudHostnameVerifier.verify("myapi.soundcloud.com", session)).isTrue();
        assertThat(soundCloudHostnameVerifier.verify("vader.sndcdn.com", session)).isTrue();
        assertThat(soundCloudHostnameVerifier.verify("tony.stark.hs.llnwd.net", session)).isTrue();
        assertThat(soundCloudHostnameVerifier.verify("tony-asd.hs.llnwd.net", session)).isTrue();
        assertThat(soundCloudHostnameVerifier.verify("sndcdn.com", session)).isTrue();
    }

    @Test
    public void verifyNotValidHostname() {
        assertThat(soundCloudHostnameVerifier.verify("blabla", session)).isFalse();
        assertThat(soundCloudHostnameVerifier.verify("myapi.soundcloud.com.ar", session)).isFalse();
        assertThat(soundCloudHostnameVerifier.verify("eoiruwerllnwd.net", session)).isFalse();
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