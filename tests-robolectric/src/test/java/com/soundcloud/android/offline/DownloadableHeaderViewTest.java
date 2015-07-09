package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.Fragment;
import android.view.View;


@RunWith(SoundCloudTestRunner.class)
public class DownloadableHeaderViewTest {

    private final String inProgressText = Robolectric.application.getResources().getString(R.string.offline_update_in_progress);
    private DownloadableHeaderView downloadableHeaderView;

    @Mock private Fragment fragment;
    @Mock private PlaybackOperations playbackOperations;
    private View header;

    @Before
    public void setUp() throws Exception {
        header = View.inflate(Robolectric.application, R.layout.track_likes_header, null);
        downloadableHeaderView = new DownloadableHeaderView(Robolectric.application.getResources());
        downloadableHeaderView.onViewCreated(header);
    }

    @Test
    public void displayInProgressTextWhenDownloading() {
        downloadableHeaderView.setHeaderText("Header test text");
        downloadableHeaderView.show(OfflineState.DOWNLOADING);

        expect(header.findViewById(R.id.header_text)).toHaveText(inProgressText);
        expect(header.findViewById(R.id.header_download_state)).toBeVisible();
    }

    @Test
    public void displayHeaderTextWhenDownloaded() {
        downloadableHeaderView.setHeaderText("Header test text");
        downloadableHeaderView.show(OfflineState.DOWNLOADED);

        expect(header.findViewById(R.id.header_text)).toHaveText("Header test text");
        expect(header.findViewById(R.id.header_download_state)).toBeVisible();
    }

    @Test
    public void displayHeadTextWhenNoOffline() {
        downloadableHeaderView.setHeaderText("Header test text");
        downloadableHeaderView.show(OfflineState.NO_OFFLINE);

        expect(header.findViewById(R.id.header_text)).toHaveText("Header test text");
        expect(header.findViewById(R.id.header_download_state)).toBeGone();
    }
}