package com.soundcloud.android.offline;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentServiceTest {

    @Mock private DownloadController controller;

    private OfflineContentService service;

    @Before
    public void setUp() {
        service = new OfflineContentService(controller);
    }

    @Test
    public void startServiceWithDownloadActionStartsDownloadController() {
        service.onHandleIntent(createDownloadTracksIntent());

        verify(controller).downloadTracks();
    }

    private Intent createDownloadTracksIntent() {
        Intent intent = new Intent(Robolectric.application, OfflineContentService.class);
        intent.setAction(OfflineContentService.ACTION_DOWNLOAD_TRACKS);
        return intent;
    }

}