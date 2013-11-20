package com.soundcloud.android.dagger;

import com.soundcloud.android.SoundCloudApplication;
import dagger.ObjectGraph;

public class ObjectGraphCreatorImpl implements ObjectGraphCreator {
    @Override
    public ObjectGraph fromAppGraphWithModules(Object... modules) {
        return SoundCloudApplication.instance.getObjectGraph().plus(modules);
    }
}
