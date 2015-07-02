package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventQueue.CURRENT_DOWNLOAD;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAY_QUEUE_TRACK;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.MidTierTrackEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.paywall.PaywallImpressionController;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.RefreshRecyclerViewAdapterSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.PagedTracksRecyclerItemAdapter;
import com.soundcloud.android.view.adapters.PrependItemToListSubscriber;
import com.soundcloud.android.view.adapters.RemoveEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.lightcycle.LightCycle;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

class TrackLikesPresenter extends RecyclerViewPresenter<TrackItem> {

    final @LightCycle CollapsingScrollHelper scrollHelper;
    final @LightCycle TrackLikesActionMenuController actionMenuController;
    final @LightCycle TrackLikesHeaderPresenter headerPresenter;

    private final TrackLikeOperations likeOperations;
    private final FeatureOperations featureOperations;
    private final Navigator navigator;
    private final OfflinePlaybackOperations playbackOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PagedTracksRecyclerItemAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;
    private final PaywallImpressionController paywallImpressionController;

    private CompositeSubscription viewLifeCycle;

    @Inject
    TrackLikesPresenter(TrackLikeOperations likeOperations,
                        OfflinePlaybackOperations playbackOperations,
                        OfflineContentOperations offlineContentOperations,
                        PagedTracksRecyclerItemAdapter adapter,
                        TrackLikesActionMenuController actionMenuController,
                        TrackLikesHeaderPresenter headerPresenter,
                        Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider, EventBus eventBus,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        FeatureOperations featureOperations,
                        Navigator navigator,
                        CollapsingScrollHelper scrollHelper,
                        PaywallImpressionController paywallImpressionController) {
        super(swipeRefreshAttacher);
        this.likeOperations = likeOperations;
        this.playbackOperations = playbackOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.adapter = adapter;
        this.actionMenuController = actionMenuController;
        this.headerPresenter = headerPresenter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        this.featureOperations = featureOperations;
        this.navigator = navigator;
        this.scrollHelper = scrollHelper;
        this.paywallImpressionController = paywallImpressionController;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<TrackItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(likeOperations.likedTracks(), TrackItem.fromPropertySets())
                .withAdapter(adapter)
                .withPager(likeOperations.pagingFunction())
                .build();
    }

    @Override
    protected CollectionBinding<TrackItem> onRefreshBinding() {
        return CollectionBinding.from(likeOperations.updatedLikedTracks(), TrackItem.fromPropertySets())
                .withAdapter(adapter)
                .withPager(likeOperations.pagingFunction())
                .build();
    }

    @Override
    protected void onSubscribeBinding(CollectionBinding<TrackItem> collectionBinding, CompositeSubscription viewLifeCycle) {
        headerPresenter.onSubscribeListObservers(collectionBinding);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        getEmptyView().setImage(R.drawable.empty_like);
        getEmptyView().setMessageText(R.string.list_empty_user_likes_message);

        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(PLAY_QUEUE_TRACK,
                        new UpdatePlayingTrackSubscriber(adapter, adapter.getTrackRenderer())),
                eventBus.queue(CURRENT_DOWNLOAD)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new UpdateCurrentDownloadSubscriber(adapter)),
                eventBus.subscribe(ENTITY_STATE_CHANGED,
                        new UpdateEntityListSubscriber(adapter)),

                likeOperations.onTrackLiked()
                        .map(TrackItem.fromPropertySet())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new PrependItemToListSubscriber<>(adapter)),
                likeOperations.onTrackUnliked()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RemoveEntityListSubscriber(adapter)),

                offlineContentOperations.getOfflineContentOrLikesStatus()
                        .subscribe(new RefreshRecyclerViewAdapterSubscriber(adapter))
        );

        paywallImpressionController.attachRecyclerView(getRecyclerView());
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        actionMenuController.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        // TODO create subscription light cycle
        paywallImpressionController.detachRecyclerView(getRecyclerView());
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onItemClicked(View view, int position) {
        // here we assume that the list you are looking at is up to date with the database, which is not necessarily the case
        // a sync may have happened in the background. This is def. an edge case, but worth handling maybe??
        TrackItem item = adapter.getItem(position);
        if (item == null) {
            String exceptionMessage = "Adapter item is null on item click, with adapter: " + adapter + ", on position " + position;
            ErrorUtils.handleSilentException(new IllegalStateException(exceptionMessage));
        } else if (shouldShowUpsell(item)) {
            eventBus.publish(EventQueue.TRACKING, MidTierTrackEvent.forClick(item.getEntityUrn()));
            navigator.openUpgrade(view.getContext());
        } else {
            Urn initialTrack = item.getEntityUrn();
            PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_LIKES);
            playbackOperations
                    .playLikes(initialTrack, position, playSessionSource)
                    .subscribe(expandPlayerSubscriberProvider.get());
        }
    }

    private boolean shouldShowUpsell(TrackItem item) {
        return item.isMidTier() && featureOperations.upsellMidTier();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}
