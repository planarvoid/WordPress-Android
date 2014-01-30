package com.soundcloud.android.injection;

import com.soundcloud.android.dagger.DependencyInjector;
import dagger.ObjectGraph;

public class MockInjector implements DependencyInjector {

    private ObjectGraph objectGraph;

    public MockInjector(Object... modules) {
        objectGraph = ObjectGraph.create(modules);
    }

    @Override
    public ObjectGraph fromAppGraphWithModules(Object... modules) {
        return objectGraph.plus(modules);
    }

    public static ObjectGraph create(Object... modules) {
        return new MockInjector().fromAppGraphWithModules(modules);
    }

    public static MockInjector createInjector(Object... modules) {
        return new MockInjector(modules);
    }
}
