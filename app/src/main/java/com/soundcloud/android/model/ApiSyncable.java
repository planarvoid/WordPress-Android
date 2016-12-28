package com.soundcloud.android.model;

import com.soundcloud.android.events.EntityStateChangedEvent;

public interface ApiSyncable {
    EntityStateChangedEvent toUpdateEvent();

    Urn getUrn();
}
