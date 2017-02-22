package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;

import android.support.annotation.NonNull;

public class NotificationTrackTest extends AndroidUnitTest {

    private TrackItem trackItem;

    @Before
    public void setUp() throws Exception {
        trackItem = PlayableFixtures.expectedTrackForPlayer();
    }

    @Test
    public void titleIsReplacedWithAdvertisementTextForAudioAd() {
        NotificationTrack viewModel = setupAudioAd();

        assertThat(viewModel.getTitle()).isEqualTo(resources().getString(R.string.ads_advertisement));
    }

    @Test
    public void creatorIsPopulatedForNormalTrack() {
        NotificationTrack viewModel = setupNormalTrack();

        assertThat(viewModel.getCreatorName()).isEqualTo(trackItem.creatorName());
    }

    @Test
    public void creatorIsEmptyForAudioAd() {
        NotificationTrack viewModel = setupAudioAd();

        assertThat(viewModel.getCreatorName()).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void titleIsTrackTitleForNormalTrack() {
        NotificationTrack viewModel = setupNormalTrack();

        assertThat(viewModel.getTitle()).isEqualTo(trackItem.title());
    }

    @NonNull
    private NotificationTrack setupAudioAd() {
        return new NotificationTrack(resources(), trackItem, true);
    }

    @NonNull
    private NotificationTrack setupNormalTrack() {
        return new NotificationTrack(resources(), trackItem, false);
    }

}
