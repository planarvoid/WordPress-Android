package com.soundcloud.android.playlists;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadStateView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.IconToggleButton;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.strings.Strings;
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
    private final CondensedNumberFormatter numberFormatter;
    private final Resources resources;

    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final DownloadStateView downloadStateView;

    private PopupMenuWrapper popupMenuWrapper;
    private OnEngagementListener listener;

    @Bind(R.id.toggle_like) ToggleButton likeToggle;
    @Bind(R.id.toggle_download) IconToggleButton downloadToggle;
    @Bind(R.id.playlist_details_overflow_button) View overflowButton;

    @Inject
    public PlaylistEngagementsView(Context context, CondensedNumberFormatter numberFormatter,
                                   PopupMenuWrapper.Factory popupMenuWrapperFactory,
                                   DownloadStateView downloadStateView) {
        this.context = context;
        this.numberFormatter = numberFormatter;
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
        showDeletePlaylist();
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
        showDeletePlaylist();
    }

    private void showDeletePlaylist() {
        popupMenuWrapper.setItemVisible(R.id.delete_playlist, true);
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

    private void updateToggleButton(@Nullable ToggleButton button, int actionStringID, int descriptionPluralID, int count, boolean checked,
                                    int checkedStringId) {
        final String buttonLabel = count < 0 ? Strings.EMPTY : numberFormatter.format(count);
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

        void onDeletePlaylist();
    }
}
