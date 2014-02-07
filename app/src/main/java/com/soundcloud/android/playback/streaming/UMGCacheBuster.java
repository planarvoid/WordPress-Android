package com.soundcloud.android.playback.streaming;

import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.rx.observers.DefaultObserver;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.SharedPreferences;

class UMGCacheBuster extends ScheduledOperations {

    @VisibleForTesting
    protected static final String LAST_TRACK_PREFERENCE = "last_track_played";
    private final SharedPreferences mSharedPrefs;
    private final StreamStorage mStreamStorage;
    private String mLastUrlRequested;
    private Observer<Object> mObserver;

    public UMGCacheBuster(StreamStorage streamStorage){
        //Important to make this single threaded
        this(Schedulers.newThread(), new DefaultObserver<Object>() {}, SoundCloudApplication.instance, streamStorage);
    }

    @VisibleForTesting
    protected UMGCacheBuster(Scheduler scheduler, Observer<Object> observer, Context mContext, StreamStorage streamStorage){
        super(scheduler, scheduler);
        mStreamStorage = streamStorage;
        mSharedPrefs = mContext.getSharedPreferences(LAST_TRACK_PREFERENCE, Context.MODE_PRIVATE);
        mObserver = observer;
    }

    public void bustIt(final String comparisonUrl) {
        schedule(Observable.create(new Observable.OnSubscribeFunc<Object>() {

            @Override
            public Subscription onSubscribe(Observer<? super Object> observer) {
                Preconditions.checkArgument(isNotBlank(comparisonUrl), "Comparison URL must be non null/not empty");
                if(mLastUrlRequested == null){
                    mLastUrlRequested = mSharedPrefs.getString(LAST_TRACK_PREFERENCE, comparisonUrl);
                }
                if (isNotBlank(mLastUrlRequested) && !mLastUrlRequested.equals(comparisonUrl)){
                    SharedPreferences.Editor editor = mSharedPrefs.edit();
                    editor.putString(LAST_TRACK_PREFERENCE, comparisonUrl);
                    editor.commit();
                    mStreamStorage.removeAllDataForItem(mLastUrlRequested);
                }
                mLastUrlRequested = comparisonUrl;
                observer.onCompleted();
                return Subscriptions.empty();
            }
        })).subscribe(mObserver);

    }
}
