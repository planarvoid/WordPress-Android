package com.soundcloud.android.playlists;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.CollapsingToolbarStyleHelper;
import com.soundcloud.android.view.CollapsingToolbarStyleHelperFactory;
import com.soundcloud.android.view.CustomFontTitleToolbar;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class NewPlaylistDetailHeaderScrollHelper extends DefaultSupportFragmentLightCycle<Fragment> implements CollapsingToolbarStyleHelper.PositionProvider, AppBarLayout.OnOffsetChangedListener {

    @Nullable @BindView(R.id.top_gradient) View topGradient;
    @Nullable @BindView(R.id.header_scrim) View scrim;

    @BindView(R.id.appbar) AppBarLayout appBarLayout;
    @BindView(R.id.toolbar_id) CustomFontTitleToolbar toolbar;

    private final CollapsingToolbarStyleHelperFactory helperFactory;
    private boolean collapsed;

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
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        collapsed =  Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange();
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
        appBarLayout.setExpanded(expanded, true);
    }

    @Override
    public int getStatusBarHeight() {
        return 0;
    }

    @Override
    public int changeStatusPosition() {
        return 0;
    }

    @Override
    public int changeToolbarStylePosition() {
        return 0;
    }

    @Override
    public Pair<Float, Float> scrimAnimateBounds() {
        return Pair.of(.2f, .5f);
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