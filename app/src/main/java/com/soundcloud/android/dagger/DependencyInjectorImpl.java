package com.soundcloud.android.dagger;

import android.app.Activity;
import android.support.v4.app.Fragment;

public class DependencyInjectorImpl implements DependencyInjector{
    @Override
    public void inject(Fragment target) {
        ObjectGraphProvider objectGraphProvider = getObjectGraphProvider(target);
        if (objectGraphProvider == null){
            throw new IllegalArgumentException("No ObjectGraph provider found in target ancestry");
        } else {
            objectGraphProvider.getObjectGraph().inject(target);
        }
    }

    private ObjectGraphProvider getObjectGraphProvider(Fragment target){
        final Activity hostActivity = target.getActivity();
        if (hostActivity != null) {
            if (hostActivity instanceof ObjectGraphProvider) {
                return ((ObjectGraphProvider) hostActivity);
            } else {
                return ((ObjectGraphProvider) hostActivity.getApplication());
            }
        }
        return null;
    }
}
