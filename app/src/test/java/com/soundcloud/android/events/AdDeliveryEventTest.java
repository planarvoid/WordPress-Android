package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdDeliveryEventTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(30L);
    private static final Urn VIDEO_AD_URN = Urn.forAd("dfp", "video");

    @Test
    public void shouldCreateEventForAdDelivery() {
        AdDeliveryEvent event = AdDeliveryEvent.adDelivered(Optional.of(TRACK_URN),
                                                            VIDEO_AD_URN,
                                                            "uuid",
                                                            false,
                                                            true);

        assertThat(event.getKind()).isEqualTo(AdDeliveryEvent.AD_DELIVERED_KIND);
        assertThat(event.get("monetizable_track_urn")).isEqualTo(TRACK_URN.toString());
        assertThat(event.adUrn).isEqualTo(VIDEO_AD_URN);
        assertThat(event.adRequestId).isEqualTo("uuid");
        assertThat(event.playerVisible).isFalse();
        assertThat(event.inForeground).isTrue();
    }
}
