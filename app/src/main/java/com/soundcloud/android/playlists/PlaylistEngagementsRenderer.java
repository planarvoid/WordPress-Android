package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadStateRenderer;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.ui.LikeButtonPresenter;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.view.IconToggleButton;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.Resources;
import android.view.MenuItem;
import android.view.View;
import android.widget.Checkable;
import android.widget.ToggleButton;

import javax.inject.Inject;

class PlaylistEngagementsRenderer {

    private final Context context;
    private final Resources resources;
    private final FeatureFlags featureFlags;
    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final DownloadStateRenderer downloadStateRenderer;
    private final LikeButtonPresenter likeButtonPresenter;
    private final PlaylistDetailInfoProvider infoProvider;


    @Inject
    PlaylistEngagementsRenderer(Context context,
                                FeatureFlags featureFlags,
                                PopupMenuWrapper.Factory popupMenuWrapperFactory,
                                DownloadStateRenderer downloadStateRenderer,
                                LikeButtonPresenter likeButtonPresenter,
                                PlaylistDetailInfoProvider infoProvider) {
        this.context = context;
        this.featureFlags = featureFlags;
        this.likeButtonPresenter = likeButtonPresenter;
        this.resources = context.getResources();
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.downloadStateRenderer = downloadStateRenderer;
        this.infoProvider = infoProvider;
    }

    void bind(View view, PlaylistDetailsViewModel item, PlaylistDetailsInputs onEngagementListener) {
        view.findViewById(R.id.playlist_engagement_bar).setVisibility(View.VISIBLE); // it is not visible by default
        final PlaylistDetailsMetadata metadata = item.metadata();
        bindEngagementBar(view, item, onEngagementListener);
        setInfoText(infoProvider.getPlaylistInfoLabel(metadata.offlineState(), metadata.headerText()), view);
    }

    private void bindEngagementBar(View view, PlaylistDetailsViewModel item, PlaylistDetailsInputs onEngagementListener) {
        final PlaylistDetailsMetadata metadata = item.metadata();
        configureLikeButton(view, metadata, onEngagementListener);
        configureOverflow(view, metadata, onEngagementListener);
        configureDownloadToggle(item, view, onEngagementListener);
        downloadStateRenderer.show(metadata.offlineState(), view);
    }

    private void configureLikeButton(View view, PlaylistDetailsMetadata item, PlaylistDetailsInputs onEngagementListener) {
        ToggleButton likeToggle = ButterKnife.findById(view, R.id.toggle_like);
        likeToggle.setOnClickListener(v -> onEngagementListener.onToggleLike(likeToggle.isChecked()));

        updateToggleButton(likeToggle,
                           R.string.accessibility_like_action,
                           R.plurals.accessibility_stats_likes,
                           Optional.of(item.likesCount()),
                           item.isLikedByUser(),
                           R.string.accessibility_stats_user_liked);
    }

    private void configureOverflow(View view, PlaylistDetailsMetadata item, PlaylistDetailsInputs onEngagementListener) {
        View overflowButton = ButterKnife.findById(view, R.id.playlist_details_overflow_button);
        overflowButton.setOnClickListener(v -> {
            PopupMenuWrapper menu = popupMenuWrapperFactory.build(view.getContext(), overflowButton);
            menu.inflate(R.menu.playlist_details_actions);

            configureMyOptions(item, menu);
            configurePrivacyOptions(item, menu);
            configureShuffleOptions(item, menu);
            configurePlayNext(menu);
            configureOfflineAvailability(item, menu, onEngagementListener);
            menu.setOnMenuItemClickListener(new PopupListener(onEngagementListener));
            menu.show();
        });
    }

    private void configureDownloadToggle(PlaylistDetailsViewModel item, View view, PlaylistDetailsInputs listener) {
        final IconToggleButton downloadToggle = ButterKnife.findById(view, R.id.toggle_download);
        final PlaylistDetailsMetadata metadata = item.metadata();
        switch (metadata.offlineOptions()) {
            case AVAILABLE:
                showOfflineOptions(metadata.isMarkedForOffline(), downloadToggle, listener);
                break;
            case UPSELL:
                showUpsell(downloadToggle, listener);
                break;
            case NONE:
                hideOfflineOptions(downloadToggle);
                break;
        }
    }

