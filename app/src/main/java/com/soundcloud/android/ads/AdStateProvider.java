package com.soundcloud.android.ads;

import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
class AdStateProvider {

    private Map<String, AdPlayStateTransition> states = new ConcurrentHashMap<>(AdConstants.MAX_INLAYS_ON_SCREEN);

    @Inject
    AdStateProvider() {}

    public void put(String uuid, AdPlayStateTransition state) {
        states.put(uuid, state);
    }

    public Optional<AdPlayStateTransition> get(String uuid) {
        return states.containsKey(uuid) ? Optional.of(states.get(uuid)) : Optional.absent();
    }

    public void remove(String uuid) {
        states.remove(uuid);
    }

}
