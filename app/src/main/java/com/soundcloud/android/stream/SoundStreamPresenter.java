package com.soundcloud.android.stream;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
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

public class SoundStreamPresenter extends RecyclerViewPresenter<StreamItem> {

    private final SoundStreamOperations streamOperations;
    private final PlaybackOperations playbackOperations;
    private final SoundStreamAdapter adapter;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final EventBus eventBus;
    private final MixedItemClickListener itemClickListener;

    private CompositeSubscription viewLifeCycle;
    private boolean isOnboardingSuccess;

    @Inject
    SoundStreamPresenter(SoundStreamOperations streamOperations,
                         PlaybackOperations playbackOperations,
                         SoundStreamAdapter adapter,
                         ImagePauseOnScrollListener imagePauseOnScrollListener,
                         SwipeRefreshAttacher swipeRefreshAttacher,
                         Provider<ExpandPlayerSubscriber> subscriberProvider,
                         EventBus eventBus,
                         MixedItemClickListener.Factory itemClickListenerFactory) {
        super(swipeRefreshAttacher);
        this.streamOperations = streamOperations;
        this.playbackOperations = playbackOperations;
        this.adapter = adapter;
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.subscriberProvider = subscriberProvider;
        this.eventBus = eventBus;
        this.itemClickListener = itemClickListenerFactory.create(Screen.SIDE_MENU_STREAM, null);
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    public void setOnboardingSuccess(boolean onboardingSuccess) {
        this.isOnboardingSuccess = onboardingSuccess;
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
            emptyView.setButtonActions(new Intent(Actions.WHO_TO_FOLLOW));
        } else {
            emptyView.setMessageText(R.string.error_onboarding_fail);
        }
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final ListItem item = adapter.getItem(position);
        if (item instanceof PromotedTrackItem) {
            playFromPromotedTrack(position, (PromotedTrackItem) item);
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

    private void playFromPromotedTrack(int position, PromotedTrackItem promotedTrack) {
        publishPromotedItemClickEvent(promotedTrack);
        PlaySessionSource source = new PlaySessionSource(Screen.SIDE_MENU_STREAM);
        source.setPromotedSourceInfo(PromotedSourceInfo.fromItem(promotedTrack));
        playTracks(position, promotedTrack.getEntityUrn(), source);
    }

    private void publishPromotedItemClickEvent(PromotedListItem item) {
        eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forItemClick(item, Screen.SIDE_MENU_STREAM.get()));
    }

    private void playTracks(int position, Urn playableUrn, PlaySessionSource playSessionSource) {
        playbackOperations
                .playPosts(streamOperations.trackUrnsForPlayback(), playableUrn, position, playSessionSource)
                .subscribe(subscriberProvider.get());
    }

}
