package com.soundcloud.android.offline;

import static org.assertj.android.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;

public class DownloadStateRendererTest extends AndroidUnitTest {

    private final String inProgressText = resources().getString(R.string.offline_update_in_progress);
    private DownloadStateRenderer downloadStateRenderer;

    @Mock private Fragment fragment;
    @Mock private PlaybackInitiator playbackInitiator;
    private View view;

    @Before
    public void setUp() throws Exception {
        view = View.inflate(context(), R.layout.track_likes_header, null);
        downloadStateRenderer = new DownloadStateRenderer(resources());
    }

    @Ignore("Blocks on AnimUtils")
    public void displayInProgressTextWhenDownloading() {
        downloadStateRenderer.setHeaderText("Header test text", view);
        downloadStateRenderer.show(OfflineState.DOWNLOADING, view);

        assertThat(getHeaderText()).hasText(inProgressText);
        assertThat(view.findViewById(R.id.header_download_state)).isVisible();
    }

    @Test
    public void displayHeaderTextWhenDownloaded() {
        downloadStateRenderer.setHeaderText("Header test text", view);
        downloadStateRenderer.show(OfflineState.DOWNLOADED, view);

        assertThat(getHeaderText()).hasText("Header test text");
        assertThat(view.findViewById(R.id.header_download_state)).isVisible();
    }

    @Test
    public void displayHeadTextWhenNoOffline() {
        downloadStateRenderer.setHeaderText("Header test text", view);
        downloadStateRenderer.show(OfflineState.NOT_OFFLINE, view);

        assertThat(getHeaderText()).hasText("Header test text");
        assertThat(view.findViewById(R.id.header_download_state)).isGone();
    }

    private TextView getHeaderText() {
        return (TextView) view.findViewById(R.id.header_text);
    }
}
