package com.soundcloud.android.dagger;

import android.support.v4.app.Fragment;

public interface DependencyInjector {

    public void inject(Fragment target);
}
