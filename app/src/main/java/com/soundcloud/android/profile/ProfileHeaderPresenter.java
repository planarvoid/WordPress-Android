package com.soundcloud.android.profile;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;

import android.content.res.Resources;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class ProfileHeaderPresenter implements ScrollableProfileItem.Listener {

    private static final String CREATOR_NAME = "Artist Name";

    private final int maxHeaderHeight;

    // Header views
    @InjectView(R.id.toolbar_id) Toolbar toolbar;
    @InjectView(R.id.fake_toolbar_title) TextView toolbarTitle;
    @InjectView(R.id.username) TextView username;
    @InjectView(R.id.header_info_layout) View headerInfoLayout;
    @InjectView(R.id.sliding_tabs) View tabs;

    private boolean showingToolbarTitle;

    private Set<ScrollableProfileItem> scrollableFragments;
    private float showTitleToleranceY;

    public ProfileHeaderPresenter(View headerView, ImageOperations imageOperations) {
        ButterKnife.inject(this, headerView);

        final Resources resources = headerView.getResources();
        maxHeaderHeight = resources.getDimensionPixelSize(R.dimen.profile_header_expanded_height);
        scrollableFragments = new HashSet<>();

        imageOperations.displayInAdapterView(Urn.forUser(172720L),
                ApiImageSize.getNotificationLargeIconImageSize(resources),
                (ImageView) headerView.findViewById(R.id.image));

        toolbarTitle.setAlpha(0);
        toolbarTitle.setVisibility(View.VISIBLE);
        toolbarTitle.setText(CREATOR_NAME);

        username.setText(CREATOR_NAME);
        ((TextView) headerView.findViewById(R.id.followers_count)).setText("97,362");

        showTitleToleranceY = resources.getDisplayMetrics().density * 5;
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

        if (tabs.getTranslationY() - stackedPosition > showTitleToleranceY ){
            if (showingToolbarTitle){
                showingToolbarTitle = false;
                toolbarTitle.animate().alpha(0).start();
            }
        } else {
            if (!showingToolbarTitle){
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
        if (targetTranslation != currentTranslationY){
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

        ProfileHeaderPresenter create(View headerView){
            return new ProfileHeaderPresenter(headerView, imageOperations);
        }
    }

}