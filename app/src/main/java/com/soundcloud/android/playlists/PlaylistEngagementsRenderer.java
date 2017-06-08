package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.ui.LikeButtonPresenter;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.view.OfflineStateButton;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.content.res.Resources;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

class PlaylistEngagementsRenderer {

    private final Context context;
    private final Resources resources;
    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final LikeButtonPresenter likeButtonPresenter;
    private final AccountOperations accountOperations;
    private final IntroductoryOverlayPresenter introductoryOverlayPresenter;
    private final OfflineSettingsOperations offlineSettings;
    private final ConnectionHelper connectionHelper;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Inject
    PlaylistEngagementsRenderer(Context context,
                                PopupMenuWrapper.Factory popupMenuWrapperFactory,
                                LikeButtonPresenter likeButtonPresenter,
                                AccountOperations accountOperations,
                                IntroductoryOverlayPresenter introductoryOverlayPresenter,
                                OfflineSettingsOperations offlineSettings,
                                ConnectionHelper connectionHelper,
                                ChangeLikeToSaveExperiment changeLikeToSaveExperiment,
                                ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        this.context = context;
        this.likeButtonPresenter = likeButtonPresenter;
        this.resources = context.getResources();
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.accountOperations = accountOperations;
        this.introductoryOverlayPresenter = introductoryOverlayPresenter;
        this.offlineSettings = offlineSettings;
        this.connectionHelper = connectionHelper;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
    }

    void bind(View view, PlaylistDetailsInputs onEngagementListener, PlaylistDetailsMetadata metadata) {
        view.findViewById(R.id.playlist_engagement_bar).setVisibility(View.VISIBLE); // it is not visible by default
        bindEngagementBar(view, onEngagementListener, metadata);
        setInfoText(view, metadata.headerText());
    }

    private void setInfoText(View view, String text) {
        TextView infoText = ButterKnife.findById(view, R.id.header_text);
        infoText.setText(text);
    }

    private void bindEngagementBar(View view, PlaylistDetailsInputs onEngagementListener, PlaylistDetailsMetadata metadata) {
        configureLikeButton(view, metadata, onEngagementListener);
        configureOverflow(view, metadata, onEngagementListener);
        configureDownloadButton(view, onEngagementListener, metadata);
    }

