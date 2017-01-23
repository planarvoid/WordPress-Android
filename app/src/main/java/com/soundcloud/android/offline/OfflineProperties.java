package com.soundcloud.android.offline;

import static java.util.Collections.emptyMap;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Map;

@AutoValue
public abstract class OfflineProperties {
    public static OfflineProperties from(Map<Urn, OfflineState> offlineEntitiesStates, OfflineState likedTracksState) {
        return new AutoValue_OfflineProperties(offlineEntitiesStates, likedTracksState);
    }

    public static OfflineProperties empty() {
        return from(emptyMap(), OfflineState.NOT_OFFLINE);
    }

    abstract Map<Urn, OfflineState> offlineEntitiesStates();

    public abstract OfflineState likedTracksState();

    public OfflineState state(Urn urn) {
        final Map<Urn, OfflineState> statesMap = offlineEntitiesStates();
        if (statesMap.containsKey(urn)) {
            return statesMap.get(urn);
        } else {
            return OfflineState.NOT_OFFLINE;
        }
    }
}
