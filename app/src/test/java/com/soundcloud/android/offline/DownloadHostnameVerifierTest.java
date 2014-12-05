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
public class DownloadHostnameVerifierTest {

    @Mock SSLSession session;
    @Mock X509Certificate certificate;

    private DownloadHostnameVerifier downloadHostnameVerifier;

    @Before
    public void setUp() throws Exception {
        Certificate[] certs = {certificate};
        downloadHostnameVerifier = new DownloadHostnameVerifier();
        when(session.getPeerCertificates()).thenReturn(certs);
        when(certificate.getSubjectAlternativeNames()).thenReturn(getAlternativeNames());
    }

    @Test
    public void verifyValidHostname() {
        expect(downloadHostnameVerifier.verify("myapi.soundcloud.com", session)).toBeTrue();
        expect(downloadHostnameVerifier.verify("vader.sndcdn.com", session)).toBeTrue();
        expect(downloadHostnameVerifier.verify("tony.stark.hs.llnwd.net", session)).toBeTrue();
        expect(downloadHostnameVerifier.verify("tony-asd.hs.llnwd.net", session)).toBeTrue();
    }

    @Test
    public void verifyNotValidHostname() {
        expect(downloadHostnameVerifier.verify("blabla", session)).toBeFalse();
        expect(downloadHostnameVerifier.verify("myapi.soundcloud.com.ar", session)).toBeFalse();
        expect(downloadHostnameVerifier.verify("eoiruwerllnwd.net", session)).toBeFalse();
    }

    private Collection<List<?>> getAlternativeNames() {
        List<List<?>> result = new ArrayList<>();
        List<Object> entry = new ArrayList<>();
        entry.add(0, DownloadHostnameVerifier.ALT_TYPE_DNS);
        entry.add(1, "myapi.soundcloud.com");
        result.add(entry);
        return result;
    }
}