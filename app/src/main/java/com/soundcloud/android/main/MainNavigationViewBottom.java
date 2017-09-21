package com.soundcloud.android.main;

import butterknife.BindView;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.navigation.NavigationStateController;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.support.design.widget.BottomNavigationView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainNavigationViewBottom extends MainNavigationView {

    @BindView(R.id.bottom_navigation_view) BottomNavigationView bottomNavigationView;

    private final NavigationStateController navigationStateController;

    public MainNavigationViewBottom(EnterScreenDispatcher enterScreenDispatcher,
                                    NavigationModel navigationModel,
                                    EventTracker eventTracker,
                                    IntroductoryOverlayPresenter introductoryOverlayPresenter,
                                    NavigationStateController navigationStateController) {
        super(enterScreenDispatcher, navigationModel, eventTracker, introductoryOverlayPresenter);
        this.navigationStateController = navigationStateController;
    }

    @Override
    protected void onSetupView(MainPagerAdapter pagerAdapter) {
        bottomNavigationView.setOnNavigationItemReselectedListener(item -> pagerAdapter.resetScroll(item.getItemId()));
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemPosition = item.getItemId();
            setNavigationState(itemPosition);
            pager.setCurrentItem(itemPosition);
            return true;
        });

        setupMenuItems(bottomNavigationView, pagerAdapter);

        onSelectItem(pager.getCurrentItem());
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
    protected Optional<View> getMoreTabCustomView() {
        return Optional.absent();
    }
}
