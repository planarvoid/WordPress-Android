package com.soundcloud.android.playlists;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.CollapsingToolbarStyleHelper;
import com.soundcloud.android.view.CollapsingToolbarStyleHelperFactory;
import com.soundcloud.android.view.CustomFontTitleToolbar;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewTreeObserver;

import javax.inject.Inject;

class NewPlaylistDetailHeaderScrollHelper extends DefaultSupportFragmentLightCycle<Fragment> implements CollapsingToolbarStyleHelper.PositionProvider, AppBarLayout.OnOffsetChangedListener {

    public static final float SCRIM_ANIMATE_START = .2f;
    @Nullable @BindView(R.id.top_gradient) View topGradient;
    @Nullable @BindView(R.id.header_scrim) View scrim;

    @Nullable @BindView(R.id.appbar) AppBarLayout appBarLayout;
    @Nullable @BindView(R.id.toolbar_id) CustomFontTitleToolbar toolbar;

    @BindView(R.id.str_layout) MultiSwipeRefreshLayout swipeRefreshLayout;

    private final CollapsingToolbarStyleHelperFactory helperFactory;
    private int changeStatusPosition;

    private boolean atTop = true;
    private boolean isEditing;

    @Inject
    NewPlaylistDetailHeaderScrollHelper(CollapsingToolbarStyleHelperFactory helperFactory) {
        this.helperFactory = helperFactory;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.bind(this, view);
        setupCollapsingToolbar(view);
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && toolbar != null){
            addMeasurementsListenerCompat(fragment, toolbar);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void addMeasurementsListenerCompat(final Fragment fragment, final CustomFontTitleToolbar toolbar) {
        toolbar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                toolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                doMeasurements(fragment);
            }
        });
    }

    private void doMeasurements(Fragment fragment) {
        AppBarLayout appBarLayout = (AppBarLayout) fragment.getView().findViewById(R.id.appbar);
        if (appBarLayout != null) {
            // this should make the status colors change when the scrim turns white
            changeStatusPosition = (int) (-appBarLayout.getTotalScrollRange() * (1 - SCRIM_ANIMATE_START));
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        atTop = verticalOffset == 0;
        configureSwipeToRefresh();
    }

    public void setIsEditing(boolean isEditing){
        this.isEditing = isEditing;
        configureSwipeToRefresh();
    }

    private void configureSwipeToRefresh() {
        boolean shouldBeEnabled = atTop && !isEditing;

        // do not remove this check. Setting the same value twice is not a no-op and causes animation problems
        if (swipeRefreshLayout.isEnabled() != shouldBeEnabled) {
            swipeRefreshLayout.setEnabled(shouldBeEnabled);
        }
    }

    private void setupCollapsingToolbar(View activity) {
        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.collapsing_toolbar);
        if (collapsingToolbarLayout != null) {
            toolbar.setDarkMode();
            appBarLayout.addOnOffsetChangedListener(helperFactory.create(toolbar, scrim, topGradient, this));
            appBarLayout.addOnOffsetChangedListener(this);
        }
    }

    public void setExpanded(boolean expanded) {
        if (appBarLayout != null) {
            appBarLayout.setExpanded(expanded, true);
        }
    }

    @Override
    public int getStatusBarHeight() {
        return 0;
    }

    @Override
    public int changeStatusPosition() {
        return changeStatusPosition;
    }

    @Override
    public int changeToolbarStylePosition() {
        return 0;
    }

    @Override
    public Pair<Float, Float> scrimAnimateBounds() {
        return Pair.of(SCRIM_ANIMATE_START, .5f);
    }

    @Override
    public Pair<Float, Float> toolbarAnimateBounds() {
        return Pair.of(.1f, .3f);
    }

    @Override
    public Pair<Float, Float> toolbarGradientAnimateBounds() {
        return Pair.of(1f, .4f);
    }
}