    private void configureShuffleOptions(PlaylistDetailsMetadata item, PopupMenuWrapper menu) {
        if (item.canShuffle()) {
            menu.setItemEnabled(R.id.shuffle, true);
        } else {
            menu.setItemEnabled(R.id.shuffle, false);
        }
    }

    private void configureMyOptions(PlaylistDetailsMetadata item, PopupMenuWrapper menu) {
        if (item.showOwnerOptions()) {
            menu.setItemVisible(R.id.edit_playlist, featureFlags.isEnabled(Flag.EDIT_PLAYLIST_V2));
            menu.setItemVisible(R.id.delete_playlist, true);
        } else {
            menu.setItemVisible(R.id.edit_playlist, false);
            menu.setItemVisible(R.id.delete_playlist, false);
        }
    }

    private void configurePrivacyOptions(PlaylistDetailsMetadata item, PopupMenuWrapper menu) {
        if (!item.isPrivate()) {
            boolean repostedByUser = item.isRepostedByUser();
            menu.setItemVisible(R.id.share, true);
            menu.setItemVisible(R.id.repost, !repostedByUser);
            menu.setItemVisible(R.id.unpost, repostedByUser);
        } else {
            menu.setItemVisible(R.id.share, false);
            menu.setItemVisible(R.id.repost, false);
            menu.setItemVisible(R.id.unpost, false);
        }
    }

    private void showOfflineOptions(final boolean isAvailable, IconToggleButton downloadToggle, PlaylistDetailsInputs listener) {
        downloadToggle.setVisibility(View.VISIBLE);
        downloadToggle.setChecked(isAvailable);

        // do not use setOnCheckedChangeListener or all hell will break loose
        downloadToggle.setOnClickListener(v -> {
            boolean changedState = ((Checkable) v).isChecked();
            downloadToggle.setChecked(!changedState); // Ignore isChecked - button is subscribed to state changes
            if (isAvailable) {
                listener.onMakeOfflineUnavailable();
            } else {
                listener.onMakeOfflineAvailable();
            }
        });
    }

    private void configureOfflineAvailability(PlaylistDetailsMetadata item, PopupMenuWrapper menu, PlaylistDetailsInputs onEngagementListener) {
        switch (item.offlineOptions()) {
            case AVAILABLE:
                if (item.isMarkedForOffline()) {
                    showMenuRemove(menu);
                } else {
                    showMenuDownload(menu);
                }
                break;
            case UPSELL:
                showMenuUpsell(menu);
                onEngagementListener.onOverflowUpsellImpression();
                break;
            case NONE:
                hideOfflineMenuItems(menu);
                break;
        }
    }

    private void showUpsell(IconToggleButton downloadToggle, PlaylistDetailsInputs listener) {
        downloadToggle.setVisibility(View.VISIBLE);
        downloadToggle.setChecked(false);
        downloadToggle.setOnClickListener(v -> {
            listener.onMakeOfflineUpsell();
            downloadToggle.setChecked(false);
        });
    }

    private void hideOfflineOptions(IconToggleButton downloadToggle) {
        downloadToggle.setVisibility(View.GONE);
    }

    private void showMenuUpsell(PopupMenuWrapper menu ) {
        menu.setItemVisible(R.id.make_offline_available, false);
        menu.setItemVisible(R.id.make_offline_unavailable, false);
        menu.setItemVisible(R.id.upsell_offline_content, true);
    }

    private void showMenuDownload(PopupMenuWrapper menu ) {
        menu.setItemVisible(R.id.make_offline_available, true);
        menu.setItemVisible(R.id.make_offline_unavailable, false);
        menu.setItemVisible(R.id.upsell_offline_content, false);
    }

