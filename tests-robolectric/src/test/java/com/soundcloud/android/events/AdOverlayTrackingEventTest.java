package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class AdOverlayTrackingEventTest {

    private TrackSourceInfo sourceInfo;

    @Before
    public void setUp() throws Exception {
        sourceInfo = new TrackSourceInfo("originScreen", true);
    }

    @Test
    public void shouldCreateEventFromLeaveBehindImpression() {
        PropertySet audioAd = TestPropertySets.leaveBehindForPlayer();
        AdOverlayTrackingEvent uiEvent = AdOverlayTrackingEvent.forImpression(1000L, audioAd, Urn.forTrack(456), Urn.forUser(123), sourceInfo);
        expect(uiEvent.getKind()).toEqual(AdOverlayTrackingEvent.KIND_IMPRESSION);
        expect(uiEvent.getTimestamp()).toEqual(1000L);
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).toEqual(audioAd.get(LeaveBehindProperty.LEAVE_BEHIND_URN));
        expect(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).toEqual(Urn.forTrack(456).toString());
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).toEqual(audioAd.get(LeaveBehindProperty.IMAGE_URL).toString());
        expect(uiEvent.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL)).toEqual(audioAd.get(LeaveBehindProperty.CLICK_THROUGH_URL).toString());
        expect(uiEvent.getTrackingUrls()).toContain("leaveBehindTrackingImpressionUrl1", "leaveBehindTrackingImpressionUrl1");
    }

    @Test
    public void shouldCreateEventFromLeaveBehindClick() {
        PropertySet audioAd = TestPropertySets.leaveBehindForPlayer();
        AdOverlayTrackingEvent uiEvent = AdOverlayTrackingEvent.forClick(1000L, audioAd, Urn.forTrack(456), Urn.forUser(123), sourceInfo);
        expect(uiEvent.getKind()).toEqual(AdOverlayTrackingEvent.KIND_CLICK);
        expect(uiEvent.getTimestamp()).toEqual(1000L);
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).toEqual(audioAd.get(LeaveBehindProperty.LEAVE_BEHIND_URN));
        expect(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).toEqual(Urn.forTrack(456).toString());
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).toEqual(audioAd.get(LeaveBehindProperty.IMAGE_URL).toString());
        expect(uiEvent.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL)).toEqual(audioAd.get(LeaveBehindProperty.CLICK_THROUGH_URL).toString());
        expect(uiEvent.getTrackingUrls()).toContain("leaveBehindTrackingClickTroughUrl1", "leaveBehindTrackingClickTroughUrl2");
    }
}