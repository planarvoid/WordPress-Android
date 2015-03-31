package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadableHeaderView;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Context;
import android.content.res.Resources;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

public class NewPlaylistEngagementsView extends PlaylistEngagementsView implements PopupMenuWrapper.PopupMenuWrapperListener {

    @InjectView(R.id.toggle_like) ToggleButton likeToggle;
    @InjectView(R.id.playlist_details_overflow_button) View overflowButton;

    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final DownloadableHeaderView downloadableHeaderView;
    private PopupMenuWrapper popupMenuWrapper;

    public NewPlaylistEngagementsView(Context context, Resources resources,
                                      PopupMenuWrapper.Factory popupMenuWrapperFactory, DownloadableHeaderView downloadableHeaderView) {
        super(context, resources);
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.downloadableHeaderView = downloadableHeaderView;
    }

    @Override
    public void onViewCreated(View view) {
        final ViewGroup holder = (ViewGroup) view.findViewById(R.id.playlist_action_bar_holder);
        View engagementsView = View.inflate(view.getContext(), R.layout.new_playlist_action_bar, holder);
        ButterKnife.inject(this, engagementsView);

        popupMenuWrapper = popupMenuWrapperFactory.build(view.getContext(), overflowButton);
        popupMenuWrapper.inflate(R.menu.playlist_details_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);

        downloadableHeaderView.onViewCreated(engagementsView);
    }

    @OnClick(R.id.toggle_like)
    void onToggleLikeClicked() {
        getListener().onToggleLike(likeToggle.isChecked());
    }

    @OnClick(R.id.playlist_details_overflow_button)
    void onOverflowButtonClicked() {
        popupMenuWrapper.show();
    }

    @Override
    public void onDestroyView() {
        ButterKnife.reset(this);
    }

    @Override
    void setOfflineOptionsMenu(boolean isAvailable) {
        popupMenuWrapper.setItemVisible(R.id.make_offline_available, !isAvailable);
        popupMenuWrapper.setItemVisible(R.id.make_offline_unavailable, isAvailable);
        popupMenuWrapper.setItemVisible(R.id.upsell_offline_content, false);
    }

    void showDefaultState() {
        downloadableHeaderView.showNoOfflineState();
    }

    void showDownloadingState() {
        downloadableHeaderView.showDownloadingState();
    }

    void showDownloadedState() {
        downloadableHeaderView.showDownloadedState();
    }

    @Override
    void showUpsell() {
        popupMenuWrapper.setItemVisible(R.id.upsell_offline_content, true);
        popupMenuWrapper.setItemVisible(R.id.make_offline_available, false);
        popupMenuWrapper.setItemVisible(R.id.make_offline_unavailable, false);
    }

    @Override
    void hideOfflineContentOptions() {
        popupMenuWrapper.setItemVisible(R.id.make_offline_available, false);
        popupMenuWrapper.setItemVisible(R.id.make_offline_unavailable, false);
        popupMenuWrapper.setItemVisible(R.id.upsell_offline_content, false);
    }

    @Override
    void showPublicOptionsForYourTrack() {
        popupMenuWrapper.setItemVisible(R.id.share, true);
        popupMenuWrapper.setItemVisible(R.id.repost, false);
        popupMenuWrapper.setItemVisible(R.id.unpost, false);
    }

    @Override
    void showPublicOptions(boolean repostedByUser) {
        popupMenuWrapper.setItemVisible(R.id.share, true);
        popupMenuWrapper.setItemVisible(R.id.repost, !repostedByUser);
        popupMenuWrapper.setItemVisible(R.id.unpost, repostedByUser);
    }

    @Override
    void hidePublicOptions() {
        popupMenuWrapper.setItemVisible(R.id.share, false);
        popupMenuWrapper.setItemVisible(R.id.repost, false);
        popupMenuWrapper.setItemVisible(R.id.unpost, false);
    }

    @Override
    public void updateLikeItem(int likesCount, boolean likedByUser) {
        updateToggleButton(likeToggle,
                R.string.accessibility_like_action,
                R.plurals.accessibility_stats_likes,
                likesCount,
                likedByUser,
                R.string.accessibility_stats_user_liked);
    }

    @Override
    void enableShuffle() {
        popupMenuWrapper.setItemEnabled(R.id.shuffle, true);
    }

    @Override
    void disableShuffle() {
        popupMenuWrapper.setItemEnabled(R.id.shuffle, false);
    }

    @Override
    void setInfoText(String message) {
        downloadableHeaderView.setHeaderText(message);
    }

    @Override
    public void onDismiss() {
        // no-op
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
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
                getListener().onUpsell();
                return true;
            case R.id.shuffle:
                getListener().onPlayShuffled();
                return true;
            default:
                throw new IllegalArgumentException("Unexpected menu item clicked " + menuItem);
        }
    }
}
