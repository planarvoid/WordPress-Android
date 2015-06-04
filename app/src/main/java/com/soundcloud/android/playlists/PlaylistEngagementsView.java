package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.offline.DownloadableHeaderView;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class PlaylistEngagementsView implements PopupMenuWrapper.PopupMenuWrapperListener {

    private final Context context;
    private final Resources resources;

    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final DownloadableHeaderView downloadableHeaderView;

    private PopupMenuWrapper popupMenuWrapper;
    private OnEngagementListener listener;

    @InjectView(R.id.toggle_like) ToggleButton likeToggle;
    @InjectView(R.id.playlist_details_overflow_button) View overflowButton;

    @Inject
    public PlaylistEngagementsView(Context context, Resources resources,
                                   PopupMenuWrapper.Factory popupMenuWrapperFactory, DownloadableHeaderView downloadableHeaderView) {
        this.context = context;
        this.resources = resources;
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.downloadableHeaderView = downloadableHeaderView;
    }

    public void onViewCreated(View view) {
        final ViewGroup holder = (ViewGroup) view.findViewById(R.id.playlist_action_bar_holder);
        View engagementsView = View.inflate(view.getContext(), R.layout.new_playlist_action_bar, holder);
        ButterKnife.inject(this, engagementsView);

        popupMenuWrapper = popupMenuWrapperFactory.build(view.getContext(), overflowButton);
        popupMenuWrapper.inflate(R.menu.playlist_details_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);

        downloadableHeaderView.onViewCreated(engagementsView);
    }

    void show(DownloadState state) {
        downloadableHeaderView.show(state);
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
        ButterKnife.reset(this);
    }

    void setOfflineOptionsMenu(boolean isAvailable) {
        popupMenuWrapper.setItemVisible(R.id.make_offline_available, !isAvailable);
        popupMenuWrapper.setItemVisible(R.id.make_offline_unavailable, isAvailable);
        popupMenuWrapper.setItemVisible(R.id.upsell_offline_content, false);
    }

    void showUpsell() {
        popupMenuWrapper.setItemVisible(R.id.upsell_offline_content, true);
        popupMenuWrapper.setItemVisible(R.id.make_offline_available, false);
        popupMenuWrapper.setItemVisible(R.id.make_offline_unavailable, false);
    }

    void hideOfflineContentOptions() {
        popupMenuWrapper.setItemVisible(R.id.make_offline_available, false);
        popupMenuWrapper.setItemVisible(R.id.make_offline_unavailable, false);
        popupMenuWrapper.setItemVisible(R.id.upsell_offline_content, false);
    }

    void showPublicOptionsForYourTrack() {
        popupMenuWrapper.setItemVisible(R.id.share, true);
        popupMenuWrapper.setItemVisible(R.id.repost, false);
        popupMenuWrapper.setItemVisible(R.id.unpost, false);
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
        downloadableHeaderView.setHeaderText(message);
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
            case R.id.make_offline_available:
                getListener().onMakeOfflineAvailable(true);
                return true;
            case R.id.make_offline_unavailable:
                getListener().onMakeOfflineAvailable(false);
                return true;
            case R.id.upsell_offline_content:
                getListener().onUpsell(context);
                return true;
            case R.id.shuffle:
                getListener().onPlayShuffled();
                return true;
            default:
                throw new IllegalArgumentException("Unexpected menu item clicked " + menuItem);
        }
    }

    void setOnEngagement(OnEngagementListener listener){
        this.listener = listener;
    }

    private OnEngagementListener getListener() {
        return listener;
    }

    private void updateToggleButton(@Nullable ToggleButton button, int actionStringID, int descriptionPluralID, int count, boolean checked,
                                      int checkedStringId) {
        final String buttonLabel = ScTextUtils.shortenLargeNumber(count);
        button.setTextOn(buttonLabel);
        button.setTextOff(buttonLabel);
        button.setChecked(checked);
        button.invalidate();

        if (AndroidUtils.accessibilityFeaturesAvailable(context)
                && TextUtils.isEmpty(button.getContentDescription())) {
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

    public interface OnEngagementListener {
        void onToggleLike(boolean isLiked);
        void onToggleRepost(boolean isReposted, boolean showResultToast);
        void onShare();
        void onMakeOfflineAvailable(boolean isMarkedForOffline);
        void onUpsell(Context context);
        void onPlayShuffled();
    }
}
