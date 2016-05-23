package com.soundcloud.android.discovery;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class ViewAllRecommendedTracksPresenter extends RecyclerViewPresenter<List<DiscoveryItem>, DiscoveryItem> {
    private final RecommendedTracksOperations operations;
    private final DiscoveryAdapter adapter;
    private final EventBus eventBus;
    private Subscription subscription;

    @Inject
    ViewAllRecommendedTracksPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                      RecommendedTracksOperations operations,
                                      RecommendationBucketRendererFactory recommendationBucketRendererFactory,
                                      DiscoveryAdapterFactory adapterFactory,
                                      EventBus eventBus) {
        super(swipeRefreshAttacher, Options.custom().build());
        this.operations = operations;
        this.adapter = adapterFactory.create(recommendationBucketRendererFactory.create(Screen.RECOMMENDATIONS_MORE,
                                                                                        false));
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        subscription = eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        super.onDestroyView(fragment);

        subscription.unsubscribe();
    }

    @Override
    protected CollectionBinding<List<DiscoveryItem>, DiscoveryItem> onBuildBinding(Bundle bundle) {
        return createCollectionBinding();
    }

    @Override
    protected CollectionBinding<List<DiscoveryItem>, DiscoveryItem> onRefreshBinding() {
        return createCollectionBinding();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private CollectionBinding<List<DiscoveryItem>, DiscoveryItem> createCollectionBinding() {
        return CollectionBinding
                .from(getSource())
                .withAdapter(adapter)
                .build();
    }

    private Observable<List<DiscoveryItem>> getSource() {
        return operations.allBuckets()
                         .concatWith(Observable.just(new RecommendationsFooterItem()))
                         .toList();
    }
}
