package com.soundcloud.android.stations;

import static com.soundcloud.android.playback.PlaySessionSource.forStation;
import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PlaySessionOriginScreenProvider;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.ExpandPlayerObserver;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.tracks.DelayedLoadingDialogPresenter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Function;

import android.content.Context;

import javax.inject.Inject;

public class StartStationPresenter {

    private final DelayedLoadingDialogPresenter.Builder dialogBuilder;
    private final StationsOperations stationsOperations;
    private final PlaybackInitiator playbackInitiator;
    private final EventBusV2 eventBus;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final PlaySessionOriginScreenProvider screenProvider;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private Disposable disposable = Disposables.empty();

    @Inject
    public StartStationPresenter(DelayedLoadingDialogPresenter.Builder dialogBuilder,
                                 StationsOperations stationsOperations,
                                 PlaybackInitiator playbackInitiator,
                                 EventBusV2 eventBus,
                                 PlaybackFeedbackHelper playbackFeedbackHelper,
                                 PlaySessionOriginScreenProvider screenProvider,
                                 PerformanceMetricsEngine performanceMetricsEngine) {
        this.dialogBuilder = dialogBuilder;
        this.stationsOperations = stationsOperations;
        this.playbackInitiator = playbackInitiator;
        this.eventBus = eventBus;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.screenProvider = screenProvider;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    void startStation(Context context, Urn stationUrn, DiscoverySource discoverySource) {
        playStation(context, stationsOperations.station(stationUrn), discoverySource);
    }

    void startStation(Context context,
                      Maybe<StationRecord> station,
                      DiscoverySource discoverySource,
                      int trackPosition) {
        playStation(context, station, discoverySource, trackPosition);
    }

    private void playStation(Context context,
                             final Maybe<StationRecord> station,
                             final DiscoverySource discoverySource,
                             final int position) {
        disposable = station.flatMapSingle(toPlaybackResult(discoverySource, position))
                            .subscribeWith(new ExpandAndDismissDialogObserver(context,
                                                                              eventBus,
                                                                              playbackFeedbackHelper,
                                                                              getLoadingDialogPresenter(context),
                                                                              performanceMetricsEngine));

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromStartStation());
    }

    private Function<StationRecord, Single<PlaybackResult>> toPlaybackResult(final DiscoverySource discoverySource,
                                                                             final int position) {
        return stationRecord -> {
            checkArgument(!stationRecord.getTracks().isEmpty(), "The station does not have any tracks.");

            return playbackInitiator.playStation(stationRecord.getUrn(),
                                                 stationRecord.getTracks(),
                                                 createPlaySessionSource(discoverySource, stationRecord),
                                                 stationRecord.getTracks().get(position).getTrackUrn(),
                                                 position);
        };
    }

    private PlaySessionSource createPlaySessionSource(DiscoverySource discoverySource, StationRecord stationRecord) {
        return forStation(screenProvider.getOriginScreen(),
                          stationRecord.getUrn(),
                          discoverySource);
    }

    void playStation(Context context,
                     final Maybe<StationRecord> station,
                     final DiscoverySource discoverySource) {
        disposable = station.flatMapSingle(toPlaybackResult(discoverySource))
                            .subscribeWith(new ExpandAndDismissDialogObserver(context,
                                                                              eventBus,
                                                                              playbackFeedbackHelper,
                                                                              getLoadingDialogPresenter(context),
                                                                              performanceMetricsEngine));

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromStartStation());
    }

    private DelayedLoadingDialogPresenter getLoadingDialogPresenter(Context context) {
        return dialogBuilder
                .setLoadingMessage(context.getString(R.string.stations_loading_station))
                .setOnErrorToastText(context.getString(R.string.stations_unable_to_start_station))
                .setOnCancelListener(dialog -> disposable.dispose())
                .create()
                .show(context);
    }

    private Function<StationRecord, Single<PlaybackResult>> toPlaybackResult(final DiscoverySource source) {
        return station -> {
            checkArgument(!station.getTracks().isEmpty(), "The station does not have any tracks.");

            Urn trackToPlay = Urn.NOT_SET;
            int position = 0;

            if (station.getPreviousPosition() != Stations.NEVER_PLAYED) {
                trackToPlay = station.getTracks().get(station.getPreviousPosition()).getTrackUrn();
                position = (station.getPreviousPosition() + 1) % station.getTracks().size();
            }
            return playbackInitiator.playStation(station.getUrn(),
                                                 station.getTracks(),
                                                 createPlaySessionSource(source, station),
                                                 trackToPlay,
                                                 position);
        };
    }

    private static class ExpandAndDismissDialogObserver extends ExpandPlayerObserver {

        private final Context context;
        private final DelayedLoadingDialogPresenter delayedLoadingDialogPresenter;

        ExpandAndDismissDialogObserver(Context context,
                                       EventBusV2 eventBus,
                                       PlaybackFeedbackHelper playbackFeedbackHelper,
                                       DelayedLoadingDialogPresenter delayedLoadingDialogPresenter,
                                       PerformanceMetricsEngine performanceMetricsEngine) {
            super(eventBus, playbackFeedbackHelper, performanceMetricsEngine);
            this.context = context;
            this.delayedLoadingDialogPresenter = delayedLoadingDialogPresenter;
        }

        @Override
        public void onError(Throwable e) {
            delayedLoadingDialogPresenter.onError(context);
            ErrorUtils.handleSilentException(e);
        }

        @Override
        public void onSuccess(PlaybackResult result) {
            super.onSuccess(result);
            if (result.isSuccess()) {
                expandPlayer();
                delayedLoadingDialogPresenter.onSuccess();
            } else {
                onPlaybackError();
                delayedLoadingDialogPresenter.onError(context);
            }
        }
    }
}
