package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.PopupMenuWrapperListener;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

public class PlaylistItemMenuPresenter implements PopupMenuWrapperListener {

    private final Context context;
    private final EventBus eventBus;
    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final LoadPlaylistCommand loadPlaylistCommand;
    private final LikeOperations likeOperations;
    private final ScreenProvider screenProvider;

    private PropertySet playlist;
    private Subscription playlistSubscription = Subscriptions.empty();

    @Inject
    public PlaylistItemMenuPresenter(Context context, EventBus eventBus, PopupMenuWrapper.Factory popupMenuWrapperFactory,
                                     LoadPlaylistCommand loadPlaylistCommand, LikeOperations likeOperations, ScreenProvider screenProvider) {
        this.context = context;
        this.eventBus = eventBus;
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.loadPlaylistCommand = loadPlaylistCommand;
        this.likeOperations = likeOperations;
        this.screenProvider = screenProvider;
    }

    public void show(View button, PropertySet playlist) {
        this.playlist = playlist;
        final PopupMenuWrapper menu = setupMenu(button);

        loadPlaylist(menu);
    }

    @Override
    public void onDismiss() {
        playlistSubscription.unsubscribe();
        playlistSubscription = Subscriptions.empty();
        playlist = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.add_to_likes:
                handleLike();
                return true;
            default:
                return false;
        }
    }

    private void handleLike() {
        final Urn trackUrn = playlist.get(TrackProperty.URN);
        final Boolean addLike = !playlist.get(TrackProperty.IS_LIKED);
        getToggleLikeObservable(addLike)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LikeToggleSubscriber(context, addLike));
        eventBus.publish(EventQueue.TRACKING,
                UIEvent.fromToggleLike(addLike, ScreenElement.LIST.get(),
                        screenProvider.getLastScreenTag(), trackUrn));
    }

    private Observable<PropertySet> getToggleLikeObservable(boolean addLike) {
        return addLike ? likeOperations.addLike(playlist) : likeOperations.removeLike(playlist);
    }

    private PopupMenuWrapper setupMenu(View button) {
        PopupMenuWrapper menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.playlist_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);
        menu.setItemEnabled(R.id.add_to_likes, false);
        menu.show();
        return menu;
    }

    private void loadPlaylist(PopupMenuWrapper menu) {
        playlistSubscription.unsubscribe();
        playlistSubscription = loadPlaylistCommand
                .with(playlist.get(PlaylistProperty.URN))
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlaylistSubscriber(playlist, menu));
    }

    private static class PlaylistSubscriber extends DefaultSubscriber<PropertySet> {
        private final PropertySet playlist;
        private final PopupMenuWrapper menu;

        public PlaylistSubscriber(PropertySet playlist, PopupMenuWrapper menu) {
            this.playlist = playlist;
            this.menu = menu;
        }

        @Override
        public void onNext(PropertySet details) {
            playlist.update(details);
            updateLikeActionTitle(playlist.get(TrackProperty.IS_LIKED));
            menu.setItemEnabled(R.id.add_to_likes, true);
        }

        private void updateLikeActionTitle(boolean isLiked) {
            final MenuItem item = menu.findItem(R.id.add_to_likes);
            if (isLiked) {
                item.setTitle(R.string.unlike);
            } else {
                item.setTitle(R.string.like);
            }
        }
    }
}
