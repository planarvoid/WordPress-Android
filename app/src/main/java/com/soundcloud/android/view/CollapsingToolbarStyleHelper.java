package com.soundcloud.android.view;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.view.status.StatusBarColorController;
import com.soundcloud.java.collections.Pair;

import android.support.design.widget.AppBarLayout;
import android.view.View;

@AutoFactory
public class CollapsingToolbarStyleHelper implements AppBarLayout.OnOffsetChangedListener {

    private StatusBarColorController statusBarColorController;
    private final CustomFontTitleToolbar toolbar;
    private final View scrim;
    private final View topGradient;
    private final PositionProvider positionProvider;

    private boolean lightStatus;

    public interface PositionProvider {

        int getStatusBarHeight();

        int changeStatusPosition();

        int changeToolbarStylePosition();

        Pair<Float,Float> scrimAnimateBounds();

        Pair<Float,Float> toolbarAnimateBounds();

        Pair<Float,Float> toolbarGradientAnimateBounds();
    }

    public CollapsingToolbarStyleHelper(@Provided StatusBarColorController statusBarColorController,
                                        CustomFontTitleToolbar toolbar,
                                        View scrim,
                                        View topGradient,
                                        PositionProvider positionProvider) {
        this.statusBarColorController = statusBarColorController;
        this.toolbar = toolbar;
        this.scrim = scrim;
        this.topGradient = topGradient;
        this.positionProvider = positionProvider;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        final float fullRange = scrim.getHeight() - toolbar.getHeight() - positionProvider.getStatusBarHeight();
        scrim.setAlpha(getCurrentAlpha(verticalOffset, fullRange, positionProvider.scrimAnimateBounds()));
        toolbar.setTitleAlpha(getCurrentAlpha(verticalOffset, fullRange, positionProvider.toolbarAnimateBounds()));
        topGradient.setAlpha(getCurrentAlpha(verticalOffset, fullRange, positionProvider.toolbarGradientAnimateBounds()));
        setStatusBarColor(verticalOffset);
        setToolBarColor(toolbar, verticalOffset, getToolbarStyleChangePosition(appBarLayout));
    }

    private int getToolbarStyleChangePosition(AppBarLayout appBarLayout) {
        return positionProvider.changeToolbarStylePosition() != 0 ? positionProvider.changeToolbarStylePosition() :
               // default if not set
               animateToolbarBasedOnTitle(appBarLayout);
    }

    private int animateToolbarBasedOnTitle(AppBarLayout appBarLayout) {
        return -(int) (appBarLayout.getTotalScrollRange() - appBarLayout.getTotalScrollRange() * positionProvider.toolbarAnimateBounds().second());
    }

    private void setStatusBarColor(int verticalOffset) {
        if (lightStatus && verticalOffset > positionProvider.changeStatusPosition()) {
            lightStatus = false;
            statusBarColorController.clearLightStatusBar();
        } else if (!lightStatus && verticalOffset < positionProvider.changeStatusPosition()) {
            lightStatus = true;
            statusBarColorController.setLightStatusBar();
        }
    }

    private void setToolBarColor(CustomFontTitleToolbar toolbar, int verticalOffset, double changeArrowPosition) {

        if (verticalOffset > changeArrowPosition) {
            toolbar.setDarkMode();
        } else if (verticalOffset < changeArrowPosition) {
            toolbar.setLightMode();
        }
    }

    private float getCurrentAlpha(int verticalOffset, float fullRange, Pair<Float,Float> bounds) {
        final float currentPosition = (fullRange + verticalOffset);
        final float startPosition = (bounds.first() * fullRange);
        final float range = (bounds.second() - bounds.first()) * fullRange;
        final float endPosition = startPosition + range;
        final float adjustedPosition = bounds.second() > bounds.first() ?
                                       Math.min(endPosition, Math.max(currentPosition, startPosition)) :
                                       Math.max(endPosition, Math.min(currentPosition, startPosition));
        return 1 - Math.abs(adjustedPosition - startPosition) / Math.abs(range);

    }
}
