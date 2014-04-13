package com.soundcloud.android.stream;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.PropertySet;
import rx.Observable;

import javax.inject.Inject;

class SoundStreamOperations {

    private final SoundStreamStorage soundStreamStorage;

    @Inject
    SoundStreamOperations(SoundStreamStorage soundStreamStorage) {
        this.soundStreamStorage = soundStreamStorage;
    }

    public Observable<PropertySet> getStreamItems() {
        final Urn currentUserUrn = Urn.forUser(123); // TODO
        return soundStreamStorage.loadStreamItemsAsync(currentUserUrn);
    }

}