    private void configureLikeButton(View view, PlaylistDetailsMetadata item, PlaylistDetailsInputs onEngagementListener) {
        ToggleButton likeToggle = ButterKnife.findById(view, R.id.toggle_like);
        likeToggle.setOnClickListener(v -> onEngagementListener.onToggleLike(likeToggle.isChecked()));
        updateLikeToggleButton(likeToggle,
                               changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.ACCESSIBILITY_LIKE_ACTION),
                               changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.ACCESSIBILITY_STATS_LIKES),
                               Optional.of(item.likesCount()),
                               item.isLikedByUser(),
                               changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.ACCESSIBILITY_STATS_USER_LIKED),
                               changeLikeToSaveExperiment.isEnabled());

        if (changeLikeToSaveExperiment.isTooltipEnabled() && !accountOperations.isLoggedInUser(item.creatorUrn())) {
            introductoryOverlayPresenter.showIfNeeded(IntroductoryOverlayKey.ADD_TO_COLLECTION,
                                                      likeToggle, changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.LIKE_YOUR_FAVORITE_TRACKS_TITLE),
                                                      changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.LIKE_YOUR_FAVORITE_TRACKS_DESCRIPTION));
        }
    }

    private void configureOverflow(View view, PlaylistDetailsMetadata item, PlaylistDetailsInputs onEngagementListener) {
        View overflowButton = ButterKnife.findById(view, R.id.playlist_details_overflow_button);
        overflowButton.setOnClickListener(v -> {
            PopupMenuWrapper menu = popupMenuWrapperFactory.build(view.getContext(), overflowButton);
            menu.inflate(R.menu.playlist_details_actions);

            configureMyOptions(item, menu);
            configureRepostOptions(item, menu);
            configureShareOptions(item, menu);
            configureShuffleOptions(item, menu);
            configurePlayNext(menu);
            configureOfflineAvailability(item, menu, onEngagementListener);
            menu.setOnMenuItemClickListener(new PopupListener(onEngagementListener));
            menu.show();
        });

        if (accountOperations.isLoggedInUser(item.creatorUrn())) {
            introductoryOverlayPresenter.showIfNeeded(IntroductoryOverlayKey.EDIT_PLAYLIST,
                                                      overflowButton, R.string.edit_playlists_introductory_overlay_title,
                                                      R.string.edit_playlists_introductory_overlay_description);
        }
    }

    private void configureDownloadButton(View barView, PlaylistDetailsInputs listener, PlaylistDetailsMetadata metadata) {
        switch (metadata.offlineOptions()) {
            case AVAILABLE:
                showOfflineOptions(barView, metadata, listener);
                break;
            case UPSELL:
                showUpsell(barView, listener);
                break;
            case NONE:
                hideOfflineOptions(barView);
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
        if (item.isOwner()) {
            menu.setItemVisible(R.id.edit_playlist, true);
            menu.setItemVisible(R.id.delete_playlist, true);
        } else {
            menu.setItemVisible(R.id.edit_playlist, false);
            menu.setItemVisible(R.id.delete_playlist, false);
        }
    }

    private void configureShareOptions(PlaylistDetailsMetadata item, PopupMenuWrapper menu) {
        if (item.isPrivate()) {
            menu.setItemVisible(R.id.share, false);
        } else {
            menu.setItemVisible(R.id.share, true);
        }
    }

    private void configureRepostOptions(PlaylistDetailsMetadata item, PopupMenuWrapper menu) {
        if (item.isPrivate() || item.isOwner()) {
            menu.setItemVisible(R.id.repost, false);
            menu.setItemVisible(R.id.unpost, false);
        } else {
            boolean repostedByUser = item.isRepostedByUser();
            menu.setItemVisible(R.id.repost, !repostedByUser);
            menu.setItemVisible(R.id.unpost, repostedByUser);
        }
    }

    private void showOfflineOptions(View barView, PlaylistDetailsMetadata item, PlaylistDetailsInputs listener) {
        OfflineStateButton stateButton = ButterKnife.findById(barView, R.id.offline_state_button);
        stateButton.setVisibility(View.VISIBLE);
        setOfflineButtonState(stateButton, item.offlineState());
        stateButton.setOnClickListener(v -> toggleOffline(item, listener));
    }

    private void setOfflineButtonState(OfflineStateButton stateButton, OfflineState offlineState) {
        if (OfflineState.REQUESTED == offlineState && shouldShowNoWifi()) {
            stateButton.showNoWiFi();
        } else if (OfflineState.REQUESTED == offlineState && shouldShowNoConnection()) {
            stateButton.showNoConnection();
        } else {
            stateButton.setState(offlineState);
        }
    }

    private boolean shouldShowNoWifi() {
        return offlineSettings.isWifiOnlyEnabled() && !connectionHelper.isWifiConnected();
    }

    private boolean shouldShowNoConnection() {
        return !connectionHelper.isNetworkConnected();
    }

    private void toggleOffline(PlaylistDetailsMetadata item, PlaylistDetailsInputs listener) {
        if (item.isMarkedForOffline()) {
            listener.onMakeOfflineUnavailable();
        } else {
            listener.onMakeOfflineAvailable();
        }
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

    private void showUpsell(View barView, PlaylistDetailsInputs listener) {
        OfflineStateButton stateButton = ButterKnife.findById(barView, R.id.offline_state_button);
        stateButton.setVisibility(View.VISIBLE);
        stateButton.setOnClickListener(v -> listener.onMakeOfflineUpsell());
    }

    private void hideOfflineOptions(View barView) {
        View button = ButterKnife.findById(barView, R.id.offline_state_button);
        button.setVisibility(View.GONE);
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

    private void updateLikeToggleButton(ToggleButton button, int actionStringID, int descriptionPluralID,
                                        Optional<Integer> countOptional, boolean checked, int checkedStringId, boolean selected) {
        countOptional.ifPresent(count -> {
            final int drawableLiked = selected
                                      ? R.drawable.ic_added_to_collection
                                      : R.drawable.ic_liked;
            final int drawableUnliked = selected
                                        ? R.drawable.ic_add_to_collection
                                        : R.drawable.ic_like;
            likeButtonPresenter.setLikeCount(button, count, drawableLiked, drawableUnliked);
        });
        button.setSelected(selected);
        button.setChecked(checked);
        updateLikeToggleButtonContentDescription(button, actionStringID, descriptionPluralID, countOptional, checked, checkedStringId);
    }

    private void updateLikeToggleButtonContentDescription(ToggleButton button,
                                                          int actionStringID,
                                                          int descriptionPluralID,
                                                          Optional<Integer> countOptional,
                                                          boolean checked,
                                                          int checkedStringId) {
        if (AndroidUtils.accessibilityFeaturesAvailable(context) && Strings.isBlank(button.getContentDescription())) {
            final StringBuilder builder = new StringBuilder();
            builder.append(resources.getString(actionStringID));

            if (countOptional.isPresent() && countOptional.get() >= 0) {
                builder.append(", ");
                builder.append(resources.getQuantityString(descriptionPluralID, countOptional.get(), countOptional.get()));
            }

            if (checked) {
                builder.append(", ");
                builder.append(resources.getString(checkedStringId));
            }

            button.setContentDescription(builder.toString());
        }
    }

    private static final class PopupListener implements PopupMenuWrapper.PopupMenuWrapperListener {

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
                    listener.onShareClicked();
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

}
