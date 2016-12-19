package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class LikedStationsPresenter extends RecyclerViewPresenter<List<StationViewModel>, StationViewModel> {

    private final DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            operations.disableLikedStationsOnboarding();
        }
    };

    private final Func1<StationRecord, StationViewModel> toViewModel = new Func1<StationRecord, StationViewModel>() {
        @Override
        public StationViewModel call(StationRecord station) {
            return new StationViewModel(station, playQueueManager.getCollectionUrn().equals(station.getUrn()));
        }
    };

    private final StationsOperations operations;
    private final StationsAdapter adapter;
    private final Resources resources;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;

    @LightCycle final StationsNowPlayingController stationsNowPlayingController;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public LikedStationsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                  StationsOperations operations,
                                  StationsAdapter adapter,
                                  Resources resources,
                                  PlayQueueManager playQueueManager,
                                  EventBus eventBus, StationsNowPlayingController stationsNowPlayingController) {
        super(swipeRefreshAttacher, Options.defaults());
        this.operations = operations;
        this.adapter = adapter;
        this.resources = resources;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.stationsNowPlayingController = stationsNowPlayingController;
        this.stationsNowPlayingController.setAdapter(adapter);
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<StationViewModel>, StationViewModel> onBuildBinding(Bundle bundle) {
        return CollectionBinding
                .from(stationsSource())
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<List<StationViewModel>, StationViewModel> onRefreshBinding() {
        return CollectionBinding
                .from(operations.syncLikedStations().flatMap(RxUtils.continueWith(stationsSource())))
                .withAdapter(adapter)
                .build();
    }

    private Observable<List<StationViewModel>> stationsSource() {
        return operations.collection(StationsCollectionsTypes.LIKED)
                         .map(toViewModel)
                         .toList();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        configureRecyclerView(view);
        configureEmptyView();

        subscription = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                               .filter(EntityStateChangedEvent.IS_STATION_COLLECTION_UPDATED)
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(new RefreshLikedStationsSubscriber());
    }

    @Override
    public void onResume(Fragment fragment) {
        if (operations.shouldShowLikedStationsOnboarding()) {
            showOnboardingDialog(fragment.getActivity());
        }
    }

    @Override
    public void onDestroy(Fragment fragment) {
        subscription.unsubscribe();
        super.onDestroy(fragment);
    }

    private void configureRecyclerView(View view) {
        RecyclerView recyclerView = getRecyclerView();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(),
                                                            resources.getInteger(R.integer.stations_grid_span_count)));
    }

    private void configureEmptyView() {
        final EmptyView emptyView = getEmptyView();
        emptyView.setMessageText(R.string.liked_stations_empty_view_message);
        emptyView.setImage(R.drawable.empty_collection_stations);
    }

    private void showOnboardingDialog(Activity activity) {
        final View view = new CustomFontViewBuilder(activity)
                .setIcon(R.drawable.like_station_onboarding)
                .setTitle(R.string.liked_stations_onboarding_dialog_title)
                .setMessage(R.string.liked_stations_onboarding_dialog_message).get();

        new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(R.string.liked_stations_onboarding_dialog_button, null)
                .setOnDismissListener(onDismissListener)
                .create()
                .show();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private class RefreshLikedStationsSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {

        @Override
        public void onNext(EntityStateChangedEvent args) {
            adapter.clear();
            retryWith(onRefreshBinding());
        }
    }
}
