package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
abstract class DiscoveryResult {
    abstract List<DiscoveryCard> cards();
    abstract Optional<ViewError> syncError();

    static DiscoveryResult create(List<DiscoveryCard> cards, Optional<ViewError> syncError) {
        return new AutoValue_DiscoveryResult(cards, syncError);
    }
}
