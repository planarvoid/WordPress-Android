package com.soundcloud.android.offline;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class DownloadRequestTest {

    @Test
    public void downloadRequestsImplementsEqualsAndHashcode() {
        EqualsVerifier.forClass(DownloadRequest.class).verify();
    }

}
