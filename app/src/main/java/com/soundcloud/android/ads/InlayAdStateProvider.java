package com.soundcloud.android.ads;

import com.soundcloud.android.events.InlayAdEvent.InlayPlayStateTransition;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
class InlayAdStateProvider {

    private Map<String, InlayPlayStateTransition> states = new ConcurrentHashMap<>(AdConstants.MAX_INLAYS_ON_SCREEN);

    @Inject
    InlayAdStateProvider() {}

    public void put(String uuid, InlayPlayStateTransition state) {
        states.put(uuid, state);
    }

    public Optional<InlayPlayStateTransition> get(String uuid) {
        return states.containsKey(uuid) ? Optional.of(states.get(uuid)) : Optional.absent();
    }

    public void remove(String uuid) {
        states.remove(uuid);
    }

}
