package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeTrackingEventTest {

    @Test
    public void createsEventForWhyAdsImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forWhyAdsImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1006");
    }

    @Test
    public void createsEventForWhyAdsClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forWhyAdsClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1006");
    }

    @Test
    public void createsEventForNavImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forNavImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1007");
    }

    @Test
    public void createsEventForNavClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forNavClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1007");
    }

    @Test
    public void createsEventForSettingsImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forSettingsImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1008");
    }

    @Test
    public void createsEventForSettingsClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forSettingsClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1008");
    }

    @Test
    public void createsEventForLikesImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forLikesImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1009");
    }

    @Test
    public void createsEventForLikesClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forLikesClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1009");
    }

    @Test
    public void createsEventForPlaylistItemImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlaylistItemImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1011");
    }

    @Test
    public void createsEventForPlaylistItemClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlaylistItemClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1011");
    }

    @Test
    public void createsEventForPlaylistPageImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlaylistPageImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1012");
    }

    @Test
    public void createsEventForPlaylistPageClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlaylistPageClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1012");
    }

    @Test
    public void createsEventForUpgradeButtonImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forUpgradeButtonImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:3002");
    }

    @Test
    public void createsEventForUpgradeButtonClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forUpgradeButtonClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:3002");
    }

    @Test
    public void createsEventForUpgradeCompleteImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forUpgradeSuccess();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPGRADE_SUCCESS);
    }

}