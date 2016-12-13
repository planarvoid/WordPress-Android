package com.soundcloud.android.profile;

import static android.os.Build.VERSION_CODES.M;
import static com.soundcloud.android.view.status.StatusBarUtils.getStatusBarHeight;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.CustomFontTitleToolbar;
import com.soundcloud.android.view.status.StatusBarColorController;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;

import javax.inject.Inject;

@TargetApi(M)
class BannerProfileScrollHelper
        extends ProfileScrollHelper {

    private final StatusBarColorController statusBarColorController;
    @Nullable @Bind(R.id.top_gradient) View topGradient;
    @Nullable @Bind(R.id.header_scrim) View scrim;

    private int statusBarHeight;
    private int changeArrowPosition;
    private int changeStatusPosition;
    private boolean lightStatus;

    @Inject
    BannerProfileScrollHelper(StatusBarColorController statusBarColorController) {
        this.statusBarColorController = statusBarColorController;
    }

    @Override
    public void onCreate(final AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        ButterKnife.bind(this, activity);
        setupCollapsingToolbar(activity);
    }

    @Override
    public void onResume(final AppCompatActivity activity) {
        toolbar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                toolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                doMeasurements(activity);
            }
        });
    }

    private void doMeasurements(AppCompatActivity activity) {
        View banner = activity.findViewById(R.id.profile_banner);
        if (banner != null) {
            int bannerHeight = banner.getHeight();
            statusBarHeight = getStatusBarHeight(activity);
            changeStatusPosition = -bannerHeight + statusBarHeight / 2;
            changeArrowPosition = -bannerHeight + statusBarHeight + toolbar.getHeight() / 2;
        }
    }

    private void setupCollapsingToolbar(AppCompatActivity activity) {
        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.collapsing_toolbar);
        if (collapsingToolbarLayout != null) {
            toolbar.setDarkMode();
            listenForOffsetChanges();
        }
    }

    private void listenForOffsetChanges() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            final float fullRange = scrim.getHeight() - BannerProfileScrollHelper.this.toolbar.getHeight() - statusBarHeight;
            scrim.setAlpha(getCurrentAlpha(verticalOffset, fullRange, .2f, 1f));
            toolbar.setTitleAlpha(getCurrentAlpha(verticalOffset, fullRange, .0f, .3f));
            float currentAlpha = getCurrentAlpha(verticalOffset, fullRange, 1f, .7f);
            topGradient.setAlpha(currentAlpha);
            setStatusBarColor(verticalOffset);
            setToolBarColor(toolbar, verticalOffset);
        });
    }

    private void setStatusBarColor(int verticalOffset) {
        if (lightStatus && verticalOffset > changeStatusPosition) {
            lightStatus = false;
            statusBarColorController.clearLightStatusBar();
        } else if (!lightStatus && verticalOffset < changeStatusPosition) {
            lightStatus = true;
            statusBarColorController.setLightStatusBar();
        }
    }

    private void setToolBarColor(CustomFontTitleToolbar toolbar, int verticalOffset) {
        if (verticalOffset > changeArrowPosition) {
            toolbar.setDarkMode();
        } else if (verticalOffset < changeArrowPosition) {
            toolbar.setLightMode();
        }
    }

    private float getCurrentAlpha(int verticalOffset, float fullRange, float start, float end) {
        final float currentPosition = (fullRange + verticalOffset);
        final float startPosition = (start * fullRange);
        final float range = (end - start) * fullRange;
        final float endPosition = startPosition + range;
        final float adjustedPosition = end > start ?
                           Math.min(endPosition, Math.max(currentPosition, startPosition)) :
                           Math.max(endPosition, Math.min(currentPosition, startPosition));
        return 1 - Math.abs(adjustedPosition - startPosition) / Math.abs(range);

    }
}
