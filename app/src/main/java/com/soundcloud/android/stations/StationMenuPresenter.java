package com.soundcloud.android.stations;

import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

import android.content.Context;
import android.view.View;

import javax.inject.Inject;

public class StationMenuPresenter implements StationMenuRenderer.Listener {

    private final StationsOperations stationsOperations;
    private final Context context;
    private final StationMenuRendererFactory rendererFactory;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final FeedbackController feedbackController;
    private final NavigationExecutor navigationExecutor;

    private StationMenuRenderer renderer;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Inject
    StationMenuPresenter(Context context,
                         StationMenuRendererFactory rendererFactory,
                         StationsOperations stationsOperations,
                         ChangeLikeToSaveExperiment changeLikeToSaveExperiment,
                         FeedbackController feedbackController,
                         NavigationExecutor navigationExecutor) {
        this.context = context;
        this.rendererFactory = rendererFactory;
        this.stationsOperations = stationsOperations;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.feedbackController = feedbackController;
        this.navigationExecutor = navigationExecutor;
    }

    public void show(View button, Urn stationUrn) {
        renderer = rendererFactory.create(this, button);
        loadStation(stationUrn);
    }

    @Override
    public void handleLike(StationWithTracks station) {
        boolean addLike = !station.isLiked();

        disposable.add(RxJava.toV2Single(stationsOperations.toggleStationLike(station.getUrn(), addLike))
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribeWith(new LikeToggleSubscriber(context, addLike, changeLikeToSaveExperiment, feedbackController, navigationExecutor)));
    }

    @Override
    public void onDismiss() {
        disposable.clear();
    }

    private void loadStation(Urn urn) {
        disposable.add(RxJava.toV2Observable(stationsOperations.stationWithTracks(urn, Optional.absent()))
                             .firstElement()
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribeWith(new DefaultMaybeObserver<StationWithTracks>() {
                                 @Override
                                 public void onSuccess(StationWithTracks station) {
                                     renderer.render(station);
                                 }
                             }));
    }
}
