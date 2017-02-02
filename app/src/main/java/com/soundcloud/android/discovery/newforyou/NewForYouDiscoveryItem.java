package com.soundcloud.android.discovery.newforyou;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;

@AutoValue
public abstract class NewForYouDiscoveryItem extends DiscoveryItem {
    public abstract NewForYou newForYou();

    public static NewForYouDiscoveryItem create(NewForYou newForYou) {
        return new AutoValue_NewForYouDiscoveryItem(Kind.NewForYouItem, newForYou);
    }
}
