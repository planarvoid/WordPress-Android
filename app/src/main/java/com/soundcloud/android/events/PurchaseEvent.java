package com.soundcloud.android.events;

public class PurchaseEvent extends LegacyTrackingEvent {

    public static final String KIND_HIGH_TIER_SUB = "high_tier_sub";

    private static final String KEY_VALUE = "raw_price";
    private static final String KEY_CURRENCY = "raw_currency";

    private PurchaseEvent(String kind, String rawPrice, String rawCurrency) {
        super(kind);
        put(KEY_VALUE, rawPrice);
        put(KEY_CURRENCY, rawCurrency);
    }

    public static PurchaseEvent forHighTierSub(String rawPrice, String rawCurrency) {
        return new PurchaseEvent(KIND_HIGH_TIER_SUB, rawPrice, rawCurrency);
    }

    public String getPrice() {
        return get(KEY_VALUE);
    }

    public String getCurrency() {
        return get(KEY_CURRENCY);
    }

}
