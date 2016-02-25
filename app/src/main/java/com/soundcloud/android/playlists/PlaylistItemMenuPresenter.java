package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.tracks.OverflowMenuOptions;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

public class PlaylistItemMenuPresenter implements PopupMenuWrapper.PopupMenuWrapperListener {

    private final Context appContext;
    private final EventBus eventBus;
    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final AccountOperations accountOperations;
    private final PlaylistOperations playlistOperations;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final ShareOperations shareOperations;
    private final ScreenProvider screenProvider;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final Navigator navigator;

    private PlaylistItem playlist;
    private Subscription playlistSubscription = RxUtils.invalidSubscription();
    private OverflowMenuOptions menuOptions;

    @Inject
    public PlaylistItemMenuPresenter(Context appContext, EventBus eventBus,
                                     PopupMenuWrapper.Factory popupMenuWrapperFactory,
                                     AccountOperations accountOperations, PlaylistOperations playlistOperations, LikeOperations likeOperations,
                                     RepostOperations repostOperations, ShareOperations shareOperations, ScreenProvider screenProvider,
                                     FeatureOperations featureOperations, OfflineContentOperations offlineContentOperations, Navigator navigator) {
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.accountOperations = accountOperations;
        this.playlistOperations = playlistOperations;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.shareOperations = shareOperations;
        this.screenProvider = screenProvider;
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.navigator = navigator;
    }

    public void show(View button, PlaylistItem playlist) {
        show(button, playlist, OverflowMenuOptions.builder().build());
    }

