package com.soundcloud.android.payments;

import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

class AvailableWebProducts {

    private static final String MID_TIER_PLAN_ID = "mid_tier";
    private static final String HIGH_TIER_PLAN_ID = "high_tier";

    private Optional<WebProduct> midTier = Optional.absent();
    private Optional<WebProduct> highTier = Optional.absent();

    static AvailableWebProducts single(WebProduct product) {
        return new AvailableWebProducts(Collections.singletonList(product));
    }

    static AvailableWebProducts empty() {
        return new AvailableWebProducts(Collections.<WebProduct>emptyList());
    }

    AvailableWebProducts(List<WebProduct> products) {
        for (WebProduct product : products) {
            if (MID_TIER_PLAN_ID.equals(product.getPlanId())) {
                midTier = Optional.of(product);
            } else if (HIGH_TIER_PLAN_ID.equals(product.getPlanId())) {
                highTier = Optional.of(product);
            }
        }
    }

    Optional<WebProduct> midTier() {
        return midTier;
    }

    Optional<WebProduct> highTier() {
        return highTier;
    }

}
