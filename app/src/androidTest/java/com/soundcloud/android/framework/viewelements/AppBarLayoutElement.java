package com.soundcloud.android.framework.viewelements;

import com.soundcloud.android.framework.Han;

import android.support.design.widget.AppBarLayout;
import android.view.View;

public class AppBarLayoutElement {
    private final AppBarLayout appBarLayout;
    private Han driver;

    AppBarLayoutElement(View view, Han driver) {
        appBarLayout = (AppBarLayout) view;
        this.driver = driver;
    }

    public void collapse() {
        driver.collapseAppBarLayout(appBarLayout);
    }
}