    private void showMenuRemove(PopupMenuWrapper menu ) {
        menu.setItemVisible(R.id.make_offline_available, false);
        menu.setItemVisible(R.id.make_offline_unavailable, true);
        menu.setItemVisible(R.id.upsell_offline_content, false);
    }

    private void hideOfflineMenuItems(PopupMenuWrapper menu ) {
        menu.setItemVisible(R.id.make_offline_available, false);
        menu.setItemVisible(R.id.make_offline_unavailable, false);
        menu.setItemVisible(R.id.upsell_offline_content, false);
    }

    private void configurePlayNext(PopupMenuWrapper menu ) {
        menu.setItemVisible(R.id.play_next, true);
    }

    private void setInfoText(String message, View view) {
        downloadStateRenderer.setHeaderText(message, view);
    }

    private static class PopupListener implements PopupMenuWrapper.PopupMenuWrapperListener {

        private final PlaylistDetailsInputs listener;

        private PopupListener(PlaylistDetailsInputs listener) {
            this.listener = listener;
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem, Context context) {
            switch (menuItem.getItemId()) {
                case R.id.play_next:
                    listener.onPlayNext();
                    return true;
                case R.id.repost:
                    listener.onToggleRepost(true);
                    return true;
                case R.id.unpost:
                    listener.onToggleRepost(false);
                    return true;
                case R.id.share:
                    listener.onShare();
                    return true;
                case R.id.shuffle:
                    listener.onPlayShuffled();
                    return true;
                case R.id.edit_playlist:
                    listener.onEnterEditMode();
                    return true;
                case R.id.upsell_offline_content:
                    listener.onOverflowUpsell();
                    return true;
                case R.id.make_offline_available:
                    listener.onMakeOfflineAvailable();
                    return true;
                case R.id.make_offline_unavailable:
                    listener.onMakeOfflineUnavailable();
                    return true;
                case R.id.delete_playlist:
                    listener.onDeletePlaylist();
                    return true;
                default:
                    throw new IllegalArgumentException("Unexpected menu item clicked " + menuItem);
            }
        }

        @Override
        public void onDismiss() {
            // no-op
        }
    }



    private void updateToggleButton(@Nullable ToggleButton button, int actionStringID, int descriptionPluralID,
                                    Optional<Integer> count, boolean checked, int checkedStringId) {
        if (button != null) {
            if (count.isPresent()) {
                likeButtonPresenter.setLikeCount(button, count.get(), R.drawable.ic_liked, R.drawable.ic_like);
            }
            button.setChecked(checked);

            if (AndroidUtils.accessibilityFeaturesAvailable(context)
                    && Strings.isBlank(button.getContentDescription())) {
                final StringBuilder builder = new StringBuilder();
                builder.append(resources.getString(actionStringID));

                if (count.isPresent() && count.get() >= 0) {
                    builder.append(", ");
                    builder.append(resources.getQuantityString(descriptionPluralID, count.get(), count.get()));
                }

                if (checked) {
                    builder.append(", ");
                    builder.append(resources.getString(checkedStringId));
                }

                button.setContentDescription(builder.toString());
            }
        }
    }

    @VisibleForTesting
    static class PlaylistDetailInfoProvider {

        private final OfflineSettingsOperations offlineSettings;
        private final NetworkConnectionHelper connectionHelper;
        private final Resources resources;

        @Inject
        PlaylistDetailInfoProvider(OfflineSettingsOperations offlineSettings, NetworkConnectionHelper connectionHelper, Resources resources) {
            this.offlineSettings = offlineSettings;
            this.connectionHelper = connectionHelper;
            this.resources = resources;
        }

        String getPlaylistInfoLabel(OfflineState offlineState, String defaultInfoText) {
            if (offlineState == OfflineState.REQUESTED) {
                if (offlineSettings.isWifiOnlyEnabled() && !connectionHelper.isWifiConnected()) {
                    return resources.getString(R.string.offline_no_wifi);
                } else if (!connectionHelper.isNetworkConnected()) {
                    return resources.getString(R.string.offline_no_connection);
                }
            }
            return defaultInfoText;
        }
    }

}
