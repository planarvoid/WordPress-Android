package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PurchaseEventTest {

    @Test
    public void createsHighTierPurchaseEvent() {
        PurchaseEvent event = PurchaseEvent.forHighTierSub("9.99", "USD");

        assertThat(event.getKind()).isEqualTo(PurchaseEvent.KIND_HIGH_TIER_SUB);
        assertThat(event.getPrice()).isEqualTo("9.99");
        assertThat(event.getCurrency()).isEqualTo("USD");
    }

}
