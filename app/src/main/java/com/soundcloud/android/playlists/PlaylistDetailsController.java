package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.CURRENT_DOWNLOAD;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.view.adapters.ListItemAdapter;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;


abstract class PlaylistDetailsController implements EmptyViewAware, TrackItemMenuPresenter.RemoveTrackListener {

    private final PlaylistTrackItemRenderer trackRenderer;
    private final ListItemAdapter<TrackItem> adapter;
    private final EventBus eventBus;

    private Subscription eventSubscriptions = Subscriptions.empty();
    private Urn playlistUrn = Urn.NOT_SET;

    protected ListView listView;
    private Listener listener;

    void setContent(PlaylistWithTracks playlist) {
        eventSubscriptions.unsubscribe();
        adapter.clear();
        for (TrackItem track : playlist.getTracks()) {
            adapter.addItem(track);
        }
        adapter.notifyDataSetChanged();
        subscribeToContentUpdate();
    }

    static interface Listener {
        void onPlaylistContentChanged();
    }

    protected PlaylistDetailsController(PlaylistTrackItemRenderer trackRenderer, ListItemAdapter<TrackItem> adapter,
                                        EventBus eventBus) {
        this.trackRenderer = trackRenderer;
        this.adapter = adapter;
        this.eventBus = eventBus;
        trackRenderer.setRemoveTrackListener(this);
    }

    public void showTrackRemovalOptions(final Urn urn, Listener listener){
        this.playlistUrn = urn;
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

    public Urn getPlaylistUrn(){
        return playlistUrn;
    }

    ListItemAdapter<TrackItem> getAdapter() {
        return adapter;
    }

    boolean hasTracks() {
        // do not use isEmpty, as it will return false if loading
        return adapter.getItems().size() > 0;
    }

    abstract boolean hasContent();

    abstract void setListShown(boolean show);

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        subscribeToContentUpdate();
    }

    private void subscribeToContentUpdate() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(adapter, trackRenderer)),
                eventBus.subscribe(CURRENT_DOWNLOAD, new UpdateCurrentDownloadSubscriber(adapter)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter))
        );
    }

    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
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
