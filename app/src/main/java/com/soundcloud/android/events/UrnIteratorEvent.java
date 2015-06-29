package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;

public interface UrnIteratorEvent {

    /**
     * @return for a single change event, this returns the single URN; if more than one entity changed,
     * returns the first available URN.
     */
    public Urn getNextUrn();
}
