package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScrollHelper;
import com.soundcloud.android.view.CustomFontTitleToolbar;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class ProfileScrollHelper
        extends DefaultActivityLightCycle<AppCompatActivity>
        implements ScrollHelper.ScrollScreen {

    private List<ProfileScreen> profileScreens = new ArrayList<>();
    private ScrollHelper scrollHelper;

    private AppBarLayout appBarLayout;
    private View headerView;
    private View contentView;
    private Toolbar toolbar;
    private float elevationTarget;
    private WindowInsetsCompat lastInsets;

    @Inject
    public ProfileScrollHelper() {
    }

    @Override
    public void onCreate(final AppCompatActivity activity, Bundle bundle) {
        scrollHelper = new ScrollHelper(this);
        appBarLayout = (AppBarLayout) activity.findViewById(R.id.appbar);
        headerView = activity.findViewById(R.id.profile_header);
        contentView = activity.findViewById(R.id.pager);
        toolbar = (Toolbar) activity.findViewById(R.id.toolbar_id);
        elevationTarget = activity.getResources().getDimension(R.dimen.toolbar_elevation);

        setupCollapsingToolbar(activity);
    }

    private void setupCollapsingToolbar(AppCompatActivity activity) {
        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.collapsing_toolbar);
        if (collapsingToolbarLayout != null) {

            final CustomFontTitleToolbar toolbar = (CustomFontTitleToolbar) activity.findViewById(R.id.toolbar_id);
            final View scrim = activity.findViewById(R.id.header_scrim);

            listenForWindowInsetChanges();
            listenForOffsetChanges(activity, toolbar, scrim);
        }
    }

    private void listenForOffsetChanges(AppCompatActivity activity,
                                        final CustomFontTitleToolbar toolbar,
                                        final View scrim) {
        ((AppBarLayout) activity.findViewById(R.id.appbar)).addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                final float fullRange = scrim.getHeight() - ProfileScrollHelper.this.toolbar.getHeight() -
                        getTopInset();
                scrim.setAlpha(getCurrentAlpha(verticalOffset, fullRange, .2f, 1f));
                toolbar.setTitleAlpha(getCurrentAlpha(verticalOffset, fullRange, .0f, .3f));
            }
        });
    }

    private int getTopInset() {
        return lastInsets == null ? 0 : lastInsets.getSystemWindowInsetTop();
    }

    private void listenForWindowInsetChanges() {
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout,
                                                  new OnApplyWindowInsetsListener() {
                                                      @Override
                                                      public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                                                          return setWindowInsets(insets);
                                                      }
                                                  });
    }

    private WindowInsetsCompat setWindowInsets(WindowInsetsCompat insets) {
        if (lastInsets != insets) {
            lastInsets = insets;
            headerView.requestLayout();
        }
        return insets.consumeSystemWindowInsets();
    }

    private float getCurrentAlpha(int verticalOffset, float fullRange, float start, float end) {
        final float currentPosition = (fullRange + verticalOffset);
        final float startPosition = (start * fullRange);
        final float range = (end - start) * fullRange;
        final float endPosition = startPosition + range;
        final float adjustedPosition = Math.min(endPosition, Math.max(currentPosition, startPosition));
        return 1 - (adjustedPosition - startPosition) / range;
    }

    @Override
    public void onStart(final AppCompatActivity activity) {
        scrollHelper.attach();
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        scrollHelper.detach();
        profileScreens.clear();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        scrollHelper = null;
    }

    public void addProfileCollection(ProfileScreen profileScreen) {
        profileScreens.add(profileScreen);
    }

    public void removeProfileScreen(ProfileScreen profileScreen) {
        profileScreens.remove(profileScreen);
    }

    @Override
    public void setEmptyViewHeight(int height) {
        for (ProfileScreen profileScreen : profileScreens) {
            profileScreen.setEmptyViewHeight(height);
        }
    }

    @Override
    public void setSwipeToRefreshEnabled(boolean enabled) {
        for (ProfileScreen profileScreen : profileScreens) {
            profileScreen.setSwipeToRefreshEnabled(enabled);
        }
    }

    @Override
    public AppBarLayout getAppBarLayout() {
        return appBarLayout;
    }

    @Override
    public View getHeaderView() {
        return headerView;
    }

    @Override
    public View getContentView() {
        return contentView;
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    public float getElevationTarget() {
        return elevationTarget;
    }
}
