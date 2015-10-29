package com.soundcloud.android.stream;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamNotificationEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesDialogPresenter;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookInvitesItemRenderer.OnFacebookInvitesClickListener;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stations.StationsOnboardingStreamItemRenderer;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class SoundStreamPresenter extends RecyclerViewPresenter<StreamItem> implements OnFacebookInvitesClickListener, StationsOnboardingStreamItemRenderer.Listener {

    private final SoundStreamOperations streamOperations;
    private final SoundStreamAdapter adapter;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final RecyclerViewParallaxer recyclerViewParallaxer;
    private final EventBus eventBus;
    private final FacebookInvitesDialogPresenter facebookInvitesDialogPresenter;
    private final MixedItemClickListener itemClickListener;
    private final StationsOperations stationsOperations;
    private final FeatureFlags featureFlags;

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
                         RecyclerViewParallaxer recyclerViewParallaxer,
                         FacebookInvitesDialogPresenter facebookInvitesDialogPresenter,
                         FeatureFlags featureFlags) {
        super(swipeRefreshAttacher, getRecyclerOptions(featureFlags));
        this.streamOperations = streamOperations;
        this.adapter = adapter;
        this.stationsOperations = stationsOperations;
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.eventBus = eventBus;
        this.recyclerViewParallaxer = recyclerViewParallaxer;
        this.facebookInvitesDialogPresenter = facebookInvitesDialogPresenter;
        this.featureFlags = featureFlags;
        this.itemClickListener = itemClickListenerFactory.create(Screen.STREAM, null);
        adapter.setOnFacebookInvitesClickListener(this);
        adapter.setOnStationsOnboardingStreamClickListener(this);
    }

    private static Options getRecyclerOptions(FeatureFlags featureFlags) {
        if (featureFlags.isEnabled(Flag.NEW_STREAM)) {
            return Options.custom().useItemClickListener().build();
        } else {
            return Options.list().build();
        }
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        this.fragment = fragment;
        getBinding().connect();
    }

    @Override
    public void onStationOnboardingItemClosed(int position) {
        stationsOperations.disableOnboarding();
        removeItem(position);
    }

    @Override
    protected CollectionBinding<StreamItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(streamOperations.initialStreamItems())
                .withAdapter(adapter)
                .withPager(streamOperations.pagingFunction())
                .build();
    }

    @Override
    protected CollectionBinding<StreamItem> onRefreshBinding() {
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
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter))
        );
    }

    private void addScrollListeners() {
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        if (featureFlags.isEnabled(Flag.NEW_STREAM)) {
            getRecyclerView().addOnScrollListener(recyclerViewParallaxer);
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
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
        itemClickListener.onPostClick(streamOperations.trackUrnsForPlayback(), view, position, item);
    }

    private void publishPromotedItemClickEvent(PromotedListItem item) {
        eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forItemClick(item, Screen.STREAM.get()));
    }

    @Override
    public void onFacebookInvitesCloseButtonClicked(int position) {
        FacebookInvitesItem facebookInvitesItem = getFacebookInvitesItemAtPosition(position);

        if (facebookInvitesItem != null) {
            publishFacebookInviteDismissed(facebookInvitesItem);
            removeItem(position);
        }
    }

    @Nullable
    private FacebookInvitesItem getFacebookInvitesItemAtPosition(int position) {
        StreamItem item = adapter.getItem(position);
        if (item instanceof FacebookInvitesItem) {
            return (FacebookInvitesItem) item;
        } else {
            return null;
        }
    }

    @Override
    public void onFacebookInvitesInviteButtonClicked(int position) {
        FacebookInvitesItem facebookInvitesItem = getFacebookInvitesItemAtPosition(position);

        if (facebookInvitesItem != null) {
            publishFacebookInviteClicked(facebookInvitesItem);
            facebookInvitesDialogPresenter.show(fragment.getActivity());
            removeItem(position);
        }
    }

    private void removeItem(int position) {
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }

    private void publishFacebookInviteDismissed(FacebookInvitesItem item) {
        eventBus.publish(EventQueue.TRACKING, StreamNotificationEvent.forFacebookInviteDismissed(item));
    }

    private void publishFacebookInviteClicked(FacebookInvitesItem item) {
        eventBus.publish(EventQueue.TRACKING, StreamNotificationEvent.forFacebookInviteClick(item));
    }

}
