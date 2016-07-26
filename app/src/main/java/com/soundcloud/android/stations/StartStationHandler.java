package com.soundcloud.android.stations;

import static com.soundcloud.android.playback.DiscoverySource.STATIONS;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Inject;

public class StartStationHandler {

    private final Navigator navigator;
    private final StartStationPresenter stationPresenter;
    private final FeatureFlags featureFlags;
    private final EventBus eventBus;

    @Inject
    public StartStationHandler(Navigator navigator,
                               StartStationPresenter stationPresenter,
                               FeatureFlags featureFlags,
                               EventBus eventBus) {
        this.navigator = navigator;
        this.stationPresenter = stationPresenter;
        this.featureFlags = featureFlags;
        this.eventBus = eventBus;
    }

    public void startStation(Context context, Urn stationUrn, DiscoverySource discoverySource) {
        if (featureFlags.isEnabled(Flag.STATION_INFO_PAGE)) {
            navigator.openStationInfo(context, stationUrn, discoverySource);
        } else {
            stationPresenter.startStation(context, stationUrn, discoverySource);
        }
    }

    public void startStation(Context context, Urn stationUrn) {
        if (featureFlags.isEnabled(Flag.STATION_INFO_PAGE)) {
            navigator.openStationInfo(context, stationUrn, STATIONS);
        } else {
            stationPresenter.startStation(context, stationUrn, STATIONS);
        }
    }

    public void startStationWithSeedTrack(Context context, Urn seedTrack) {
        if (featureFlags.isEnabled(Flag.STATION_INFO_PAGE)) {
            navigator.openStationInfo(context, Urn.forTrackStation(seedTrack.getNumericId()), seedTrack, STATIONS);
        } else {
            stationPresenter.startStationForTrack(context, seedTrack);
        }
    }

    public void startStationFromPlayer(Context context, Urn trackUrn, boolean trackBlocked) {
        if (featureFlags.isEnabled(Flag.STATION_INFO_PAGE)) {
            openStationInfo(context, trackUrn, trackBlocked);
        } else {
            if (trackBlocked) {
                stationPresenter.startStation(context, Urn.forTrackStation(trackUrn.getNumericId()));
            } else {
                stationPresenter.startStationForTrack(context, trackUrn);
            }
        }
    }

    private void openStationInfo(Context context, Urn trackUrn, boolean trackBlocked) {
        eventBus.queue(EventQueue.PLAYER_UI)
                .first(PlayerUIEvent.PLAYER_IS_COLLAPSED)
                .subscribe(new StartStationPageSubscriber(context, trackUrn, trackBlocked));

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClose());
    }

    private class StartStationPageSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        private final Context context;
        private final Urn trackUrn;
        private final boolean trackBlocked;

        StartStationPageSubscriber(Context context, Urn trackUrn, boolean trackBlocked) {
            this.context = context;
            this.trackUrn = trackUrn;
            this.trackBlocked = trackBlocked;
        }

        @Override
        public void onNext(PlayerUIEvent args) {
            final Urn stationUrn = Urn.forTrackStation(trackUrn.getNumericId());

            if (trackBlocked) {
                navigator.openStationInfo(context, stationUrn, trackUrn, STATIONS);
            } else {
                navigator.openStationInfo(context, stationUrn, STATIONS);
            }
        }
    }

}
