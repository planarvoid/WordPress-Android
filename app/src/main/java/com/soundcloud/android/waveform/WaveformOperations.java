package com.soundcloud.android.waveform;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

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
    private final Scheduler scheduler;
    private final WaveformParser waveformParser;

    @Inject
    WaveformOperations(Context context,
                       WaveformFetchCommand waveformFetcher,
                       WaveformStorage storage,
                       WaveformParser waveformParser,
                       @Named(ApplicationModule.RX_LOW_PRIORITY) Scheduler scheduler) {
        this.context = context;
        this.waveformFetcher = waveformFetcher;
        this.waveformStorage = storage;
        this.scheduler = scheduler;
        this.waveformParser = waveformParser;
    }

    public Single<WaveformData> waveformDataFor(final Urn trackUrn, final String waveformUrl) {
        return waveformStorage.waveformData(trackUrn)
                              .switchIfEmpty(fetchAndStore(trackUrn, waveformUrl).toMaybe())
                              .toSingle()
                              .subscribeOn(scheduler);
    }

    public void clearWaveforms() {
        waveformStorage.clear();
    }

    private Single<WaveformData> fetchAndStore(Urn trackUrn, String waveformUrl) {
        return waveformFetcher.toSingle(waveformUrl)
                              .doOnSuccess(storeAction(trackUrn))
                              .onErrorResumeNext(fetchDefault());
    }

    @VisibleForTesting
    Single<WaveformData> fetchDefault() {
        return Single.fromCallable(() -> waveformParser.parse(context.getAssets().open(DEFAULT_WAVEFORM_ASSET_FILE)));
    }

    private Consumer<WaveformData> storeAction(final Urn trackUrn) {
        return waveformData -> waveformStorage.store(trackUrn, waveformData);
    }
}
