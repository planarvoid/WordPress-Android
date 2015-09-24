package com.soundcloud.android.offline;

import com.soundcloud.android.utils.Log;
import com.soundcloud.java.strings.Strings;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

class SoundCloudHostnameVerifier implements HostnameVerifier {
    static final int ALT_TYPE_DNS = 2;
    private static final String PATTERN = ".*.\\.hs\\.llnwd\\.net$|.*.?sndcdn\\.com$|.*.?soundcloud\\.com$";

    @Override
    public boolean verify(String hostname, SSLSession session) {
        try {
            final Certificate[] certificates = session.getPeerCertificates();
            List<String> names = getSubjectAltNames((X509Certificate) certificates[0]);

            return verifyHostName(hostname, names);
        } catch (SSLException e) {
            Log.e(OfflineContentService.TAG, "Error retrieving peer certificates for " + hostname, e);
            return false;
        }
    }

    private boolean verifyHostName(String hostname, List<String> altNames) {
        if (altNames.isEmpty() || Strings.isBlank(hostname)) {
            return false;
        }
        altNames.add(hostname);
        for (String host : altNames) {
            if (!host.toLowerCase(Locale.US).matches(PATTERN)) {
                return false;
            }
        }
        return true;
    }

    //TODO: OkHttp version 2.1.1 will provide public method to extract alt names from cert
    private List<String> getSubjectAltNames(X509Certificate certificate) {
        List<String> result = new ArrayList<>();
        try {
            Collection<List<?>> subjectAltNames = certificate.getSubjectAlternativeNames();
            if (subjectAltNames == null) {
                return Collections.emptyList();
            }

            for (List<?> entry : subjectAltNames) {
                if (entry == null || entry.size() < 2) {
                    continue;
                }
                Integer altNameType = (Integer) entry.get(0);
                if (altNameType == null) {
                    continue;
                }
                if (altNameType == ALT_TYPE_DNS) {
                    String altName = (String) entry.get(1);
                    if (altName != null) {
                        result.add(altName);
                    }
                }
            }
            return result;
        } catch (CertificateParsingException e) {
            return Collections.emptyList();
        }
    }
}
