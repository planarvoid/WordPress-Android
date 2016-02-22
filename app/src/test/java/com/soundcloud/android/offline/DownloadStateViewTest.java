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

public class DownloadStateViewTest extends AndroidUnitTest {

    private final String inProgressText = resources().getString(R.string.offline_update_in_progress);
    private DownloadStateView downloadStateView;

    @Mock private Fragment fragment;
    @Mock private PlaybackInitiator playbackInitiator;
    private View header;

    @Before
    public void setUp() throws Exception {
        header = View.inflate(context(), R.layout.track_likes_header, null);
        downloadStateView = new DownloadStateView(resources());
        downloadStateView.onViewCreated(header);
    }

    @Ignore("Blocks on AnimUtils")
    public void displayInProgressTextWhenDownloading() {
        downloadStateView.setHeaderText("Header test text");
        downloadStateView.show(OfflineState.DOWNLOADING);

        assertThat(getHeaderText()).hasText(inProgressText);
        assertThat(header.findViewById(R.id.header_download_state)).isVisible();
    }

    @Test
    public void displayHeaderTextWhenDownloaded() {
        downloadStateView.setHeaderText("Header test text");
        downloadStateView.show(OfflineState.DOWNLOADED);

        assertThat(getHeaderText()).hasText("Header test text");
        assertThat(header.findViewById(R.id.header_download_state)).isVisible();
    }

    @Test
    public void displayHeadTextWhenNoOffline() {
        downloadStateView.setHeaderText("Header test text");
        downloadStateView.show(OfflineState.NOT_OFFLINE);

        assertThat(getHeaderText()).hasText("Header test text");
        assertThat(header.findViewById(R.id.header_download_state)).isGone();
    }

    private TextView getHeaderText() {
        return (TextView) header.findViewById(R.id.header_text);
    }
}
