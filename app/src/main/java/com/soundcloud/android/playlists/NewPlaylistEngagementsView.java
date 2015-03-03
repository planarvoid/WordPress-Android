package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.ui.PopupMenuWrapperListener;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Context;
import android.content.res.Resources;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

public class NewPlaylistEngagementsView extends PlaylistEngagementsView implements PopupMenuWrapperListener {

    @InjectView(R.id.toggle_like) ToggleButton likeToggle;
    @InjectView(R.id.overflow_button) View overflowButton;
    @InjectView(R.id.playlist_info_text) TextView infoText;

    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private PopupMenuWrapper popupMenuWrapper;

    public NewPlaylistEngagementsView(Context context, Resources resources, PopupMenuWrapper.Factory popupMenuWrapperFactory) {
        super(context, resources);
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
    }

    @Override
    public void onViewCreated(View view) {
        final ViewGroup holder = (ViewGroup) view.findViewById(R.id.playlist_action_bar_holder);
        View engagementsView = View.inflate(view.getContext(), R.layout.new_playlist_action_bar, holder);
        ButterKnife.inject(this, engagementsView);

        popupMenuWrapper = popupMenuWrapperFactory.build(view.getContext(), overflowButton);
        popupMenuWrapper.inflate(R.menu.playlist_details_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);

        likeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListener().onToggleLike(likeToggle.isChecked());
            }
        });

        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenuWrapper.show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        ButterKnife.reset(this);
    }

    @Override
    public void showAndUpdateRepostItem(int repostsCount, boolean repostedByUser) {
        popupMenuWrapper.setItemVisible(R.id.repost, !repostedByUser);
        popupMenuWrapper.setItemVisible(R.id.unpost, repostedByUser);
    }

    @Override
    public void hideRepostItem() {
        popupMenuWrapper.setItemVisible(R.id.repost, false);
        popupMenuWrapper.setItemVisible(R.id.unpost, false);
    }

    @Override
    void showOfflineAvailability(boolean isAvailable) {
        popupMenuWrapper.setItemVisible(R.id.make_offline_available, !isAvailable);
        popupMenuWrapper.setItemVisible(R.id.make_offline_unavailable, isAvailable);
        popupMenuWrapper.setItemVisible(R.id.upsell_offline_content, false);
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
    public void showShareItem() {
        popupMenuWrapper.setItemVisible(R.id.share, true);
    }

    @Override
    public void hideShareItem() {
        popupMenuWrapper.setItemVisible(R.id.share, false);
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
    void setInfoText(String message) {
        infoText.setText(message);
    }

    @Override
    public void onDismiss() {
        // no-op
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.repost:
                getListener().onToggleRepost(true);
                return true;
            case R.id.unpost:
                getListener().onToggleRepost(false);
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
            default:
                throw new IllegalArgumentException("Unexpected menu item clicked " + menuItem);
        }
    }
}
