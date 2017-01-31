package com.soundcloud.android.profile;

import static android.os.Build.VERSION_CODES.M;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.CollapsingToolbarStyleHelper;
import com.soundcloud.android.view.CollapsingToolbarStyleHelperFactory;
import com.soundcloud.android.view.status.StatusBarUtils;
import com.soundcloud.java.collections.Pair;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;

import javax.inject.Inject;

@TargetApi(M)
class BannerProfileScrollHelper
        extends ProfileScrollHelper implements CollapsingToolbarStyleHelper.PositionProvider {

    @Nullable @BindView(R.id.top_gradient) View topGradient;
    @Nullable @BindView(R.id.header_scrim) View scrim;

    private int statusBarHeight;
    private int changeArrowPosition;
    private int changeStatusPosition;
    private CollapsingToolbarStyleHelperFactory helperFactory;

    @Inject
    BannerProfileScrollHelper(CollapsingToolbarStyleHelperFactory helperFactory) {
        this.helperFactory = helperFactory;
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

    @Override
    public int getStatusBarHeight() {
        return statusBarHeight;
    }

    @Override
    public int changeStatusPosition() {
        return changeStatusPosition;
    }

    @Override
    public int changeToolbarStylePosition() {
        return changeArrowPosition;
    }

    @Override
    public Pair<Float, Float> scrimAnimateBounds() {
        return Pair.of(.2f, 1f);
    }

    @Override
    public Pair<Float, Float> toolbarAnimateBounds() {
        return Pair.of(.1f, .3f);
    }

    @Override
    public Pair<Float, Float> toolbarGradientAnimateBounds() {
        return Pair.of(1f, .7f);
    }

    private void doMeasurements(AppCompatActivity activity) {
        View banner = activity.findViewById(R.id.profile_banner);
        if (banner != null) {
            int bannerHeight = banner.getHeight();
            statusBarHeight = StatusBarUtils.getStatusBarHeight(activity);
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
        appBarLayout.addOnOffsetChangedListener(getOnOffsetChangedListener());
    }

    @NonNull
    private CollapsingToolbarStyleHelper getOnOffsetChangedListener() {
        return helperFactory.create(toolbar, scrim, topGradient, this);
    }


}
