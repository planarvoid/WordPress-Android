package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class PurchaseEvent extends TrackingEvent {

    public enum Subscription {
        MID_TIER("cn058f"),
        HIGH_TIER("1n0o91");

        private final String adjustToken;

        Subscription(String adjustToken) {
            this.adjustToken = adjustToken;
        }

        public String adjustToken() {
            return adjustToken;
        }
    }

    public static PurchaseEvent forMidTierSub(String rawPrice, String rawCurrency) {
        return new AutoValue_PurchaseEvent(defaultId(), defaultTimestamp(), Optional.absent(), Subscription.MID_TIER, rawPrice, rawCurrency);
    }

    public static PurchaseEvent forHighTierSub(String rawPrice, String rawCurrency) {
        return new AutoValue_PurchaseEvent(defaultId(), defaultTimestamp(), Optional.absent(), Subscription.HIGH_TIER, rawPrice, rawCurrency);
    }

    public abstract Subscription subscription();

    public String adjustToken() {
        return subscription().adjustToken();
    }

    public abstract String price();

    public abstract String currency();

    @Override
    public String getKind() {
        return subscription().toString();
    }

    @Override
    public PurchaseEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_PurchaseEvent(this.id(), this.timestamp(), Optional.of(referringEvent), this.subscription(), this.price(), this.currency());
    }
}
