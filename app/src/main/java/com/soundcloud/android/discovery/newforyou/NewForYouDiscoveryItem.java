package com.soundcloud.android.discovery.newforyou;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.OldDiscoveryItem;

@AutoValue
public abstract class NewForYouDiscoveryItem extends OldDiscoveryItem {
    public abstract NewForYou newForYou();

    public static NewForYouDiscoveryItem create(NewForYou newForYou) {
        return new AutoValue_NewForYouDiscoveryItem(Kind.NewForYouItem, newForYou);
    }
}
