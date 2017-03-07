package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class PlaylistItemMenuRenderer implements PopupMenuWrapper.PopupMenuWrapperListener {

    interface Listener {

        void handlePlayNext();

        void handleLike(PlaylistItem playlist);

        void handleRepost(boolean repostedByOwner);

        void handleShare(Context context, PlaylistItem playlistItem);

        void handleUpsell(Context context);

        void saveOffline(PlaylistItem playlist);

        void removeFromOffline(Context context);

        void deletePlaylist(Context context);

        void onDismiss();

    }

    private final Listener listener;
    private final AccountOperations accountOperations;
    private final ScreenProvider screenProvider;
    private final EventBus eventBus;
    private final FeatureOperations featureOperations;
    private PopupMenuWrapper menu;
    private PlaylistItem playlist;

    PlaylistItemMenuRenderer(Listener listener,
                             View button,
                             @Provided PopupMenuWrapper.Factory popupMenuWrapperFactory,
                             @Provided AccountOperations accountOperations,
                             @Provided ScreenProvider screenProvider,
                             @Provided EventBus eventBus,
                             @Provided FeatureOperations featureOperations) {
        this.listener = listener;
        this.accountOperations = accountOperations;
        this.screenProvider = screenProvider;
        this.eventBus = eventBus;
        this.featureOperations = featureOperations;

        this.menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.playlist_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);
    }

    void render(PlaylistItem playlist) {
        this.playlist = playlist;
        setupMenu(playlist);
    }

    private void setupMenu(PlaylistItem playlist) {
        updateLikeActionTitle(playlist.isUserLike());
        configureAdditionalEngagementsOptions(playlist);
        configureOfflineOptions(playlist);
        configurePlayNextOption();
        menu.show();
    }

    private void configureAdditionalEngagementsOptions(PlaylistItem playlist) {
        menu.setItemVisible(R.id.toggle_repost, canRepost(playlist));
        menu.setItemVisible(R.id.share, !playlist.isPrivate());
        menu.setItemVisible(R.id.delete_playlist, isOwned(playlist));
        updateRepostActionTitle(playlist.isUserRepost());
    }

    private boolean canRepost(PlaylistItem playlist) {
        return !isOwned(playlist) && !playlist.isPrivate();
    }

    private boolean isOwned(PlaylistItem playlist) {
        return accountOperations.isLoggedInUser(playlist.creatorUrn());
    }

    private void updateLikeActionTitle(boolean isLiked) {
        final MenuItem item = menu.findItem(R.id.add_to_likes);
        item.setTitle(isLiked
                      ? R.string.btn_unlike
                      : R.string.btn_like);
    }

    private void updateRepostActionTitle(boolean isReposted) {
        final MenuItem item = menu.findItem(R.id.toggle_repost);
        item.setTitle(isReposted
                      ? R.string.unpost
                      : R.string.repost);
    }

    private void configureOfflineOptions(PlaylistItem playlist) {
        final Optional<Boolean> maybeMarkedForOffline = playlist.isMarkedForOffline();

        if (maybeMarkedForOffline.isPresent()) {
            final boolean markedForOffline = maybeMarkedForOffline.get();

            if (featureOperations.isOfflineContentEnabled()) {
                showOfflineContentOption(markedForOffline);
            } else if (featureOperations.upsellOfflineContent()) {
                showUpsellOption();
            } else {
                hideAllOfflineContentOptions();
            }
        } else {
            hideAllOfflineContentOptions();
        }

        if (menu.findItem(R.id.upsell_offline_content).isVisible()) {
            eventBus.publish(EventQueue.TRACKING,
                             UpgradeFunnelEvent.forPlaylistItemImpression(screenProvider.getLastScreenTag(),
                                                                          playlist.getUrn()));
        }
    }

    private void showOfflineContentOption(boolean isMarkedForOffline) {
        if (isMarkedForOffline) {
            showOfflineRemovalOption();
        } else {
            showOfflineDownloadOption();
        }
    }

    private void hideAllOfflineContentOptions() {
        menu.setItemVisible(R.id.make_offline_available, false);
        menu.setItemVisible(R.id.make_offline_unavailable, false);
        menu.setItemVisible(R.id.upsell_offline_content, false);
    }

    private void showUpsellOption() {
        menu.setItemVisible(R.id.make_offline_available, false);
        menu.setItemVisible(R.id.make_offline_unavailable, false);
        menu.setItemVisible(R.id.upsell_offline_content, true);
    }

    private void showOfflineDownloadOption() {
        menu.setItemVisible(R.id.make_offline_available, true);
        menu.setItemVisible(R.id.make_offline_unavailable, false);
        menu.setItemVisible(R.id.upsell_offline_content, false);
    }

    private void showOfflineRemovalOption() {
        menu.setItemVisible(R.id.make_offline_available, false);
        menu.setItemVisible(R.id.make_offline_unavailable, true);
        menu.setItemVisible(R.id.upsell_offline_content, false);
    }

    private void configurePlayNextOption() {
        menu.setItemVisible(R.id.play_next, true);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        // todo : should that move to the renderer ?
        switch (menuItem.getItemId()) {
            case R.id.play_next:
                listener.handlePlayNext();
                return true;
            case R.id.add_to_likes:
                listener.handleLike(playlist);
                return true;
            case R.id.toggle_repost:
                listener.handleRepost(!playlist.isUserRepost());
                return true;
            case R.id.share:
                listener.handleShare(context, playlist);
                return true;
            case R.id.upsell_offline_content:
                listener.handleUpsell(context);
                return true;
            case R.id.make_offline_available:
                listener.saveOffline(playlist);
                return true;
            case R.id.make_offline_unavailable:
                listener.removeFromOffline(context);
                return true;
            case R.id.delete_playlist:
                listener.deletePlaylist(context);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDismiss() {
        menu = null;
        listener.onDismiss();
    }

}
