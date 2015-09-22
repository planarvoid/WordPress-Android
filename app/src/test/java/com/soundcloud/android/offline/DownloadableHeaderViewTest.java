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

public class DownloadableHeaderViewTest extends AndroidUnitTest {

    private final String inProgressText = resources().getString(R.string.offline_update_in_progress);
    private DownloadableHeaderView downloadableHeaderView;

    @Mock private Fragment fragment;
    @Mock private PlaybackInitiator playbackInitiator;
    private View header;

    @Before
    public void setUp() throws Exception {
        header = View.inflate(context(), R.layout.track_likes_header, null);
        downloadableHeaderView = new DownloadableHeaderView(resources());
        downloadableHeaderView.onViewCreated(header);
    }

    @Ignore("Blocks on AnimUtils")
    public void displayInProgressTextWhenDownloading() {
        downloadableHeaderView.setHeaderText("Header test text");
        downloadableHeaderView.show(OfflineState.DOWNLOADING);

        assertThat(getHeaderText()).hasText(inProgressText);
        assertThat(header.findViewById(R.id.header_download_state)).isVisible();
    }

    @Test
    public void displayHeaderTextWhenDownloaded() {
        downloadableHeaderView.setHeaderText("Header test text");
        downloadableHeaderView.show(OfflineState.DOWNLOADED);

        assertThat(getHeaderText()).hasText("Header test text");
        assertThat(header.findViewById(R.id.header_download_state)).isVisible();
    }

    @Test
    public void displayHeadTextWhenNoOffline() {
        downloadableHeaderView.setHeaderText("Header test text");
        downloadableHeaderView.show(OfflineState.NO_OFFLINE);

        assertThat(getHeaderText()).hasText("Header test text");
        assertThat(header.findViewById(R.id.header_download_state)).isGone();
    }

    private TextView getHeaderText() {
        return (TextView) header.findViewById(R.id.header_text);
    }
}
