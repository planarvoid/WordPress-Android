package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PromotedTrackEventTest {

    private PromotedTrackItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());

    @Test
    public void createsEventForPromoterClick() {
        PromotedTrackEvent click = PromotedTrackEvent.forPromoterClick(promotedTrack, 123L, "stream");

        assertCommonProperties(click);
        expect(click.getKind()).toEqual("click");
        expect(click.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN)).toEqual(promotedTrack.getEntityUrn().toString());
        expect(click.get(AdTrackingKeys.KEY_CLICK_TARGET_URN)).toEqual(promotedTrack.getPromoterUrn().get().toString());
        expect(click.get(AdTrackingKeys.KEY_PROMOTER_URN)).toEqual(promotedTrack.getPromoterUrn().get().toString());
    }

    @Test
    public void createsEventForTrackClick() {
        PromotedTrackEvent click = PromotedTrackEvent.forTrackClick(promotedTrack, 123L, "stream");

        assertCommonProperties(click);
        expect(click.getKind()).toEqual("click");
        expect(click.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN)).toEqual(promotedTrack.getEntityUrn().toString());
        expect(click.get(AdTrackingKeys.KEY_CLICK_TARGET_URN)).toEqual(promotedTrack.getEntityUrn().toString());
        expect(click.get(AdTrackingKeys.KEY_PROMOTER_URN)).toEqual(promotedTrack.getPromoterUrn().get().toString());
    }

    @Test
    public void createsEventForImpression() {
        PromotedTrackEvent impression = PromotedTrackEvent.forImpression(promotedTrack, 123L, "stream");

        assertCommonProperties(impression);
        expect(impression.getKind()).toEqual("impression");
        expect(impression.get(AdTrackingKeys.KEY_PROMOTER_URN)).toEqual(promotedTrack.getPromoterUrn().get().toString());
    }

    @Test
    public void omitsPromoterUrnPropertyIfPromoterIsAbsent() {
        PromotedTrackItem noPromoter = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrackWithoutPromoter());
        PromotedTrackEvent click = PromotedTrackEvent.forTrackClick(noPromoter, 123L, "stream");

        expect(click.get(AdTrackingKeys.KEY_PROMOTER_URN)).toBeNull();
    }

    private void assertCommonProperties(PromotedTrackEvent event) {
        expect(event.getTimestamp()).toEqual(123L);
        expect(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN)).toEqual("stream");
        expect(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).toEqual("promoted");
        expect(event.get(AdTrackingKeys.KEY_AD_URN)).toEqual(promotedTrack.getAdUrn());
        expect(event.get(AdTrackingKeys.KEY_AD_TRACK_URN)).toEqual(promotedTrack.getEntityUrn().toString());
    }

}
