package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdOverlayTrackingEventTest extends AndroidUnitTest {

    private TrackSourceInfo sourceInfo;

    @Before
    public void setUp() throws Exception {
        sourceInfo = new TrackSourceInfo("originScreen", true);
    }

    @Test
    public void shouldCreateEventFromLeaveBehindImpression() {
        LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(456));
        AdOverlayTrackingEvent uiEvent = AdOverlayTrackingEvent.forImpression(1000L, leaveBehindAd, Urn.forTrack(456), Urn.forUser(123), sourceInfo);
        assertThat(uiEvent.getKind()).isEqualTo(AdOverlayTrackingEvent.KIND_IMPRESSION);
        assertThat(uiEvent.getTimestamp()).isEqualTo(1000L);
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(leaveBehindAd.getAdUrn().toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(456).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).isEqualTo(leaveBehindAd.getImageUrl());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL)).isEqualTo(leaveBehindAd.getClickthroughUrl().toString());
        assertThat(uiEvent.getTrackingUrls()).containsExactly("leave_impression1", "leave_impression2");
    }

    @Test
    public void shouldCreateEventFromLeaveBehindClick() {
        LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(456));
        AdOverlayTrackingEvent uiEvent = AdOverlayTrackingEvent.forClick(1000L, leaveBehindAd, Urn.forTrack(456), Urn.forUser(123), sourceInfo);
        assertThat(uiEvent.getKind()).isEqualTo(AdOverlayTrackingEvent.KIND_CLICK);
        assertThat(uiEvent.getTimestamp()).isEqualTo(1000L);
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(leaveBehindAd.getAdUrn().toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(456).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).isEqualTo(leaveBehindAd.getImageUrl());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL)).isEqualTo(leaveBehindAd.getClickthroughUrl().toString());
        assertThat(uiEvent.getTrackingUrls()).containsExactly("leave_click1", "leave_click2");
    }
}