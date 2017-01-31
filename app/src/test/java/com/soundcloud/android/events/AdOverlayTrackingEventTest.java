package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

public class AdOverlayTrackingEventTest extends AndroidUnitTest {

    private TrackSourceInfo sourceInfo;

    @Before
    public void setUp() throws Exception {
        sourceInfo = new TrackSourceInfo("originScreen", true);
    }

    @Test
    public void shouldCreateEventFromLeaveBehindImpression() {
        LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(456));
        AdOverlayTrackingEvent uiEvent = AdOverlayTrackingEvent.forImpression(1000L,
                                                                              leaveBehindAd,
                                                                              Urn.forTrack(456),
                                                                              Urn.forUser(123),
                                                                              sourceInfo);
        assertThat(uiEvent.eventName()).isEqualTo(AdOverlayTrackingEvent.EventName.KIND_IMPRESSION);
        assertThat(uiEvent.getTimestamp()).isEqualTo(1000L);
        assertThat(uiEvent.adUrn()).isEqualTo(leaveBehindAd.getAdUrn());
        assertThat(uiEvent.monetizableTrack()).isEqualTo(Urn.forTrack(456));
        assertThat(uiEvent.adArtworkUrl()).isEqualTo(leaveBehindAd.getImageUrl());
        assertThat(uiEvent.trackingUrls()).containsExactly("leave_impression1", "leave_impression2");
    }

    @Test
    public void shouldCreateEventFromLeaveBehindClick() {
        LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(456));
        AdOverlayTrackingEvent uiEvent = AdOverlayTrackingEvent.forClick(1000L,
                                                                         leaveBehindAd,
                                                                         Urn.forTrack(456),
                                                                         Urn.forUser(123),
                                                                         sourceInfo);
        assertThat(uiEvent.eventName()).isEqualTo(AdOverlayTrackingEvent.EventName.KIND_CLICK);
        assertThat(uiEvent.getTimestamp()).isEqualTo(1000L);
        assertThat(uiEvent.adUrn()).isEqualTo(leaveBehindAd.getAdUrn());
        assertThat(uiEvent.monetizableTrack()).isEqualTo(Urn.forTrack(456));
        assertThat(uiEvent.adArtworkUrl()).isEqualTo(leaveBehindAd.getImageUrl());
        assertThat(uiEvent.clickTarget().get()).isEqualTo(leaveBehindAd.getClickthroughUrl());
        assertThat(uiEvent.trackingUrls()).containsExactly("leave_click1", "leave_click2");
    }
}
