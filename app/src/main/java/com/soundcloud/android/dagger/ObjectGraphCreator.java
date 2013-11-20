package com.soundcloud.android.dagger;

import dagger.ObjectGraph;

public interface ObjectGraphCreator {
    public ObjectGraph fromAppGraphWithModules(Object... modules);


}
