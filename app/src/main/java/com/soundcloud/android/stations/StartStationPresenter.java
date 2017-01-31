package com.soundcloud.android.stations;

import static com.soundcloud.android.playback.PlaySessionSource.forStation;
import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
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

import javax.inject.Inject;

public class StartStationPresenter {

    private final DelayedLoadingDialogPresenter.Builder dialogBuilder;
    private final StationsOperations stationsOperations;
    private final PlaybackInitiator playbackInitiator;
    private final EventBus eventBus;
    private final PlaybackToastHelper playbackToastHelper;
    private final ScreenProvider screenProvider;
    private final MiniplayerExperiment miniplayerExperiment;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public StartStationPresenter(DelayedLoadingDialogPresenter.Builder dialogBuilder,
                                 StationsOperations stationsOperations, PlaybackInitiator playbackInitiator,
                                 EventBus eventBus, PlaybackToastHelper playbackToastHelper,
                                 ScreenProvider screenProvider,
                                 MiniplayerExperiment miniplayerExperiment) {
        this.dialogBuilder = dialogBuilder;
        this.stationsOperations = stationsOperations;
        this.playbackInitiator = playbackInitiator;
        this.eventBus = eventBus;
        this.playbackToastHelper = playbackToastHelper;
        this.screenProvider = screenProvider;
        this.miniplayerExperiment = miniplayerExperiment;
    }

    void startStation(Context context, Urn stationUrn, DiscoverySource discoverySource) {
        playStation(context, stationsOperations.station(stationUrn), discoverySource);
    }

    void startStation(Context context,
                      Observable<StationRecord> station,
                      DiscoverySource discoverySource,
                      int trackPosition) {
        playStation(context, station, discoverySource, trackPosition);
    }

    private void playStation(Context context,
                             final Observable<StationRecord> station,
                             final DiscoverySource discoverySource,
                             final int position) {
        subscription = station
                .flatMap(toPlaybackResult(discoverySource, position))
                .subscribe(new ExpandAndDismissDialogSubscriber(context, eventBus, playbackToastHelper,
                                                                getLoadingDialogPresenter(context),
                                                                miniplayerExperiment));

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromStartStation());
    }

    private Func1<StationRecord, Observable<PlaybackResult>> toPlaybackResult(final DiscoverySource discoverySource,
                                                                              final int position) {
        return stationRecord -> {
            checkArgument(!stationRecord.getTracks().isEmpty(), "The station does not have any tracks.");

            final PlaySessionSource playSessionSource = forStation(screenProvider.getLastScreenTag(),
                                                                   stationRecord.getUrn(),
                                                                   discoverySource);
            return playbackInitiator.playStation(stationRecord.getUrn(),
                                                 stationRecord.getTracks(),
                                                 playSessionSource,
                                                 stationRecord.getTracks().get(position).getTrackUrn(),
                                                 position);
        };
    }

    void playStation(Context context,
                     final Observable<StationRecord> station,
                     final DiscoverySource discoverySource) {
        subscription = station
                .flatMap(toPlaybackResult(discoverySource))
                .subscribe(new ExpandAndDismissDialogSubscriber(context, eventBus, playbackToastHelper,
                                                                getLoadingDialogPresenter(context),
                                                                miniplayerExperiment));

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromStartStation());
    }

    private DelayedLoadingDialogPresenter getLoadingDialogPresenter(Context context) {
        return dialogBuilder
                .setLoadingMessage(context.getString(R.string.stations_loading_station))
                .setOnErrorToastText(context.getString(R.string.stations_unable_to_start_station))
                .setOnCancelListener(dialog -> subscription.unsubscribe())
                .create()
                .show(context);
    }

    private Func1<StationRecord, Observable<PlaybackResult>> toPlaybackResult(final DiscoverySource source) {
        return station -> {
            checkArgument(!station.getTracks().isEmpty(), "The station does not have any tracks.");

            Urn trackToPlay = Urn.NOT_SET;
            int position = 0;

            if (station.getPreviousPosition() != Stations.NEVER_PLAYED) {
                trackToPlay = station.getTracks().get(station.getPreviousPosition()).getTrackUrn();
                position = (station.getPreviousPosition() + 1) % station.getTracks().size();
            }
            final PlaySessionSource playSessionSource = forStation(screenProvider.getLastScreenTag(),
                                                                   station.getUrn(),
                                                                   source);
            return playbackInitiator.playStation(station.getUrn(),
                                                 station.getTracks(),
                                                 playSessionSource,
                                                 trackToPlay,
                                                 position);
        };
    }

    private static class ExpandAndDismissDialogSubscriber extends ExpandPlayerSubscriber {

        private final Context context;
        private final DelayedLoadingDialogPresenter delayedLoadingDialogPresenter;

        ExpandAndDismissDialogSubscriber(Context context,
                                         EventBus eventBus,
                                         PlaybackToastHelper playbackToastHelper,
                                         DelayedLoadingDialogPresenter delayedLoadingDialogPresenter,
                                         MiniplayerExperiment miniplayerExperiment) {
            super(eventBus, playbackToastHelper, miniplayerExperiment);
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
