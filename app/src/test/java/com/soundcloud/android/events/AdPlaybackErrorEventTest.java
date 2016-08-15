package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoAdSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdPlaybackErrorEventTest extends AndroidUnitTest {

    @Test
    public void shouldCreateEventForVideoAdBufferFailure() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final VideoAdSource source = videoAd.getFirstSource();
        final AdPlaybackErrorEvent event = AdPlaybackErrorEvent.failToBuffer(videoAd, TestPlayerTransitions.buffering(), source);

        assertThat(event.getKind()).isEqualTo(AdPlaybackErrorEvent.KIND_FAIL_TO_BUFFER);
        assertThat(event.getBitrate()).isEqualTo(source.getBitRateKbps());
        assertThat(event.getHost()).isEqualTo(source.getUrl());
    }
    
}