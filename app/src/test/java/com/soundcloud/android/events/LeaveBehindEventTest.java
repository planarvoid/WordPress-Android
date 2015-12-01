package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class LeaveBehindEventTest extends AndroidUnitTest {

    @Test
    public void createsEventFromPlayerExpanded() {
        final LeaveBehindAd leaveBehind = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));
        final AdOverlayEvent event = AdOverlayEvent.shown(Urn.forTrack(123), leaveBehind, null);
        assertThat(event.getKind()).isEqualTo(0);
    }

    @Test
    public void createsEventFromPlayerCollapsed() {
        final AdOverlayEvent event = AdOverlayEvent.hidden();
        assertThat(event.getKind()).isEqualTo(1);
    }

}