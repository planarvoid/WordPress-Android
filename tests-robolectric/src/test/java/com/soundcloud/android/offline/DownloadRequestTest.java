package com.soundcloud.android.offline;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class DownloadRequestTest {

    @Test
    public void downloadRequestsImplementsEqualsAndHashcode() {
        EqualsVerifier.forClass(DownloadRequest.class).verify();
    }

}
