package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Optional;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.propeller.PropertySet;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

public class PlaylistItemMenuPresenter implements PopupMenuWrapper.PopupMenuWrapperListener {

    private final Context context;
    private final EventBus eventBus;
    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final LoadPlaylistCommand loadPlaylistCommand;
    private final LikeOperations likeOperations;
    private final ScreenProvider screenProvider;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;

    private PlaylistItem playlist;
    private Subscription playlistSubscription = Subscriptions.empty();
    private boolean allowOfflineOptions;

    @Inject
    public PlaylistItemMenuPresenter(Context context, EventBus eventBus, PopupMenuWrapper.Factory popupMenuWrapperFactory,
                                     LoadPlaylistCommand loadPlaylistCommand, LikeOperations likeOperations, ScreenProvider screenProvider, FeatureOperations featureOperations, OfflineContentOperations offlineContentOperations) {
        this.context = context;
        this.eventBus = eventBus;
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.loadPlaylistCommand = loadPlaylistCommand;
        this.likeOperations = likeOperations;
        this.screenProvider = screenProvider;
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
    }

    public void show(View button, PlaylistItem playlist, boolean allowOfflineOptions) {
        this.playlist = playlist;
        this.allowOfflineOptions = allowOfflineOptions;
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
            case R.id.upsell_offline_content:
                // TODO
                return true;
            case R.id.make_offline_available:
                fireAndForget(offlineContentOperations.makePlaylistAvailableOffline(playlist.getEntityUrn()));
                return true;
            case R.id.make_offline_unavailable:
                fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(playlist.getEntityUrn()));
                return true;
            default:
                return false;
        }
    }

    private void handleLike() {
        final Urn playlistUrn = playlist.getEntityUrn();
        final Boolean addLike = !playlist.isLiked();
        likeOperations.toggleLike(playlistUrn, addLike)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LikeToggleSubscriber(context, addLike));
        eventBus.publish(EventQueue.TRACKING,
                UIEvent.fromToggleLike(addLike, ScreenElement.LIST.get(),
                        screenProvider.getLastScreenTag(), playlistUrn));
    }

    private PopupMenuWrapper setupMenu(View button) {
        PopupMenuWrapper menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.playlist_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);

        updateLikeActionTitle(menu, playlist.isLiked());
        configureInitialOfflineOptions(menu);

        menu.show();
        return menu;
    }

    private void updateLikeActionTitle(PopupMenuWrapper menu, boolean isLiked) {
        final MenuItem item = menu.findItem(R.id.add_to_likes);
        if (isLiked) {
            item.setTitle(R.string.unlike);
        } else {
            item.setTitle(R.string.like);
        }
    }

    private void configureInitialOfflineOptions(PopupMenuWrapper menu) {
        final Optional<Boolean> maybeMarkedForOffline = playlist.isMarkedForOffline();
        if (maybeMarkedForOffline.isPresent() && allowOfflineOptions) {
            configureOfflineOptions(menu, maybeMarkedForOffline);
        } else {
            hideAllOfflineContentOptions(menu);
        }
    }

    private void configureOfflineOptions(PopupMenuWrapper menu, Optional<Boolean> maybeMarkedForOffline) {
        if (featureOperations.isOfflineContentEnabled() && allowOfflineOptions && maybeMarkedForOffline.isPresent()) {
            showOfflineContentOption(menu, maybeMarkedForOffline.get());
        } else if (featureOperations.isOfflineContentUpsellEnabled() && allowOfflineOptions) {
            showUpsellOption(menu);
        } else {
            hideAllOfflineContentOptions(menu);
        }
    }

    private void showOfflineContentOption(PopupMenuWrapper menu, boolean isMarkedForOffline) {
        if (isMarkedForOffline) {
            showOfflineRemovalOption(menu);
        } else {
            showOfflineDownloadOption(menu);
        }
    }

    private void hideAllOfflineContentOptions(PopupMenuWrapper menu) {
        menu.setItemVisible(R.id.make_offline_available, false);
        menu.setItemVisible(R.id.make_offline_unavailable, false);
        menu.setItemVisible(R.id.upsell_offline_content, false);
    }

    private void showUpsellOption(PopupMenuWrapper menu) {
        menu.setItemVisible(R.id.make_offline_available, false);
        menu.setItemVisible(R.id.make_offline_unavailable, false);
        menu.setItemVisible(R.id.upsell_offline_content, true);
    }

    private void showOfflineDownloadOption(PopupMenuWrapper menu) {
        menu.setItemVisible(R.id.make_offline_available, true);
        menu.setItemVisible(R.id.make_offline_unavailable, false);
        menu.setItemVisible(R.id.upsell_offline_content, false);
    }

    private void showOfflineRemovalOption(PopupMenuWrapper menu) {
        menu.setItemVisible(R.id.make_offline_available, false);
        menu.setItemVisible(R.id.make_offline_unavailable, true);
        menu.setItemVisible(R.id.upsell_offline_content, false);
    }

    private void loadPlaylist(PopupMenuWrapper menu) {
        playlistSubscription.unsubscribe();
        playlistSubscription = loadPlaylistCommand
                .with(playlist.getEntityUrn())
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlaylistSubscriber(playlist, menu));
    }

    private class PlaylistSubscriber extends DefaultSubscriber<PropertySet> {
        private final PlaylistItem playlist;
        private final PopupMenuWrapper menu;

        public PlaylistSubscriber(PlaylistItem playlist, PopupMenuWrapper menu) {
            this.playlist = playlist;
            this.menu = menu;
        }

        @Override
        public void onNext(PropertySet details) {
            playlist.update(details);
            updateLikeActionTitle(menu, playlist.isLiked());
            configureOfflineOptions(menu, playlist.isMarkedForOffline());
        }
    }
}
