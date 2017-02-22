package com.soundcloud.android.playback.widget;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.strings.Strings;
import org.junit.Test;

public class WidgetItemTest extends AndroidUnitTest {

    private TrackItem trackItem = PlayableFixtures.expectedTrackForWidget();

    private WidgetItem viewModel;

    @Test
    public void creatorIsPopulatedForNormalTrack() {
        viewModel = WidgetItem.fromTrackItem(trackItem);

        assertThat(viewModel.getCreatorName()).isEqualTo(trackItem.creatorName());
    }

    @Test
    public void titleIsTrackTitleForNormalTrack() {
        viewModel = WidgetItem.fromTrackItem(trackItem);

        assertThat(viewModel.getTitle()).isEqualTo(trackItem.title());
    }

    @Test
    public void titleIsAdvertisementTextForAudioAd() {
        viewModel = WidgetItem.forAudioAd(resources());

        assertThat(viewModel.getTitle()).isEqualTo(resources().getString(R.string.ads_advertisement));
    }


    @Test
    public void audioAdIsPlayableFromWidget() {
        viewModel = WidgetItem.forAudioAd(resources());

        assertThat(viewModel.isPlayableFromWidget()).isTrue();
    }

    @Test
    public void videoAdIsNotPlayableFromWidget() {
        viewModel = WidgetItem.forVideoAd(resources());

        assertThat(viewModel.isPlayableFromWidget()).isFalse();
    }

    @Test
    public void titleIsAdvertisementTextForVideoAd() {
        viewModel = WidgetItem.forVideoAd(resources());

        assertThat(viewModel.getTitle()).isEqualTo(resources().getString(R.string.ads_reopen_to_continue_short));
    }

    @Test
    public void creatorIsEmptyForAd() {
        viewModel = WidgetItem.forAudioAd(resources());

        assertThat(viewModel.getCreatorName()).isEqualTo(Strings.EMPTY);
    }

}
