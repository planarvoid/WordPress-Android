package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class UpgradeTrackingEventTest {

    private Urn PLAYLIST_URN = Urn.forPlaylist(123L);

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
    public void createsEventForPlayerImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlayerImpression(Urn.forTrack(123));

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1017");
        assertThat(event.get(AdTrackingKeys.KEY_PAGE_URN)).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void createsEventForNavClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlayerClick(Urn.forTrack(123));

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1017");
        assertThat(event.get(AdTrackingKeys.KEY_PAGE_URN)).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void createsEventForSettingsImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forSettingsImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1007");
    }

    @Test
    public void createsEventForSettingsClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forSettingsClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1007");
    }

    @Test
    public void createsEventForUpgradeButtonInSettingsImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forUpgradeFromSettingsImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1008");
    }

    @Test
    public void createsEventForUpgradeButtonInSettingsClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forUpgradeFromSettingsClick();

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
    public void createsEventForSearchResultsImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forSearchResultsImpression(Screen.SEARCH_EVERYTHING);

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1025");
        assertThat(event.get(UpgradeTrackingEvent.KEY_PAGE_NAME)).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void createsEventforSearchResultsClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forSearchResultsClick(Screen.SEARCH_EVERYTHING);

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1025");
        assertThat(event.get(UpgradeTrackingEvent.KEY_PAGE_NAME)).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void createsEventForSearchPremiumResultsImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forSearchPremiumResultsImpression(Screen.SEARCH_PREMIUM_CONTENT);

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1026");
        assertThat(event.get(UpgradeTrackingEvent.KEY_PAGE_NAME)).isEqualTo(Screen.SEARCH_PREMIUM_CONTENT.get());
    }

    @Test
    public void createsEventforSearchPremiumResultsClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forSearchPremiumResultsClick(Screen.SEARCH_EVERYTHING);

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1026");
        assertThat(event.get(UpgradeTrackingEvent.KEY_PAGE_NAME)).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void createsEventForPlaylistItemImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlaylistItemImpression("screen", PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1011");
    }

    @Test
    public void createsEventForPlaylistItemClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlaylistItemClick("screen", PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1011");
    }

    @Test
    public void createsEventForPlaylistPageImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlaylistPageImpression(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1012");
    }

    @Test
    public void createsEventForPlaylistPageClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlaylistPageClick(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1012");
    }

    @Test
    public void createsEventForStreamUpsellImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forStreamImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1027");
    }

    @Test
    public void createsEventForStreamUpsellClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forStreamClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1027");
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

    @Test
    public void createsEventForResubscribeImpression() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forResubscribeImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_RESUBSCRIBE_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:4002");
        assertThat(event.get(UpgradeTrackingEvent.KEY_PAGE_NAME)).isEqualTo("collection:offline_offboarding");
    }

    @Test
    public void createsEventForResubscribeButtonClick() {
        UpgradeTrackingEvent event = UpgradeTrackingEvent.forResubscribeClick();

        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_RESUBSCRIBE_CLICK);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:4002");
        assertThat(event.get(UpgradeTrackingEvent.KEY_PAGE_NAME)).isEqualTo("collection:offline_offboarding");
    }
}
