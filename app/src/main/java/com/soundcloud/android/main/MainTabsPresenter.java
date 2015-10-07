package com.soundcloud.android.main;

import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class MainTabsPresenter extends NavigationPresenter {

    private final BaseLayoutHelper layoutHelper;

    @Inject
    MainTabsPresenter(BaseLayoutHelper layoutHelper) {
        this.layoutHelper = layoutHelper;
    }

    @Override
    public void setBaseLayout(AppCompatActivity activity) {
        layoutHelper.setBaseTabsLayout(activity);
    }

    @Override
    public void trackScreen() {
        // TODO: Track screens based on current tab selection
    }

}
