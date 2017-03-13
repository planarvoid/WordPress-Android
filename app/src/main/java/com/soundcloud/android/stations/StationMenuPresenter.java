package com.soundcloud.android.stations;

import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.view.View;

import javax.inject.Inject;

public class StationMenuPresenter implements StationMenuRenderer.Listener {

    private final StationsOperations stationsOperations;
    private final Context context;
    private final StationMenuRendererFactory rendererFactory;

    private StationMenuRenderer renderer;
    private Subscription stationSubscription = RxUtils.invalidSubscription();

    @Inject
    StationMenuPresenter(Context context,
                         StationMenuRendererFactory rendererFactory,
                         StationsOperations stationsOperations) {
        this.context = context;
        this.rendererFactory = rendererFactory;
        this.stationsOperations = stationsOperations;
    }

    public void show(View button, Urn stationUrn) {
        renderer = rendererFactory.create(this, button);
        loadStation(stationUrn);
    }

    @Override
    public void handleLike(StationWithTracks station) {
        boolean addLike = !station.isLiked();

        stationsOperations.toggleStationLike(station.getUrn(), addLike)
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(new LikeToggleSubscriber(context, addLike));
    }

    @Override
    public void onDismiss() {
        stationSubscription.unsubscribe();
        stationSubscription = RxUtils.invalidSubscription();
    }

    private void loadStation(Urn urn) {
        stationSubscription.unsubscribe();
        stationSubscription = stationsOperations.stationWithTracks(urn, Optional.absent())
                                                .first()
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(new StationSubscriber());
    }

    private class StationSubscriber extends DefaultSubscriber<StationWithTracks> {
        @Override
        public void onNext(StationWithTracks station) {
            renderer.render(station);
        }
    }
}
