package com.soundcloud.android.waveform;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action1;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;

public class WaveformOperations {

    public static final int DEFAULT_WAVEFORM_CACHE_SIZE = 20;

    private static final String DEFAULT_WAVEFORM_ASSET_FILE = "default_waveform.json";

    private final Context context;
    private final WaveformFetchCommand waveformFetcher;
    private final WaveformStorage waveformStorage;
    private final ClearTableCommand clearTableCommand;
    private final Scheduler scheduler;
    private final WaveformParser waveformParser;

    @Inject
    WaveformOperations(Context context, WaveformFetchCommand waveformFetcher, WaveformStorage storage, WaveformParser waveformParser, ClearTableCommand clearTableCommand,
                       @Named(ApplicationModule.LOW_PRIORITY) Scheduler scheduler) {
        this.context = context;
        this.waveformFetcher = waveformFetcher;
        this.waveformStorage = storage;
        this.clearTableCommand = clearTableCommand;
        this.scheduler = scheduler;
        this.waveformParser = waveformParser;
    }

    public Observable<WaveformData> waveformDataFor(final Urn trackUrn, final String waveformUrl) {
        return waveformStorage.load(trackUrn)
                .switchIfEmpty(fetchAndStore(trackUrn, waveformUrl))
                .subscribeOn(scheduler);
    }

    public void clearWaveforms() {
        clearTableCommand.call(Table.Waveforms);
    }

    private Observable<WaveformData> fetchAndStore(Urn trackUrn, String waveformUrl) {
        return waveformFetcher.toObservable(waveformUrl)
                .doOnNext(storeAction(trackUrn))
                .onErrorResumeNext(fetchDefault());
    }

    @VisibleForTesting
    protected Observable<WaveformData> fetchDefault() {
        return Observable.create(new Observable.OnSubscribe<WaveformData>() {
            @Override
            public void call(Subscriber<? super WaveformData> subscriber) {
                try {
                    subscriber.onNext(waveformParser.parse(context.getAssets().open(DEFAULT_WAVEFORM_ASSET_FILE)));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    private Action1<WaveformData> storeAction(final Urn trackUrn) {
        return new Action1<WaveformData>() {
            @Override
            public void call(WaveformData waveformData) {
                fireAndForget(waveformStorage.store(trackUrn, waveformData));
            }
        };
    }
}
