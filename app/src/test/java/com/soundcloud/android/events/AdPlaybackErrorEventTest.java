package com.soundcloud.android.events;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoAdSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import org.junit.Test;

public class AdPlaybackErrorEventTest {

    @Test
    public void shouldCreateEventForVideoAdBufferFailure() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final VideoAdSource source = videoAd.firstVideoSource();
        final AdPlaybackErrorEvent event = AdPlaybackErrorEvent.failToBuffer(videoAd, TestPlayerTransitions.buffering(), source);

        assertThat(event.bitrate()).isEqualTo(source.bitRateKbps());
        assertThat(event.host()).isEqualTo(source.url());
    }
    
}
