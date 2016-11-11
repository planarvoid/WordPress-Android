package com.soundcloud.android.stations;

import static com.soundcloud.android.playback.DiscoverySource.STATIONS;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Inject;

public class StartStationHandler {

    private final Navigator navigator;
    private final EventBus eventBus;

    @Inject
    public StartStationHandler(Navigator navigator, EventBus eventBus) {
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    public void startStation(Context context, Urn stationUrn, DiscoverySource discoverySource) {
        navigator.openStationInfo(context, stationUrn, discoverySource);
    }

    public void startStation(Context context, Urn stationUrn) {
        navigator.openStationInfo(context, stationUrn, STATIONS);
    }

    public void startStationWithSeedTrack(Context context, Urn seedTrack) {
        navigator.openStationInfo(context, Urn.forTrackStation(seedTrack.getNumericId()), seedTrack, STATIONS);
    }

    public void startStationFromPlayer(Context context, Urn trackUrn, boolean trackBlocked) {
        openStationInfo(context, trackUrn, trackBlocked);
    }

    private void openStationInfo(Context context, Urn trackUrn, boolean trackBlocked) {
        eventBus.queue(EventQueue.PLAYER_UI)
                .first(PlayerUIEvent.PLAYER_IS_COLLAPSED)
                .subscribe(new StartStationPageSubscriber(context, trackUrn, trackBlocked));

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
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
