package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

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
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;

    private PropertySet playlist;
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

    public void show(View button, PropertySet playlist, boolean allowOfflineOptions) {
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
                fireAndForget(offlineContentOperations.makePlaylistAvailableOffline(playlist.get(PlaylistProperty.URN)));
                return true;
            case R.id.make_offline_unavailable:
                fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(playlist.get(PlaylistProperty.URN)));
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

        configureLikeOption(menu);
        configureInitialOfflineOptions(menu);

        menu.show();
        return menu;
    }

    private void configureLikeOption(PopupMenuWrapper menu) {
        if (playlist.contains(TrackProperty.IS_LIKED)){
            updateLikeActionTitle(menu, playlist.get(TrackProperty.IS_LIKED));
            menu.setItemEnabled(R.id.add_to_likes, true);
        } else {
            menu.setItemEnabled(R.id.add_to_likes, false);
        }
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
        if (playlist.contains(PlaylistProperty.IS_MARKED_FOR_OFFLINE) && allowOfflineOptions) {
            configureOfflineOptions(menu);
        } else {
            hideAllOfflineContentOptions(menu);
        }
    }

    private void configureOfflineOptions(PopupMenuWrapper menu) {
        if (featureOperations.isOfflineContentEnabled() && allowOfflineOptions) {
            showOfflineContentOption(menu);
        } else if (featureOperations.isOfflineContentUpsellEnabled() && allowOfflineOptions) {
            showUpsellOption(menu);
        } else {
            hideAllOfflineContentOptions(menu);
        }
    }

    private void showOfflineContentOption(PopupMenuWrapper menu) {
        if (playlist.get(PlaylistProperty.IS_MARKED_FOR_OFFLINE)) {
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
                .with(playlist.get(PlaylistProperty.URN))
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlaylistSubscriber(playlist, menu));
    }

    private class PlaylistSubscriber extends DefaultSubscriber<PropertySet> {
        private final PropertySet playlist;
        private final PopupMenuWrapper menu;

        public PlaylistSubscriber(PropertySet playlist, PopupMenuWrapper menu) {
            this.playlist = playlist;
            this.menu = menu;
        }

        @Override
        public void onNext(PropertySet details) {
            playlist.update(details);
            configureLikeOption(menu);
            configureOfflineOptions(menu);
        }
    }
}
