package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpsellTrackingEventTest {

    @Test
    public void createsEventForWhyAdsImpression() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forWhyAdsImpression();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_IMPRESSION);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1006");
    }

    @Test
    public void createsEventForWhyAdsClick() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forWhyAdsClick();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_CLICK);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1006");
    }

    @Test
    public void createsEventForNavImpression() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forNavImpression();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_IMPRESSION);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1007");
    }

    @Test
    public void createsEventForNavClick() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forNavClick();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_CLICK);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1007");
    }

    @Test
    public void createsEventForSettingsImpression() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forSettingsImpression();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_IMPRESSION);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1008");
    }

    @Test
    public void createsEventForSettingsClick() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forSettingsClick();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_CLICK);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1008");
    }

    @Test
    public void createsEventForLikesImpression() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forLikesImpression();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_IMPRESSION);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1009");
    }

    @Test
    public void createsEventForLikesClick() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forLikesClick();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_CLICK);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1009");
    }

    @Test
    public void createsEventForPlaylistItemImpression() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forPlaylistItemImpression();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_IMPRESSION);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1011");
    }

    @Test
    public void createsEventForPlaylistItemClick() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forPlaylistItemClick();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_CLICK);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1011");
    }

    @Test
    public void createsEventForPlaylistPageImpression() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forPlaylistPageImpression();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_IMPRESSION);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1012");
    }

    @Test
    public void createsEventForPlaylistPageClick() {
        UpsellTrackingEvent event = UpsellTrackingEvent.forPlaylistPageClick();

        assertThat(event.getKind()).isEqualTo(UpsellTrackingEvent.KIND_CLICK);
        assertThat(event.get(UpsellTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1012");
    }

}