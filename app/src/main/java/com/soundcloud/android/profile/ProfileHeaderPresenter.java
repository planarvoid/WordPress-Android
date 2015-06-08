package com.soundcloud.android.profile;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;

import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class ProfileHeaderPresenter implements ScrollableProfileItem.Listener {

    private final int maxHeaderHeight;
    private final ImageOperations imageOperations;

    @InjectView(R.id.toolbar_id) Toolbar toolbar;
    @InjectView(R.id.fake_toolbar_title) TextView toolbarTitle;
    @InjectView(R.id.header_info_layout) View headerInfoLayout;
    @InjectView(R.id.indicator) View tabs;

    @InjectView(R.id.username) TextView username;
    @InjectView(R.id.image) ImageView image;
    @InjectView(R.id.followers_count) TextView followerCount;
    @InjectView(R.id.follow_button) ToggleButton followButton;

    private boolean showingToolbarTitle;

    private Set<ScrollableProfileItem> scrollableFragments;
    private float showTitleToleranceY;

    public ProfileHeaderPresenter(View headerView, ImageOperations imageOperations) {
        this.imageOperations = imageOperations;

        ButterKnife.inject(this, headerView);

        maxHeaderHeight = headerView.getResources().getDimensionPixelSize(R.dimen.profile_header_expanded_height);
        scrollableFragments = new HashSet<>();

        setInitialTitleState();
    }

    private void setInitialTitleState() {
        toolbarTitle.setAlpha(0);
        toolbarTitle.setVisibility(View.VISIBLE);
        showTitleToleranceY = toolbarTitle.getResources().getDisplayMetrics().density * 5;
    }

    public void setUserDetails(ProfileUser user) {
        toolbarTitle.setText(user.getName());
        username.setText(user.getName());
        followerCount.setText(user.getFollowerCount());
        followButton.setChecked(user.isFollowed());
        imageOperations.displayInAdapterView(user.getUrn(),
                ApiImageSize.getNotificationLargeIconImageSize(image.getResources()),
                image);
    }

    @Override
    public void onVerticalScroll(int dy, int visibleSpacerHeight) {
        final boolean goingDown = dy >= 0;
        final boolean atTheTop = visibleSpacerHeight >= maxHeaderHeight + getCurrentHeaderTranslation();
        final boolean toolbarLocked = toolbar.getTranslationY() == 0;

        if (goingDown || atTheTop || !toolbarLocked) {
            final int toolbarMovement = moveToolbar(dy, visibleSpacerHeight);
            moveTabs(toolbarMovement != 0 ? toolbarMovement : dy);
            configureToolbarTitle();
        }
    }

    private void configureToolbarTitle() {
        final int stackedPosition = getMinHeaderTranslation() + toolbar.getHeight();
        toolbarTitle.setTranslationY(Math.min(0, tabs.getTranslationY() - stackedPosition));

        if (tabs.getTranslationY() - stackedPosition > showTitleToleranceY) {
            if (showingToolbarTitle) {
                showingToolbarTitle = false;
                toolbarTitle.animate().alpha(0).start();
            }
        } else {
            if (!showingToolbarTitle) {
                showingToolbarTitle = true;
                toolbarTitle.animate().alpha(1).start();
            }
        }
    }

    private float getCurrentHeaderTranslation() {
        return tabs.getTranslationY();
    }

    private int moveToolbar(int dy, int visibleHeaderHeight) {

        final boolean goingDown = dy >= 0;
        if (goingDown && visibleHeaderHeight > toolbar.getHeight() + tabs.getHeight()) {
            return 0;
        }

        final float currentTranslationY = toolbar.getTranslationY();
        final float targetTranslation = Math.max(-toolbar.getMeasuredHeight(), Math.min(0, currentTranslationY - dy));
        if (targetTranslation != currentTranslationY) {
            toolbar.setTranslationY(targetTranslation);
        }
        return (int) (currentTranslationY - targetTranslation);
    }

    public void registerScrollableFragment(ScrollableProfileItem fragment) {
        scrollableFragments.add(fragment);
        fragment.setScrollListener(this);
    }

    public void unregisterScrollableFragment(ScrollableProfileItem fragment) {
        scrollableFragments.remove(fragment);
    }

    private int moveTabs(int dy) {
        final float currentTranslationY = getCurrentHeaderTranslation();
        final float targetTranslation = Math.max(getMinHeaderTranslation(), Math.min(0, currentTranslationY - dy));

        if (targetTranslation != currentTranslationY) {
            tabs.setTranslationY(targetTranslation);
            headerInfoLayout.setTranslationY(targetTranslation);
            for (ScrollableProfileItem scrollableFragment : scrollableFragments) {
                scrollableFragment.configureOffsets((int) (maxHeaderHeight + targetTranslation), maxHeaderHeight);
            }
        }
        return (int) (currentTranslationY - targetTranslation);
    }

    private int getMinHeaderTranslation() {
        return -maxHeaderHeight + tabs.getMeasuredHeight();
    }

    public static class ProfileHeaderPresenterFactory {

        private final ImageOperations imageOperations;

        @Inject
        public ProfileHeaderPresenterFactory(ImageOperations imageOperations) {
            this.imageOperations = imageOperations;
        }

        ProfileHeaderPresenter create(View headerView) {
            return new ProfileHeaderPresenter(headerView, imageOperations);
        }
    }

}