    public void show(View button, PlaylistItem playlist, OverflowMenuOptions menuOptions) {
        this.playlist = playlist;
        this.menuOptions = menuOptions;

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
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.add_to_likes:
                handleLike();
                return true;
            case R.id.toggle_repost:
                handleRepost();
                return true;
            case R.id.share:
                handleShare(context);
                return true;
            case R.id.upsell_offline_content:
                navigator.openUpgrade(context);
                eventBus.publish(EventQueue.TRACKING,
                        UpgradeTrackingEvent.forPlaylistItemClick(screenProvider.getLastScreenTag(), playlist.getEntityUrn()));
                return true;
            case R.id.make_offline_available:
                saveOffline();
                return true;
            case R.id.make_offline_unavailable:
                removeFromOffline(toFragmentManager(context));
                return true;
            case R.id.delete_playlist:
                deletePlaylist(toFragmentManager(context));
                return true;
            default:
                return false;
        }
    }

    private static FragmentManager toFragmentManager(Context context) {
        return ((FragmentActivity) context).getSupportFragmentManager();
    }

    private void saveOffline() {
        fireAndForget(offlineContentOperations.makePlaylistAvailableOffline(playlist.getEntityUrn()));
        eventBus.publish(EventQueue.TRACKING, OfflineInteractionEvent.fromAddOfflinePlaylist(
                screenProvider.getLastScreenTag(),
                playlist.getEntityUrn(),
                getPromotedSourceIfExists()));
    }

    private void removeFromOffline(FragmentManager fragmentManager) {
        if (offlineContentOperations.isOfflineCollectionEnabled()) {
            ConfirmRemoveOfflineDialogFragment.showForPlaylist(fragmentManager, playlist.getEntityUrn(), getPromotedSourceIfExists());
        } else {
            fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(playlist.getEntityUrn()));
            eventBus.publish(EventQueue.TRACKING, OfflineInteractionEvent.fromRemoveOfflinePlaylist(
                    screenProvider.getLastScreenTag(),
                    playlist.getEntityUrn(),
                    getPromotedSourceIfExists()));
        }
    }

    private void deletePlaylist(FragmentManager fragmentManager) {
        DeletePlaylistDialogFragment.show(fragmentManager, playlist.getEntityUrn());
    }

    private PromotedSourceInfo getPromotedSourceIfExists() {
        if (playlist instanceof PromotedPlaylistItem) {
            return PromotedSourceInfo.fromItem((PromotedPlaylistItem) playlist);
        }
        return null;
    }

    private EventContextMetadata getEventContextMetadata() {
        return EventContextMetadata.builder()
                .invokerScreen(ScreenElement.LIST.get())
                .contextScreen(screenProvider.getLastScreenTag())
                .pageName(screenProvider.getLastScreenTag())
                .isFromOverflow(true)
                .build();
    }

    private void handleLike() {
        final Urn playlistUrn = playlist.getEntityUrn();
        boolean addLike = !playlist.isLiked();
        likeOperations.toggleLike(playlistUrn, addLike)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LikeToggleSubscriber(appContext, addLike));

        eventBus.publish(EventQueue.TRACKING,
                UIEvent.fromToggleLike(addLike,
                        playlist.getEntityUrn(),
                        getEventContextMetadata(),
                        getPromotedSourceIfExists(),
                        EntityMetadata.from(playlist)));

        if (isUnlikingNotOwnedPlaylistInOfflineMode(addLike)) {
            fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(playlistUrn));
        }
    }

    private void handleRepost() {
        final Urn playlistUrn = playlist.getEntityUrn();
        boolean addRepost = !playlist.isReposted();
        repostOperations.toggleRepost(playlistUrn, addRepost)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RepostResultSubscriber(appContext, addRepost));

        eventBus.publish(EventQueue.TRACKING,
                UIEvent.fromToggleRepost(addRepost,
                        playlist.getEntityUrn(),
                        getEventContextMetadata(),
                        getPromotedSourceIfExists(),
                        EntityMetadata.from(playlist)));
    }

    private void handleShare(Context context) {
        shareOperations.share(context, playlist.getSource(),
                getEventContextMetadata(),
                getPromotedSourceIfExists());
    }

    private boolean isUnlikingNotOwnedPlaylistInOfflineMode(boolean addLike) {
        boolean offlineContentEnabled = featureOperations.isOfflineContentEnabled() && menuOptions.showOffline();
        return offlineContentEnabled && !addLike && !playlist.isPosted();
    }

    private PopupMenuWrapper setupMenu(View button) {
        PopupMenuWrapper menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.playlist_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);

        updateLikeActionTitle(menu, playlist.isLiked());
        configureAdditionalEngagementsOptions(menu);
        configureInitialOfflineOptions(menu);

        menu.show();
        return menu;
    }

    private void configureAdditionalEngagementsOptions(PopupMenuWrapper menu) {
        menu.setItemVisible(R.id.toggle_repost, canRepost(playlist));
        menu.setItemVisible(R.id.share, !playlist.isPrivate());
        menu.setItemVisible(R.id.delete_playlist, isOwned(playlist));
        updateRepostActionTitle(menu, playlist.isReposted());
    }

    private boolean canRepost(PlaylistItem playlist) {
        return !isOwned(playlist) && !playlist.isPrivate();
    }

    private boolean isOwned(PlaylistItem playlist) {
        return accountOperations.isLoggedInUser(playlist.getCreatorUrn());
    }

    private void updateLikeActionTitle(PopupMenuWrapper menu, boolean isLiked) {
        final MenuItem item = menu.findItem(R.id.add_to_likes);
        item.setTitle(isLiked
                ? R.string.btn_unlike
                : R.string.btn_like);
    }

    private void updateRepostActionTitle(PopupMenuWrapper menu, boolean isReposted) {
        final MenuItem item = menu.findItem(R.id.toggle_repost);
        item.setTitle(isReposted
                ? R.string.unpost
                : R.string.repost);
    }

    private void configureInitialOfflineOptions(PopupMenuWrapper menu) {
        final Optional<Boolean> maybeMarkedForOffline = playlist.isMarkedForOffline();
        if (maybeMarkedForOffline.isPresent() && menuOptions.showOffline()) {
            configureOfflineOptions(menu, maybeMarkedForOffline);
        } else {
            hideAllOfflineContentOptions(menu);
        }
        if (menu.findItem(R.id.upsell_offline_content).isVisible()) {
            eventBus.publish(EventQueue.TRACKING,
                    UpgradeTrackingEvent.forPlaylistItemImpression(screenProvider.getLastScreenTag(), playlist.getEntityUrn()));
        }
    }

    private void configureOfflineOptions(PopupMenuWrapper menu, Optional<Boolean> maybeMarkedForOffline) {
        if (featureOperations.isOfflineContentEnabled() && menuOptions.showOffline() && maybeMarkedForOffline.isPresent()) {
            showOfflineContentOption(menu, maybeMarkedForOffline.get());
        } else if (featureOperations.upsellOfflineContent() && menuOptions.showOffline()) {
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

    // this is really ugly. We should introduce a PlaylistRepository.
    // https://github.com/soundcloud/SoundCloud-Android/issues/2942
    private void loadPlaylist(PopupMenuWrapper menu) {
        playlistSubscription.unsubscribe();
        playlistSubscription = playlistOperations
                .playlist(playlist.getEntityUrn())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlaylistSubscriber(playlist, menu));
    }

    private final class PlaylistSubscriber extends DefaultSubscriber<PlaylistWithTracks> {
        private final PlaylistItem playlist;
        private final PopupMenuWrapper menu;

        public PlaylistSubscriber(PlaylistItem playlist, PopupMenuWrapper menu) {
            this.playlist = playlist;
            this.menu = menu;
        }

        @Override
        public void onNext(PlaylistWithTracks details) {
            playlist.update(details.getSourceSet());
            updateLikeActionTitle(menu, playlist.isLiked());
            updateRepostActionTitle(menu, playlist.isReposted());
            configureOfflineOptions(menu, playlist.isMarkedForOffline());
        }
    }
}
