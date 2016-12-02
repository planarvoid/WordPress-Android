package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

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

        assertThat(event).isInstanceOf(AdDeliveryEvent.class);
        assertThat(event.monetizableUrn().get()).isEqualTo(TRACK_URN);
        assertThat(event.adUrn()).isEqualTo(VIDEO_AD_URN);
        assertThat(event.adRequestId()).isEqualTo("uuid");
        assertThat(event.playerVisible()).isFalse();
        assertThat(event.inForeground()).isTrue();
    }
}
