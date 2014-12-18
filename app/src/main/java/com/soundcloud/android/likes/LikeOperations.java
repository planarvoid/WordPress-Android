package com.soundcloud.android.likes;


import com.soundcloud.propeller.PropertySet;
import rx.Observable;

import javax.inject.Inject;

public class LikeOperations {

    private final LikeStorage storage;

    @Inject
    public LikeOperations(LikeStorage storage) {
        this.storage = storage;
    }

    public Observable<PropertySet> likedTracks() {
        return storage.trackLikes();
    }
}
