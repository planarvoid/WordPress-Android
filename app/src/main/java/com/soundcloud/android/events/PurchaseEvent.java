package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class PurchaseEvent extends NewTrackingEvent {
    public enum Subscription {
        HIGH_TIER("high_tier_sub");

        private final String key;

        Subscription(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    public static PurchaseEvent forHighTierSub(String rawPrice, String rawCurrency) {
        return new AutoValue_PurchaseEvent(defaultId(), defaultTimestamp(), Optional.absent(), Subscription.HIGH_TIER, rawPrice, rawCurrency);
    }

    public abstract Subscription subscription();

    public abstract String price();

    public abstract String currency();

    @Override
    public String getKind() {
        return subscription().toString();
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_PurchaseEvent(this.id(), this.timestamp(), Optional.of(referringEvent), this.subscription(), this.price(), this.currency());
    }
}
