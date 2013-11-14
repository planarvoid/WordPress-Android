package com.soundcloud.android.dagger;

import com.soundcloud.android.SoundCloudApplication;
import dagger.ObjectGraph;

import android.app.Activity;
import android.support.v4.app.Fragment;

public class DaggerHelper {

    public static void inject(Fragment target) {
        final Fragment parentFragment = target.getParentFragment();
        if (parentFragment != null && parentFragment instanceof ObjectGraphProvider) {
            ((ObjectGraphProvider) parentFragment).getObjectGraph().inject(target);
            return;
        }

        final Activity hostActivity = target.getActivity();
        if (hostActivity != null) {
            if (hostActivity instanceof ObjectGraphProvider) {
                ((ObjectGraphProvider) hostActivity).getObjectGraph().inject(target);
            } else {
                ((ObjectGraphProvider) hostActivity.getApplication()).getObjectGraph().inject(target);
            }
        }
    }

    public static ObjectGraph fromApplicationGraph(Activity activity, Object... modules) {
        return ((SoundCloudApplication) activity.getApplication()).getObjectGraph().plus(modules);
    }

    public static ObjectGraph fromApplicationGraph(Fragment fragment, Object... modules) {
        return fromApplicationGraph(fragment.getActivity(), modules);
    }

}
