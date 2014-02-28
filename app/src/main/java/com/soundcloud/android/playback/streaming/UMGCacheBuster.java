package com.soundcloud.android.playback.streaming;

import static com.google.common.base.Preconditions.checkArgument;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.ScTextUtils;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

class UMGCacheBuster extends ScheduledOperations {

    private final StreamStorage mStreamStorage;
    private String mCurrentUrl = ScTextUtils.EMPTY_STRING;
    private final Observer<Object> mObserver;

    public UMGCacheBuster(StreamStorage streamStorage) {
        this(ScSchedulers.IO_SCHEDULER, new DefaultObserver<Object>() {}, streamStorage);
    }

    @VisibleForTesting
    protected UMGCacheBuster(Scheduler scheduler, Observer<Object> observer, StreamStorage streamStorage) {
        super(scheduler, scheduler);
        mStreamStorage = streamStorage;
        mObserver = observer;
    }

    public void bustIt(final String comparisonUrl) {
        checkArgument(isNotBlank(comparisonUrl), "Comparison URL must be non null/not empty");

        final CacheBustingObservable cacheBustingObservable;

        synchronized (this) {
            if (ScTextUtils.isBlank(mCurrentUrl)) {
                mCurrentUrl = comparisonUrl;
                return;
            }

            if (mCurrentUrl.equals(comparisonUrl)) {
                return;
            }

            cacheBustingObservable = new CacheBustingObservable(mCurrentUrl);
            mCurrentUrl = comparisonUrl;
        }

        schedule(Observable.create(cacheBustingObservable)).subscribe(mObserver);

    }

    public String getCurrentPlayingUrl() {
        synchronized (this){
            return mCurrentUrl;
        }
    }

    private class CacheBustingObservable implements Observable.OnSubscribeFunc<Object> {
        private final String mUrlToRemoveFor;

        private CacheBustingObservable(String mUrlToRemoveFor) {
            this.mUrlToRemoveFor = mUrlToRemoveFor;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Object> observer) {
            mStreamStorage.removeAllDataForItem(mUrlToRemoveFor);
            observer.onCompleted();
            return Subscriptions.empty();
        }
    }
}
