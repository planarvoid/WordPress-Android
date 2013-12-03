package com.soundcloud.android.dagger;

import com.soundcloud.android.SoundCloudApplication;
import dagger.ObjectGraph;

import android.app.Activity;
import android.support.v4.app.Fragment;

public class DaggerDependencyInjector implements DependencyInjector{
    @Override
    public void inject(Fragment target) {
        ObjectGraphProvider objectGraphProvider = getObjectGraphProvider(target);
        if (objectGraphProvider == null){
            throw new IllegalArgumentException("No ObjectGraph provider found in target ancestry");
        } else {
            objectGraphProvider.getObjectGraph().inject(target);
        }
    }

    @Override
    public ObjectGraph fromAppGraphWithModules(Object... modules) {
        return SoundCloudApplication.instance.getObjectGraph().plus(modules);
    }

    private ObjectGraphProvider getObjectGraphProvider(Fragment target) {
        final Fragment parentFragment = target.getParentFragment();
        // we found that getActivity on a child fragment can actually return the previous activity it was attached to
        // so always get the host activity from the parent fragment
        final Activity hostActivity = parentFragment != null ? parentFragment.getActivity() : target.getActivity();
        if (hostActivity == null) {
            throw new IllegalStateException("Fragment requested to be injected, host activity is not a object graph provider : " + target.getClass().getSimpleName());
        }
        if (hostActivity instanceof ObjectGraphProvider) {
            return ((ObjectGraphProvider) hostActivity);
        } else {
            return ((ObjectGraphProvider) hostActivity.getApplication());
        }
    }
}
