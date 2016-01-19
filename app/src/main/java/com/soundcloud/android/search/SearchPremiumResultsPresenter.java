package com.soundcloud.android.search;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

class SearchPremiumResultsPresenter extends RecyclerViewPresenter<ListItem> {

    private static final String EXTRA_PREMIUM_CONTENT_RESULTS = "searchPremiumContent";

    private final SearchOperations searchOperations;
    private final SearchResultsAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaybackInitiator playbackInitiator;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;

    private List<PropertySet> premiumContentList;

    @Inject
    SearchPremiumResultsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                  SearchOperations searchOperations,
                                  SearchResultsAdapter adapter,
                                  Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                  PlaybackInitiator playbackInitiator,
                                  EventBus eventBus) {
        super(swipeRefreshAttacher, Options.list().build());
        this.searchOperations = searchOperations;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.playbackInitiator = playbackInitiator;
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
        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)),
                eventBus.subscribe(ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(View view, int position) {
    }

    @Override
    protected CollectionBinding<ListItem> onBuildBinding(Bundle bundle) {
        premiumContentList = bundle.getParcelableArrayList(EXTRA_PREMIUM_CONTENT_RESULTS);
        return createCollectionBinding();
    }

    @Override
    protected CollectionBinding<ListItem> onRefreshBinding() {
        return createCollectionBinding();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private CollectionBinding<ListItem> createCollectionBinding() {
        //TODO: Call operations: coming up on next PR
        return CollectionBinding.from(Observable.<Iterable<ListItem>>empty()).withAdapter(adapter).build();
    }
}
