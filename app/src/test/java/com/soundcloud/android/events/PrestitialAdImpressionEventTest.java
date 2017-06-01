package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class PrestitialAdImpressionEventTest extends AndroidUnitTest {

    @Test
    public void createsPrestitialAdImpressionEventForDisplay() {
        final PrestitialAdImpressionEvent event = PrestitialAdImpressionEvent.createForDisplay(AdFixtures.visualPrestitialAd());

        assertThat(event.impressionName()).isEqualTo("display");
        assertThat(event.impressionUrls()).contains("visual_impression1", "visual_impression2");
        assertThat(event.monetizationType()).isEqualTo("prestitial");
        assertThat(event.urn()).isEqualTo(Urn.forAd("ads", "123"));
    }

    @Test
    public void createsSponsoredSessionImpressionEventForOptIn() {
        final PrestitialAdImpressionEvent event = PrestitialAdImpressionEvent.createForSponsoredSession(AdFixtures.sponsoredSessionAd(), false);

        assertThat(event.impressionName()).isEqualTo("display");
        assertThat(event.impressionUrls()).contains("sponsored_session_impression1", "sponsored_session_impression2");
        assertThat(event.monetizationType()).isEqualTo("sponsored_session");
        assertThat(event.urn()).isEqualTo(Urn.forAd("ads", "123"));
    }

    @Test
    public void createsSponsoredSessionImpressionEventForEndCard() {
        final PrestitialAdImpressionEvent event = PrestitialAdImpressionEvent.createForSponsoredSession(AdFixtures.sponsoredSessionAd(), true);

        assertThat(event.impressionName()).isEqualTo("end_card");
        assertThat(event.impressionUrls()).isEmpty();
        assertThat(event.monetizationType()).isEqualTo("sponsored_session");
        assertThat(event.urn()).isEqualTo(Urn.forAd("ads", "123"));
    }
}