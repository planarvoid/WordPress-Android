package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.CustomFontTitleToolbar;
import com.soundcloud.android.view.status.StatusBarColorController;
import com.soundcloud.java.collections.Pair;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/***
 * This class exists purely because the scrolling in the support library is totally broken, and has been for years:
 * https://code.google.com/p/android/issues/detail?id=177729
 */
@AutoFactory
class PlaylistDetailsHeaderAnimator {

    private static final Pair<Float, Float> TOOLBAR_ANIMATE_BOUNDS = Pair.of(.1f, .3f);
    private static final Pair<Float, Float> SCRIM_ANIMATE_BOUNDS = Pair.of(.2f, .5f);
    private static final Pair<Float, Float> TOOLBAR_GRADIENT_ANIMATE_BOUNDS = Pair.of(1f, .4f);
    private static final int ANIMATION_DURATION = 300;
    public static final int SEVEN_PERCENT_OF_255 = 17;

    private final StatusBarColorController statusBarColorController;
    private final CustomFontTitleToolbar toolbar;
    private final View topGradient;
    private final View systemBarsHolder;
    private final int elevationTarget;

    private boolean isInEditMode;
    private boolean configuredBottomPadding;
    private boolean opaqueSystemBars;

    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            configureFromScroll(recyclerView);
        }
    };

    private RecyclerView.AdapterDataObserver adapterDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            configuredBottomPadding = false;
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            configuredBottomPadding = false;
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            configuredBottomPadding = false;
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            configuredBottomPadding = false;
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            configuredBottomPadding = false;
        }
    };

    PlaylistDetailsHeaderAnimator(CustomFontTitleToolbar toolbar,
                                  View topGradient,
                                  View systemBarsHolder,
                                  @Provided StatusBarColorController statusBarColorController,
                                  @Provided Resources resources) {
        this.statusBarColorController = statusBarColorController;
        this.toolbar = toolbar;
        this.topGradient = topGradient;
        this.systemBarsHolder = systemBarsHolder;
        this.elevationTarget = resources.getDimensionPixelSize(R.dimen.toolbar_elevation);
    }

    public void attach(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        recyclerView.addOnScrollListener(scrollListener);
        adapter.registerAdapterDataObserver(adapterDataObserver);
    }

    public void detatch(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        recyclerView.removeOnScrollListener(scrollListener);
        adapter.unregisterAdapterDataObserver(adapterDataObserver);
    }

    void setIsInEditMode(boolean value, RecyclerView recyclerView) {
        if (value != isInEditMode) {
            if (value) {
                enterEditMode(recyclerView);
            } else {
                exitEditMode(recyclerView);
            }
        }
        isInEditMode = value;
    }

    private void enterEditMode(RecyclerView recyclerView) {
        toolbar.setTitleAlpha(1);
        topGradient.setAlpha(0);
        statusBarColorController.setStatusBarColor(ColorUtils.setAlphaComponent(Color.BLACK, SEVEN_PERCENT_OF_255));
        configureSystemBars(true, true);
        setToolbarStyle(false);
        recyclerView.setPadding(0, toolbar.getBottom(), 0, recyclerView.getPaddingBottom());
    }

    private void exitEditMode(RecyclerView recyclerView) {
        recyclerView.post(() -> {
            recyclerView.smoothScrollToPosition(0);
            recyclerView.setPadding(0, 0, 0, recyclerView.getPaddingBottom());
        });

    }

    private void configureFromScroll(RecyclerView recyclerView) {
        if (!isInEditMode) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (isShowingHeader(layoutManager)) {
                View headerView = recyclerView.findViewHolderForAdapterPosition(0).itemView;
                View scrim = headerView.findViewById(R.id.scrim);
                if (scrim != null) {

                    int fullRange = scrim.getHeight();
                    int verticalOffset = headerView.getTop();

                    // configure all of our views based on the scroll position
                    float rangeBasedAlpha = ViewUtils.getRangeBasedAlpha(verticalOffset, fullRange, SCRIM_ANIMATE_BOUNDS);
                    scrim.setAlpha(rangeBasedAlpha);
                    statusBarColorController.setStatusBarColor(ColorUtils.setAlphaComponent(Color.BLACK, (int) (SEVEN_PERCENT_OF_255 * rangeBasedAlpha)));

                    toolbar.setTitleAlpha(ViewUtils.getRangeBasedAlpha(verticalOffset, fullRange, TOOLBAR_ANIMATE_BOUNDS));
                    topGradient.setAlpha(ViewUtils.getRangeBasedAlpha(verticalOffset, fullRange, TOOLBAR_GRADIENT_ANIMATE_BOUNDS));
                    configureSystemBars(Math.abs(verticalOffset) > scrim.getHeight() - toolbar.getBottom(), false);
                    setToolbarStyle(verticalOffset > -(int) (fullRange - fullRange * TOOLBAR_ANIMATE_BOUNDS.second()));

                    if (!configuredBottomPadding && lastItemIsOnScreen(recyclerView, layoutManager)) {
                        configreExtraPaddingForFullScrolling(recyclerView, layoutManager, verticalOffset);
                        configuredBottomPadding = true;
                    }

                }
            } else if (!configuredBottomPadding) {
                // if we aren't looking at the first item, then clearly there are enough items, so do not add padding
                recyclerView.setPadding(0, recyclerView.getPaddingTop(), 0, 0);
            }
        }
    }

    private boolean isShowingHeader(LinearLayoutManager layoutManager) {
        return layoutManager.findFirstVisibleItemPosition() == 0;
    }

    private void setToolbarStyle(boolean darkMode) {
        if (darkMode) {
            toolbar.setDarkMode();
        } else {
            toolbar.setLightMode();
        }
    }

    private void configureSystemBars(boolean shouldBeOpaque, boolean animateBackground) {
        if (shouldBeOpaque && !opaqueSystemBars) {
            opaqueSystemBars = true;
            statusBarColorController.setLightStatusBar();
            animateSystemBarElevation(0, elevationTarget);

            if (animateBackground) {
                animateSystemBarBackground(Color.TRANSPARENT, Color.WHITE);
            } else {
                systemBarsHolder.setBackgroundColor(Color.WHITE);
            }

        } else if (!shouldBeOpaque && opaqueSystemBars) {
            opaqueSystemBars = false;
            statusBarColorController.clearLightStatusBar();
            animateSystemBarElevation(elevationTarget, 0);

            if (animateBackground) {
                animateSystemBarBackground(Color.WHITE, Color.TRANSPARENT);
            } else {
                systemBarsHolder.setBackgroundColor(Color.TRANSPARENT);
            }

        }
    }

    private void animateSystemBarElevation(int from, int to) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(systemBarsHolder, "Elevation", from, to);
        anim.setDuration(ANIMATION_DURATION);
        anim.start();
    }

    private void animateSystemBarBackground(int from, int to) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        colorAnimation.setDuration(ANIMATION_DURATION);
        colorAnimation.addUpdateListener(animator -> systemBarsHolder.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }

    private void configreExtraPaddingForFullScrolling(RecyclerView recyclerView, LinearLayoutManager layoutManager, int verticalOffset) {
        /**
         * Because we always want to be able to collapse the header.
         * This method adds extra padding to the list view so we can move the header totally out of view.
         */

        View lastItemView = recyclerView.findViewHolderForAdapterPosition(layoutManager.findLastVisibleItemPosition()).itemView;
        View engagementView = recyclerView.findViewById(R.id.playlist_engagement_bar);

        int tilCollapsed = (engagementView.getBottom() + verticalOffset) - toolbar.getBottom();
        int naturalEnd = lastItemView.getBottom() - recyclerView.getHeight();
        int extraSpaceNeeded = tilCollapsed - naturalEnd;
        recyclerView.setPadding(0, recyclerView.getPaddingTop(), 0, Math.max(0, extraSpaceNeeded));
    }

    private boolean lastItemIsOnScreen(RecyclerView recyclerView, LinearLayoutManager layoutManager) {
        return layoutManager.findLastVisibleItemPosition() == recyclerView.getAdapter().getItemCount() - 1;
    }

}
