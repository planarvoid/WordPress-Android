package com.soundcloud.android.associations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;

public interface RepostCreator {
    public Observable<PropertySet> toggleRepost(final Urn soundUrn, final boolean addRepost);
}
