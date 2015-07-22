package com.soundcloud.android.waveform;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import javax.inject.Inject;
import javax.inject.Named;

public class WaveformOperations {

    public static final int DEFAULT_WAVEFORM_CACHE_SIZE = 20;

    private final WaveformFetcher waveformFetcher;
    private final WaveformStorage waveformStorage;
    private final ClearTableCommand clearTableCommand;
    private final Scheduler scheduler;

    @Inject
    WaveformOperations(WaveformFetcher waveformFetcher, WaveformStorage storage, ClearTableCommand clearTableCommand,
                       @Named(ApplicationModule.LOW_PRIORITY) Scheduler scheduler) {
        this.waveformFetcher = waveformFetcher;
        this.waveformStorage = storage;
        this.clearTableCommand = clearTableCommand;
        this.scheduler = scheduler;
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
        return waveformFetcher.fetch(waveformUrl)
                .doOnNext(storeAction(trackUrn))
                .onErrorResumeNext(waveformFetcher.fetchDefault());
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
