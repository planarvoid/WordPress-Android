package com.soundcloud.android.playback.streaming;

import static com.google.common.base.Preconditions.checkArgument;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ScTextUtils;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;

class UMGCacheBuster extends ScheduledOperations {

    private final StreamStorage mStreamStorage;
    private String mCurrentUrl = ScTextUtils.EMPTY_STRING;
    private final Observer<Object> mObserver;

    public UMGCacheBuster(StreamStorage streamStorage) {
        this(ScSchedulers.IO_SCHEDULER, new DefaultSubscriber<Object>() {}, streamStorage);
    }

    @VisibleForTesting
    protected UMGCacheBuster(Scheduler scheduler, Observer<Object> observer, StreamStorage streamStorage) {
        super(scheduler, scheduler);
        mStreamStorage = streamStorage;
        mObserver = observer;
    }

    public boolean bustIt(final String comparisonUrl) {
        checkArgument(isNotBlank(comparisonUrl), "Comparison URL must be non null/not empty");

        final CacheBustingObservable cacheBustingObservable;

        synchronized (this) {
            if (ScTextUtils.isBlank(mCurrentUrl)) {
                mCurrentUrl = comparisonUrl;
                return false;
            }

            if (mCurrentUrl.equals(comparisonUrl)) {
                return false;
            }

            cacheBustingObservable = new CacheBustingObservable(mCurrentUrl);
            mCurrentUrl = comparisonUrl;
        }

        schedule(Observable.create(cacheBustingObservable)).subscribe(mObserver);
        return true;
    }

    public String getLastUrl() {
        synchronized (this){
            return mCurrentUrl;
        }
    }

    private class CacheBustingObservable implements Observable.OnSubscribe<Object> {
        private final String mUrlToRemoveFor;

        private CacheBustingObservable(String mUrlToRemoveFor) {
            this.mUrlToRemoveFor = mUrlToRemoveFor;
        }

        @Override
        public void call(Subscriber<? super Object> observer) {
            mStreamStorage.removeAllDataForItem(mUrlToRemoveFor);
            observer.onCompleted();
        }
    }
}
