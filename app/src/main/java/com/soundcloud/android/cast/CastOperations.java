package com.soundcloud.android.cast;

import rx.Observable;
import rx.schedulers.TimeInterval;

public interface CastOperations {
    @Deprecated
    Observable<TimeInterval<Long>> intervalForProgressPull();
}
