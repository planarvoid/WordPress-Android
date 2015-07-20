package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.tracks.PromotedTrackItem;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PromotedTrackingEventTest {

    private PromotedListItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());

    @Test
    public void createsEventForPromoterClick() {
        PromotedTrackingEvent click = PromotedTrackingEvent.forPromoterClick(promotedTrack, "stream");

        assertCommonProperties(click);
        expect(click.getKind()).toEqual("click");
        expect(click.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN)).toEqual(promotedTrack.getEntityUrn().toString());
        expect(click.get(AdTrackingKeys.KEY_CLICK_TARGET_URN)).toEqual(promotedTrack.getPromoterUrn().get().toString());
        expect(click.get(AdTrackingKeys.KEY_PROMOTER_URN)).toEqual(promotedTrack.getPromoterUrn().get().toString());
    }

    @Test
    public void createsEventForTrackClick() {
        PromotedTrackingEvent click = PromotedTrackingEvent.forItemClick(promotedTrack, "stream");

        assertCommonProperties(click);
        expect(click.getKind()).toEqual("click");
        expect(click.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN)).toEqual(promotedTrack.getEntityUrn().toString());
        expect(click.get(AdTrackingKeys.KEY_CLICK_TARGET_URN)).toEqual(promotedTrack.getEntityUrn().toString());
        expect(click.get(AdTrackingKeys.KEY_PROMOTER_URN)).toEqual(promotedTrack.getPromoterUrn().get().toString());
    }

    @Test
    public void createsEventForImpression() {
        PromotedTrackingEvent impression = PromotedTrackingEvent.forImpression(promotedTrack, "stream");

        assertCommonProperties(impression);
        expect(impression.getKind()).toEqual("impression");
        expect(impression.get(AdTrackingKeys.KEY_PROMOTER_URN)).toEqual(promotedTrack.getPromoterUrn().get().toString());
    }

    @Test
    public void omitsPromoterUrnPropertyIfPromoterIsAbsent() {
        PromotedListItem noPromoter = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrackWithoutPromoter());
        PromotedTrackingEvent click = PromotedTrackingEvent.forItemClick(noPromoter, "stream");

        expect(click.get(AdTrackingKeys.KEY_PROMOTER_URN)).toBeNull();
    }

    private void assertCommonProperties(PromotedTrackingEvent event) {
        expect(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN)).toEqual("stream");
        expect(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).toEqual("promoted");
        expect(event.get(AdTrackingKeys.KEY_AD_URN)).toEqual(promotedTrack.getAdUrn());
        expect(event.get(AdTrackingKeys.KEY_AD_TRACK_URN)).toEqual(promotedTrack.getEntityUrn().toString());
    }

}
