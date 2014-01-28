package com.soundcloud.android.dagger;

import dagger.ObjectGraph;

public interface DependencyInjector {
    ObjectGraph fromAppGraphWithModules(Object... modules);
}
