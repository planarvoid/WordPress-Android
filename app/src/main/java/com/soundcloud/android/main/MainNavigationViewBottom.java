package com.soundcloud.android.main;

import butterknife.BindView;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.support.design.widget.BottomNavigationView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainNavigationViewBottom extends MainNavigationView {

    @BindView(R.id.bottom_navigation_view) BottomNavigationView bottomNavigationView;

    private final NavigationModel navigationModel;

    public MainNavigationViewBottom(EnterScreenDispatcher enterScreenDispatcher,
                                    NavigationModel navigationModel,
                                    EventTracker eventTracker,
                                    IntroductoryOverlayPresenter introductoryOverlayPresenter) {
        super(enterScreenDispatcher, navigationModel, eventTracker, introductoryOverlayPresenter);
        this.navigationModel = navigationModel;
    }

    @Override
    protected void onSetupView(MainPagerAdapter pagerAdapter) {
        bottomNavigationView.setOnNavigationItemReselectedListener(item -> pagerAdapter.resetScroll(item.getItemId()));
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            pager.setCurrentItem(item.getItemId());
            return true;
        });

        setupMenuItems(bottomNavigationView, pagerAdapter);

        onSelectItem(pager.getCurrentItem());
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
        bottomNavigationView.setSelectedItemId(position);
    }

    @Override
    protected Optional<View> getMoreTabCustomView() {
        return Optional.absent();
    }
}
