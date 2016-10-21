package com.soundcloud.android.playlists;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadStateView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.ui.LikeButtonPresenter;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.IconToggleButton;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.Resources;
import android.view.MenuItem;
import android.view.View;
import android.widget.Checkable;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class PlaylistEngagementsView implements PopupMenuWrapper.PopupMenuWrapperListener {

    private final Context context;
    private final Resources resources;

    private final FeatureFlags featureFlags;
    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final DownloadStateView downloadStateView;
    private final LikeButtonPresenter likeButtonPresenter;

    private PopupMenuWrapper popupMenuWrapper;
    private OnEngagementListener listener;
    private PlaylistHeaderItem playlistHeaderItem;

    @Bind(R.id.toggle_like)
    ToggleButton likeToggle;
    @Bind(R.id.toggle_download)
    IconToggleButton downloadToggle;
    @Bind(R.id.playlist_details_overflow_button)
    View overflowButton;

    @Inject
    public PlaylistEngagementsView(Context context,
                                   FeatureFlags featureFlags,
                                   PopupMenuWrapper.Factory popupMenuWrapperFactory,
                                   DownloadStateView downloadStateView,
                                   LikeButtonPresenter likeButtonPresenter) {
        this.context = context;
        this.featureFlags = featureFlags;
        this.likeButtonPresenter = likeButtonPresenter;
        this.resources = context.getResources();
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.downloadStateView = downloadStateView;
    }

    @Deprecated // For the legacy screen
    public void bindView(View view) {
        bindEngagementBar(view, false);
    }

    public void bindView(View view, PlaylistHeaderItem item, boolean isEditMode) {
        this.playlistHeaderItem = item;
        final String playlistInfoText = getPlaylistInfoText(item);
        bindEngagementBar(view, isEditMode);
        setInfoText(playlistInfoText);
        bindEditBar(view, playlistInfoText, isEditMode);
    }

    private void bindEngagementBar(View view, boolean isEditMode) {
        final View bar = view.findViewById(R.id.playlist_engagement_bar);

        ButterKnife.bind(this, bar);
        bar.setVisibility(View.VISIBLE);
        popupMenuWrapper = popupMenuWrapperFactory.build(view.getContext(), overflowButton);
        popupMenuWrapper.inflate(R.menu.playlist_details_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);
        downloadStateView.onViewCreated(bar);
        bar.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        configurePlayNext();
    }

    private void bindEditBar(View view, String playlistInfoText, boolean isEditMode) {
        // No inflated in the legacy playlist screen
        final TextView editInfo = (TextView) view.findViewById(R.id.playlist_edit_bar);
        editInfo.setText(playlistInfoText);
        editInfo.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
    }

    private String getPlaylistInfoText(PlaylistHeaderItem item) {
        final String trackCount = context.getResources().getQuantityString(
                R.plurals.number_of_sounds, item.getTrackCount(),
                item.getTrackCount());

        return context.getString(R.string.playlist_new_info_header_text_trackcount_duration,
                                 trackCount, item.geFormattedDuration());
    }

    void showOfflineState(OfflineState state) {
        downloadStateView.show(state);
    }

    void showNoWifi() {
        downloadStateView.setHeaderText(resources.getString(R.string.offline_no_wifi));
    }

    void showNoConnection() {
        downloadStateView.setHeaderText(resources.getString(R.string.offline_no_connection));
    }

    @OnClick(R.id.toggle_like)
    void onToggleLikeClicked() {
        getListener().onToggleLike(likeToggle.isChecked());
    }

    @OnClick(R.id.playlist_details_overflow_button)
    void onOverflowButtonClicked() {
        popupMenuWrapper.show();
    }

    public void onDestroyView() {
        ButterKnife.unbind(this);
    }

    void showMakeAvailableOfflineButton(final boolean isAvailable) {
        downloadToggle.setVisibility(View.VISIBLE);
        setOfflineAvailability(isAvailable);

        // do not use setOnCheckedChangeListener or all hell will break loose
        downloadToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean changedState = ((Checkable) v).isChecked();
                downloadToggle.setChecked(!changedState); // Ignore isChecked - button is subscribed to state changes
                listener.onMakeOfflineAvailable(!isAvailable);
            }
        });
    }

    void setOfflineAvailability(boolean isAvailable) {
        downloadToggle.setChecked(isAvailable);
    }

    void showUpsell() {
        downloadToggle.setVisibility(View.VISIBLE);
        setOfflineAvailability(false);
        downloadToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onUpsell(v.getContext());
                setOfflineAvailability(false);
            }
        });
    }

    void hideMakeAvailableOfflineButton() {
        downloadToggle.setVisibility(View.GONE);
    }

    void showMyOptions() {
        popupMenuWrapper.setItemVisible(R.id.edit_playlist, featureFlags.isEnabled(Flag.EDIT_PLAYLIST));
        popupMenuWrapper.setItemVisible(R.id.delete_playlist, true);
    }

    void hideMyOptions() {
        popupMenuWrapper.setItemVisible(R.id.edit_playlist, false);
        popupMenuWrapper.setItemVisible(R.id.delete_playlist, false);
    }

    void showPublicOptions(boolean repostedByUser) {
        popupMenuWrapper.setItemVisible(R.id.share, true);
        popupMenuWrapper.setItemVisible(R.id.repost, !repostedByUser);
        popupMenuWrapper.setItemVisible(R.id.unpost, repostedByUser);
    }

    void hidePublicOptions() {
        popupMenuWrapper.setItemVisible(R.id.share, false);
        popupMenuWrapper.setItemVisible(R.id.repost, false);
        popupMenuWrapper.setItemVisible(R.id.unpost, false);
    }

    void configurePlayNext() {
        popupMenuWrapper.setItemVisible(R.id.play_next, featureFlags.isEnabled(Flag.PLAY_QUEUE));
    }

    public void updateLikeItem(int likesCount, boolean likedByUser) {
        updateToggleButton(likeToggle,
                           R.string.accessibility_like_action,
                           R.plurals.accessibility_stats_likes,
                           likesCount,
                           likedByUser,
                           R.string.accessibility_stats_user_liked);
    }

    void enableShuffle() {
        popupMenuWrapper.setItemEnabled(R.id.shuffle, true);
    }

    void disableShuffle() {
        popupMenuWrapper.setItemEnabled(R.id.shuffle, false);
    }

    void setInfoText(String message) {
        downloadStateView.setHeaderText(message);
    }

    @Override
    public void onDismiss() {
        // no-op
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.play_next:
                getListener().onPlayNext(playlistHeaderItem.getUrn());
                return true;
            case R.id.repost:
                getListener().onToggleRepost(true, true);
                return true;
            case R.id.unpost:
                getListener().onToggleRepost(false, true);
                return true;
            case R.id.share:
                getListener().onShare();
                return true;
            case R.id.shuffle:
                getListener().onPlayShuffled();
                return true;
            case R.id.edit_playlist:
                getListener().onEditPlaylist();
                return true;
            case R.id.delete_playlist:
                getListener().onDeletePlaylist();
                return true;
            default:
                throw new IllegalArgumentException("Unexpected menu item clicked " + menuItem);
        }
    }

    void setOnEngagementListener(OnEngagementListener listener) {
        this.listener = listener;
    }

    private OnEngagementListener getListener() {
        return listener;
    }

    private void updateToggleButton(@Nullable ToggleButton button, int actionStringID, int descriptionPluralID,
                                    int count, boolean checked, int checkedStringId) {
        if (button != null) {
            likeButtonPresenter.setLikeCount(button, count, R.drawable.ic_liked, R.drawable.ic_like);
            button.setChecked(checked);

            if (AndroidUtils.accessibilityFeaturesAvailable(context)
                    && Strings.isBlank(button.getContentDescription())) {
                final StringBuilder builder = new StringBuilder();
                builder.append(resources.getString(actionStringID));

                if (count >= 0) {
                    builder.append(", ");
                    builder.append(resources.getQuantityString(descriptionPluralID, count, count));
                }

                if (checked) {
                    builder.append(", ");
                    builder.append(resources.getString(checkedStringId));
                }

                button.setContentDescription(builder.toString());
            }
        }
    }

    public interface OnEngagementListener {

        void onPlayNext(Urn playlistUrn);

        void onToggleLike(boolean isLiked);

        void onToggleRepost(boolean isReposted, boolean showResultToast);

        void onShare();

        void onMakeOfflineAvailable(boolean isMarkedForOffline);

        void onUpsell(Context context);

        void onPlayShuffled();

        void onDeletePlaylist();

        void onEditPlaylist();
    }
}
