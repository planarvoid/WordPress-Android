package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import org.junit.Test;

public class PromotedTrackingEventTest extends AndroidUnitTest {

    private PromotedListItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());

    @Test
    public void createsEventForPromoterClick() {
        PromotedTrackingEvent click = PromotedTrackingEvent.forPromoterClick(promotedTrack, "stream");

        assertCommonProperties(click);
        assertThat(click.getKind()).isEqualTo("click");
        assertThat(click.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN)).isEqualTo(promotedTrack.getEntityUrn().toString());
        assertThat(click.get(AdTrackingKeys.KEY_CLICK_TARGET_URN)).isEqualTo(promotedTrack.getPromoterUrn().get().toString());
        assertThat(click.get(AdTrackingKeys.KEY_PROMOTER_URN)).isEqualTo(promotedTrack.getPromoterUrn().get().toString());
    }

    @Test
    public void createsEventForTrackClick() {
        PromotedTrackingEvent click = PromotedTrackingEvent.forItemClick(promotedTrack, "stream");

        assertCommonProperties(click);
        assertThat(click.getKind()).isEqualTo("click");
        assertThat(click.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN)).isEqualTo(promotedTrack.getEntityUrn().toString());
        assertThat(click.get(AdTrackingKeys.KEY_CLICK_TARGET_URN)).isEqualTo(promotedTrack.getEntityUrn().toString());
        assertThat(click.get(AdTrackingKeys.KEY_PROMOTER_URN)).isEqualTo(promotedTrack.getPromoterUrn().get().toString());
    }

    @Test
    public void createsEventForImpression() {
        PromotedTrackingEvent impression = PromotedTrackingEvent.forImpression(promotedTrack, "stream");

        assertCommonProperties(impression);
        assertThat(impression.getKind()).isEqualTo("impression");
        assertThat(impression.get(AdTrackingKeys.KEY_PROMOTER_URN)).isEqualTo(promotedTrack.getPromoterUrn().get().toString());
    }

    @Test
    public void omitsPromoterUrnPropertyIfPromoterIsAbsent() {
        PromotedListItem noPromoter = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrackWithoutPromoter());
        PromotedTrackingEvent click = PromotedTrackingEvent.forItemClick(noPromoter, "stream");

        assertThat(click.get(AdTrackingKeys.KEY_PROMOTER_URN)).isNull();
    }

    private void assertCommonProperties(PromotedTrackingEvent event) {
        assertThat(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN)).isEqualTo("stream");
        assertThat(event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("promoted");
        assertThat(event.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(promotedTrack.getAdUrn());
        assertThat(event.get(AdTrackingKeys.KEY_AD_TRACK_URN)).isEqualTo(promotedTrack.getEntityUrn().toString());
    }

}
