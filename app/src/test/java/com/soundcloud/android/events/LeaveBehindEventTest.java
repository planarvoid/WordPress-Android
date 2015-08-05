package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

public class LeaveBehindEventTest {

    @Test
    public void createsEventFromPlayerExpanded() {
        AdOverlayEvent event = AdOverlayEvent.shown(Urn.forTrack(123), PropertySet.create(), null);
        assertThat(event.getKind()).isEqualTo(0);
    }

    @Test
    public void createsEventFromPlayerCollapsed() {
        AdOverlayEvent event = AdOverlayEvent.hidden();
        assertThat(event.getKind()).isEqualTo(1);
    }

}