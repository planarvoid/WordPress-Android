package com.soundcloud.android.view;


import com.soundcloud.android.lightcycle.DefaultFragmentLightCycle;

import android.view.View;

public abstract class HeaderViewController extends DefaultFragmentLightCycle {
    public abstract View getHeaderView();
}
