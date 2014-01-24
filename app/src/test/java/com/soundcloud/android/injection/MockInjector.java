package com.soundcloud.android.injection;

import com.google.common.collect.Lists;
import com.soundcloud.android.dagger.DependencyInjector;
import dagger.ObjectGraph;

import java.util.Collections;
import java.util.List;

public class MockInjector implements DependencyInjector{

    private ObjectGraph objectGraph;

    public MockInjector(Object... modules){
        List<Object> graphModules = (modules == null || modules.length == 0) ?
                Collections.emptyList() : Lists.newArrayList(modules);
        objectGraph = ObjectGraph.create(modules);
    }

    @Override
    public ObjectGraph fromAppGraphWithModules(Object... modules) {
        return objectGraph.plus(modules);
    }

    public static ObjectGraph create(Object... modules){
        return new MockInjector().fromAppGraphWithModules(modules);
    }

    public static MockInjector createInjector(Object... modules){
        return new MockInjector(modules);
    }
}
