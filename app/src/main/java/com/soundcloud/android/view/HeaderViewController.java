package com.soundcloud.android.view;

import com.soundcloud.android.main.DefaultFragmentLifeCycle;

import android.support.v4.app.Fragment;
import android.view.View;

public abstract class HeaderViewController extends DefaultFragmentLifeCycle<Fragment> {
    public abstract View getHeaderView();
}
