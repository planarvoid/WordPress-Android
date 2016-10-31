package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.tracks.LegacyUpdatePlayingTrackSubscriber;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.upsell.UpsellItemRenderer;
import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.android.view.adapters.LegacyUpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.LegacyUpdateEntityListSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;

abstract class PlaylistDetailsController implements EmptyViewAware, TrackItemMenuPresenter.RemoveTrackListener, UpsellItemRenderer.Listener {

    private final PlaylistTrackItemRenderer trackRenderer;
    private final ListItemAdapter<TypedListItem> adapter;
    private final PlaylistUpsellOperations upsellOperations;
    private final EventBus eventBus;
    private final Navigator navigator;

    private Subscription eventSubscriptions = RxUtils.invalidSubscription();
    private Urn playlistUrn = Urn.NOT_SET;

    protected ListView listView;
    private Listener listener;

    void setContent(PlaylistWithTracks playlist, PromotedSourceInfo promotedSourceInfo) {
        playlistUrn = playlist.getUrn();
        eventSubscriptions.unsubscribe();
        trackRenderer.setPlaylistInformation(promotedSourceInfo, playlist.getUrn(), playlist.getCreatorUrn());
        adapter.clear();
        for (TypedListItem listItem : upsellOperations.toListItems(playlist)) {
            adapter.addItem(listItem);
        }
        adapter.notifyDataSetChanged();
        subscribeToContentUpdate();
    }

    interface Listener {
        void onPlaylistContentChanged();
    }

    protected PlaylistDetailsController(PlaylistTrackItemRenderer trackRenderer,
                                        PlaylistUpsellItemRenderer upsellItemRenderer,
                                        ListItemAdapter<TypedListItem> adapter,
                                        PlaylistUpsellOperations upsellOperations,
                                        EventBus eventBus,
                                        Navigator navigator) {
        this.trackRenderer = trackRenderer;
        this.adapter = adapter;
        this.upsellOperations = upsellOperations;
        this.eventBus = eventBus;
        this.navigator = navigator;
        upsellItemRenderer.setListener(this);
        trackRenderer.setRemoveTrackListener(this);
    }

    public void showTrackRemovalOptions(Listener listener) {
        this.listener = listener;
    }

    public void onPlaylistTrackRemoved(int position) {
        AnimUtils.removeItemFromList(listView, position, new AnimUtils.ItemRemovalCallback() {
            @Override
            public void onAnimationComplete(int position) {
                adapter.removeItem(position);
                adapter.notifyDataSetChanged();
                listener.onPlaylistContentChanged();
            }
        });
    }

    public Urn getPlaylistUrn() {
        return playlistUrn;
    }

    ListItemAdapter<TypedListItem> getAdapter() {
        return adapter;
    }

    boolean hasTracks() {
        // do not use isEmpty, as it will return false if loading
        return adapter.getItems().size() > 0;
    }

    abstract void setEmptyStateMessage(String title, String description);

    abstract boolean hasContent();

    abstract void setListShown(boolean show);

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        subscribeToContentUpdate();
    }

    private void subscribeToContentUpdate() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new LegacyUpdatePlayingTrackSubscriber(adapter, trackRenderer)),
                eventBus.subscribe(OFFLINE_CONTENT_CHANGED, new LegacyUpdateCurrentDownloadSubscriber(adapter)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new LegacyUpdateEntityListSubscriber(adapter))
        );
    }

    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }

    @Override
    public void onUpsellItemDismissed(int position) {
        upsellOperations.disableUpsell();
        adapter.removeItem(position);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onUpsellItemClicked(Context context) {
        navigator.openUpgrade(context);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistTracksClick(playlistUrn));
    }

    @Override
    public void onUpsellItemCreated() {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistTracksImpression(playlistUrn));
    }

    public static class Provider {
        private final javax.inject.Provider<PlaylistDetailsController> injectionProvider;

        @Inject
        Provider(javax.inject.Provider<PlaylistDetailsController> injectionProvider) {
            this.injectionProvider = injectionProvider;
        }

        public PlaylistDetailsController create() {
            return injectionProvider.get();
        }
    }
}
