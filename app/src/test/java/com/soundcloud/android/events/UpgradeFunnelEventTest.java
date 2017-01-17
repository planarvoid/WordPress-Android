package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class UpgradeFunnelEventTest {

    private Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    @Test
    public void createsEventForWhyAdsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forWhyAdsImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.WHY_ADS.toString());
    }

    @Test
    public void createsEventForWhyAdsClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forWhyAdsClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.WHY_ADS.toString());
    }

    @Test
    public void createsEventForPlayerImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlayerImpression(Urn.forTrack(123));

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYER.toString());
        assertThat(event.pageUrn().get()).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void createsEventForNavClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlayerClick(Urn.forTrack(123));

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYER.toString());
        assertThat(event.pageUrn().get()).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void createsEventForSettingsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSettingsImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.SETTINGS.toString());
    }

    @Test
    public void createsEventForSettingsClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSettingsClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.SETTINGS.toString());
    }

    @Test
    public void createsEventForUpgradeButtonInSettingsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeFromSettingsImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.SETTINGS_UPGRADE.toString());
    }

    @Test
    public void createsEventForUpgradeButtonInSettingsClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeFromSettingsClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.SETTINGS_UPGRADE.toString());
    }

    @Test
    public void createsEventForLikesImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forLikesImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.LIKES.toString());
    }

    @Test
    public void createsEventForLikesClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forLikesClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.LIKES.toString());
    }

    @Test
    public void createsEventForSearchResultsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchResultsImpression(Screen.SEARCH_EVERYTHING);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.SEARCH_RESULTS.toString());
        assertThat(event.pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void createsEventforSearchResultsClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchResultsClick(Screen.SEARCH_EVERYTHING);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.SEARCH_RESULTS.toString());
        assertThat(event.pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void createsEventForSearchPremiumResultsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchPremiumResultsImpression(Screen.SEARCH_PREMIUM_CONTENT);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.SEARCH_RESULTS_GO.toString());
        assertThat(event.pageName().get()).isEqualTo(Screen.SEARCH_PREMIUM_CONTENT.get());
    }

    @Test
    public void createsEventforSearchPremiumResultsClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchPremiumResultsClick(Screen.SEARCH_EVERYTHING);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.SEARCH_RESULTS_GO.toString());
        assertThat(event.pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void createsEventForPlaylistItemImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistItemImpression("screen", PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYLIST_ITEM.toString());
    }

    @Test
    public void createsEventForPlaylistItemClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistItemClick("screen", PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYLIST_ITEM.toString());
    }

    @Test
    public void createsEventForPlaylistPageImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistPageImpression(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYLIST_PAGE.toString());
    }

    @Test
    public void createsEventForPlaylistPageClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistPageClick(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYLIST_PAGE.toString());
    }

    @Test
    public void createsEventForPlaylistOverflowImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistOverflowImpression(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYLIST_OVERFLOW.toString());
    }

    @Test
    public void createsEventForPlaylistOverflowClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistOverflowClick(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYLIST_OVERFLOW.toString());
    }

    @Test
    public void createsEventForStreamUpsellImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forStreamImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.STREAM.toString());
    }

    @Test
    public void createsEventForStreamUpsellClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forStreamClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.STREAM.toString());
    }

    @Test
    public void createsEventForCollectionUpsellImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forCollectionImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.COLLECTION.toString());
    }

    @Test
    public void createsEventForCollectionUpsellClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forCollectionClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.COLLECTION.toString());
    }

    @Test
    public void createsEventForPlaylistTracksUpsellImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistTracksImpression(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYLIST_TRACKS.toString());
        assertThat(event.pageUrn().get()).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void createsEventForPlaylistTracksUpsellClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistTracksClick(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.PLAYLIST_TRACKS.toString());
        assertThat(event.pageUrn().get()).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void createsEventForUpgradeButtonImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeButtonImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.UPGRADE_BUTTON.toString());
    }

    @Test
    public void createsEventForUpgradeButtonClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeButtonClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.UPGRADE_BUTTON.toString());
    }

    @Test
    public void createsEventForPromoButtonImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradePromoImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.UPGRADE_PROMO.toString());
    }

    @Test
    public void createsEventForPromoButtonClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradePromoClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.UPGRADE_PROMO.toString());
    }

    @Test
    public void createsEventForUpgradeCompleteImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeSuccess();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPGRADE_SUCCESS.toString());
    }

    @Test
    public void createsEventForResubscribeImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forResubscribeImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.RESUBSCRIBE_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.RESUBSCRIBE_BUTTON.toString());
        assertThat(event.pageName().get()).isEqualTo("collection:offline_offboarding");
    }

    @Test
    public void createsEventForResubscribeButtonClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forResubscribeClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.RESUBSCRIBE_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.Tcode.RESUBSCRIBE_BUTTON.toString());
        assertThat(event.pageName().get()).isEqualTo("collection:offline_offboarding");
    }
}
