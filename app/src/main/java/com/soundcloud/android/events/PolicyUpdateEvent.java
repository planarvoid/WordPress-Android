package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;

import java.util.Collections;
import java.util.List;

public class PolicyUpdateEvent {

    private final List<Urn> updatedTracks;

    private PolicyUpdateEvent(List<Urn> updated) {
        this.updatedTracks = updated;
    }

    public static PolicyUpdateEvent success(List<Urn> updatedTracks) {
        return new PolicyUpdateEvent(updatedTracks);
    }

    public List<Urn> getTracks() {
        return updatedTracks;
    }
}
