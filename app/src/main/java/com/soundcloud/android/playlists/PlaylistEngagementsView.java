package com.soundcloud.android.playlists;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
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
import android.view.ViewGroup;
import android.widget.Checkable;
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

    @Bind(R.id.toggle_like) ToggleButton likeToggle;
    @Bind(R.id.toggle_download) IconToggleButton downloadToggle;
    @Bind(R.id.playlist_details_overflow_button) View overflowButton;

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

    public void onViewCreated(View view) {
        final ViewGroup holder = (ViewGroup) view.findViewById(R.id.playlist_action_bar_holder);
        View engagementsView = View.inflate(view.getContext(), R.layout.playlist_engagement_bar, holder);
        ButterKnife.bind(this, engagementsView);

        popupMenuWrapper = popupMenuWrapperFactory.build(view.getContext(), overflowButton);
        popupMenuWrapper.inflate(R.menu.playlist_details_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);

        downloadStateView.onViewCreated(engagementsView);
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

    void showPublicOptionsForYourTrack() {
        popupMenuWrapper.setItemVisible(R.id.share, true);
        popupMenuWrapper.setItemVisible(R.id.repost, false);
        popupMenuWrapper.setItemVisible(R.id.unpost, false);
        popupMenuWrapper.setItemVisible(R.id.edit_playlist, featureFlags.isEnabled(Flag.EDIT_PLAYLIST));
        popupMenuWrapper.setItemVisible(R.id.delete_playlist, true);
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
        popupMenuWrapper.setItemVisible(R.id.edit_playlist, false);
        popupMenuWrapper.setItemVisible(R.id.delete_playlist, false);
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

    void setOnEngagement(OnEngagementListener listener) {
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
