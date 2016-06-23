package com.soundcloud.android.stations;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.tracks.DelayedLoadingDialogPresenter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

import android.content.Context;
import android.content.DialogInterface;

import javax.inject.Inject;

public class StartStationPresenter {
    private final Func1<StationRecord, Observable<PlaybackResult>> toPlaybackResult = new Func1<StationRecord, Observable<PlaybackResult>>() {
        @Override
        public Observable<PlaybackResult> call(StationRecord station) {
            checkArgument(!station.getTracks().isEmpty(), "The station does not have any tracks.");
            final PlaySessionSource playSessionSource = PlaySessionSource.forStation(screenProvider.getLastScreenTag(),
                                                                                     station.getUrn());
            return playbackInitiator.playStation(station.getUrn(),
                                                 station.getTracks(),
                                                 playSessionSource,
                                                 station.getPreviousPosition());
        }
    };

    private final DelayedLoadingDialogPresenter.Builder dialogBuilder;
    private final StationsOperations stationsOperations;
    private final PlaybackInitiator playbackInitiator;
    private final EventBus eventBus;
    private final PlaybackToastHelper playbackToastHelper;
    private final ScreenProvider screenProvider;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public StartStationPresenter(
            DelayedLoadingDialogPresenter.Builder dialogBuilder,
            StationsOperations stationsOperations,
            PlaybackInitiator playbackInitiator,
            EventBus eventBus,
            PlaybackToastHelper playbackToastHelper,
            ScreenProvider screenProvider) {
        this.dialogBuilder = dialogBuilder;
        this.stationsOperations = stationsOperations;
        this.playbackInitiator = playbackInitiator;
        this.eventBus = eventBus;
        this.playbackToastHelper = playbackToastHelper;
        this.screenProvider = screenProvider;
    }

    public void startStation(Context context, Urn stationUrn) {
        startStation(context, stationsOperations.station(stationUrn));
    }

    public void startStationForUser(Context context, final Urn userUrn) {
        final Urn stationUrn = Urn.forArtistStation(userUrn.getNumericId());
        startStation(context, stationsOperations.station(stationUrn));
    }

    public void startStationForTrack(Context context, final Urn seed) {
        final Urn station = Urn.forTrackStation(seed.getNumericId());
        startStation(context, stationsOperations.stationWithSeed(station, seed));
    }

    private void startStation(Context context, Observable<StationRecord> station) {
        DelayedLoadingDialogPresenter delayedLoadingDialogPresenter = dialogBuilder
                .setLoadingMessage(context.getString(R.string.stations_loading_station))
                .setOnErrorToastText(context.getString(R.string.stations_unable_to_start_station))
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        subscription.unsubscribe();
                    }
                })
                .create()
                .show(context);

        subscription = station
                .flatMap(toPlaybackResult)
                .subscribe(new ExpandAndDismissDialogSubscriber(context,
                                                                eventBus,
                                                                playbackToastHelper,
                                                                delayedLoadingDialogPresenter));
    }

    private static class ExpandAndDismissDialogSubscriber extends ExpandPlayerSubscriber {

        private final Context context;
        private final DelayedLoadingDialogPresenter delayedLoadingDialogPresenter;

        public ExpandAndDismissDialogSubscriber(Context context,
                                                EventBus eventBus,
                                                PlaybackToastHelper playbackToastHelper,
                                                DelayedLoadingDialogPresenter delayedLoadingDialogPresenter) {
            super(eventBus, playbackToastHelper);
            this.context = context;
            this.delayedLoadingDialogPresenter = delayedLoadingDialogPresenter;
        }

        @Override
        public void onError(Throwable e) {
            delayedLoadingDialogPresenter.onError(context);
            ErrorUtils.handleSilentException(e);
        }

        @Override
        public void onNext(PlaybackResult result) {
            if (result.isSuccess()) {
                expandPlayer();
                delayedLoadingDialogPresenter.onSuccess();
            } else {
                delayedLoadingDialogPresenter.onError(context);
            }
        }
    }
}
