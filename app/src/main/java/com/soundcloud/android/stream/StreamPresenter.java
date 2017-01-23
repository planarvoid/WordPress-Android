package com.soundcloud.android.stream;

import static com.soundcloud.android.events.FacebookInvitesEvent.forCreatorClick;
import static com.soundcloud.android.events.FacebookInvitesEvent.forCreatorDismiss;
import static com.soundcloud.android.events.FacebookInvitesEvent.forListenerClick;
import static com.soundcloud.android.events.FacebookInvitesEvent.forListenerDismiss;
import static com.soundcloud.android.events.FacebookInvitesEvent.forListenerShown;
import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdItemRenderer;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.StreamAdsController;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.WhyAdsDialogPresenter;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.facebookinvites.FacebookCreatorInvitesItemRenderer;
import com.soundcloud.android.facebookinvites.FacebookInvitesDialogPresenter;
import com.soundcloud.android.facebookinvites.FacebookListenerInvitesItemRenderer;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.stations.StationsOnboardingStreamItemRenderer;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.stream.StreamItem.FacebookListenerInvites;
import com.soundcloud.android.stream.StreamItem.Kind;
import com.soundcloud.android.sync.timeline.TimelinePresenter;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriberFactory;
import com.soundcloud.android.upsell.UpsellItemRenderer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer;
import com.soundcloud.android.view.adapters.RepostEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdatePlaylistListSubscriber;
import com.soundcloud.android.view.adapters.UpdateTrackListSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class StreamPresenter extends TimelinePresenter<StreamItem> implements
        FacebookListenerInvitesItemRenderer.Listener,
        StationsOnboardingStreamItemRenderer.Listener,
        FacebookCreatorInvitesItemRenderer.Listener,
        UpsellItemRenderer.Listener,
        AdItemRenderer.Listener,
        NewItemsIndicator.Listener, StreamHighlightsItemRenderer.Listener {

    private final StreamOperations streamOperations;
    private final StreamAdapter adapter;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final StreamAdsController streamAdsController;
    private final StreamDepthPublisherFactory streamDepthPublisherFactory;
    private final StreamSwipeRefreshAttacher swipeRefreshAttacher;
    private final EventBus eventBus;
    private final FacebookInvitesDialogPresenter invitesDialogPresenter;
    private final MixedItemClickListener itemClickListener;
    private final UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory;
    private final FollowingOperations followingOperations;
    private final StationsOperations stationsOperations;
    private final Navigator navigator;
    private final NewItemsIndicator newItemsIndicator;
    private final WhyAdsDialogPresenter whyAdsDialogPresenter;

    private Optional<StreamDepthPublisher> streamDepthPublisher = Optional.absent();
    private CompositeSubscription viewLifeCycleSubscription;
    private Fragment fragment;
    private boolean hasFocus;

    @Inject
    StreamPresenter(StreamOperations streamOperations,
                    StreamAdapter adapter,
                    StationsOperations stationsOperations,
                    ImagePauseOnScrollListener imagePauseOnScrollListener,
                    StreamAdsController streamAdsController,
                    StreamDepthPublisherFactory streamDepthPublisherFactory,
                    EventBus eventBus,
                    MixedItemClickListener.Factory itemClickListenerFactory,
                    StreamSwipeRefreshAttacher swipeRefreshAttacher,
                    FacebookInvitesDialogPresenter invitesDialogPresenter,
                    Navigator navigator,
                    NewItemsIndicator newItemsIndicator,
                    FollowingOperations followingOperations,
                    WhyAdsDialogPresenter whyAdsDialogPresenter,
                    UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory) {
        super(swipeRefreshAttacher, Options.staggeredGrid(R.integer.grids_num_columns).build(),
              newItemsIndicator, streamOperations, adapter);
        this.streamOperations = streamOperations;
        this.adapter = adapter;
        this.stationsOperations = stationsOperations;
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.streamAdsController = streamAdsController;
        this.streamDepthPublisherFactory = streamDepthPublisherFactory;
        this.swipeRefreshAttacher = swipeRefreshAttacher;
        this.eventBus = eventBus;
        this.invitesDialogPresenter = invitesDialogPresenter;
        this.navigator = navigator;
        this.newItemsIndicator = newItemsIndicator;
        this.whyAdsDialogPresenter = whyAdsDialogPresenter;

        this.itemClickListener = itemClickListenerFactory.create(Screen.STREAM, null);
        this.updatePlayableAdapterSubscriberFactory = updatePlayableAdapterSubscriberFactory;
        this.followingOperations = followingOperations;
        adapter.setOnFacebookInvitesClickListener(this);
        adapter.setOnFacebookCreatorInvitesClickListener(this);
        adapter.setOnStationsOnboardingStreamClickListener(this);
        adapter.setOnUpsellClickListener(this);
        adapter.setOnAppInstallClickListener(this);
        adapter.setOnVideoAdClickListener(this);
        adapter.setOnStreamHighlightsClickListener(this);
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        this.fragment = fragment;
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<StreamItem>, StreamItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(streamOperations.initialStreamItems()
                                                      .observeOn(AndroidSchedulers.mainThread())
                                                      .doOnNext(streamItems -> adapter.clear()))
                                .withAdapter(adapter)
                                .withPager(streamOperations.pagingFunction())
                                .build();
    }

    @Override
    protected CollectionBinding<List<StreamItem>, StreamItem> onRefreshBinding() {
        newItemsIndicator.hideAndReset();
        return CollectionBinding.from(streamOperations.updatedStreamItems())
                                .withAdapter(adapter)
                                .withPager(streamOperations.pagingFunction())
                                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        streamAdsController.onViewCreated(getRecyclerView(), adapter);

        final StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) getRecyclerView().getLayoutManager();
        streamDepthPublisher = Optional.of(streamDepthPublisherFactory.create(layoutManager, hasFocus));

        configureEmptyView();
        addScrollListeners();

        viewLifeCycleSubscription = new CompositeSubscription(
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, updatePlayableAdapterSubscriberFactory.create(adapter)),
                eventBus.subscribe(EventQueue.TRACK_CHANGED, new UpdateTrackListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.PLAYLIST_CHANGED, new UpdatePlaylistListSubscriber(adapter)),
                eventBus.queue(EventQueue.FOLLOWING_CHANGED).subscribe(adapter::onFollowingEntityChange),
                eventBus.subscribe(EventQueue.LIKE_CHANGED, new LikeEntityListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.REPOST_CHANGED, new RepostEntityListSubscriber(adapter)),
                fireAndForget(eventBus.queue(EventQueue.STREAM)
                                      .filter(StreamEvent::isStreamRefreshed)
                                      .flatMap(continueWith(updateIndicatorFromMostRecent()))),
                followingOperations.onUserFollowed().subscribe(urn -> swipeRefreshAttacher.forceRefresh()),
                followingOperations.onUserUnfollowed().subscribe(urn -> swipeRefreshAttacher.forceRefresh())
        );
    }

    void onFocusChange(boolean hasFocus) {
        this.hasFocus = hasFocus;
        if (streamDepthPublisher.isPresent()) {
            streamDepthPublisher.get().onFocusChange(hasFocus);
        }
    }

    private void addScrollListeners() {
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        getRecyclerView().addOnScrollListener(streamAdsController);
        getRecyclerView().addOnScrollListener(new RecyclerViewParallaxer());
        getRecyclerView().addOnScrollListener(streamDepthPublisher.get());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        streamAdsController.onDestroyView();
        if (streamDepthPublisher.isPresent()) {
            streamDepthPublisher.get().unsubscribe();
            streamDepthPublisher = Optional.absent();
        }
        viewLifeCycleSubscription.unsubscribe();
        adapter.unsubscribe();
        newItemsIndicator.destroy();
        super.onDestroyView(fragment);
    }

    private void configureEmptyView() {
        final EmptyView emptyView = getEmptyView();
        emptyView.setImage(R.drawable.empty_stream);
        emptyView.setMessageText(R.string.list_empty_stream_message);
        emptyView.setActionText(R.string.list_empty_stream_action);
        emptyView.setButtonActions(new Intent(Actions.SEARCH));
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final StreamItem item = adapter.getItem(position);
        final Optional<ListItem> listItem = item.getListItem();
        if (listItem.isPresent()) {
            if (item.isPromoted()) {
                publishPromotedItemClickEvent((PromotedListItem) listItem.get());
            }
            itemClickListener.legacyOnPostClick(streamOperations.urnsForPlayback(), view, position, listItem.get());
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private void publishPromotedItemClickEvent(PromotedListItem item) {
        eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forItemClick(item, Screen.STREAM.get()));
    }

    @Override
    public void onListenerInvitesClicked(int position) {
        if (adapter.getItem(position).kind() == Kind.FACEBOOK_LISTENER_INVITES) {
            final FacebookListenerInvites item = (FacebookListenerInvites) adapter.getItem(position);
            trackInvitesEvent(forListenerClick((item.hasPictures())));
            invitesDialogPresenter.showForListeners(fragment.getActivity());
            removeItem(position);
        }
    }

    @Override
    public void onListenerInvitesDismiss(int position) {
        if (adapter.getItem(position).kind() == Kind.FACEBOOK_LISTENER_INVITES) {
            final FacebookListenerInvites item = (FacebookListenerInvites) adapter.getItem(position);
            trackInvitesEvent(forListenerDismiss(item.hasPictures()));
            removeItem(position);
        }
    }

    @Override
    public void onListenerInvitesLoaded(boolean hasPictures) {
        trackInvitesEvent(forListenerShown(hasPictures));
    }

    @Override
    public void onCreatorInvitesClicked(int position) {
        if (adapter.getItem(position).kind() == Kind.FACEBOOK_CREATORS) {
            StreamItem.FacebookCreatorInvites item = (StreamItem.FacebookCreatorInvites) adapter.getItem(
                    position);

            if (!Urn.NOT_SET.equals(item.trackUrn())) {
                trackInvitesEvent(forCreatorClick());
                invitesDialogPresenter.showForCreators(fragment.getActivity(), item.trackUrl(), item.trackUrn());
            }
            removeItem(position);
        }
    }

    @Override
    public void onCreatorInvitesDismiss(int position) {
        if (adapter.getItem(position).kind() == Kind.FACEBOOK_CREATORS) {
            trackInvitesEvent(forCreatorDismiss());
            removeItem(position);
        }
    }

    @Override
    public void onStationOnboardingItemClosed(int position) {
        stationsOperations.disableOnboardingStreamItem();
        removeItem(position);
    }

    @Override
    public void onUpsellItemDismissed(int position) {
        streamOperations.disableUpsell();
        removeItem(position);
    }

    @Override
    public void onUpsellItemClicked(Context context) {
        navigator.openUpgrade(fragment.getActivity());
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forStreamClick());
    }

    @Override
    public void onUpsellItemCreated() {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forStreamImpression());
    }

    @Override
    public void onStreamHighlightsClicked(List<Urn> highlights) {
        navigator.openStreamHighlights(fragment.getActivity(), highlights);
    }

    private void removeItem(int position) {
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }

    private void trackInvitesEvent(FacebookInvitesEvent event) {
        eventBus.publish(EventQueue.TRACKING, event);
    }

    @Override
    public int getNewItemsTextResourceId() {
        return R.plurals.stream_new_posts;
    }

    @Override
    public void onAdItemClicked(Context context, AdData adData) {
        final String clickthrough = adData instanceof AppInstallAd ? ((AppInstallAd) adData).getClickThroughUrl()
                                                                   : ((VideoAd) adData).getClickThroughUrl();

        navigator.openAdClickthrough(context, Uri.parse(clickthrough));
        if (adData instanceof AppInstallAd) {
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromAppInstallAdClickThrough((AppInstallAd) adData));
        }
    }

    @Override
    public void onWhyAdsClicked(Context context) {
        whyAdsDialogPresenter.show(context);
    }
}
