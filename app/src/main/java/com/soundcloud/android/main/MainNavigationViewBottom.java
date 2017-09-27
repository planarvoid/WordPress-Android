package com.soundcloud.android.main;

import butterknife.BindView;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.navigation.NavigationStateController;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Px;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainNavigationViewBottom extends MainNavigationView {

    private static final String EXTRA_BOTTOM_NAV_OFFSET = "bottomnav_offset";

    @BindView(R.id.bottom_navigation_view) BottomNavigationView bottomNavigationView;
    @Px private int bottomNavHeightPx;
    private float bottomNavOffset;

    private final EnterScreenDispatcher enterScreenDispatcher;
    private final NavigationStateController navigationStateController;

    public MainNavigationViewBottom(EnterScreenDispatcher enterScreenDispatcher,
                                    NavigationModel navigationModel,
                                    EventTracker eventTracker,
                                    IntroductoryOverlayPresenter introductoryOverlayPresenter,
                                    NavigationStateController navigationStateController) {
        super(enterScreenDispatcher, navigationModel, eventTracker, introductoryOverlayPresenter);
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.navigationStateController = navigationStateController;
    }

    @Override
    protected void onSetupView(RootActivity activity, Bundle savedInstanceState, MainPagerAdapter pagerAdapter) {
        bottomNavHeightPx = bottomNavigationView.getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);

        if (savedInstanceState != null) {
            onPlayerSlide(savedInstanceState.getFloat(EXTRA_BOTTOM_NAV_OFFSET));
        }

        bottomNavigationView.setOnNavigationItemReselectedListener(item -> pagerAdapter.resetScroll(item.getItemId()));
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemPosition = item.getItemId();
            setNavigationState(itemPosition);
            enterScreenDispatcher.onPageSelected(itemPosition);
            showFragment(activity, navigationModel.getItem(itemPosition).createFragment());
            return true;
        });

        setupMenuItems(bottomNavigationView, pagerAdapter);
        showFragment(activity, navigationModel.getItem(0).createFragment());
    }

    private void showFragment(RootActivity activity, Fragment fragment) {
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out);
        transaction.replace(R.id.main_container, fragment);
        transaction.commit();
        appBarLayout.setExpanded(true, true);
    }

    private void setNavigationState(int position) {
        navigationStateController.setState(navigationModel.getItem(position).getScreen());
    }

    private void setupMenuItems(BottomNavigationView bottomNavigationView, MainPagerAdapter pagerAdapter) {
        int tabCount = pagerAdapter.getCount();
        Menu menu = bottomNavigationView.getMenu();
        menu.clear();

        final Context context = bottomNavigationView.getContext();

        for (int pageIndex = 0; pageIndex < tabCount; pageIndex++) {
            NavigationModel.Target target = navigationModel.getItem(pageIndex);
            MenuItem add = menu.add(0, pageIndex, pageIndex, context.getString(target.getName()));
            add.setIcon(target.getIcon());
        }
    }

    @Override
    protected void onSelectItem(int position) {
        setNavigationState(position);
        bottomNavigationView.setSelectedItemId(position);
    }

    @Override
    protected NavigationModel.Target currentTargetItem() {
        return navigationModel.getItem(bottomNavigationView.getSelectedItemId());
    }

    @Override
    void hideToolbar() {
        //no op
    }

    @Override
    void showToolbar() {
        //no op
    }

    @Override
    void onPlayerSlide(float slideOffset) {
        bottomNavOffset = slideOffset;
        bottomNavigationView.setTranslationY(bottomNavHeightPx * slideOffset);
    }

    @Override
    protected Optional<View> getMoreTabCustomView() {
        return Optional.absent();
    }

    @Override
    public void onSaveInstanceState(RootActivity host, Bundle bundle) {
        super.onSaveInstanceState(host, bundle);
        bundle.putFloat(EXTRA_BOTTOM_NAV_OFFSET, bottomNavOffset);
    }
}
