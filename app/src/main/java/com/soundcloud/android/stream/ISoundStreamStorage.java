package com.soundcloud.android.stream;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;

public interface ISoundStreamStorage {
    public Observable<PropertySet> streamItemsBefore(final long timestamp, final Urn userUrn, final int limit);
    public Observable<Urn> trackUrns();
}
