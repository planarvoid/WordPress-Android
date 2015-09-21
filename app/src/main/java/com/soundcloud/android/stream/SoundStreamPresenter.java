package com.soundcloud.android.stream;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamNotificationEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesDialogPresenter;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookInvitesItemRenderer.OnFacebookInvitesClickListener;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

public class SoundStreamPresenter extends RecyclerViewPresenter<StreamItem> implements OnFacebookInvitesClickListener {

    private final SoundStreamOperations streamOperations;
    private final PlaybackOperations playbackOperations;
    private final SoundStreamAdapter adapter;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final EventBus eventBus;
    private final FacebookInvitesDialogPresenter facebookInvitesDialogPresenter;
    private final MixedItemClickListener itemClickListener;

    private CompositeSubscription viewLifeCycle;
    private boolean isOnboardingSuccess;
    private Fragment fragment;

    @Inject
    SoundStreamPresenter(SoundStreamOperations streamOperations,
                         PlaybackOperations playbackOperations,
                         SoundStreamAdapter adapter,
                         ImagePauseOnScrollListener imagePauseOnScrollListener,
                         SwipeRefreshAttacher swipeRefreshAttacher,
                         Provider<ExpandPlayerSubscriber> subscriberProvider,
                         EventBus eventBus,
                         MixedItemClickListener.Factory itemClickListenerFactory,
                         FacebookInvitesDialogPresenter facebookInvitesDialogPresenter) {
        super(swipeRefreshAttacher);
        this.streamOperations = streamOperations;
        this.playbackOperations = playbackOperations;
        this.adapter = adapter;
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.subscriberProvider = subscriberProvider;
        this.eventBus = eventBus;
        this.facebookInvitesDialogPresenter = facebookInvitesDialogPresenter;
        this.itemClickListener = itemClickListenerFactory.create(Screen.SIDE_MENU_STREAM, null);
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        this.fragment = fragment;
        getBinding().connect();
    }

    public void setOnboardingSuccess(boolean onboardingSuccess) {
        this.isOnboardingSuccess = onboardingSuccess;
    }

    @Override
    protected CollectionBinding<StreamItem> onBuildBinding(Bundle fragmentArgs) {
        adapter.setOnFacebookInvitesClickListener(this);
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
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(adapter, adapter.getTrackRenderer())),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    private void configureEmptyView() {
        final EmptyView emptyView = getEmptyView();
        emptyView.setImage(R.drawable.empty_stream);
        if (isOnboardingSuccess) {
            emptyView.setMessageText(R.string.list_empty_stream_message);
            emptyView.setActionText(R.string.list_empty_stream_action);
            emptyView.setButtonActions(new Intent(Actions.SEARCH));
        } else {
            emptyView.setMessageText(R.string.error_onboarding_fail);
        }
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
        } else if (item instanceof FacebookInvitesItem) {
            // no-op
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
        eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forItemClick(item, Screen.SIDE_MENU_STREAM.get()));
    }

    @Override
    public void onFacebookInvitesCloseButtonClicked(int position) {
        FacebookInvitesItem facebookInvitesItem = getFacebookInvitesItemAtPosition(position);

        if (facebookInvitesItem != null) {
            publishFacebookInviteDismissed(facebookInvitesItem);
            removeFacebookInvitesNotification(position);
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
            removeFacebookInvitesNotification(position);
        }
    }

    private void publishFacebookInviteDismissed(FacebookInvitesItem item) {
        eventBus.publish(EventQueue.TRACKING, StreamNotificationEvent.forFacebookInviteDismissed(item));
    }

    private void publishFacebookInviteClicked(FacebookInvitesItem item) {
        eventBus.publish(EventQueue.TRACKING, StreamNotificationEvent.forFacebookInviteClick(item));
    }

    private void removeFacebookInvitesNotification(int position) {
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }
}
