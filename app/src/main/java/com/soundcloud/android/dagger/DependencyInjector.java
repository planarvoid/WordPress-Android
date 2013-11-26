package com.soundcloud.android.dagger;

import dagger.ObjectGraph;

import android.support.v4.app.Fragment;

public interface DependencyInjector {

    void inject(Fragment target);

    ObjectGraph fromAppGraphWithModules(Object... modules);
}
