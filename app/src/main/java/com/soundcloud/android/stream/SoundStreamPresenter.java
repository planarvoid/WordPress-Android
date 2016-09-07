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
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.facebookinvites.FacebookCreatorInvitesItemRenderer;
import com.soundcloud.android.facebookinvites.FacebookInvitesDialogPresenter;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookListenerInvitesItemRenderer;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.stations.StationsOnboardingStreamItemRenderer;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.timeline.TimelinePresenter;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.upsell.UpsellItemRenderer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class SoundStreamPresenter extends TimelinePresenter<TypedListItem> implements
        FacebookListenerInvitesItemRenderer.Listener,
        StationsOnboardingStreamItemRenderer.Listener,
        FacebookCreatorInvitesItemRenderer.Listener,
        UpsellItemRenderer.Listener,
        NewItemsIndicator.Listener {

    private static final Func1<StreamEvent, Boolean> FILTER_STREAM_REFRESH_EVENTS = new Func1<StreamEvent, Boolean>() {
        @Override
        public Boolean call(StreamEvent streamEvent) {
            return streamEvent.isStreamRefreshed();
        }
    };

    private final SoundStreamOperations streamOperations;
    private final SoundStreamAdapter adapter;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final EventBus eventBus;
    private final FacebookInvitesDialogPresenter invitesDialogPresenter;
    private final MixedItemClickListener itemClickListener;
    private final StationsOperations stationsOperations;
    private final Navigator navigator;
    private final NewItemsIndicator newItemsIndicator;

    private CompositeSubscription viewLifeCycle;
    private Fragment fragment;

    @Inject
    SoundStreamPresenter(SoundStreamOperations streamOperations,
                         SoundStreamAdapter adapter,
                         StationsOperations stationsOperations,
                         ImagePauseOnScrollListener imagePauseOnScrollListener,
                         SwipeRefreshAttacher swipeRefreshAttacher,
                         EventBus eventBus,
                         MixedItemClickListener.Factory itemClickListenerFactory,
                         FacebookInvitesDialogPresenter invitesDialogPresenter,
                         Navigator navigator,
                         NewItemsIndicator newItemsIndicator) {
        super(swipeRefreshAttacher, Options.staggeredGrid(R.integer.grids_num_columns).build(),
              newItemsIndicator, streamOperations, adapter);
        this.streamOperations = streamOperations;
        this.adapter = adapter;
        this.stationsOperations = stationsOperations;
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.eventBus = eventBus;
        this.invitesDialogPresenter = invitesDialogPresenter;
        this.navigator = navigator;
        this.newItemsIndicator = newItemsIndicator;

        this.itemClickListener = itemClickListenerFactory.create(Screen.STREAM, null);

        adapter.setOnFacebookInvitesClickListener(this);
        adapter.setOnFacebookCreatorInvitesClickListener(this);
        adapter.setOnStationsOnboardingStreamClickListener(this);
        adapter.setOnUpsellClickListener(this);
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        this.fragment = fragment;
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<TypedListItem>, TypedListItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(streamOperations.initialStreamItems())
                                .withAdapter(adapter)
                                .withPager(streamOperations.pagingFunction())
                                .build();
    }

    @Override
    protected CollectionBinding<List<TypedListItem>, TypedListItem> onRefreshBinding() {
        newItemsIndicator.hideAndReset();
        return CollectionBinding.from(streamOperations.updatedStreamItems())
                                .withAdapter(adapter)
                                .withPager(streamOperations.pagingFunction())
                                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        configureEmptyView();
        addScrollListeners();

        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                fireAndForget(eventBus.queue(EventQueue.STREAM)
                        .filter(FILTER_STREAM_REFRESH_EVENTS)
                        .flatMap(continueWith(updateIndicatorFromMostRecent())))
        );
    }

    private void addScrollListeners() {
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        getRecyclerView().addOnScrollListener(new RecyclerViewParallaxer());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
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
        final ListItem item = adapter.getItem(position);
        if (item instanceof PromotedTrackItem) {
            publishPromotedItemClickEvent((PromotedTrackItem) item);
            handleListItemClick(view, position, item);
        } else if (item instanceof PromotedPlaylistItem) {
            publishPromotedItemClickEvent((PromotedPlaylistItem) item);
            handleListItemClick(view, position, item);
        } else {
            handleListItemClick(view, position, item);
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private void handleListItemClick(View view, int position, ListItem item) {
        itemClickListener.legacyOnPostClick(streamOperations.urnsForPlayback(), view, position, item);
    }

    private void publishPromotedItemClickEvent(PromotedListItem item) {
        eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forItemClick(item, Screen.STREAM.get()));
    }

    @Override
    public void onListenerInvitesClicked(int position) {
        FacebookInvitesItem facebookInvitesItem = getInvitesItemAtPosition(position);

        if (facebookInvitesItem != null) {
            trackInvitesEvent(forListenerClick(facebookInvitesItem));
            invitesDialogPresenter.showForListeners(fragment.getActivity());
            removeItem(position);
        }
    }

    @Override
    public void onListenerInvitesDismiss(int position) {
        FacebookInvitesItem facebookInvitesItem = getInvitesItemAtPosition(position);

        if (facebookInvitesItem != null) {
            trackInvitesEvent(forListenerDismiss(facebookInvitesItem));
            removeItem(position);
        }
    }

    @Override
    public void onListenerInvitesLoaded(FacebookInvitesItem item) {
        trackInvitesEvent(forListenerShown(item));
    }

    @Override
    public void onCreatorInvitesClicked(int position) {
        FacebookInvitesItem item = getInvitesItemAtPosition(position);
        if (item != null) {
            if (hasCreatorInvitesTrack(item)) {
                trackInvitesEvent(forCreatorClick());
                invitesDialogPresenter.showForCreators(fragment.getActivity(), item.getTrackUrl(), item.getTrackUrn());
            }
            removeItem(position);
        }
    }

    private boolean hasCreatorInvitesTrack(FacebookInvitesItem item) {
        return !Urn.NOT_SET.equals(item.getTrackUrn());
    }

    @Override
    public void onCreatorInvitesDismiss(int position) {
        FacebookInvitesItem item = getInvitesItemAtPosition(position);
        if (item != null) {
            trackInvitesEvent(forCreatorDismiss());
            removeItem(position);
        }
    }

    @Nullable
    private FacebookInvitesItem getInvitesItemAtPosition(int position) {
        TypedListItem item = adapter.getItem(position);
        if (item instanceof FacebookInvitesItem) {
            return (FacebookInvitesItem) item;
        } else {
            return null;
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
}