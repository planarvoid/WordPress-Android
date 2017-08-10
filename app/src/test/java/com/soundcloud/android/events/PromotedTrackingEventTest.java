package com.soundcloud.android.events;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PromotedTrackingEventTest {

    private TrackItem promotedTrack = PlayableFixtures.expectedPromotedTrack();

    @Test
    public void createsEventForPromoterClick() {
        PromotedTrackingEvent click = PromotedTrackingEvent.forPromoterClick(promotedTrack, "stream");

        assertCommonProperties(click);
        assertThat(click.kind()).isEqualTo(PromotedTrackingEvent.Kind.KIND_CLICK);
        assertThat(click.clickObject().get()).isEqualTo(promotedTrack.getUrn());
        assertThat(click.clickTarget().get()).isEqualTo(promotedTrack.promoterUrn().get());
        assertThat(click.promoterUrn().get()).isEqualTo(promotedTrack.promoterUrn().get());
        assertThat(click.impressionObject().isPresent()).isFalse();
    }

    @Test
    public void createsEventForTrackClick() {
        PromotedTrackingEvent click = PromotedTrackingEvent.forItemClick(promotedTrack, "stream");

        assertCommonProperties(click);
        assertThat(click.kind()).isEqualTo(PromotedTrackingEvent.Kind.KIND_CLICK);
        assertThat(click.clickObject().get()).isEqualTo(promotedTrack.getUrn());
        assertThat(click.clickTarget().get()).isEqualTo(promotedTrack.getUrn());
        assertThat(click.promoterUrn().get()).isEqualTo(promotedTrack.promoterUrn().get());
        assertThat(click.impressionObject().isPresent()).isFalse();
    }

    @Test
    public void createsEventForImpression() {
        PromotedTrackingEvent impression = PromotedTrackingEvent.forImpression(promotedTrack, "stream");

        assertCommonProperties(impression);
        assertThat(impression.kind()).isEqualTo(PromotedTrackingEvent.Kind.KIND_IMPRESSION);
        assertThat(impression.promoterUrn().get()).isEqualTo(promotedTrack.promoterUrn().get());
        assertThat(impression.impressionObject().get()).isEqualTo(promotedTrack.getUrn());
    }

    @Test
    public void omitsPromoterUrnPropertyIfPromoterIsAbsent() {
        TrackItem noPromoter = PlayableFixtures.expectedPromotedTrackWithoutPromoter();
        PromotedTrackingEvent click = PromotedTrackingEvent.forItemClick(noPromoter, "stream");

        assertThat(click.promoterUrn().isPresent()).isFalse();
        assertThat(click.impressionObject().isPresent()).isFalse();
    }

    private void assertCommonProperties(PromotedTrackingEvent event) {
        assertThat(event.originScreen()).isEqualTo("stream");
        assertThat(event.monetizationType()).isEqualTo("promoted");
        assertThat(event.adUrn()).isEqualTo(promotedTrack.adUrn());
    }

}
