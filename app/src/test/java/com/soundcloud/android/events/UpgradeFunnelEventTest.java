package com.soundcloud.android.events;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class UpgradeFunnelEventTest {

    private Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    @Test
    public void createsEventForWhyAdsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forWhyAdsImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.WHY_ADS.code());
    }

    @Test
    public void createsEventForWhyAdsClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forWhyAdsClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.WHY_ADS.code());
    }

    @Test
    public void createsEventForPlayerImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlayerImpression(Urn.forTrack(123));

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYER.code());
        assertThat(event.pageUrn().get()).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void createsEventForNavClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlayerClick(Urn.forTrack(123));

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYER.code());
        assertThat(event.pageUrn().get()).isEqualTo(Urn.forTrack(123).toString());
    }

    @Test
    public void createsEventForUpgradeButtonInSettingsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeFromSettingsImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.SETTINGS_UPGRADE.code());
        assertThat(event.adjustToken().get()).isEqualTo(UpgradeFunnelEvent.AdjustToken.SETTINGS);
    }

    @Test
    public void createsEventForUpgradeButtonInSettingsClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeFromSettingsClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.SETTINGS_UPGRADE.code());
    }

    @Test
    public void createsEventForLikesImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forLikesImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.LIKES.code());
    }

    @Test
    public void createsEventForLikesClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forLikesClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.LIKES.code());
    }

    @Test
    public void createsEventForSearchResultsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchResultsImpression(Screen.SEARCH_EVERYTHING);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.SEARCH_RESULTS.code());
        assertThat(event.pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void createsEventforSearchResultsClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchResultsClick(Screen.SEARCH_EVERYTHING);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.SEARCH_RESULTS.code());
        assertThat(event.pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void createsEventForSearchPremiumResultsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchPremiumResultsImpression(Screen.SEARCH_PREMIUM_CONTENT);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.SEARCH_RESULTS_GO.code());
        assertThat(event.pageName().get()).isEqualTo(Screen.SEARCH_PREMIUM_CONTENT.get());
    }

    @Test
    public void createsEventforSearchPremiumResultsClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchPremiumResultsClick(Screen.SEARCH_EVERYTHING);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.SEARCH_RESULTS_GO.code());
        assertThat(event.pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
    }

    @Test
    public void createsEventForPlaylistItemImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistItemImpression("screen", PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYLIST_ITEM.code());
    }

    @Test
    public void createsEventForPlaylistItemClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistItemClick("screen", PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYLIST_ITEM.code());
    }

    @Test
    public void createsEventForPlaylistPageImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistPageImpression(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYLIST_PAGE.code());
    }

    @Test
    public void createsEventForPlaylistPageClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistPageClick(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYLIST_PAGE.code());
    }

    @Test
    public void createsEventForPlaylistOverflowImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistOverflowImpression(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYLIST_OVERFLOW.code());
    }

    @Test
    public void createsEventForPlaylistOverflowClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistOverflowClick(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYLIST_OVERFLOW.code());
    }

    @Test
    public void createsEventForStreamUpsellImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forStreamImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.STREAM.code());
    }

    @Test
    public void createsEventForStreamUpsellClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forStreamClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.STREAM.code());
    }

    @Test
    public void createsEventForCollectionUpsellImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forCollectionImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.COLLECTION.code());
    }

    @Test
    public void createsEventForCollectionUpsellClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forCollectionClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.COLLECTION.code());
    }

    @Test
    public void createsEventForPlaylistTracksUpsellImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistTracksImpression(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYLIST_TRACKS.code());
        assertThat(event.pageUrn().get()).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void createsEventForPlaylistTracksUpsellClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistTracksClick(PLAYLIST_URN);

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.PLAYLIST_TRACKS.code());
        assertThat(event.pageUrn().get()).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void createsEventForUpgradeButtonImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forConversionBuyButtonImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CONVERSION_BUY.code());
    }

    @Test
    public void createsEventForUpgradeButtonClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forConversionBuyButtonClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CONVERSION_BUY.code());
    }

    @Test
    public void createsEventForPromoButtonImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forConversionPromoImpression();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CONVERSION_PROMO.code());
    }

    @Test
    public void createsEventForPromoButtonClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forConversionPromoClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CONVERSION_PROMO.code());
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
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.RESUBSCRIBE_BUTTON.code());
        assertThat(event.pageName().get()).isEqualTo("collection:offline_offboarding");
    }

    @Test
    public void createsEventForResubscribeButtonClick() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forResubscribeClick();

        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.RESUBSCRIBE_CLICK.toString());
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.RESUBSCRIBE_BUTTON.code());
        assertThat(event.pageName().get()).isEqualTo("collection:offline_offboarding");
    }
}
